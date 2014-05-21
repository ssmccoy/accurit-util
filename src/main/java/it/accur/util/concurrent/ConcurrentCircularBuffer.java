package it.accur.util.concurrent;

import java.util.concurrent.atomic.AtomicInteger;
import java.lang.reflect.Array;

/**
 * A circular array buffer with a copy-and-swap cursor.
 *
 * <p>This class provides an list of T objects who's size is <em>unstable</em>.
 * It's intended for capturing data where the frequency of sampling greatly
 * outweighs the frequency of inspection (for instance, monitoring).</p>
 *
 * <p>This object keeps in memory a fixed size buffer which is used for
 * capturing objects.  It copies the objects to a snapshot array which may be
 * worked with.  The size of the snapshot array will vary based on the
 * stability of the array during the copy operation.</p>
 *
 * <p>Adding buffer to the buffer is <em>O(1)</em>, and lockless.  Taking a
 * stable copy of the sample is <em>O(n)</em>.</p>
 *
 * @author <a href="mailto:ssmccoy@blisted.org">Scott S. McCoy</a>
 */
public class ConcurrentCircularBuffer <T> {
    private final AtomicInteger cursor = new AtomicInteger();
    private final T[]      buffer;
    private final Class<T> type;

    /**
     * Create a new concurrent circular buffer.
     *
     * @param type The type of the array.  This is captured for the same reason
     * it's required by {@link java.util.List.toArray()}.
     *
     * @param bufferSize The size of the buffer.
     *
     * @throws IllegalArgumentException if the bufferSize is a non-positive
     * value.
     */
    public ConcurrentCircularBuffer (final Class <T> type, 
                                     final int bufferSize) 
    {
        if (bufferSize < 1) {
            throw new IllegalArgumentException(
                "Buffer size must be a positive value"
                );
        }

        this.type    = type;
        this.buffer = (T[]) new Object [ bufferSize ];
    }

    /**
     * Add a new object to this buffer.
     *
     * <p>Add a new object to the cursor-point of the buffer.</p>
     *
     * @param sample The object to add.
     */
    public void add (T sample) {
        buffer[ cursor.getAndIncrement() % buffer.length ] = sample;
    }

    /**
     * Return a stable snapshot of the buffer.
     *
     * <p>Capture a stable snapshot of the buffer as an array.  The snapshot
     * may not be the same length as the buffer, any objects which were
     * unstable during the copy will be factored out.</p>
     * 
     * @return An array snapshot of the buffer.
     */
    public T[] snapshot () {
        T[] snapshots = (T[]) new Object [ buffer.length ];
            
        /* Determine the size of the snapshot by the number of affected
         * records.  Trim the size of the snapshot by the number of records
         * which are considered to be unstable during the copy (the amount the
         * cursor may have moved while the copy took place).
         *
         * If the cursor eliminated the sample (if the sample size is so small
         * compared to the rate of mutation that it did a full-wrap during the
         * copy) then just treat the buffer as though the cursor is
         * buffer.length - 1 and it was not changed during copy (this is
         * unlikley, but it should typically provide fairly stable results).
         */
        int before = cursor.get();

        /* If the cursor hasn't yet moved, skip the copying and simply return a
         * zero-length array.
         */
        if (before == 0) {
            return (T[]) Array.newInstance(type, 0);
        }

        System.arraycopy(buffer, 0, snapshots, 0, buffer.length);

        int after          = cursor.get();
        int size           = buffer.length - (after - before);
        int snapshotCursor = before - 1;

        /* Highly unlikely, but the entire buffer was replaced while we
         * waited...just use the buffer and reset the cursor to
         * the buffer size - 1. */
        if (size <= 0) {
            size           = buffer.length;
            snapshotCursor = size - 1;
        }

        int start = snapshotCursor - (size - 1);
        int end   = snapshotCursor;

        if (snapshotCursor < snapshots.length) {
            size   = snapshotCursor + 1;
            start  = 0;
        }

        /* Copy the sample snapshot to a new array the size of our stable
         * snapshot area.
         */
        T[] result = (T[]) Array.newInstance(type, size);

        int startOfCopy = start % snapshots.length;
        int endOfCopy   = end   % snapshots.length;

        /* If the buffer space wraps the physical end of the array, use two
         * copies to construct the new array.
         */
        if (startOfCopy > endOfCopy) {
            System.arraycopy(snapshots, startOfCopy,
                             result, 0, 
                             snapshots.length - startOfCopy);
            System.arraycopy(snapshots, 0,
                             result, (snapshots.length - startOfCopy),
                             endOfCopy + 1);
        }
        else {
            /* Otherwise it's a single continuous segment, copy the whole thing
             * into the result.
             */
            System.arraycopy(snapshots, startOfCopy,
                             result, 0, endOfCopy - startOfCopy + 1);
        }

        return (T[]) result;
    }

    /**
     * Get a stable snapshot of the complete buffer.
     *
     * <p>This operation fetches a snapshot of the buffer using the algorithm
     * defined in {@link snapshot()}.  If there was concurrent modification of
     * the buffer during the copy, however, it will retry until a full stable
     * snapshot of the buffer was acquired.</p>
     *
     * <p><em>Note, for very busy buffers on large symmetric multiprocessing
     * machines and supercomputers running data processing intensive
     * applications, this operation has the potential of being fairly
     * expensive.  In practice on commodity hardware, dualcore processors and
     * non-processing intensive systems (such as web services) it very rarely
     * retries.</em></p>
     *
     * @return A full copy of the internal buffer.
     */
    public T[] completeSnapshot () {
        T[] snapshot = snapshot();

        /* Try again until we get a snapshot that's the same size as the
         * buffer...  This is very often a single iteration, but it depends on
         * how busy the system is.
         */
        while (snapshot.length != buffer.length) {
            snapshot = snapshot();
        }

        return snapshot;
    }

    /**
     * The size of this buffer.
     */
    public int size () {
        return buffer.length;
    }
}
