package core;

import javax.sound.sampled.*;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class Analyser {

    // private static final int SAMPLE_RATE = 88200; //96,000
//     private static final int SAMPLE_RATE = 96000;
    public static final int SAMPLE_RATE = 44100;
    private static final int DFT_SAMPLE_WIDTH = 1 << 12;
    private static final int DFT_PER_SECOND = 30;
    private static final int MISSED_SAMPLES = 1;
    private static final int SAMPLES_BETWEEN_DFTS = SAMPLE_RATE / DFT_PER_SECOND;

    private final Queue<TimeStep> timeStepBufferQueue = new LinkedBlockingQueue<>();
    private final QueueThread queueThread;
    private final AnalyserThread analyserThread;
    private TimeStep lastTimeStep;


    private int stepsShown;

    private TargetDataLine targetLine;

    private boolean isPaused = true;
    private final Session session;

    public Analyser(Session session) {

        this.session = session;
        queueThread = new QueueThread();
        analyserThread = new AnalyserThread();

    }

    public void start() throws IllegalArgumentException, LineUnavailableException {

        openMicrophoneStream();
        targetLine.start();
        analyserThread.start();

    }

    public void pause() {
        isPaused = true;
    }

    public void resume() {
        isPaused = false;
    }

    public void stop() {
        if (targetLine != null && targetLine.isOpen()) {
            targetLine.close();
        }
    }

    public boolean isPaused() {
        return isPaused;
    }

    public boolean isAlive() {
        return targetLine != null && targetLine.isOpen();
    }

    public boolean isRunning() {
        return isAlive() && !isPaused();
    }

    private void openMicrophoneStream() throws IllegalArgumentException, LineUnavailableException {

        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, true);
        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, format);

        targetLine = (TargetDataLine) AudioSystem.getLine(targetInfo);
        targetLine.open(format, SAMPLE_RATE / 5);

    }

    public int getStepsShown() {
        return stepsShown;
    }

    private final class AnalyserThread extends Thread {

        AnalyserThread() {

            setName("Analyser Thread");

        }

        @Override
        public void run() {

            int numBytesRead;
            int sampleBufferSize = targetLine.getBufferSize() / 2;
            byte[] read = new byte[targetLine.getBufferSize()];

            float[] data = new float[sampleBufferSize];
            float[] buffer = new float[sampleBufferSize];

            int forward = MISSED_SAMPLES * DFT_SAMPLE_WIDTH / 2; // widthInSamples / 2
            int centre = forward + sampleBufferSize;

            queueThread.start();

            stepsShown = 0;

            while (targetLine.isOpen()) {

                numBytesRead = targetLine.read(read, 0, read.length);
                if (numBytesRead == -1)
                    break;

                if (isPaused)
                    continue;

                float[] temp = buffer;
                buffer = data;
                data = temp;

                for (int i = 0; i < data.length; i++) {

                    data[i] = ((read[i * 2] << 8) | (read[i * 2 + 1] & 0xFF)) / 32768.0F;
                }

                while (centre + forward < sampleBufferSize * 2) {

                    float[] samples = new float[DFT_SAMPLE_WIDTH];
                    for (int i = 0; i < DFT_SAMPLE_WIDTH; i++) {

                        int location = centre - MISSED_SAMPLES * DFT_SAMPLE_WIDTH / 2 + i * MISSED_SAMPLES;
                        if (location < sampleBufferSize) {
                            samples[i] = buffer[location];
                        } else {
                            samples[i] = data[location - sampleBufferSize];
                        }
                    }
                    TimeStep current = new TimeStep(samples, lastTimeStep);
                    timeStepBufferQueue.add(current);
                    lastTimeStep = current;

                    centre += SAMPLES_BETWEEN_DFTS;

                }

                centre -= sampleBufferSize;

            }

        }

    }

    private final class QueueThread extends Thread {

        QueueThread() {

            setName("Analyser Queue Thread");

        }

        @Override
        public void run() {

            double mspt = 1000d / DFT_PER_SECOND; // miliseconds per tick
            long last = System.currentTimeMillis();
            long current = last;
            double difference = 0;

            while (true) {

                last = current;
                current = System.currentTimeMillis();
                difference += current - last;

                while (difference > mspt) {
                    difference -= mspt;

                    while (isPaused) {
                        timeStepBufferQueue.poll();
                    }

                    if (!timeStepBufferQueue.isEmpty()) {
                        synchronized (session) {
                            session.addTimeStep(timeStepBufferQueue.poll());
                            stepsShown++;
                        }
                    }

                }

                try {

                    sleep(1);

                } catch (InterruptedException e) {

                    e.printStackTrace();
                    break;

                }

            }

        }

    }

}
