package it.accur.util;

import java.util.Properties;
import java.io.IOException;
import java.io.InputStream;

/**
 * A collection of utilities for dealing with classpaths and classpath
 * resources.
 */
public class Classpaths {
    /**
     * Load a provided list of properties in an override-chain.
     *
     * <p>Given a list of filenames in order of override-level, create a chain
     * of properties objects and populate each object with the contents of the
     * provided filenames.</p>
     *
     * <p><em>Note, this method treats the first filename as a required
     * resource.</em></p>
     *
     * @return <code>null</code> if the first resource was not found, a {@link
     * Properties} object otherwise.
     */
    public static Properties loadProperties (final String ... filenames)
    throws IOException {
        Properties properties = new Properties();

        for (String filename : filenames) {
            if (filename == null) {
                throw new IllegalArgumentException("Filenames may not be null");
            }

            properties = new Properties(properties);

            boolean found = populateProperties(properties, filename);

            /* The first file is required, the override chain is optional. */
            if (filename == filenames[0] && !found) {
                return null;
            }
        }

        return properties;
    }

    /**
     * Populate a properties object with the contents of a classpath resource.
     *
     * <p>Populates the given properties object with the classpath resource of
     * the supplied filename.  Includes passive behavior of
     * <code>required</code> is <code>false</code>, but <em>fail-fast</em>
     * behavior is <code>required</code> is <code>true</code>.</p>
     *
     * @param classloader The classloader to load the properties chain from.
     * @param properties The {@link Properties} object to populate.
     * @param filename The filename to locate.
     *
     * @return <code>true</code> if the resource was successfully loaded.
     */
    public static boolean populateProperties (final ClassLoader classloader,
                                              final Properties  properties,
                                              final String      filename)
    throws IOException {
        InputStream stream = classloader.getResourceAsStream(filename);

        if (stream != null) {
            try {
                properties.load(stream);

                return true;
            }
            finally {
                stream.close();
            }
        }

        return false;
    }

    /**
     * Populate a properties object with the contents of a classpath resource.
     *
     * <p>Populates the given properties object with the classpath resource of
     * the supplied filename.  Includes passive behavior of
     * <code>required</code> is <code>false</code>, but <em>fail-fast</em>
     * behavior is <code>required</code> is <code>true</code>.</p>
     *
     * @param properties The {@link Properties} object to populate.
     * @param filename The filename to locate.
     *
     * @return <code>true</code> if the resource was successfully loaded.
     */
    public static boolean populateProperties (final Properties properties, 
                                              final String     filename)
    throws IOException {
        return populateProperties(Classpaths.class.getClassLoader(),
                                  properties, filename);
    }
}
