package net.spacerust.client.voice;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.spacerust.client.SpaceRustClient;
import net.spacerust.client.config.RustSettings;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Native 3D proximity Voice Chat engine using Java Sound (javax.sound.sampled)
 * + IMA ADPCM over "spacerust:voice" plugin messaging.
 */
public final class RustVoiceEngine {

    public static final Identifier VOICE_CHANNEL = new Identifier("spacerust", "voice");
    public static final int SAMPLE_RATE = 16000;
    public static final int FRAME_SAMPLES = 640; // 40ms @ 16kHz
    public static final float MAX_VOICE_DISTANCE = 40.0f;

    private static final AudioFormat CAPTURE_FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    private static final AudioFormat PLAYBACK_FORMAT = new AudioFormat(SAMPLE_RATE, 16, 2, true, false);

    private static final RustVoiceEngine INSTANCE = new RustVoiceEngine();

    public static RustVoiceEngine get() {
        return INSTANCE;
    }

    private static final class SpeakerState {
        final String name;
        double x, y, z;
        long lastHeardMs;
        final ArrayBlockingQueue<short[]> queue = new ArrayBlockingQueue<>(10);

        SpeakerState(String name, double x, double y, double z, long now) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.lastHeardMs = now;
        }
    }

    private final ConcurrentHashMap<UUID, SpeakerState> speakers = new ConcurrentHashMap<>();
    private final ArrayBlockingQueue<short[]> loopbackQueue = new ArrayBlockingQueue<>(10);

    private volatile boolean started = false;
    private volatile boolean selfTalking = false;
    private volatile boolean loopbackTest = false;
    private volatile float liveMicLevel = 0.0f;
    private volatile String activeInputKey = "";
    private volatile String activeOutputKey = "";
    private volatile boolean forceReopenInput = false;
    private volatile boolean forceReopenOutput = false;

    private RustVoiceEngine() {
    }

    public synchronized void register() {
        if (started) return;
        started = true;

        ClientPlayNetworking.registerGlobalReceiver(VOICE_CHANNEL, (client, handler, buf, responseSender) -> {
            byte[] all = new byte[buf.readableBytes()];
            buf.readBytes(all);
            onNetworkPacket(all);
        });

        Thread capThread = new Thread(this::captureLoop, "SpaceRust-VoiceCapture");
        capThread.setDaemon(true);
        capThread.start();

        Thread mixThread = new Thread(this::playbackLoop, "SpaceRust-VoicePlayback");
        mixThread.setDaemon(true);
        mixThread.start();
    }

    public void setLoopbackTest(boolean enabled) {
        this.loopbackTest = enabled;
        if (!enabled) {
            loopbackQueue.clear();
        }
    }

    public boolean isLoopbackTest() {
        return loopbackTest;
    }

    public void invalidateDevices() {
        forceReopenInput = true;
        forceReopenOutput = true;
    }

    public boolean isSelfTalking() {
        return selfTalking;
    }

    public float getLiveMicLevel() {
        return liveMicLevel;
    }

    public List<String> getActiveSpeakerNames() {
        long now = System.currentTimeMillis();
        List<String> list = new ArrayList<>();
        for (SpeakerState st : speakers.values()) {
            if ((now - st.lastHeardMs) < 350L) {
                list.add(st.name);
            }
        }
        return list;
    }

    public static List<String> listInputDevices() {
        Set<String> set = new LinkedHashSet<>();
        set.add(""); // "" represents "По умолчанию"
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, CAPTURE_FORMAT);
        for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
            try {
                Mixer mixer = AudioSystem.getMixer(mi);
                if (mixer.isLineSupported(info)) {
                    set.add(mi.getName());
                }
            } catch (Throwable ignored) {
            }
        }
        return new ArrayList<>(set);
    }

    public static List<String> listOutputDevices() {
        Set<String> set = new LinkedHashSet<>();
        set.add(""); // "" represents "По умолчанию"
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, PLAYBACK_FORMAT);
        for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
            try {
                Mixer mixer = AudioSystem.getMixer(mi);
                if (mixer.isLineSupported(info)) {
                    set.add(mi.getName());
                }
            } catch (Throwable ignored) {
            }
        }
        return new ArrayList<>(set);
    }

    public static String displayDeviceName(String raw) {
        return (raw == null || raw.isEmpty()) ? "По умолчанию" : raw;
    }

    private TargetDataLine openCaptureLine(String mixerName) {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, CAPTURE_FORMAT);
        if (mixerName != null && !mixerName.isEmpty()) {
            for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
                if (mi.getName().equals(mixerName)) {
                    try {
                        Mixer m = AudioSystem.getMixer(mi);
                        if (m.isLineSupported(info)) {
                            TargetDataLine line = (TargetDataLine) m.getLine(info);
                            line.open(CAPTURE_FORMAT, FRAME_SAMPLES * 4);
                            line.start();
                            return line;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        try {
            if (AudioSystem.isLineSupported(info)) {
                TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(CAPTURE_FORMAT, FRAME_SAMPLES * 4);
                line.start();
                return line;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private SourceDataLine openPlaybackLine(String mixerName) {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, PLAYBACK_FORMAT);
        if (mixerName != null && !mixerName.isEmpty()) {
            for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
                if (mi.getName().equals(mixerName)) {
                    try {
                        Mixer m = AudioSystem.getMixer(mi);
                        if (m.isLineSupported(info)) {
                            SourceDataLine line = (SourceDataLine) m.getLine(info);
                            line.open(PLAYBACK_FORMAT, FRAME_SAMPLES * 8);
                            line.start();
                            return line;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        try {
            if (AudioSystem.isLineSupported(info)) {
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(PLAYBACK_FORMAT, FRAME_SAMPLES * 8);
                line.start();
                return line;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void captureLoop() {
        TargetDataLine line = null;
        byte[] raw = new byte[FRAME_SAMPLES * 2];
        short[] samples = new short[FRAME_SAMPLES];
        boolean wasSending = false;

        while (true) {
            try {
                RustSettings cfg = RustSettings.get();
                MinecraftClient client = MinecraftClient.getInstance();
                boolean pttPressed = client != null
                        && client.player != null
                        && client.world != null
                        && client.currentScreen == null
                        && SpaceRustClient.keyVoice != null
                        && SpaceRustClient.keyVoice.isPressed();
                boolean wantCapture = cfg.voiceEnabled && (loopbackTest || pttPressed);

                if (!wantCapture) {
                    if (wasSending) {
                        wasSending = false;
                        selfTalking = false;
                        sendStopPacket();
                    }
                    liveMicLevel *= 0.6f;
                    if (line != null) {
                        closeQuietly(line);
                        line = null;
                    }
                    Thread.sleep(25);
                    continue;
                }

                String desired = cfg.voiceInputDevice == null ? "" : cfg.voiceInputDevice;
                if (line == null || forceReopenInput || !desired.equals(activeInputKey)) {
                    closeQuietly(line);
                    line = openCaptureLine(desired);
                    activeInputKey = desired;
                    forceReopenInput = false;
                    if (line == null) {
                        selfTalking = false;
                        liveMicLevel = 0.0f;
                        Thread.sleep(200);
                        continue;
                    }
                }

                int read = line.read(raw, 0, raw.length);
                if (read < raw.length) {
                    Thread.sleep(5);
                    continue;
                }

                double sumSq = 0.0;
                float gain = cfg.voiceMicGain;
                for (int i = 0; i < FRAME_SAMPLES; i++) {
                    int lo = raw[i * 2] & 0xFF;
                    int hi = raw[i * 2 + 1];
                    int s = (short) ((hi << 8) | lo);
                    int scaled = Math.round(s * gain);
                    if (scaled > 32767) scaled = 32767;
                    else if (scaled < -32768) scaled = -32768;
                    samples[i] = (short) scaled;
                    double norm = scaled / 32768.0;
                    sumSq += norm * norm;
                }
                float rms = (float) Math.sqrt(sumSq / FRAME_SAMPLES);
                liveMicLevel = Math.max(rms, liveMicLevel * 0.5f);

                if (rms < cfg.voiceNoiseGate) {
                    selfTalking = false;
                    if (wasSending) {
                        wasSending = false;
                        sendStopPacket();
                    }
                    continue;
                }

                selfTalking = true;
                byte[] encoded = VoiceCodec.encode(samples, FRAME_SAMPLES);
                if (loopbackTest) {
                    short[] copy = VoiceCodec.decode(encoded, 0, encoded.length);
                    while (loopbackQueue.size() >= 8) loopbackQueue.poll();
                    loopbackQueue.offer(copy);
                } else {
                    sendVoiceFrame(encoded);
                    wasSending = true;
                }
            } catch (Throwable t) {
                closeQuietly(line);
                line = null;
                selfTalking = false;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private void playbackLoop() {
        SourceDataLine line = null;
        byte[] stereoOut = new byte[FRAME_SAMPLES * 4]; // 16-bit stereo LE
        int[] leftMix = new int[FRAME_SAMPLES];
        int[] rightMix = new int[FRAME_SAMPLES];
        long lastAudioMs = 0L;

        while (true) {
            try {
                RustSettings cfg = RustSettings.get();
                long now = System.currentTimeMillis();

                boolean hasActiveSpeakers = false;
                Iterator<Map.Entry<UUID, SpeakerState>> it = speakers.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, SpeakerState> entry = it.next();
                    SpeakerState st = entry.getValue();
                    if (!st.queue.isEmpty()) {
                        hasActiveSpeakers = true;
                    } else if (now - st.lastHeardMs > 2000L) {
                        it.remove();
                    }
                }

                boolean hasAudio = cfg.voiceEnabled && (hasActiveSpeakers || !loopbackQueue.isEmpty());
                if (!hasAudio) {
                    if (line != null && (now - lastAudioMs > 4000L || !cfg.voiceEnabled)) {
                        closeQuietly(line);
                        line = null;
                    }
                    Thread.sleep(20);
                    continue;
                }

                String desired = cfg.voiceOutputDevice == null ? "" : cfg.voiceOutputDevice;
                if (line == null || forceReopenOutput || !desired.equals(activeOutputKey)) {
                    closeQuietly(line);
                    line = openPlaybackLine(desired);
                    activeOutputKey = desired;
                    forceReopenOutput = false;
                    if (line == null) {
                        Thread.sleep(200);
                        continue;
                    }
                }

                lastAudioMs = now;
                Arrays.fill(leftMix, 0);
                Arrays.fill(rightMix, 0);

                MinecraftClient client = MinecraftClient.getInstance();
                float masterVol = cfg.voicePlayerVolume;
                if (client != null && client.options != null) {
                    masterVol *= client.options.getSoundVolume(net.minecraft.sound.SoundCategory.MASTER);
                }

                // 1. Optional self loopback test (Settings -> VOICE CHAT)
                short[] loopFrame = loopbackQueue.poll();
                if (loopFrame != null) {
                    int n = Math.min(FRAME_SAMPLES, loopFrame.length);
                    for (int i = 0; i < n; i++) {
                        int val = Math.round(loopFrame[i] * masterVol);
                        leftMix[i] += val;
                        rightMix[i] += val;
                    }
                }

                // 2. Mix remote speakers with 3D distance attenuation & stereo pan
                if (client != null && client.player != null) {
                    double lx = client.player.getX();
                    double ly = client.player.getY();
                    double lz = client.player.getZ();
                    double yawRad = Math.toRadians(client.player.yaw);
                    double rightX = -Math.cos(yawRad);
                    double rightZ = -Math.sin(yawRad);

                    for (SpeakerState st : speakers.values()) {
                        short[] frame = st.queue.poll();
                        if (frame == null) continue;

                        double dx = st.x - lx;
                        double dy = st.y - ly;
                        double dz = st.z - lz;
                        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        if (dist > MAX_VOICE_DISTANCE) continue;

                        float atten = (float) Math.pow(Math.max(0.0, 1.0 - dist / MAX_VOICE_DISTANCE), 1.35);
                        double pan = 0.0;
                        if (dist > 0.5) {
                            pan = Math.max(-1.0, Math.min(1.0, (dx * rightX + dz * rightZ) / dist));
                        }
                        float gainL = masterVol * atten * (float) Math.sqrt(0.5 * (1.0 - pan));
                        float gainR = masterVol * atten * (float) Math.sqrt(0.5 * (1.0 + pan));

                        int n = Math.min(FRAME_SAMPLES, frame.length);
                        for (int i = 0; i < n; i++) {
                            leftMix[i] += Math.round(frame[i] * gainL);
                            rightMix[i] += Math.round(frame[i] * gainR);
                        }
                    }
                }

                for (int i = 0; i < FRAME_SAMPLES; i++) {
                    int l = Math.max(-32768, Math.min(32767, leftMix[i]));
                    int r = Math.max(-32768, Math.min(32767, rightMix[i]));
                    int o = i * 4;
                    stereoOut[o] = (byte) (l & 0xFF);
                    stereoOut[o + 1] = (byte) ((l >> 8) & 0xFF);
                    stereoOut[o + 2] = (byte) (r & 0xFF);
                    stereoOut[o + 3] = (byte) ((r >> 8) & 0xFF);
                }

                line.write(stereoOut, 0, stereoOut.length);
            } catch (Throwable t) {
                closeQuietly(line);
                line = null;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private void onNetworkPacket(byte[] raw) {
        if (raw == null || raw.length < 17) return;
        if (!RustSettings.get().voiceEnabled) return;
        try {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.wrappedBuffer(raw));
            byte op = buf.readByte();
            long msb = buf.readLong();
            long lsb = buf.readLong();
            UUID speakerId = new UUID(msb, lsb);

            if (op == 0x03) {
                SpeakerState st = speakers.get(speakerId);
                if (st != null) {
                    st.lastHeardMs = 0L;
                }
                return;
            }
            if (op != 0x02) return;

            String name = buf.readString(32);
            double x = buf.readFloat();
            double y = buf.readFloat();
            double z = buf.readFloat();
            int len = buf.readUnsignedShort();
            if (len <= 4 || len > buf.readableBytes()) return;
            byte[] encoded = new byte[len];
            buf.readBytes(encoded);

            short[] pcm = VoiceCodec.decode(encoded, 0, encoded.length);
            if (pcm.length == 0) return;

            long now = System.currentTimeMillis();
            SpeakerState st = speakers.computeIfAbsent(speakerId, k -> new SpeakerState(name, x, y, z, now));
            st.x = x;
            st.y = y;
            st.z = z;
            st.lastHeardMs = now;
            while (st.queue.size() >= 8) {
                st.queue.poll();
            }
            st.queue.offer(pcm);
        } catch (Throwable ignored) {
        }
    }

    private static void sendVoiceFrame(byte[] adpcm) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getNetworkHandler() == null) return;
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer(3 + adpcm.length));
        buf.writeByte(0x01);
        buf.writeShort(adpcm.length);
        buf.writeBytes(adpcm);
        client.execute(() -> {
            if (MinecraftClient.getInstance().getNetworkHandler() != null) {
                ClientPlayNetworking.send(VOICE_CHANNEL, buf);
            }
        });
    }

    private static void sendStopPacket() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getNetworkHandler() == null) return;
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer(1));
        buf.writeByte(0x00);
        client.execute(() -> {
            if (MinecraftClient.getInstance().getNetworkHandler() != null) {
                ClientPlayNetworking.send(VOICE_CHANNEL, buf);
            }
        });
    }

    private static void closeQuietly(javax.sound.sampled.DataLine line) {
        if (line != null) {
            try {
                line.stop();
            } catch (Throwable ignored) {
            }
            try {
                line.close();
            } catch (Throwable ignored) {
            }
        }
    }
}
