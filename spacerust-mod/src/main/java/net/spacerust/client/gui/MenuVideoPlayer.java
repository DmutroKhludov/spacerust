package net.spacerust.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.opengl.GL11;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Decodes and renders looping MP4 background videos from menu_videos/
 * using triple-buffered direct ByteBuffers and double-buffered OpenGL RGB textures
 * for sharp 1600x900 playback without stalling the Minecraft render thread.
 */
public class MenuVideoPlayer {

    private static final int VIDEO_W = 1600;
    private static final int VIDEO_H = 900;
    private static final int FRAME_BYTES = VIDEO_W * VIDEO_H * 3; // RGB24 (1600*3 = 4800, 4-byte aligned)
    private static final long FRAME_DURATION_NS = 33_333_333L; // 30 FPS

    private static final MenuVideoPlayer INSTANCE = new MenuVideoPlayer();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> INSTANCE.stop(), "SpaceRust-VideoShutdown"));
    }

    public static MenuVideoPlayer getInstance() {
        return INSTANCE;
    }

    private final Object frameLock = new Object();
    private ByteBuffer decodeBuffer = ByteBuffer.allocateDirect(FRAME_BYTES).order(ByteOrder.nativeOrder());
    private ByteBuffer readyBuffer = ByteBuffer.allocateDirect(FRAME_BYTES).order(ByteOrder.nativeOrder());
    private ByteBuffer uploadBuffer = ByteBuffer.allocateDirect(FRAME_BYTES).order(ByteOrder.nativeOrder());

    private volatile boolean running = false;
    private volatile boolean hasNewFrame = false;
    private volatile boolean hasAnyFrame = false;
    private volatile boolean initializedLookup = false;
    private volatile boolean loggedFirstFrame = false;
    private volatile String currentVideoName = "";

    private Thread decodeThread;
    private volatile Process activeProcess;
    private final int[] glTexIds = new int[]{0, 0};
    private int activeTexIdx = 0;

    private File videoDir;
    private File lastVideoMarkerFile;
    private String ffmpegExecutable;
    private final List<File> playlist = new ArrayList<>();
    private final Random rng = new Random(System.nanoTime() ^ System.currentTimeMillis());

    private MenuVideoPlayer() {
    }

    public synchronized void ensureStarted() {
        if (running) {
            return;
        }
        if (!initializedLookup) {
            locateAssets();
            initializedLookup = true;
        }
        if (playlist.isEmpty() || ffmpegExecutable == null) {
            return;
        }

        prepareFreshStartVideo();
        hasNewFrame = false;
        hasAnyFrame = false;
        loggedFirstFrame = false;
        running = true;
        decodeThread = new Thread(this::decodeLoop, "SpaceRust-MenuVideoDecoder");
        decodeThread.setDaemon(true);
        decodeThread.start();
    }

    private void prepareFreshStartVideo() {
        if (playlist.size() <= 1) return;
        String prev = currentVideoName;
        if ((prev == null || prev.isEmpty()) && lastVideoMarkerFile != null && lastVideoMarkerFile.isFile()) {
            try {
                prev = new String(Files.readAllBytes(lastVideoMarkerFile.toPath()), StandardCharsets.UTF_8).trim();
            } catch (Exception ignored) {
            }
        }
        Collections.shuffle(playlist, rng);
        if (prev != null && !prev.isEmpty() && playlist.get(0).getName().equals(prev)) {
            int swapIdx = 1 + rng.nextInt(playlist.size() - 1);
            Collections.swap(playlist, 0, swapIdx);
        }
    }

    private void saveLastVideoName(String name) {
        if (lastVideoMarkerFile == null || name == null || name.isEmpty()) return;
        try {
            Files.write(lastVideoMarkerFile.toPath(), name.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    public synchronized void stop() {
        running = false;
        hasNewFrame = false;
        hasAnyFrame = false;
        Process proc = activeProcess;
        activeProcess = null;
        if (proc != null) {
            try {
                proc.destroyForcibly();
            } catch (Exception ignored) {
            }
        }
        if (decodeThread != null) {
            decodeThread.interrupt();
            decodeThread = null;
        }
    }

    public synchronized void releaseGlTexture() {
        for (int i = 0; i < 2; i++) {
            if (glTexIds[i] != 0) {
                GlStateManager.deleteTexture(glTexIds[i]);
                glTexIds[i] = 0;
            }
        }
        hasAnyFrame = false;
        hasNewFrame = false;
    }

    public String getCurrentVideoName() {
        return currentVideoName;
    }

    private void locateAssets() {
        playlist.clear();
        videoDir = null;
        ffmpegExecutable = null;

        List<File> dirCandidates = new ArrayList<>();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.runDirectory != null) {
            dirCandidates.add(new File(client.runDirectory, "menu_videos"));
        }
        dirCandidates.add(new File("menu_videos"));
        dirCandidates.add(new File("D:\\Games\\SpaceRust\\menu_videos"));
        dirCandidates.add(new File("d:\\Спейсраст\\menu_videos"));
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isEmpty()) {
            dirCandidates.add(new File(appData, ".spacerust\\menu_videos"));
        }

        for (File dir : dirCandidates) {
            if (dir.isDirectory()) {
                File[] mp4s = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".mp4"));
                if (mp4s != null && mp4s.length > 0) {
                    videoDir = dir;
                    lastVideoMarkerFile = new File(dir, "last_video.txt");
                    Collections.addAll(playlist, mp4s);
                    Collections.shuffle(playlist, rng);
                    break;
                }
            }
        }

        if (playlist.isEmpty()) {
            System.out.println("[SpaceRust] No MP4 menu videos found in menu_videos/");
            return;
        }

        List<File> ffmpegCandidates = new ArrayList<>();
        if (videoDir != null) {
            ffmpegCandidates.add(new File(videoDir, "ffmpeg.exe"));
            ffmpegCandidates.add(new File(videoDir, "ffmpeg"));
        }
        if (client != null && client.runDirectory != null) {
            ffmpegCandidates.add(new File(client.runDirectory, "menu_videos\\ffmpeg.exe"));
            ffmpegCandidates.add(new File(client.runDirectory, "runtime\\ffmpeg.exe"));
        }
        ffmpegCandidates.add(new File("D:\\Games\\SpaceRust\\menu_videos\\ffmpeg.exe"));
        ffmpegCandidates.add(new File("d:\\Спейсраст\\menu_videos\\ffmpeg.exe"));

        for (File cand : ffmpegCandidates) {
            if (cand.isFile()) {
                ffmpegExecutable = cand.getAbsolutePath();
                break;
            }
        }

        if (ffmpegExecutable == null && testFfmpegInPath("ffmpeg")) {
            ffmpegExecutable = "ffmpeg";
        }

        System.out.println("[SpaceRust] Menu videos: " + playlist.size() + " files from " + videoDir + ", ffmpeg=" + ffmpegExecutable);
    }

    private boolean testFfmpegInPath(String cmd) {
        try {
            Process p = new ProcessBuilder(cmd, "-version").start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void decodeLoop() {
        int index = 0;

        while (running && !playlist.isEmpty()) {
            if (index >= playlist.size()) {
                Collections.shuffle(playlist, rng);
                index = 0;
            }
            File videoFile = playlist.get(index++);
            if (!videoFile.isFile()) {
                continue;
            }
            currentVideoName = videoFile.getName();
            saveLastVideoName(currentVideoName);

            Process proc = null;
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        ffmpegExecutable,
                        "-hide_banner",
                        "-loglevel", "error",
                        "-stream_loop", "-1",
                        "-i", videoFile.getAbsolutePath(),
                        "-vf", "scale=" + VIDEO_W + ":" + VIDEO_H + ":flags=bicubic,fps=30",
                        "-f", "rawvideo",
                        "-pix_fmt", "rgb24",
                        "-an", "-sn", "-dn",
                        "-"
                );
                boolean isWin = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
                pb.redirectError(isWin ? new File("NUL") : new File("/dev/null"));

                proc = pb.start();
                activeProcess = proc;

                ReadableByteChannel channel = Channels.newChannel(new BufferedInputStream(proc.getInputStream(), 1024 * 1024));
                long clipStartNs = System.nanoTime();
                long frameIdx = 0;

                while (running) {
                    decodeBuffer.clear();
                    if (!readFullFrame(channel, decodeBuffer)) {
                        break;
                    }
                    decodeBuffer.flip();

                    long targetNs = clipStartNs + frameIdx * FRAME_DURATION_NS;
                    long nowNs = System.nanoTime();
                    long sleepNs = targetNs - nowNs;
                    if (sleepNs > 1_000_000L) {
                        try {
                            Thread.sleep(sleepNs / 1_000_000L, (int) (sleepNs % 1_000_000L));
                        } catch (InterruptedException ie) {
                            break;
                        }
                    } else if (sleepNs < -250_000_000L) {
                        clipStartNs = System.nanoTime() - frameIdx * FRAME_DURATION_NS;
                    }

                    // Pointer swap only (zero byte copying under lock)
                    synchronized (frameLock) {
                        ByteBuffer tmp = readyBuffer;
                        readyBuffer = decodeBuffer;
                        decodeBuffer = tmp;
                        hasNewFrame = true;
                    }
                    frameIdx++;
                }
            } catch (Exception e) {
                if (running) {
                    System.err.println("[SpaceRust] Video decode error (" + videoFile.getName() + "): " + e.getMessage());
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException ignored) {
                        break;
                    }
                }
            } finally {
                if (proc != null) {
                    try {
                        proc.destroyForcibly();
                    } catch (Exception ignored) {
                    }
                    if (activeProcess == proc) {
                        activeProcess = null;
                    }
                }
            }
        }
    }

    private static boolean readFullFrame(ReadableByteChannel channel, ByteBuffer dst) throws IOException {
        while (dst.hasRemaining()) {
            int n = channel.read(dst);
            if (n < 0) {
                return false;
            }
        }
        return true;
    }

    private void ensureTexturesAllocated() {
        for (int i = 0; i < 2; i++) {
            if (glTexIds[i] == 0) {
                glTexIds[i] = GlStateManager.genTextures();
                GlStateManager.bindTexture(glTexIds[i]);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 0x812F);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 0x812F);
                GlStateManager.pixelStore(3314, 0);
                GlStateManager.pixelStore(3315, 0);
                GlStateManager.pixelStore(3316, 0);
                GlStateManager.pixelStore(3317, 4);
                GL11.nglTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB8, VIDEO_W, VIDEO_H, 0,
                        GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, 0L);
            }
        }
    }

    /**
     * Swaps triple-buffered direct buffers, uploads into the idle OpenGL texture,
     * and draws the cover-fitted background video.
     */
    public boolean render(int screenW, int screenH) {
        ensureStarted();
        if (!running && !hasAnyFrame) {
            return false;
        }

        ensureTexturesAllocated();

        if (hasNewFrame) {
            boolean swapped = false;
            synchronized (frameLock) {
                if (hasNewFrame) {
                    ByteBuffer tmp = uploadBuffer;
                    uploadBuffer = readyBuffer;
                    readyBuffer = tmp;
                    hasNewFrame = false;
                    swapped = true;
                }
            }
            if (swapped) {
                int nextTexIdx = 1 - activeTexIdx;
                GlStateManager.bindTexture(glTexIds[nextTexIdx]);
                GlStateManager.pixelStore(3314, 0); // GL_UNPACK_ROW_LENGTH
                GlStateManager.pixelStore(3315, 0); // GL_UNPACK_SKIP_ROWS
                GlStateManager.pixelStore(3316, 0); // GL_UNPACK_SKIP_PIXELS
                GlStateManager.pixelStore(3317, 4); // GL_UNPACK_ALIGNMENT (1600*3=4800 is 4-byte aligned)
                uploadBuffer.rewind();
                GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, VIDEO_W, VIDEO_H,
                        GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, uploadBuffer);
                activeTexIdx = nextTexIdx;
                hasAnyFrame = true;
                if (!loggedFirstFrame) {
                    loggedFirstFrame = true;
                    System.out.println("[SpaceRust] MenuVideoPlayer uploaded first video frame (" + currentVideoName + ")");
                }
            }
        }

        if (!hasAnyFrame) {
            return false;
        }

        float scale = Math.max((float) screenW / (float) VIDEO_W, (float) screenH / (float) VIDEO_H);
        float drawW = VIDEO_W * scale;
        float drawH = VIDEO_H * scale;
        float x0 = (screenW - drawW) * 0.5f;
        float y0 = (screenH - drawH) * 0.5f;
        float x1 = x0 + drawW;
        float y1 = y0 + drawH;

        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.bindTexture(glTexIds[activeTexIdx]);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(x0, y1, 0.0D).texture(0.0f, 1.0f).next();
        buffer.vertex(x1, y1, 0.0D).texture(1.0f, 1.0f).next();
        buffer.vertex(x1, y0, 0.0D).texture(1.0f, 0.0f).next();
        buffer.vertex(x0, y0, 0.0D).texture(0.0f, 0.0f).next();
        tessellator.draw();

        return true;
    }
}
