package core;

import org.jetbrains.annotations.NotNull;

import javax.sound.sampled.*;
import java.util.List;

/**
 * This class is responsible for interfacing with the source of sound being used, i.e. the microphone.
 * It reads the samples from the source and then passes them to the session to deal with
 *
 * @author Kacper Lubisz
 */
class SoundGatheringController extends Thread {

    static final int SAMPLE_RATE = 44100;
    static final int SAMPLE_BUFFER_SIZE = 2205;
    static final AudioFormat AUDIO_FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, true);

    private TargetDataLine targetLine;

    private volatile boolean isPaused = true;
    private final Session session;
    private boolean willDiscard;

    SoundGatheringController(Session session, boolean willDiscard) {

        super("Sound Gathering Thread");
        this.session = session;
        this.willDiscard = willDiscard;// whether it should not listen while it is paused

    }

    /**
     * This opens the microphone line if one is being used and starts the thread
     *
     * @throws LineUnavailableException This exception is thrown if opening a microphone line is impossible
     */
    void begin() throws LineUnavailableException {

        openMicrophoneStream();
        start();

    }

    /**
     * Permanently stops the thread from running by closing the input line
     */
    void end() {
        if (targetLine != null)
            targetLine.close();
    }

    boolean isPaused() {
        return isPaused;
    }

    void setPaused(boolean paused) {
        isPaused = paused;
    }

    /**
     * @return whether the thread already has an open stream
     */
    boolean isOpen() {
        return targetLine != null && targetLine.isOpen();
    }

    /**
     * Opens the default microphone
     *
     * @throws IllegalArgumentException If the default parameters of audio format being used aren't possible
     *                                  given the microphone
     * @throws LineUnavailableException If a microphone line cannot be opened because a microphone isn't connected
     */
    private void openMicrophoneStream() throws IllegalArgumentException, LineUnavailableException {

        AudioFormat format = AUDIO_FORMAT;
        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, format);

        targetLine = (TargetDataLine) AudioSystem.getLine(targetInfo);
        targetLine.open(format, SAMPLE_BUFFER_SIZE * 2);
        targetLine.start();

    }

    /**
     * When the thread is started
     */
    @Override
    public void run() {

        int numBytesRead;
        byte[] read = new byte[targetLine.getBufferSize()];
        float[] samples = new float[targetLine.getBufferSize() / 2];
        // two bytes to each sample

        while (targetLine.isOpen()) { // this thread stops when the microphone line is stopped

            numBytesRead = targetLine.read(read, 0, read.length);
            if (numBytesRead == -1)
                break; // this will only happen if there is a problem with the input stream, i.e. microphone is disconnected

            if (isPaused) {
                if (willDiscard) {
                    continue; // this will read the samples and then not use them
                } else {
                    while (isPaused) {
                        Thread.onSpinWait();
                    }
                }
            }
            bytesToFloats(read, samples);
            session.addSamples(samples);

        }

    }

    private static void bytesToFloats(@NotNull byte[] bytes, @NotNull float[] floats) {

        assert bytes.length % 2 == 0;

        for (int i = 0; i < floats.length; i++) {
            floats[i] = ((bytes[i * 2] << 8) | (bytes[i * 2 + 1] & 0xFF)) / 32768.0F;
        }
    }

    public static void floatsToBytes(@NotNull List<Float> floats, byte[] bytes, int inOffset, int outOffset, int size) {

        for (int i = 0; i < size; i++) {

            int nSample = Math.round(Math.min(1.0F, Math.max(-1.0F, floats.get(i + inOffset))) * 32767.0F);
            bytes[2 * (i + outOffset)] = (byte) (nSample >> 8 & 0xFF);
            bytes[2 * (i + outOffset) + 1] = (byte) (nSample & 0xFF);

        }

    }

}
