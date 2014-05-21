package it.accur.util;
import org.testng.annotations.Test;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

@Test(groups={"unit"}, testName = "ByteBufferInputStream Validation Test")
public class ByteBufferInputStreamTest {
    private static final byte[] td = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes();

    /**
     * Verify behavior is comparable to ByteArrayInputStream.
     *
     * <p>This reproduces a bug which occurred when {@link
     * ByteBufferInputStream#read()} returned a byte that was cast to int
     * unbounded.</p>
     */
    public void testStreamEquality () 
    throws IOException, ClassNotFoundException {
        ByteBuffer store = ByteBuffer.allocate(1024);

        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();

        ObjectOutputStream output = new ObjectOutputStream(byteOutput);

        /* 255 is a magic number that blows up because it causes 0xFFFFFFFF to
         * be returned from the input stream. */
        output.writeObject(255);

        output.close();

        byte[] serialized = byteOutput.toByteArray();

        ByteBuffer inputBuffer = ByteBuffer.wrap(serialized);

        store.put(inputBuffer);

        store.flip();

        byte[] test = new byte[serialized.length];

        store.get(test);

        ByteArrayInputStream byteInput = new ByteArrayInputStream(test);

        store.rewind();

        ByteBufferInputStream bufferInput = new ByteBufferInputStream(store);

        /* Verify byte-wise input is equivalent */
        for (int i = 0; i < serialized.length; i++) {
            int a = bufferInput.read(),
                b = byteInput.read();

            assert a == b : "Expected " + a + ", observed " + b;
        }

        store.rewind();

        ObjectInputStream byteObjectStream = new ObjectInputStream(
            new ByteArrayInputStream(test)
            );

        ObjectInputStream bufferObjectStream = new ObjectInputStream(
            new ByteBufferInputStream(store)
            );

        Integer a = (Integer) bufferObjectStream.readObject();
        Integer b = (Integer) byteObjectStream.readObject();

        assert a.equals(b) : "Expected " + a + ", observed " + b;
    }

    /**
     * Verify an array can pass through the input stream unmodified.
     */
    public void testDirect () 
    throws IOException {
        ByteBuffer  buffer = ByteBuffer.allocate(1024);

        byte[] tr = new byte [td.length];

        buffer.put(td);
        buffer.flip();

        /* Yikes, if you create the stream before you set the buffer limit, it
         * breaks since the remaining bytes is captured at creation time. */

        InputStream stream = new ByteBufferInputStream(buffer);

        int i = 0, c = -1;

        while ((c = stream.read()) > -1) {
            assert i <= tr.length : "More data read than written?";

            tr[i++] = (byte) c;
        }

        assert Arrays.equals(td, tr) : 
            "Data read does not match data written";
    }

    /**
     * Test the behavior of wrapping the end of a byte buffer.
     */
    public void testWrapped () 
    throws IOException {
        ByteBuffer  buffer = ByteBuffer.allocate(1024);

        byte[] tr = new byte [td.length];

        buffer.position(10);
        /* Put the first len - 10 items starting at 10.
         * Then flip the buffer and put the last 10 items at the beginning.
         * Then we should be position 10, limit 26, so we can test */

        buffer.put(td, 0, td.length - 10);
        buffer.flip();
        buffer.mark();
        buffer.put(td, td.length - 10, 10);

        assert buffer.position() == 10 : "Buffer is not where expected";

        /* Yikes, if you create the stream before you set the buffer limit, it
         * breaks since the remaining bytes is captured at creation time. */

        InputStream stream = new ByteBufferInputStream(buffer, td.length);

        int i = 0, c = -1;

        while ((c = stream.read()) > -1) {
            assert i <= tr.length : "More data read than written?";

            tr[i++] = (byte) (c & 0x000000FF);
        }

        assert Arrays.equals(td, tr) : 
            "Data read does not match data written (" +
            new String(tr) + " != " + new String(td) + ")";
    }
}
