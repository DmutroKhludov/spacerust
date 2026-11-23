package net.spacerust.client.world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Lightweight Wavefront .obj.gz loader for CoilRust 3D models (ore nodes, crates, held weapons & tools).
 * Supports both single-batch meshes (loadGz) and multi-material meshes (loadGzMulti).
 */
public class RustObjModel {

    public static class Part {
        public final String material;
        public final float[] vertices; // stride = 8: px,py,pz, u,v, nx,ny,nz
        public final int vertexCount;

        public Part(String material, float[] vertices) {
            this.material = material;
            this.vertices = vertices;
            this.vertexCount = vertices.length / 8;
        }
    }

    public static class MultiMesh {
        public final List<Part> parts;
        public final float cx, cy, cz;
        public final float spanX, spanY, spanZ;

        public MultiMesh(List<Part> parts, float cx, float cy, float cz, float spanX, float spanY, float spanZ) {
            this.parts = parts;
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
            this.spanX = spanX;
            this.spanY = spanY;
            this.spanZ = spanZ;
        }
    }

    public final float[] vertices; // stride = 8 floats per vertex
    public final int vertexCount;

    private RustObjModel(float[] vertices) {
        this.vertices = vertices;
        this.vertexCount = vertices.length / 8;
    }

    public static RustObjModel loadGz(Identifier id) {
        MultiMesh mesh = loadGzMulti(id);
        if (mesh == null || mesh.parts.isEmpty()) return null;
        int total = 0;
        for (Part p : mesh.parts) total += p.vertices.length;
        float[] merged = new float[total];
        int off = 0;
        for (Part p : mesh.parts) {
            System.arraycopy(p.vertices, 0, merged, off, p.vertices.length);
            off += p.vertices.length;
        }
        return new RustObjModel(merged);
    }

    public static MultiMesh loadGzMulti(Identifier id) {
        try {
            Resource res = MinecraftClient.getInstance().getResourceManager().getResource(id);
            List<float[]> pos = new ArrayList<>();
            List<float[]> uvs = new ArrayList<>();
            List<float[]> nrm = new ArrayList<>();
            Map<String, List<Float>> batches = new LinkedHashMap<>();
            String currentMat = "default";

            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new GZIPInputStream(res.getInputStream()), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("v ")) {
                        String[] p = line.split("\\s+");
                        if (p.length >= 4) {
                            float x = Float.parseFloat(p[1]);
                            float y = Float.parseFloat(p[2]);
                            float z = Float.parseFloat(p[3]);
                            pos.add(new float[]{x, y, z});
                            if (x < minX) minX = x; if (x > maxX) maxX = x;
                            if (y < minY) minY = y; if (y > maxY) maxY = y;
                            if (z < minZ) minZ = z; if (z > maxZ) maxZ = z;
                        }
                    } else if (line.startsWith("vt ")) {
                        String[] p = line.split("\\s+");
                        if (p.length >= 3) {
                            uvs.add(new float[]{
                                    Float.parseFloat(p[1]),
                                    1.0f - Float.parseFloat(p[2])
                            });
                        }
                    } else if (line.startsWith("vn ")) {
                        String[] p = line.split("\\s+");
                        if (p.length >= 4) {
                            nrm.add(new float[]{
                                    Float.parseFloat(p[1]),
                                    Float.parseFloat(p[2]),
                                    Float.parseFloat(p[3])
                            });
                        }
                    } else if (line.startsWith("usemtl ")) {
                        currentMat = line.substring(7).trim();
                    } else if (line.startsWith("f ")) {
                        String[] p = line.split("\\s+");
                        if (p.length >= 4) {
                            List<Float> out = batches.computeIfAbsent(currentMat, k -> new ArrayList<>());
                            for (int i = 2; i < p.length - 1; i++) {
                                emitVertex(p[1], pos, uvs, nrm, out);
                                emitVertex(p[i], pos, uvs, nrm, out);
                                emitVertex(p[i + 1], pos, uvs, nrm, out);
                            }
                        }
                    }
                }
            }

            List<Part> parts = new ArrayList<>(batches.size());
            for (Map.Entry<String, List<Float>> e : batches.entrySet()) {
                List<Float> list = e.getValue();
                if (list.isEmpty()) continue;
                float[] arr = new float[list.size()];
                for (int i = 0; i < arr.length; i++) arr[i] = list.get(i);
                parts.add(new Part(e.getKey(), arr));
            }
            if (pos.isEmpty()) {
                minX = maxX = minY = maxY = minZ = maxZ = 0f;
            }
            return new MultiMesh(
                    parts,
                    (minX + maxX) * 0.5f,
                    (minY + maxY) * 0.5f,
                    (minZ + maxZ) * 0.5f,
                    maxX - minX,
                    maxY - minY,
                    maxZ - minZ
            );
        } catch (Throwable t) {
            System.err.println("[SpaceRust] Failed to load OBJ model " + id + ": " + t.getMessage());
            return null;
        }
    }

    private static void emitVertex(String tok, List<float[]> pos, List<float[]> uvs, List<float[]> nrm, List<Float> out) {
        String[] s = tok.split("/");
        int vi = parseIdx(s, 0, pos.size());
        int ti = parseIdx(s, 1, uvs.size());
        int ni = parseIdx(s, 2, nrm.size());

        float[] p = (vi >= 0 && vi < pos.size()) ? pos.get(vi) : new float[]{0, 0, 0};
        float[] t = (ti >= 0 && ti < uvs.size()) ? uvs.get(ti) : new float[]{0, 0};
        float[] n = (ni >= 0 && ni < nrm.size()) ? nrm.get(ni) : new float[]{0, 1, 0};

        out.add(p[0]); out.add(p[1]); out.add(p[2]);
        out.add(t[0]); out.add(t[1]);
        out.add(n[0]); out.add(n[1]); out.add(n[2]);
    }

    private static int parseIdx(String[] parts, int idx, int len) {
        if (idx >= parts.length || parts[idx].isEmpty()) return -1;
        int raw = Integer.parseInt(parts[idx]);
        return raw > 0 ? raw - 1 : len + raw;
    }
}
