package it.accur.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.ByteBuffer;

/**
 * A utility class for dealing with streams.
 */
public final class Streams {
    /** Static utility class. */
    private Streams () {}

    /**
     * Read the contents of a stream as a string.
     *
     * @param input The input stream to read from.
     * @return A string of the contents of the file.
     * @throws IOException When any underlying error occurs.
     */
    public static String slurp (final InputStream input) 
    throws IOException {
        StringBuilder result = new StringBuilder();

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(input)
            );

        try {
            char[] buf = new char[1024];

            int r = 0;

            while ((r = reader.read(buf)) != -1) {
                result.append(buf, 0, r);
            }
        }
        finally {
            reader.close();
        }

        return result.toString();
    }

    /**
     * Read bytes into a dynamically sized byte array.
     *
     * <p>Given an input stream, read the input into a dynamically sized byte
     * array in chunks by reading up to <code>8094</code> bytes at a time and
     * allocating new storage space upon each read.</p> 
     *
     * @param input An input stream.
     * @return A byte array representing the contents.
     */
    public static byte[] readBytes (final InputStream input)
    throws IOException {
        byte[] result = new byte [0];
        byte[] buffer = new byte [8094];

        int r = 0;

        while ((r = input.read(buffer)) >= 0) {
            if (r > 0) {
                byte[] target = new byte [ result.length + r ];

                System.arraycopy(result, 0, target, 0, result.length);
                System.arraycopy(buffer, 0, target, result.length, r);

                result = target;
            }
        }

        return result;
    }

    /**
     * Fill the given array with content from the given stream.
     *
     * <p>Given an array and an input stream, read from the input stream until
     * the array is filled.  Block until all bytes are read, or an error
     * occurs.</p>
     *
     * @param buffer The byte array buffer to read into.
     * @param input  The input stream to read from.
     * @throws EOFException If there was insufficient data in the stream.
     * @throws IOException  If an underlying error occurs while reading.
     */
    public static void readBytes (final byte[] buffer,
                                  final InputStream stream)
    throws IOException, EOFException {
        int read = stream.read(buffer);

        if (read >= 0) {
            while (read < buffer.length) {
                int chunk = stream.read(buffer, read, buffer.length - read);

                if (chunk >= 0) {
                    read += chunk;
                }
                else {
                    throw new EOFException(
                        "Insufficient data in stream to fill buffer"
                        );
                }
            }
        }
        else {
            throw new EOFException(
                "The given stream has no contents"
                );
        }
    }

    /**
     * Copy the contents of an input stream to an output stream.
     *
     * <p>Given an input stream <em>source</em> and an output stream
     * <em>target</em>, copy the contents of source to target.  This method
     * makes no assuptions about buffering or resource handling.  If buffered
     * streams are not present, consider wrapping the provided streams in
     * buffered streams.</p>
     *
     * <p>This method does not close any streams, since it does not open them.
     * Be sure to handle resource clean up where resources are allocated.</p>
     *
     * @param source The source input stream.
     * @param target The destination output stream.
     *
     * @throws IOException If any error occurs in reading from or writing to
     * the stream.
     */
    public static void copy (final InputStream source,
                             final OutputStream target)
    throws IOException {
        ReadableByteChannel input  = Channels.newChannel(source);

        try {
            WritableByteChannel output = Channels.newChannel(target);

            try {
                ByteBuffer buffer = ByteBuffer.allocate(4096);

                while (input.read(buffer) != -1) {
                    buffer.flip();

                    output.write(buffer);

                    buffer.compact();
                }

                buffer.flip();

                while (buffer.hasRemaining()) {
                    output.write(buffer);
                }
            }
            finally {
                output.close();
            }
        }
        finally {
            input.close();
        }
    }
}
