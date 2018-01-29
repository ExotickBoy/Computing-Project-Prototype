package core;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class SoundUtils {

    private SoundUtils() {
    } // so that an object of it can't be made

    static void bytesToFloats(@NotNull byte[] bytes, @NotNull float[] floats, boolean isBigEdian) {

        assert bytes.length % 2 == 0;

        if (isBigEdian) {

            for (int i = 0; i < floats.length; i++) {
                floats[i] = ((bytes[i * 2] << 8) | (bytes[i * 2 + 1] & 0xFF)) / 32768.0F;
            }

        } else {

            for (int i = 0; i < floats.length; i++) {
                floats[i] = ((bytes[i * 2 + 1] << 8) | (bytes[i * 2] & 0xFF)) / 32768.0F;
            }

        }

    }

    static void floatsToBytes(@NotNull List<Float> floats, byte[] bytes, int inOffset, int outOffset, int size) {

        for (int i = 0; i < size; i++) {

            int nSample = Math.round(Math.min(1.0F, Math.max(-1.0F, floats.get(i + inOffset))) * 32767.0F);
            bytes[2 * (i + outOffset)] = (byte) (nSample >> 8 & 0xFF);
            bytes[2 * (i + outOffset) + 1] = (byte) (nSample & 0xFF);

        }

    }

}
