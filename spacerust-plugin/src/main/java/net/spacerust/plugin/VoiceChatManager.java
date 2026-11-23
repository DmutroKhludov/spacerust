package net.spacerust.plugin;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Purpur 1.16.5 3D Proximity Voice Chat relay over "spacerust:voice".
 * Receives compressed IMA ADPCM frames from speaking clients and forwards
 * them with speaker coordinates to nearby listeners within 40 blocks.
 */
public class VoiceChatManager implements PluginMessageListener {

    private static final double MAX_RANGE_SQ = 40.0 * 40.0;
    private static final double STOP_RANGE_SQ = 52.0 * 52.0;

    private final SpaceRustPlugin plugin;
    private final Map<UUID, long[]> rateWindow = new ConcurrentHashMap<>();

    public VoiceChatManager(SpaceRustPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player sender, byte[] message) {
        if (!SpaceRustPlugin.VOICE_CHANNEL.equals(channel) || sender == null || !sender.isOnline()) {
            return;
        }
        if (message == null || message.length < 1) {
            return;
        }

        byte op = message[0];
        if (op == 0x00) {
            relayStop(sender);
            return;
        }
        if (op != 0x01 || message.length < 7 || message.length > 600) {
            return;
        }

        int frameLen = ((message[1] & 0xFF) << 8) | (message[2] & 0xFF);
        if (frameLen < 4 || frameLen > 512 || (3 + frameLen) > message.length) {
            return;
        }

        // Anti-flood guard: max ~45 voice packets per 1000ms window
        long now = System.currentTimeMillis();
        long[] win = rateWindow.computeIfAbsent(sender.getUniqueId(), k -> new long[]{now, 0L});
        if (now - win[0] >= 1000L) {
            win[0] = now;
            win[1] = 1L;
        } else {
            win[1]++;
            if (win[1] > 45L) {
                return;
            }
        }

        Location loc = sender.getLocation();
        World world = loc.getWorld();
        if (world == null) {
            return;
        }

        byte[] packet = buildRelayPacket(sender, loc, message, 3, frameLen);
        if (packet == null) {
            return;
        }

        for (Player target : world.getPlayers()) {
            if (target == null || !target.isOnline() || target.getUniqueId().equals(sender.getUniqueId())) {
                continue;
            }
            if (target.getLocation().distanceSquared(loc) <= MAX_RANGE_SQ) {
                target.sendPluginMessage(plugin, SpaceRustPlugin.VOICE_CHANNEL, packet);
            }
        }
    }

    private void relayStop(Player sender) {
        Location loc = sender.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        ByteArrayOutputStream baos = new ByteArrayOutputStream(17);
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeByte(0x03);
            UUID uuid = sender.getUniqueId();
            dos.writeLong(uuid.getMostSignificantBits());
            dos.writeLong(uuid.getLeastSignificantBits());
            byte[] packet = baos.toByteArray();
            for (Player target : world.getPlayers()) {
                if (target != null && target.isOnline() && !target.getUniqueId().equals(uuid)
                        && target.getLocation().distanceSquared(loc) <= STOP_RANGE_SQ) {
                    target.sendPluginMessage(plugin, SpaceRustPlugin.VOICE_CHANNEL, packet);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static byte[] buildRelayPacket(Player sender, Location loc, byte[] src, int off, int len) {
        try {
            byte[] nameBytes = sender.getName().getBytes(StandardCharsets.UTF_8);
            if (nameBytes.length > 32) {
                byte[] truncated = new byte[32];
                System.arraycopy(nameBytes, 0, truncated, 0, 32);
                nameBytes = truncated;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream(36 + nameBytes.length + len);
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeByte(0x02);
            UUID uuid = sender.getUniqueId();
            dos.writeLong(uuid.getMostSignificantBits());
            dos.writeLong(uuid.getLeastSignificantBits());
            writeVarInt(dos, nameBytes.length);
            dos.write(nameBytes);
            dos.writeFloat((float) loc.getX());
            dos.writeFloat((float) (loc.getY() + 1.62));
            dos.writeFloat((float) loc.getZ());
            dos.writeShort(len);
            dos.write(src, off, len);
            return baos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & -128) != 0) {
            out.writeByte((value & 127) | 128);
            value >>>= 7;
        }
        out.writeByte(value);
    }
}
