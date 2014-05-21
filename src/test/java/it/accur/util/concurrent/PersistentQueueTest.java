package it.accur.util.concurrent;
import org.testng.annotations.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.File;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;

/* 2011-04-17T13:03:37Z-0700
 * This is only flagged as an integration test because it is so expensive..
 */
@Test(groups={"integration"}, testName = "Persistent Queue Validation Test")
public class PersistentQueueTest {
    public static final String TEN    = "0123456789";
    public static final String TWENTY = TEN + TEN;
    public static final String THIRTY = TWENTY + TEN;

    private final Logger log = LoggerFactory.getLogger(
        PersistentQueueTest.class );

    /**
     * Add the given strings to the given queue.
     *
     * @param queue The queue to add the strings to.
     * @param n The number of times to add them
     * @param strings The sequence of strings to add.
     */
    private void addStrings (final PersistentQueue <String> queue, 
                             final int n, 
                             final String ... strings) 
    {
        for (int i = 0; i < n; i++) {
            for (String string : strings) {
                assert queue.offer(string) :
                    "Not enough space to add string";
            }
        }
    }

    /**
     * Assert that the given queue contains the given list of strings (the
     * given number of times.
     *
     * @param queue The queue to test.
     * @param n The number of times to expect the string sequence.
     * @param strings The strings to expect (in order).
     */
    private void expectStrings (final PersistentQueue <String> queue,
                                final int n,
                                final String ... strings)
    {
        for (int i = 0; i < n; i++) {
            for (String string : strings) {
                String element = queue.poll();

                assert string.equals(element) :
                    "Expected " + string + " found " + element;
            }
        }
    }

    /**
     * Test the behavior of the queue's iterator.
     */
    @Test(groups={"integration"})
    public void testIterator ()
    throws IOException, InterruptedException {
        log.trace("[start] PersistentQueueTest.testIterator");
        File tempfile = File.createTempFile("PersistentQueue", ".test");

        try {
            tempfile.delete();

            PersistentQueue <Integer> queue = new PersistentQueue <Integer> (
                Integer.class, tempfile, 4, 4096
                );

            int total = 0;

            for (int i = 0; i < 20; i++) {
                queue.put(i);

                total += i;
            }

            for (Integer i : queue) {
                total -= i;
            }

            assert total == 0 : 
                "Expected sum total of balanced opposing " +
                "commutative operations to be zero";
        }
        finally {
            tempfile.delete();
        }

        log.trace("[end] PersistentQueueTest.testIterator");
    }

    /**
     * Test queue who's written space frequently wraps the end of the buffer.
     * This is likely to uncover any bugs in management of writeable block
     * semaphores.
     */
    @Test(groups={"integration"})
    public void testWrap ()
    throws IOException, InterruptedException {
        log.trace("[start] PersistentQueueTest.testWrap");
        File tempfile = File.createTempFile("PersistentQueue", ".test");

        try {
            tempfile.delete();

            PersistentQueue <Integer> queue = new PersistentQueue <Integer> (
                Integer.class, tempfile, 10, 110
                );

            for (int i = 0; i < 20; i++) {
                queue.put(i);

                int x = queue.take();

                assert x == i : "Expected " + i + " found " + x;
            }
        }
        finally {
            tempfile.delete();
        }
        log.trace("[end] PersistentQueueTest.testWrap");
    }

    /**
     * Basic test of the FIFO properties of the queue.
     */
    @Test(groups={"integration"})
    public void testSequence ()
    throws IOException, InterruptedException {
        log.trace("[start] PersistentQueueTest.testSequence");

        File tempfile = File.createTempFile("PersistentQueue", ".test");

        try {
            tempfile.delete();

            PersistentQueue <Integer> queue = new PersistentQueue <Integer> (
                Integer.class, tempfile, 4, 4096
                );

            for (int i = 0; i < 10; i++) {
                queue.put(i);

                int x = queue.poll();

                assert x == i : "Expected " + i + " found " + x;
            }

            for (int i = 0; i < 10; i++) {
                queue.put(i);
            }

            for (int i = 0; i < 10; i++) {
                int x = queue.poll();

                assert x == i : "Expected " + i + " found " + x;
            }

            assert queue.size() == 0 : 
                "Expected to end with queue size of zero";
        }
        finally {
            tempfile.delete();
        }

        log.trace("[exit] PersistentQueueTest.testSequence");
    }

