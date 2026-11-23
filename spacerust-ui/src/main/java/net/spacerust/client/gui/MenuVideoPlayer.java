package net.spacerust.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

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
 * Decodes and renders looping MP4 background videos from menu_videos/ for 1.21.11 DrawContext.
 */
public class MenuVideoPlayer {

    private static final int VIDEO_W = 1280;
    private static final int VIDEO_H = 720;
    private static final int FRAME_BYTES = VIDEO_W * VIDEO_H * 3;
    private static final long FRAME_DURATION_NS = 33_333_333L;
    private static final Identifier VIDEO_TEX_ID = Identifier.of("spacerust", "menu_video_frame");

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
    private final byte[] rowTmp = new byte[VIDEO_W * 3];

    private volatile boolean running = false;
    private volatile boolean hasNewFrame = false;
    private volatile boolean hasAnyFrame = false;
    private volatile boolean initializedLookup = false;
    private volatile boolean loggedFirstFrame = false;
    private volatile String currentVideoName = "";

    private Thread decodeThread;
    private volatile Process activeProcess;
    private NativeImageBackedTexture videoTexture;
    private boolean textureRegistered = false;

    private File videoDir;
    private File lastVideoMarkerFile;
    private String ffmpegExecutable;
    private final List<File> playlist = new ArrayList<>();
    private final Random rng = new Random(System.nanoTime() ^ System.currentTimeMillis());

    private MenuVideoPlayer() {
    }

    public synchronized void ensureStarted() {
        if (running) return;
        if (!initializedLookup) {
            locateAssets();
            initializedLookup = true;
        }
        if (playlist.isEmpty() || ffmpegExecutable == null) return;

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
            if (!videoFile.isFile()) continue;
            currentVideoName = videoFile.getName();
            saveLastVideoName(currentVideoName);

            Process proc = null;
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        ffmpegExecutable,
                        "-hide_banner", "-loglevel", "error",
                        "-stream_loop", "-1",
                        "-i", videoFile.getAbsolutePath(),
                        "-vf", "scale=" + VIDEO_W + ":" + VIDEO_H + ":flags=bicubic,fps=30",
                        "-f", "rawvideo", "-pix_fmt", "rgb24",
                        "-an", "-sn", "-dn", "-"
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
                    if (!readFullFrame(channel, decodeBuffer)) break;
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
                    try { Thread.sleep(300); } catch (InterruptedException ignored) { break; }
                }
            } finally {
                if (proc != null) {
                    try { proc.destroyForcibly(); } catch (Exception ignored) {}
                    if (activeProcess == proc) activeProcess = null;
                }
            }
        }
    }

    private static boolean readFullFrame(ReadableByteChannel channel, ByteBuffer dst) throws IOException {
        while (dst.hasRemaining()) {
            int n = channel.read(dst);
            if (n < 0) return false;
        }
        return true;
    }

    private void ensureTextureRegistered(MinecraftClient client) {
        if (textureRegistered || client == null || client.getTextureManager() == null) return;
        videoTexture = new NativeImageBackedTexture(() -> "spacerust_menu_video", VIDEO_W, VIDEO_H, false);
        client.getTextureManager().registerTexture(VIDEO_TEX_ID, videoTexture);
        textureRegistered = true;
    }

    public boolean render(DrawContext ctx, int screenW, int screenH) {
        ensureStarted();
        if (!running && !hasAnyFrame) return false;

        MinecraftClient client = MinecraftClient.getInstance();
        ensureTextureRegistered(client);
        if (videoTexture == null) return false;

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
                NativeImage ni = videoTexture.getImage();
                if (ni != null) {
                    uploadBuffer.rewind();
                    for (int y = 0; y < VIDEO_H; y++) {
                        uploadBuffer.get(rowTmp);
                        for (int x = 0; x < VIDEO_W; x++) {
                            int idx = x * 3;
                            int r = rowTmp[idx] & 0xFF;
                            int g = rowTmp[idx + 1] & 0xFF;
                            int b = rowTmp[idx + 2] & 0xFF;
                            ni.setColorArgb(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
                        }
                    }
                    videoTexture.upload();
                    hasAnyFrame = true;
                    if (!loggedFirstFrame) {
                        loggedFirstFrame = true;
                    }
                }
            }
        }

        if (!hasAnyFrame) return false;

        float scale = Math.max((float) screenW / (float) VIDEO_W, (float) screenH / (float) VIDEO_H);
        int drawW = Math.round(VIDEO_W * scale);
        int drawH = Math.round(VIDEO_H * scale);
        int x0 = Math.round((screenW - drawW) * 0.5f);
        int y0 = Math.round((screenH - drawH) * 0.5f);

        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, VIDEO_TEX_ID, x0, y0, 0f, 0f, drawW, drawH, drawW, drawH);
        return true;
    }
}
