package net.spacerust.client.building;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1:1 Rust Binary Mesh (RBM1, RBM2 and transposed RBC1) loader & renderer.
 * Loads 3D Rust models extracted from Rust in Minecraft by Creka.
 */
public class RbmModel {

    public static class Submesh {
        public final Identifier texture;
        public final float[] vertices; // 8 floats per vertex: x, y, z, u, v, nx, ny, nz
        public final int vertexCount;

        public Submesh(Identifier texture, float[] vertices) {
            this.texture = texture;
            this.vertices = vertices;
            this.vertexCount = vertices.length / 8;
        }
    }

    public final String name;
    public final List<Submesh> submeshes = new ArrayList<>();
    private static final Map<String, RbmModel> CACHE = new HashMap<>();

    public RbmModel(String name) {
        this.name = name;
    }

    public static synchronized RbmModel get(String name) {
        if (!CACHE.containsKey(name)) {
            RbmModel model = load(name);
            CACHE.put(name, model);
        }
        return CACHE.get(name);
    }

    public static RbmModel load(String name) {
        String path = "/assets/spacerust/models/building/" + name + ".rbm";
        try (InputStream in = RbmModel.class.getResourceAsStream(path)) {
            if (in == null) {
                System.err.println("[SpaceRust] Model not found on classpath: " + path);
                return null;
            }
            return parse(name, in);
        } catch (Exception e) {
            System.err.println("[SpaceRust] Error loading RBM model: " + name);
            e.printStackTrace();
            return null;
        }
    }

    public static RbmModel parse(String name, InputStream rawStream) throws Exception {
        byte[] bytes = readAllBytes(rawStream);
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
        int magic = dis.readInt();

        // RBC1 (0x52424331) transposed byte-plane compression
        if (magic == 0x52424331) {
            int length = dis.readInt();
            byte[] payload = new byte[length];
            dis.readFully(payload);

            byte[] dst = new byte[length];
            int srcIdx = 0;
            for (int mod = 0; mod < 4; mod++) {
                for (int dstIdx = mod; dstIdx < length; dstIdx += 4) {
                    dst[dstIdx] = payload[srcIdx++];
                }
            }
            dis = new DataInputStream(new ByteArrayInputStream(dst));
            magic = dis.readInt();
        }

        if (magic != 0x52424D31 && magic != 0x52424D32) {
            throw new IllegalArgumentException("Unsupported RBM magic: 0x" + Integer.toHexString(magic));
        }

        int submeshCount = dis.readInt();
        RbmModel model = new RbmModel(name);

        for (int s = 0; s < submeshCount; s++) {
            String texName = dis.readUTF();
            Identifier tex = new Identifier("spacerust", "textures/building/" + texName + ".png");
            int vCount = dis.readInt();
            float[] rawVerts = new float[vCount * 8];
            for (int i = 0; i < rawVerts.length; i++) {
                rawVerts[i] = dis.readFloat();
            }

            float[] finalVerts;
            if (magic == 0x52424D32) {
                int iCount = dis.readInt();
                finalVerts = new float[iCount * 8];
                for (int i = 0; i < iCount; i++) {
                    int idx = dis.readInt();
                    System.arraycopy(rawVerts, idx * 8, finalVerts, i * 8, 8);
                }
            } else {
                finalVerts = rawVerts;
            }

            model.submeshes.add(new Submesh(tex, finalVerts));
        }

        return model;
    }

    private static byte[] readAllBytes(InputStream is) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[8192];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay, float r, float g, float b, float a) {
        Matrix4f posMat = matrices.peek().getModel();
        Matrix3f normMat = matrices.peek().getNormal();

        for (Submesh sub : submeshes) {
            RenderLayer layer = (a < 0.99f) ? RenderLayer.getEntityTranslucent(sub.texture) : RenderLayer.getEntityCutout(sub.texture);
            VertexConsumer vc = consumers.getBuffer(layer);
            float[] v = sub.vertices;
            int count = sub.vertexCount;
            for (int i = 0; i < count; i++) {
                int off = i * 8;
                float x = v[off];
                float y = v[off + 1];
                float z = v[off + 2];
                float u = v[off + 3];
                float wv = v[off + 4];
                float nx = v[off + 5];
                float ny = v[off + 6];
                float nz = v[off + 7];

                vc.vertex(posMat, x, y, z)
                  .color(r, g, b, a)
                  .texture(u, wv)
                  .overlay(overlay)
                  .light(light)
                  .normal(normMat, nx, ny, nz)
                  .next();
            }
        }
    }
}
