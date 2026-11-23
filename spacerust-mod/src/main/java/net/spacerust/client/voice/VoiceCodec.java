package net.spacerust.client.voice;

/**
 * Lightweight 4-bit IMA ADPCM speech codec (16 kHz, mono -> 4:1 compression).
 * Encodes 640 16-bit PCM samples (40ms, 1280 bytes) into a 324-byte frame (~8.1 KB/s).
 */
public final class VoiceCodec {

    private static final int[] INDEX_TABLE = {
            -1, -1, -1, -1, 2, 4, 6, 8,
            -1, -1, -1, -1, 2, 4, 6, 8
    };

    private static final int[] STEP_TABLE = {
            7, 8, 9, 10, 11, 12, 13, 14, 16, 17,
            19, 21, 23, 25, 28, 31, 34, 37, 41, 45,
            50, 55, 60, 66, 73, 80, 88, 97, 107, 118,
            130, 143, 157, 173, 190, 209, 230, 253, 279, 307,
            337, 371, 408, 449, 494, 544, 598, 658, 724, 796,
            876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066,
            2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358,
            5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899,
            15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767
    };

    private VoiceCodec() {
    }

    public static byte[] encode(short[] pcm, int sampleCount) {
        int outBytes = 4 + (sampleCount + 1) / 2;
        byte[] out = new byte[outBytes];

        int predictor = sampleCount > 0 ? pcm[0] : 0;
        int index = 0;

        out[0] = (byte) (predictor & 0xFF);
        out[1] = (byte) ((predictor >> 8) & 0xFF);
        out[2] = (byte) index;
        out[3] = 0;

        int outIdx = 4;
        boolean lowNibble = true;
        int currentByte = 0;

        for (int i = 0; i < sampleCount; i++) {
            int sample = pcm[i];
            int step = STEP_TABLE[index];
            int diff = sample - predictor;
            int nibble = 0;
            if (diff < 0) {
                nibble = 8;
                diff = -diff;
            }
            int tempStep = step;
            if (diff >= tempStep) {
                nibble |= 4;
                diff -= tempStep;
            }
            tempStep >>= 1;
            if (diff >= tempStep) {
                nibble |= 2;
                diff -= tempStep;
            }
            tempStep >>= 1;
            if (diff >= tempStep) {
                nibble |= 1;
            }

            int diffq = step >> 3;
            if ((nibble & 4) != 0) diffq += step;
            if ((nibble & 2) != 0) diffq += step >> 1;
            if ((nibble & 1) != 0) diffq += step >> 2;

            if ((nibble & 8) != 0) {
                predictor -= diffq;
            } else {
                predictor += diffq;
            }
            if (predictor > 32767) predictor = 32767;
            else if (predictor < -32768) predictor = -32768;

            index += INDEX_TABLE[nibble];
            if (index < 0) index = 0;
            else if (index > 88) index = 88;

            if (lowNibble) {
                currentByte = nibble & 0x0F;
                lowNibble = false;
            } else {
                currentByte |= (nibble & 0x0F) << 4;
                out[outIdx++] = (byte) currentByte;
                lowNibble = true;
            }
        }
        if (!lowNibble && outIdx < out.length) {
            out[outIdx] = (byte) currentByte;
        }
        return out;
    }

    public static short[] decode(byte[] data, int offset, int length) {
        if (length < 4) {
            return new short[0];
        }
        int predictor = (short) ((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8));
        int index = data[offset + 2] & 0xFF;
        if (index > 88) index = 88;

        int byteCount = length - 4;
        int sampleCount = byteCount * 2;
        short[] out = new short[sampleCount];

        int outIdx = 0;
        for (int i = 4; i < length; i++) {
            int b = data[offset + i] & 0xFF;
            for (int n = 0; n < 2; n++) {
                int nibble = (n == 0) ? (b & 0x0F) : ((b >> 4) & 0x0F);
                int step = STEP_TABLE[index];

                int diffq = step >> 3;
                if ((nibble & 4) != 0) diffq += step;
                if ((nibble & 2) != 0) diffq += step >> 1;
                if ((nibble & 1) != 0) diffq += step >> 2;

                if ((nibble & 8) != 0) predictor -= diffq;
                else predictor += diffq;

                if (predictor > 32767) predictor = 32767;
                else if (predictor < -32768) predictor = -32768;

                index += INDEX_TABLE[nibble];
                if (index < 0) index = 0;
                else if (index > 88) index = 88;

                out[outIdx++] = (short) predictor;
            }
        }
        return out;
    }
}
