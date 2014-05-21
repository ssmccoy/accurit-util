package it.accur.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.EOFException;
import java.nio.ByteBuffer;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Simple extension of ByteArrayInputStream which exposes underlying storage
 * as a ByteBuffer.
 *
 * <p>This special input stream is a sister class to {@link
 * ByteArrayInputStream} however rather than fetching the input from a byte
 * array, it fetches it's input from a {@link ByteBuffer}.</p>
 *
 * <p>Use this class when mapping regions of a file to memory.</p>
 */
public class ByteBufferInputStream 
extends InputStream {
    private Logger log = LoggerFactory.getLogger( ByteBufferInputStream.class );
    private ByteBuffer buffer;
    private int        remaining;
    private boolean    wrapped;

    /**
     * Create a new byte buffer input stream.
     *
     * @param buffer The buffer to read from.
     */
    public ByteBufferInputStream (ByteBuffer buffer) {
        this.buffer    = buffer;
        this.remaining = buffer.remaining();
        this.wrapped   = false;
    }

    /**
     * Create a wrapped byte buffer input stream.
     *
     * <p>Create a special stream which wraps it's input beyond the current
     * buffer limit.  Once the end of the buffer is reached, the buffer will be
     * reset and reading will continue until <code>size</code> bytes are
     * read.</p>
     *
     * @param buffer The buffer to read from.
     * @param size   The number of bytes to read from the buffer.
     */
    public ByteBufferInputStream (ByteBuffer buffer, int size) {
        this.buffer    = buffer;
        this.remaining = size;
        this.wrapped   = true;
    }

    /**
     * Return the next byte available in the bytebuffer.
     *
     * <p>Return the next byte available in this byte buffer, or
     * <code>-1</code> if none exists.</p>
     *
     * @return The next available byte converted to an int, or <code>-1</code>
     * if no bytes are remaining in the buffer.
     */
    public int read () {
        if (remaining-- > 0) {
            if (wrapped && !buffer.hasRemaining()) {
                buffer.reset();
            }

            byte b = buffer.get();

            return (int) b & 0x000000FF;
        }
        else {
            return -1;
        }
    }
}
