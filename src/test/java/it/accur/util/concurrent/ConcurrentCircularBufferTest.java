package it.accur.util.concurrent;

import java.util.Arrays;
import org.testng.annotations.Test;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs three simple tests on the ConcurrentCircularBuffer class.
 *
 * @author <a href="mailto:ssmccoy@blisted.org">Scott S. McCoy</a>
 */
@Test(groups={"unit"}, testName="Test of the ConcurrentCircularBuffer")
public class ConcurrentCircularBufferTest {
    /**
     * Test calculations for snapshots of under buffers.
     */
    public void testShortSnapshot () {
        ConcurrentCircularBuffer <Integer> buffer =
            new ConcurrentCircularBuffer <Integer> (Integer.class, 50);

        for (int i = 0; i < 20; i++) {
            buffer.add(i);
        }

        Integer[] snapshot = buffer.snapshot();

        assert snapshot.length == 20 :
            "Expected a stable snapshot of 20 items, got: " +
            snapshot.length;
    }

    /**
     * Test calculations for snapshots of overfilled buffers.
     */
    public void testLongSnapshot () {
        ConcurrentCircularBuffer <Integer> buffer =
            new ConcurrentCircularBuffer <Integer> (Integer.class, 50);

        for (int i = 0; i < 100; i++) {
            buffer.add(i);
        }

        assert buffer.snapshot().length == 50 :
            "Expected a stable snapshot of the buffer of 50";
    }

    /**
     * Test pulling a snapshot of an empty buffer.
     */
    public void testEmptySnapshot () {
        ConcurrentCircularBuffer <Integer> buffer =
            new ConcurrentCircularBuffer <Integer> (Integer.class, 50);

        assert buffer.snapshot().length == 0 :
            "Expected zero-length snapshot";
    }

    /**
     * Test calculations for snapshots of partial-wrap buffers.
     */
    public void testWrappedSnapshot () {
        ConcurrentCircularBuffer <Integer> buffer =
            new ConcurrentCircularBuffer <Integer> (Integer.class, 50);

        for (int i = 0; i < 75; i++) {
            buffer.add(i);
        }

        Integer[] snapshot = buffer.snapshot();

        assert snapshot.length == 50 :
            "Expected a stable snapshot of the buffer of 50";

        for (int i = 1; i < snapshot.length; i++) {
            assert snapshot[i - 1] != null :
                "Found uninitialized value in snapshot!: " + (i - 1);

            assert snapshot[i] != null :
                "Found uninitialized value in snapshot!:" + i;
                   
            assert snapshot[i - 1] < snapshot[i] :
                "Snapshot elements are not in order!";
        }

        assert snapshot[0] == 25 &&
               snapshot[49] == 74   : 
               "Expected range of snapshot to be 25 to 74, got: " + 
               snapshot[0] + ", " + snapshot[49];
    }

    static class Incrementer implements Runnable {
        private ConcurrentCircularBuffer <Integer> buffer;

        Incrementer (final ConcurrentCircularBuffer <Integer> buffer) {
            this.buffer = buffer;
        }

        public void run () {
            for (int i = 1; i <= 10000; i++) {
                buffer.add(i);

                Thread.yield();
            }
        }
    }

    static class Gatherer implements Runnable {
        private ConcurrentCircularBuffer <Integer> buffer;

        Gatherer (final ConcurrentCircularBuffer <Integer> buffer) {
            this.buffer = buffer;
        }

        public void run () {
            for (int i = 0; i < 100; i++) {
                try {
                    Thread.sleep(10);
                }
                catch (InterruptedException exception) {
                    /* NOOP */
                }

                Integer[] snapshot = buffer.snapshot();

                assert snapshot.length > 18 :
                    "Larger trim than expected!: " + snapshot.length;

                assert snapshot.length <= 20 :
                    "Buffer is too large: " + snapshot.length;

                for (int j = 0; j < snapshot.length; j++) {
                    if (snapshot[j] == null) {
                        assert false :
                            "Found uninitialized value in snapshot: " + 
                            j + ":\n" + Arrays.toString(snapshot);
                    }
                }

                assert snapshot[0] < snapshot[snapshot.length - 1] :
                    "Values do not appear to be coming out in order";

                Thread.yield();
            }
        }
    }

    /**
     * Test concurrent gathering of snapshot data.
     *
     * <p>Kick off two regularly yielding counter threads and one gatherer
     * thread.  The gatherer pulls a snapshot once every 10ms, and does a few
     * basic consistency checks.</p>
     */
    public void testConcurrentSnapshot () 
    throws Exception {
        ConcurrentCircularBuffer <Integer> buffer =
            new ConcurrentCircularBuffer <Integer> (Integer.class, 20);

        Thread[] incrementers = new Thread [5];

        for (int i = 0; i < incrementers.length; i++) {
            incrementers[i] = new Thread(new Incrementer(buffer));

            incrementers[i].run();
        }

        Thread gatherer = new Thread(new Gatherer(buffer));

        gatherer.run();

        /* Wait for all threads to finish. */
        for (Thread thread : incrementers) {
            thread.join();
        }

        gatherer.join();
    }
}
