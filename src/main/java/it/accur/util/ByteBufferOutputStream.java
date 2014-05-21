package it.accur.util;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;

/**
 * Simple extension of ByteArrayOutputStream which exposes underlying storage
 * as a ByteBuffer.
 *
 * <p>This subclass of {@link ByteArrayOutputStream} provides access to the
 * underlying byte array as a {@link ByteBuffer} without making a separate
 * copy.  Use this for fast access inbetween nio and stream-oriented APIs such
 * as {@link java.io.ObjectOutputStream}.</p>
 *
 * <p>This API is generally <em>unsafe</em>.  The {@link #asByteBuffer()}
 * method returns an object which will modify the underlying data-structures of
 * this object.  Use caution when using this object.</p>
 */
public class ByteBufferOutputStream 
extends OutputStream {
    private ByteBuffer buffer;
    private boolean    wrapped;

    /**
     * Create a new byte buffer output stream.
     *
     * <p>Create a new output stream which writes directly to the current
     * position of the provided byte buffer.</p>
     *
     * @param buffer The buffer this stream will write to.
     */
    public ByteBufferOutputStream (final ByteBuffer buffer) {
        this.buffer  = buffer;
        this.wrapped = false;
    }

    /**
     * Create a new byte buffer which may wrap it's output.
     *
     * <p>Create a new byte buffer which will wrap it's output when the end of
     * buffer mark is reached.</p>
     *
     * @param buffer The buffer this stream will write to.
     * @param wrapped A flag indicating whether or not the stream should wrap
     * it's output beyond the end of the buffer.
     */
    public ByteBufferOutputStream (final ByteBuffer buffer,
                                   final boolean wrapped)
    {
        this.buffer  = buffer;
        this.wrapped = wrapped;
    }

    /**
     * Write one byte to this byte buffer.
     *
     * <p>Given an <code>int</code> c write the lower byte of the value to the
     * bytebuffer this stream wraps.</p>
     *
     * @param c The byte to write.
     * @throws IOException If there is insufficient space in the buffer
     * to write.
     */
    public void write (final int c) 
    throws IOException {
        try {
            byte b = (byte) c;

            buffer.put(b);

            /* If we've hit the end of the buffer, and we're a wrapped buffer,
             * reset the buffer to continue writing. */
            if (wrapped && !buffer.hasRemaining()) {
                buffer.reset();
            }
        }
        catch (BufferOverflowException exception) {
            throw new IOException(
                "Unable to write to buffer, no space remaining",
                exception
                );
        }
    }
}
