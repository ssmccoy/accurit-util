package it.accur.util;
import java.io.ByteArrayOutputStream;

/**
 * An unsafe version of {@link ByteArrayOutputStream}.
 *
 * <p>This subclass of {@link ByteArrayOutputStream} simply exposes the
 * protected members of {@link ByteArrayOutputStream}.  It is called
 * <em>Unsafe</em> because it is unsafe to use the values returned by {@link
 * #getLength()} and {@link getByteArray()} if the buffer is still being
 * written to.</p>
 */
public class UnsafeByteArrayOutputStream 
extends ByteArrayOutputStream {
    public UnsafeByteArrayOutputStream () {
        super();
    }

    public UnsafeByteArrayOutputStream (int size) {
        super(size);
    }

    public int getLength () {
        return super.count;
    }

    public byte[] getByteArray () {
        return super.buf;
    }
}
