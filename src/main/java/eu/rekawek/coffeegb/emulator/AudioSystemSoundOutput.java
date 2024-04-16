package eu.rekawek.coffeegb.emulator;

import eu.rekawek.coffeegb.Gameboy;
import eu.rekawek.coffeegb.sound.SoundOutput;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.Arrays;

public class AudioSystemSoundOutput implements SoundOutput, Runnable {

    private static final int SAMPLE_RATE = 44100;

    private static final int BUFFER_SIZE = 4096;

    private static final AudioFormat FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED, SAMPLE_RATE, 8, 2, 2, SAMPLE_RATE, false);

    private static final int DIVIDER = (int) (Gameboy.TICKS_PER_SEC / FORMAT.getSampleRate());

    private static final long NANOS_IN_SEC = 1000000000L;

    private static final long BUFFER_LENGTH_NANOS = (BUFFER_SIZE / 2) * NANOS_IN_SEC / SAMPLE_RATE;

    private byte[] buffer = new byte[BUFFER_SIZE];

    private byte[] lockedBuffer = new byte[BUFFER_SIZE];

    private final byte[] finalBuffer = new byte[BUFFER_SIZE];

    private volatile boolean enabled = true;

    private volatile int pos;

    private int tick;

    private volatile boolean doStop;

    private volatile boolean isStopped;

    private volatile boolean isPlaying;

    private volatile long writeStart;

    @Override
    public void run() {
        if (true) return; // noop, TODO: send noteblock sounds to client? hmm

        pos = 0;
        tick = 0;
        doStop = false;
        isStopped = false;
        while (pos < BUFFER_SIZE && !doStop) {
        }
        SourceDataLine line;
        try {
            line = AudioSystem.getSourceDataLine(FORMAT);
            line.open(FORMAT, BUFFER_SIZE / 2);
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
        line.start();
        while (!doStop) {
            while (pos < BUFFER_SIZE && !doStop) {
                if (System.nanoTime() - writeStart > BUFFER_LENGTH_NANOS * 1.1f) {
                    break;
                }
            }
            if (doStop) {
                break;
            }
            int localPos = pos;
            byte[] tmp = lockedBuffer;
            lockedBuffer = buffer;
            buffer = tmp;
            pos = 0;
            if (!isPlaying || !enabled) {
                Arrays.fill(finalBuffer, (byte) 0);
            } else {
                fill(lockedBuffer, localPos, finalBuffer);
            }
            writeStart = System.nanoTime();
            line.write(finalBuffer, 0, BUFFER_SIZE);
        }
        line.drain();
        line.stop();
        isStopped = true;
    }

    public void stopThread() {
        doStop = true;
        while (!isStopped) {
        }
    }

    @Override
    public void start() {
        isPlaying = true;
    }

    @Override
    public void stop() {
        isPlaying = false;
    }

    @Override
    public void play(int left, int right) {
        if (tick++ != 0) {
            tick %= DIVIDER;
            return;
        }

        while (pos >= BUFFER_SIZE) {
        }

        buffer[pos] = (byte) (left);
        buffer[pos + 1] = (byte) (right);
        pos += 2;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private static void fill(byte[] src, int srcLen, byte[] dst) {
        for (int i = 0; i < dst.length; i++) {
            if (srcLen == 0) {
                dst[i] = 0;
            } else if (i >= srcLen) {
                dst[i] = src[srcLen - 1];
            } else {
                dst[i] = src[i];
            }
        }
    }
}
