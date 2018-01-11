package core;

import javax.sound.sampled.*;

public class SoundGatheringController {

    public static final int DEFAULT_SAMPLE_RATE = 44100;

    private final SoundGatheringThread analyserThread;
    private TargetDataLine targetLine;

    private volatile boolean isPaused = true;
    private final Session session;
    private boolean willDiscard = true;

    public SoundGatheringController(Session session) {

        this.session = session;
        analyserThread = new SoundGatheringThread();

    }

    public void start() throws IllegalArgumentException, LineUnavailableException {

        openMicrophoneStream();
        targetLine.start();
        analyserThread.start();

    }

    public void stop() {
        if (targetLine != null && targetLine.isOpen()) {
            targetLine.close();
        }
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
    }

    public boolean isAlive() {
        return targetLine != null && targetLine.isOpen();
    }

    private void openMicrophoneStream() throws IllegalArgumentException, LineUnavailableException {

        AudioFormat format = new AudioFormat(DEFAULT_SAMPLE_RATE, 16, 1, true, true);
        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, format);

        targetLine = (TargetDataLine) AudioSystem.getLine(targetInfo);
        targetLine.open(format, 4410);

    }

    private final class SoundGatheringThread extends Thread {

        SoundGatheringThread() {

            super("Sound Gathering Thread");

        }

        @Override
        public void run() {

            int numBytesRead;
            int sampleBufferSize = targetLine.getBufferSize() / 2;
            byte[] read = new byte[targetLine.getBufferSize()];

            float[] samples = new float[sampleBufferSize];

            while (targetLine.isOpen()) {

                numBytesRead = targetLine.read(read, 0, read.length);
                if (numBytesRead == -1)
                    break;

                if (isPaused) {
                    if (willDiscard) {
                        continue;
                    } else {
                        while (isPaused) {
                            Thread.onSpinWait();
                        }
                    }
                }
                for (int i = 0; i < samples.length; i++) {
                    samples[i] = ((read[i * 2] << 8) | (read[i * 2 + 1] & 0xFF)) / 32768.0F;
                }
                session.addSamples(samples);

            }

        }

    }

}
