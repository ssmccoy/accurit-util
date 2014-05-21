package it.accur.util.concurrent;

import java.io.Serializable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.MappedByteBuffer;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import it.accur.util.ByteBufferOutputStream;
import it.accur.util.ByteBufferInputStream;
import it.accur.util.UnsafeByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A concurrent queue backed by a memory-mapped file buffer.
 *
 * <p>This implementation of {@link BlockingQueue} stores all values provided
 * to it in a memory-mapped buffer by serializing the provided objects to a
 * byte stream.  The queue is maintained as a <em>first in first out</em> list
 * who's entries are headed by their size.  The memory-mapped buffer is flushed
 * to a file at the platforms convenience.  The performance of this queue will
 * vary from platform to platform.</p>
 *
 * <p>This queue provides a non-ACID means for delivery that is
 * <em>generally</em> guaranteed.  During normal cases of system malfunction,
 * such as excessive load or exhaustion of limited resources such as
 * file handles or memory, this queue will be able to read the entires currently
 * stored in the queue between system outages.  During catastrophic failures
 * such as <em>sudden power failures</em>, this queue may lose data due to file
 * corruption.  It is therefore not suitable for guaranteeing delivery of
 * financial transactions.</p>
 *
 * <h3>Concurrency</h3>
 *
 * <p>This data acquisition for this queue is <em>storage bound</em>.  This
 * means that there is no intrinsic number of maximum elements which can be
 * stored in the queue, but there is a maximum amount of <em>data</em> which
 * can be stored.  The maximum amount of data will be the size of the allocated
 * buffer, as provided to the constructor {@link
 * #PersistentQueue(Class,File,int)}.  The buffer is used by serializing the
 * provided elements and copying the serialized bytes into the buffer.  If the
 * available storage for the buffer is exceeded, then calls to add new elements
 * to the queue block until sufficient storage becomes available.  Methods
 * which remove elements from this queue behave similarly to other {@link
 * BlockingQueue}s in that they will block until an element is added to the
 * queue.</p>
 *
 * <h3>Persistence</h3>
 *
 * <p>This queue is backed by a {@link MappedByteBuffer} which is opened on the
 * given {@link File}.  This buffer is infrequently flushed, allowing the
 * operating system to manage disk-write operations without intervention.  This
 * means that the disk image of the buffer is not ACID compliant.</p>
 *
 * <h3>Management/Allocation</h3>
 *
 * <p>The file areas are managed in "blocks" to ensure sufficient contingent
 * space remains available for each record to write a complete record-header.
 * The size of each <em>block</em> may be tuned but must be a minimum of 4
 * bytes (which is non-coincidentally, the size of each the record header).
 * Records written to this buffer will utilize ceil(({@link Integer.SIZE} /
 * {@link Bytes.SIZE} + sizeof element) / blocksize) blocks.</p>
 *
 * @see BlockingQueue
 */
public class PersistentQueue <E extends Serializable>
implements BlockingQueue <E> {
    /* Enough space for five ints: 
     * [fileSize][blockSize][count][head][tail] */
    private static final int INT_SIZE   = Integer.SIZE / Byte.SIZE;
    private static final int HEAD_SIZE  = INT_SIZE * 5;
    private static final int TERMINATOR = 0;

    private Logger log = LoggerFactory.getLogger( PersistentQueue.class );

    /* Semaphore maintains current value of the tail of the buffer (the space
     * between the current tail cursor and the tail of the buffer, or the
     * current tail cursor and the head of the buffer.
     */
    private Semaphore blocks;
    private Semaphore slots;

    private MappedByteBuffer map;

    /* Thread local buffers are maintained for each thread.  This allows
     * concurrent adjustment of the buffer position for multi-read and
     * pre-write conditions.  Writes obviously synchronize on the writeLock.
     */
    private ThreadLocal <ByteBuffer> local = 
        new ThreadLocal <ByteBuffer> () {
            protected ByteBuffer initialValue () {
                return map.duplicate();
            }
        };

    private Class <E> type;
    private int fileSize;
    private int blockSize;
    private int head;
    private int tail;
    private int count;
    private int firstUsableBlock;
    private Lock readLock;
    private Lock writeLock;

    {
        ReadWriteLock lock = new ReentrantReadWriteLock();

        readLock  = lock.readLock();
        writeLock = lock.writeLock();
    }

    /**
     * An interator for this queue.
     *
     * <p>This iterator does not 
     */
    private static class PersistentQueueIterator <E extends Serializable>
    implements Iterator <E> {
        private int head;
        private int tail;
        private PersistentQueue <E> queue;
        private ByteBuffer buffer;

        PersistentQueueIterator (final PersistentQueue <E> queue) {
            queue.readLock.lock();

            try {
                this.queue  = queue;
                this.buffer = queue.local.get().duplicate();
                this.head   = queue.head;
                this.tail   = queue.tail;

                buffer.position(queue.head);
            }
            finally {
                queue.readLock.unlock();
            }
        }

        public boolean hasNext () {
            return buffer.position() != tail;
        }

        /**
         * Return the next element in this queue.
         *
         * <p>This method will lock the queue for reading, and once the lock is
         * acquired, it will return the next element.  Each invocation of this
         * method validates the current positions on the queue to ensure no
         * modification has happened inbetween calls.</p>o
         *
         * @return The next element.
         * @throws ConcurrentModificationException If any modification to the
         * queue has occurred during iteration.
         */
        public E next () {
            queue.readLock.lock();

            try {
                if (queue.head != head ||
                    queue.tail != tail)
                {
                    throw new ConcurrentModificationException(
                        "The queue has been modified during iteration"
                        );
                }

                return PersistentQueue.deserialize(
                    queue.type, buffer, buffer.getInt()
                    );
            }
            finally {
                queue.readLock.unlock();

                buffer.position( 
                    queue.normalize( buffer.position() )
                    );
            }
        }

        public void remove () {
            /* */
        }
    }

    /**
     * Write the file header.
     *
     * <p>Given a buffer, write the file header.  The position of the buffer
     * will be unmodified after the write operation.</p>
     *
     * @param buffer The buffer to write the file header in.
     */
    private void storeHeader (ByteBuffer buffer) {
        writeLock.lock();

        try {
            int position = buffer.position();

            buffer.rewind();
            buffer.putInt(fileSize);
            buffer.putInt(blockSize);
            buffer.putInt(count);
            buffer.putInt(head);
            buffer.putInt(tail);

            /* Set the mark incase there is a gap (i.e. the header size is not
             * a multiple of the blocksize, or vice-versa).
             */
            buffer.position(firstUsableBlock);
            buffer.mark();

            buffer.position(position);
        }
        finally {
            writeLock.unlock();
        }
    }

    /**
     * The current size of the queue
     *
     * <p>The current size of the queue.</p>
     *
     * @return The current size of the queue.
     */
    public int size () {
        return slots.availablePermits();
    }

    /**
     * Convert the given byte-size into block-size.
     *
     * <p>Given some bytes, return the number of blocks the given number of
     * bytes consumes.  Pad the number of blocks accordingly.</p>
     *
     * @param size The number of bytes.
     */
    private int asBlocks (int size) {
        int blocks = size / blockSize;

        if ((size % blockSize) > 0) {
            blocks++;
        }

        return blocks;
    }

    /**
     * Normalize the given size by blocks.
     *
     * <p>Given a number of bytes <em>size</em>, normalize the value to the
     * nearest block-wise byte.</p>
     *
     * @param size The value to normalize.
     */
    private int normalize (int size) {
        return asBlocks(size) * blockSize;
    }

    /**
     * Convert the given block size to bytes.
     *
     * <p>Given a number of blocks, return the correlative number of bytes.</p>
     *
     * @param blocks The given number of blocks.
     */
    private int asBytes (int blocks) {
        return blocks * blockSize;
    }

    /**
     * Empty the queue.
     *
     * <p>Empty the queue, remitting all semaphores.</p>
     */
    public void clear () {
        /* Stop allocation to the queue */
        blocks.drainPermits();
        slots.drainPermits();

        writeLock.lock();

        try {
            count = 0;
            tail  = firstUsableBlock;
            head  = firstUsableBlock;

            ByteBuffer buffer = local.get();

            storeHeader(buffer);

            blocks.release( asBlocks(fileSize) - asBlocks(HEAD_SIZE) );
        }
        finally {
            writeLock.unlock();
        }
    }

    /**
     * Map a file to a persistent queue.
     * 
     * <p>This operation maps the given file to a persistent queue.  If the
     * file exists, it's expected to be in the format of this queue and have
     * previously been the same size currently supplied.  If it is not the
     * supplied size or if the file is not in the format written by this class,
     * then behavior is undefined.</p>
     *
     * <p>The type provided need not be the only concrete object type stored in
     * this queue.  It is used for reflective casting.  It must be a supertype
     * of all elements stored in the queue.</p>
     *
     * @param type The type of object stored in this queue.
     * @param file The file to map to memory.
     * @param blockSize The size of each "block" in the file.
     * @param fileSize The size of the file to allocate.
     */
    public PersistentQueue (final Class <E> type,
                            final File      file,
                            final int       blockSize,
                            final int       fileSize)
    throws FileNotFoundException, IOException {
        this.blockSize = blockSize;
        this.fileSize  = fileSize;
        this.type      = type;

        if (fileSize % blockSize != 0) {
            String message = String.format(
                    "Given file size %d is not a multiple of block size %d",
                    fileSize, blockSize
                    );

            throw new IllegalArgumentException(message);
        }

        if (blockSize < 4) {
            throw new IllegalArgumentException(
                "Given block size must be >= 4 bytes"
                );
        }

        if (fileSize < blockSize) {
            throw new IllegalArgumentException(
                "Size must have enough space for one block"
                );
        }

        if (asBlocks(fileSize) - asBlocks(HEAD_SIZE) <= 0) {
            throw new IllegalArgumentException(
                "Given sizes do not have enough blocks for a header"
                );
        }

        this.firstUsableBlock = normalize(HEAD_SIZE);

        boolean existing = file.exists();

        FileChannel channel = new RandomAccessFile(file, "rw").getChannel();

        this.map = channel.map(FileChannel.MapMode.READ_WRITE, 0, fileSize);

        if (existing) {
            map.position(0);

            /* Validate the buffer, then restore it */
            if (map.getInt() != fileSize) {
                throw new IllegalStateException(
                    "File size of buffer does not match existing file"
                    );
            }
            else if (map.getInt() != blockSize) {
                throw new IllegalStateException(
                    "Block size does not match existing file"
                    );
            }

            count = map.getInt();
            head  = map.getInt();
            tail  = map.getInt();

            map.position(firstUsableBlock);
            map.mark();
        }
        else {
            map.position(firstUsableBlock);

            count = 0;
            head  = firstUsableBlock;
            tail  = firstUsableBlock;

            /* Write the header incase no elements come -- storeHeader sets the
             * mark. */
            storeHeader(map);
        }

        this.slots  = new Semaphore(count);
        this.blocks = new Semaphore(asBlocks(fileSize) - asBlocks(HEAD_SIZE));
    }

    /**
     * Returns false.
     *
     * <p>Since objects are not deserialized from the underlying buffer until
     * they are requested, this method always returns <code>false</code>.</p>
     *
     * @param object An object.
     */
    public boolean contains (Object object) {
        return false;
    }

    /**
     * Return the next element in the queue.
     *
     * @return The next element in the queue, never <code>null</code>
     * @throws NoSuchElementException If no element exists.
     */
    public E element () 
    throws NoSuchElementException {
        E result = peek();

        if (result == null) {
            throw new NoSuchElementException("The queue is currently empty");
        }

        return result;
    }

    /**
     * Remove and return the next element in the queue.
     *
     * @return The next element in the queue, never <code>null</code>
     * @throws NoSuchElementException If no element exists.
     */
    public E remove () 
    throws NoSuchElementException {
        E result = poll();

        if (result == null) {
            throw new NoSuchElementException("The queue is currently empty");
        }

        return result;
    }

    /**
     * Determine if the queue is currently empty.
     *
     * @return <code>true</code> if the queue is empty, <code>false</code> otherwise.
     */
    public boolean isEmpty () {
        return count == 0;
    }
    
    /**
     * Read the next element from the current buffer position.
     *
     * <p>Given a buffer, and an expected size of the element, read the next
     * element from the current position of the buffer.  Wrap the end of the
     * buffer if necessary to attain the expected object size.</p>
     *
     * <p><strong>Note</strong> <em>This method is <code>static</code> to
     * ensure that it does not access local values.</em>
     *
     * @param buffer The buffer to read from.
     * @param size   The number of bytes to read before EOF.
     */
    private static <E> E deserialize (final Class <E> type,
                                      final ByteBuffer buffer,
                                      final int size)
    {
        try {
            ObjectInputStream serializer = new ObjectInputStream(
                new ByteBufferInputStream(buffer, size)
                );

            Object object = serializer.readObject();

            assert type.isInstance(object) :
                "Object is not appropriate type";

            return type.cast(object);
        }
        catch (IOException exception) {
            throw new IllegalStateException(
                "Error while reading from byte buffer",
                exception
                );
        }
        catch (ClassNotFoundException exception) {
            throw new IllegalStateException(
                "Found class which was not of type " + type,
                exception
                );
        }
    }

    /**
     * Return the first item of this queue.
     *
     * <p>This operation identifies and deserializes the current head of this
     * queue.  This will return an object of the same contents as {@link
     * #poll()} next time {@link #poll()} is called (however it will not be the
     * <em>same</em> object).</p>
     *
     * @return The element at the head of the queue, or <code>null</code> if
     * there are currently no items in the queue.
     */
    public E peek () {
        /* Before acquiring a lock, allow for early rejection.  If there are no
         * "slots" semaphores then we either have nothing in this queue or
         * pretty soon we will have nothing because all elements have been
         * claimed by some thread.
         */
        if (slots.availablePermits() == 0) {
            return null;
        }
        else {
            ByteBuffer buffer = local.get();

            readLock.lock();

            try {
                /* Double check that we still have something to read */
                if (count == 0)
                    return null;

                /* Copy the object out... */
                buffer.position(head);

                return deserialize(type, buffer, buffer.getInt());
            }
            finally {
                readLock.unlock();
            }
        }
    }

    /**
     * Remove the current head of the given buffer.
     *
     * <p>Given a working buffer, read the element at the current {@link #head}
     * position.  Adjust the current position of {@link #head} to point to the
     * next element.  Also reduce {@link #count}.</p>
     *
     * @param buffer The buffer to work with, expected to be thread-local.
     */
    private E removeHead (ByteBuffer buffer) {
        int size      = -1;

        writeLock.lock();
        readLock.lock();

        try {
            buffer.position(head);

            size     = buffer.getInt();

            int next      = normalize(buffer.position() + size);
            int bufferEnd = buffer.limit();

            /* If the next item is beyond the tail of the buffer, then calculate
             * "next" as being at the new buffer beginning... */
            if (next >= bufferEnd) {
                next = next - bufferEnd + firstUsableBlock;
            }

            /* Put the buffer back to it's appropriate position for the
             * next element read, and update the header.
             */
            buffer.position(head);

            head = next;
            count--;

            /* Update the header before demoting the lock.  This sets the
             * mark to guarantee writes wrap okay. */
            storeHeader(buffer);
        }
        finally {
            /* Demote to a read-lock, allowing other reads to happen if
             * possible. */
            writeLock.unlock();
        }

        try {
            return deserialize(type, buffer, buffer.getInt());
        }
        finally {
            readLock.unlock();

            int releaseBlocks = asBlocks(INT_SIZE + size);

            blocks.release(releaseBlocks);
        }
    }

    /**
     * Remove and return the first element in this queue, blocking as required.
     *
     * @return The first element in this queue.
     * @throws InterruptedException If the thread is interrupted while blocking.
     */
    public E take () 
    throws InterruptedException {
        slots.acquire();

        return removeHead(local.get());
    }

    /**
     * Try to remove the first element of this queue.
     *
     * @return The first element of this queue, or <code>null</code> if none is
     * available.
     */
    public E poll () {
        if (slots.tryAcquire()) {
            return removeHead(local.get());
        }
        else {
            return null;
        }
    }

    /**
     * Try to remove the first element of this queue within the given time
     * frame.
     *
     * @param timeout The number of {@link TimeUnit} units to wait.
     * @param unit The time unit of time for the timeout.
     *
     * @return The first element of this queue, or <code>null</code> if none is
     * available.
     */
    public E poll (long timeout, TimeUnit unit) 
    throws InterruptedException {
        if (slots.tryAcquire(timeout, unit)) {
            return removeHead(local.get());
        }
        else {
            return null;
        }
    }

    /**
     * Throw UnsupportedOperationException.
     *
     * @throws UnsupportedOperationException Under all conditions.
     */
    public boolean remove (final Object object) {
        throw new UnsupportedOperationException(
            "remove(Object) is not implemented"
            );
    }

    /**
     * Serialize the given object to a ByteBuffer.
     *
     * <p>The buffer will have a {@link java.nio.Buffer#limit()} of the size of
     * the serialized object.  It's {@link java.nio.Buffer#position()} will be
     * <code>0</code> and it's {@link java.nio.Buffer#mark()} will not be
     * set.</p>
     */
    private ByteBuffer serialize (E object) {
        try {
            UnsafeByteArrayOutputStream stream     = 
                new UnsafeByteArrayOutputStream();

            ObjectOutputStream          serializer = 
                new ObjectOutputStream(stream);

            serializer.writeObject(object);

            /* Return the array wrapped in a bytebuffer. */
            return ByteBuffer.wrap(stream.getByteArray(), 0, stream.getLength());
        }
        catch (IOException exception) {
            throw new IllegalStateException(
                "Error while writing to byte buffer",
                exception
                );
        }
    }

    /**
     * Write the bytes in the given incoming bytebuffer to the tail of the
     * given buffer.
     *
     * <p>Given a source and a destination buffer, writes the source buffer to
     * the destination buffer at the current position of the destination
     * buffer.  If the size of the source buffer is larger than the space
     * remaining in the destination buffer, the write is wrapped from the
     * buffer-end to the buffer's mark (see {@link ByteBuffer#mark()}).</p>
     *
     * @param buffer   The destination buffer.
     * @param incoming The source byte buffer.
     */
    private void writeTail (ByteBuffer buffer, ByteBuffer incoming) {
        writeLock.lock();

        try {
            int limit = incoming.limit();

            /* Write the size of the incoming buffer */
            buffer.putInt(limit);

            /* If this wraps the tail of the buffer, write the first
             * half...first.  Then rewind the buffer, and write the second
             * half. */
            if (buffer.remaining() < incoming.remaining()) {
                incoming.limit(buffer.remaining());

                buffer.put(incoming);

                buffer.reset();

                incoming.limit(limit);
            }

            buffer.put(incoming);

            /* Increase the count and add a new resource for acquisition. */
            count++;
            slots.release();
        }
        finally {
            writeLock.unlock();
        }
    }

    /**
     * Append the incoming buffer to the tail of this buffer.
     *
     * <p>Moves the local buffer cursor to the tail position and appends the
     * given buffer to the local buffer.  Resets the tail marker to the nearest
     * block-wise byte.</p>
     *
     * @param incoming The buffer to append to the tail of the queue's buffer.
     */
    private boolean appendTail (ByteBuffer incoming) {
        ByteBuffer buffer = local.get();

        buffer.position(firstUsableBlock);
        buffer.mark();

        writeLock.lock();

        try {
            buffer.position(tail);

            /* Occasionally, the tail will have been left at the buffer end.
             * That will cause a buffer overflow exception, so reset.
             */
            if (!buffer.hasRemaining()) {
                buffer.reset();
            }

            writeTail(buffer, incoming);

            /* Pad the tail to the nearest BLOCKSIZE count... */

            tail = normalize( buffer.position() );

            storeHeader(buffer);

            return true;
        }
        finally {
            writeLock.unlock();
        }
    }

    public boolean offer (E element) {
        ByteBuffer incoming    = serialize(element);
        int        size        = incoming.limit();
        int        requirement = asBlocks(size + INT_SIZE);

        if (!blocks.tryAcquire(requirement)) {
            return false;
        }

        return appendTail(incoming);
    }

    public boolean offer (E element, long timeout, TimeUnit unit)
    throws InterruptedException {
        ByteBuffer incoming    = serialize(element);
        int        size        = incoming.limit();
        int        requirement = asBlocks(size + INT_SIZE);

        if (!blocks.tryAcquire(requirement, timeout, unit)) {
            return false;
        }

        return appendTail(incoming);
    }

    public void put (E element) 
    throws InterruptedException {
        ByteBuffer incoming    = serialize(element);
        int        size        = incoming.limit();
        int        requirement = asBlocks(size + INT_SIZE);

        blocks.acquire(requirement);

        appendTail(incoming);
    }

    public boolean add (E e) {
        if (!offer(e)) {
            throw new IllegalStateException(
                "Insufficient blocks available for element"
                );
        }

        return true;
    }

    public boolean addAll (Collection <? extends E> c) {
        boolean result = false;

        for (E e : c) {
            result = add(e);
        }

        return result;
    }

    /**
     * Throw UnsupportedOperationException.
     *
     * @throws UnsupportedOperationException Under all conditions.
     */
    public boolean retainAll (Collection <?> c) 
    throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
            "Operation is not currently supported"
            );
    }

    /**
     * Throw UnsupportedOperationException.
     *
     * @throws UnsupportedOperationException Under all conditions.
     */
    public boolean removeAll (Collection <?> c) 
    throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
            "Operation is not currently supported"
            );
    }

    /**
     * Throw UnsupportedOperationException.
     *
     * @throws UnsupportedOperationException Under all conditions.
     */
    public boolean containsAll (Collection <?> c) 
    throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
            "Operation is not currently supported"
            );
    }

    /**
     * Throw UnsupportedOperationException.
     *
     * @throws UnsupportedOperationException Under all conditions.
     */
    public <T> T[] toArray (T[] a) {
        throw new UnsupportedOperationException(
            "Operation is not currently supported"
            );
    }

    /**
     * Throw UnsupportedOperationException.
     *
     * @throws UnsupportedOperationException Under all conditions.
     */
    public Object[] toArray () {
        throw new UnsupportedOperationException(
            "Operation is not currently supported"
            );
    }

    /**
     * Fetch an iterator for this queue.
     * 
     * <p>Fetch an iterator for this queue.  If the queue is modified
     * concurrently while being iterated, the behavior is undefined.</p>
     *
     * @return An iterator for this queue.
     */
    public Iterator <E> iterator () {
        return new PersistentQueueIterator <E> (this);
    }

    /**
     * Drain this queue into the given collection.
     *
     * <p>Drain all permits for this queue (causing it to no longer allow
     * additions).  Then locks the queue for writing and add <em>all</em> items
     * to the given collection.</p>
     *
     * @param collection The collection to drain the queue into.
     * @param size The size of the collection.
     */
    public int drainTo (final Collection <? super E> collection) {
        return drainTo(collection, Integer.MAX_VALUE);
    }

    /**
     * Drain this queue into the given collection.
     *
     * <p>Drain all permits for this queue (causing it to no longer allow
     * additions).  Then locks the queue for writing and add <em>size</em>
     * items in the queue to the given collection.</p>
     *
     * @param collection The collection to drain the queue into.
     * @param size The size of the collection.
     */
    public int drainTo (final Collection <? super E> collection,
                        final int size) {
        int permits  = slots.drainPermits();

        writeLock.lock();

        int elements = Math.min(permits, size);

        try {
            /* If we used "size" to limit the number of items to drain, then
             * remit the remaining permits. */
            if (size < elements) {
                slots.release(permits - size);
            }

            ByteBuffer buffer = local.get();

            for (int i = 0; i < elements; i++) {
                collection.add(removeHead(buffer));
            }

            return elements;
        }
        finally {
            writeLock.unlock();
        }
    }

    /**
     * Return the remaining capacity of this queue.
     *
     * <p>Because this is a memory-bound queue as opposed to a reference-bound
     * queue, method always returns {@link Integer#MAX_VALUE}.</p>
     *
     * @return {@link Integer#MAX_VALUE}
     */
    public int remainingCapacity () {
        return Integer.MAX_VALUE;
    }

    /**
     * Flush this buffer's current state to disk.
     *
     * <p>Flush the current state of this buffer to disk.  <strong>This method
     * does not syncrhonize</strong>, meaning that if <em>any</em> concurrent
     * modifications are made to the underlying buffer they may be partially
     * reflected.  It is expected that the consumer of this queue manages the
     * concurrent update externally.</p>
     */
    public void flush () {
        map.force();
    }
}