    /**
     * Test that the queue properly persists it's contents, despite a full
     * garbage collection cycle.
     */
    @Test(groups={"integration"})
    public void testRepopulation () 
    throws IOException {
        log.trace("[start] PersistentQueueTest.testRepopulation");

        File tempfile = File.createTempFile("PersistentQueue", ".test");

        try {
            /* Remove the file to make sure that we don't attempt to read the
             * header from the file since it will be empty */
            tempfile.delete();

            PersistentQueue <String> queue = new PersistentQueue <String> (
                String.class, tempfile, 4, 8192
                );

            addStrings(queue, 10, TEN);
            addStrings(queue, 10, TWENTY);
            addStrings(queue, 10, THIRTY);
            addStrings(queue, 10, TEN, TWENTY, THIRTY);

            expectStrings(queue, 10, TEN);
            expectStrings(queue, 10, TWENTY);
            expectStrings(queue, 10, THIRTY);
            expectStrings(queue, 10, TEN, TWENTY, THIRTY);

            addStrings(queue, 10, TEN);
            addStrings(queue, 10, TWENTY);
            addStrings(queue, 10, THIRTY);
            addStrings(queue, 10, TEN, TWENTY, THIRTY);

            /* Flush the queue before reopening it to ensure it gets written.
             */
            queue.flush();

            /* And then recreate it */
            queue = new PersistentQueue <String> (
                String.class, tempfile, 4, 8192
                );

            /* While we're at it, let's test peek by doing it 20 times and
             * expecting the same result each time... */
            for (int i = 0; i < 20; i++)
                assert TEN.equals(queue.peek()) :
                    "Expected peek() to consistently return " + TEN;

            expectStrings(queue, 10, TEN);
            expectStrings(queue, 10, TWENTY);
            expectStrings(queue, 10, THIRTY);
            expectStrings(queue, 10, TEN, TWENTY, THIRTY);
        }
        finally {
            tempfile.delete();
        }

        log.trace("[end] PersistentQueueTest.testRepopulation");
    }

    static class TestType implements Serializable {
        private int id;
        private String message;

        TestType (final int id,
                  final String message)
        {
            this.id      = id;
            this.message = message;
        }
    }

    @Test(groups={"integration"})
    public void testOtherObjects () 
    throws IOException, InterruptedException {
        File tempfile = File.createTempFile("PersistentQueue", ".test");

        try {
            tempfile.delete();

            final PersistentQueue <TestType> queue =
                new PersistentQueue <TestType> (TestType.class, tempfile, 4, 1024);

            for (int i = 0; i < 255; i++) {
                queue.add(new TestType(i, Integer.toString(i)));

                TestType object = queue.take();

                assert object.id == i :
                    "Expected " + i + ", observed " + object.id;

                assert object.message.equals(Integer.toString(object.id)) :
                    "Expected " + object.message + ", observed " + object.id;
            }
        }
        finally {
            tempfile.delete();
        }
    }

    /**
     * Test for a bug which leaks block-semaphores by cycling data through the
     * buffer enough times to verify there's no leak.
     */
    @Test(groups={"integration"})
    public void testForLeaks () 
    throws IOException, InterruptedException {
        File tempfile = File.createTempFile("PersistentQueue", ".test");

        try {
            tempfile.delete();

            final BlockingQueue  <Long> queue =
                new PersistentQueue <Long> (Long.class, tempfile, 89, 178);

            long ms = System.currentTimeMillis();

            for (long i = 0; i < 1024; i++) {
                log.trace("[cycling] {}", i);

                queue.add(ms);

                long o = queue.remove();

                assert ms == o : "Expected " + ms + ", observed " + o;
            }

            for (long i = 0; i < 256; i++) {
                log.trace("[cycling] {}", i);

                queue.add(i);

                long o = queue.remove();

                assert i == o : "Expected " + i + ", observed " + o;
            }
        }
        finally {
//          tempfile.delete();
        }
    }

    /**
     * Test that the queue both blocks waiting for input and properly
     * distributes input to waiters.
     */
    @Test(groups={"integration"})
    public void testConcurrentBehavior () 
    throws IOException, InterruptedException {
        final int QUEUE_ITEMS = 1024;
        final int THREADS     = 10;

        log.trace("[start] PersistentQueueTest.testConcurrentBehavior");
        File tempfile = File.createTempFile("PersistentQueue", ".test");

        try {
            /* Remove the file to make sure that we don't attempt to read the
             * header from the file since it will be empty */
            tempfile.delete();

            List <Thread> threads = new ArrayList <Thread> ();

            final AtomicInteger count = new AtomicInteger();
            final PersistentQueue <Integer> queue = 
                new PersistentQueue <Integer> (
                    Integer.class, tempfile, 9, 4104
                    );

            for (int i = 0; i < THREADS; i++) {
                Thread thread = new Thread () {
                    public void run () {
                        log.trace("[thread] start {}",
                                  Thread.currentThread().getId());

                        int x = 0;

                        try {
                            while (true) {
                                /* These yeilds are to increase context switch
                                 * randomization in an attempt to exacerbate
                                 * any potential race conditions
                                 */
                                Thread.yield();

                                Integer y = queue.poll(2L, TimeUnit.SECONDS);

                                Thread.yield();
                                
                                if (y == null) 
                                    break;

                                Thread.yield();

                                log.trace("[poll] {}", y);

                                assert y >= x : "What happened?";

                                Thread.yield();

                                x = y;

                                Thread.yield();

                                log.trace("[thread] count {}",
                                          count.getAndIncrement());
                            }
                        }
                        catch (InterruptedException exception) {
                            throw new IllegalStateException(
                                "Unexpectedly interrupted", exception
                                );
                        }
                    }
                };

                thread.start();

                threads.add( thread );
            }

            for (int i = 0; i < QUEUE_ITEMS; i++) {
                log.trace("[put] {}", i);
                queue.put(i);
            }

            for (Thread thread : threads) {
                log.trace("[thread] joining {}", thread.getId());

                thread.join();
            }

            int total = count.get();

            assert total == QUEUE_ITEMS : 
                "Expected " + QUEUE_ITEMS + ", observed " + total;
        }
        finally {
            tempfile.delete();
        }            

        log.trace("[end] PersistentQueueTest.testConcurrentBehavior");
    }
}
