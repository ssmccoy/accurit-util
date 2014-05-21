package it.accur.util;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.EOFException;

/**
 * A utility class for handling externalization.
 *
 * <p>This utility class provides utility methods with functionality that
 * arises commonly in implementing {@link java.io.Externalizable}
 * serialization.  This includes converting to and from common JDK objects
 * (such as {@link Date}) and handling predictable reads and writes of various
 * larger data types.</p>
 */
public final class External {
    private External () { /* NOOP */ }

    /**
     * Fill the given array with content from the given source.
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
                                  final ObjectInput input)
    throws IOException, EOFException {
        int read = input.read(buffer);

        if (read >= 0) {
            while (read < buffer.length) {
                int chunk = input.read(buffer, read, buffer.length - read);

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
     * Read bytes into an array.
     *
     * <p>Given an input source, read the input into a byte array.  The format
     * must be written using {@link #writeBytes(output, byte[])}.</p>
     *
     * @param input An input stream.
     * @return A byte array representing the contents.
     * @throws EOFException If there was insufficient data in the stream.
     * @throws IOException  If an underlying error occurs while reading.
     */
    public static byte[] readBytes (final ObjectInput input)
    throws IOException, EOFException {
        byte[] content = new byte [ input.readInt() ];

        readBytes(content, input);

        return content;
    }

    /**
     * Using the given output target, write the given content.
     *
     * <p>Writes the given content with a four byte (int) header indicating the
     * size of the following content.  The four byte header is read in the same
     * order when using {@link #readBytes(ObjectInput)} or 
     * {@link #readBytes(byte[],ObjectInput)}.</p>
     *
     * @param output The output target.
     * @param content The byte array to write.
     * 
     * @throws IOException If any underlying error occurs.
     */
    public static void writeBytes (final ObjectOutput output,
                            final byte[] content)
    throws IOException {
        output.writeInt(content.length);
        output.write(content);
    }

    /**
     * Write a string which may have a null value.
     *
     * <p>Given an output stream and a string, write the string to the given
     * output stream, prefixed by a boolean indicating whether or not the
     * string was actually <code>null</code>.</p>
     *
     * @param output The output 
     */
    public static void writeNullableUTF (final ObjectOutput output,
                                         final String       string)
    throws IOException {
        boolean write = string != null;

        output.writeBoolean(write);

        if (write) {
            output.writeUTF(string);
        }
    }

    /**
     * Read a string which may have a null value.
     *
     * <p>Given an input stream, read from it a boolean value indicating
     * whether or not the string is actually present, and then the string
     * itself.</p>
     *
     * @param input An input stream.
     * @return A string, or null if one was not present.
     */
    public static String readNullableUTF (final ObjectInput input)
    throws IOException {
        if (input.readBoolean()) {
            return input.readUTF();
        }
        else {
            return null;
        }
    }
}
