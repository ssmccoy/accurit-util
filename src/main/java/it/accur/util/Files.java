package it.accur.util;

import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

/**
 * A utility class for performing general actions on {@link File} objects.
 *
 * <p>This utility class is akin to {@link java.util.Arrays}.  It is a static
 * utility class which may not be instantiated, and should contain
 * functionality relevant for working with files and {@link File} objects.</p>
 */
public final class Files {
    /** Static utility */
    private Files () {}

    /**
     * Slurp the full contents of a file into a string.
     *
     * <p>Given a {@link File}, open it and read it's entire contents into a
     * buffer.  Return the buffer (converted to a {@link String}).</p>
     *
     *
     * @param file The file to read.
     * @return The contents of the file.
     * @throws IOException If any underlying IOException occurs.
     */
    public static String slurp (final File file)
    throws IOException {
        InputStream stream = new FileInputStream(file);

        try {
            return Streams.slurp(stream);
        }
        finally {
            stream.close();
        }
    }

    /**
     * Slurp the full contents of a file into a string.
     *
     * <p>Given a filename, create a {@link File} object and delegate to {@link
     * #slurp(File)}.</p>
     *
     * @param filename The name or URL of the file.
     *
     * @return The file's contents.
     * @throws IOException If any underlying error occurs.
     */
    public static String slurp (final String filename) 
    throws IOException {
        return slurp(new File(filename));
    }

    /**
     * Copy a file from one location to another.
     *
     * <p>Given a source location <em>source</em> and a target location
     * <em>target</em>, copy the given source file to the destination target
     * file target.  Overwrite the target file if it already exists.</p>
     *
     * @param source The source file.
     * @param target The target file.
     *
     * @throws IOException If any error occurs opening the source or writing to
     * the target files.
     */
    public static void copy (final File source, final File target)
    throws IOException {
        FileInputStream inputStream   = new FileInputStream(source);
        FileChannel     sourceChannel = inputStream.getChannel();

        try {
            FileOutputStream outputStream  = new FileOutputStream(target);
            FileChannel      targetChannel = outputStream.getChannel();

            try {
                targetChannel.transferFrom(sourceChannel, 0,
                                           sourceChannel.size());
            }
            finally {
                outputStream.close();
            }
        }
        finally {
            inputStream.close();
        }
    }
}
