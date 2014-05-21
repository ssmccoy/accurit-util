package it.accur.util;

import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;

import java.util.Properties;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import it.accur.util.concurrent.AtomicInitializer;

/**
 * This is an abstract service-locator for enhanced Service Provider
 * Interfaces.
 *
 * <p>This class provides the abilty to load, with a varying strategy and with
 * overridden configurations, factory objects for service provider interfaces.
 * </p>
 *
 * @author <a href="mailto:ssmccoy@blisted.org">Scott S. McCoy</a>
 */
public abstract class AbstractFactoryFinder <T> {
    private AtomicInitializer initializer    = new AtomicInitializer();
    private String            name;
    private String            configFilename;
    private String            fallback;
    private Class <?>         factoryClass;
    private Class <T>         type;

    private Properties properties = new Properties(System.getProperties());

    /**
     * Create a new abstract factory finder.
     *
     * <p>All subclasses of this class <em>must</em> call this constructor for
     * any features of this class to work correctly.</p>
     *
     * @param type The expected type this factory will create.
     * @param name The property name of the interface (typically the same as
     * <code>type.getName()</code>).
     * @param configFilename The name of the configuration file to search the
     * classpath for.
     * @param fallback The name of a fallback factory if no factory can be
     * found.
     */
    protected AbstractFactoryFinder (final Class <T> type,
                                     final String name,
                                     final String configFilename,
                                     final String fallback) 
    {
        this.type           = type;
        this.name           = name;
        this.configFilename = configFilename;
        this.fallback       = fallback;
    }

    /**
     * Create an object.
     *
     * <p>Given a class, create an object.  This traps any unexpected 
     * exceptions (which can't responsibly happen in this SPI) as fail-fast
     * system errors.</p>
     *
     * @return An instance of an object of the given class.
     * @throws ConfigurationError If the factory is inaccessible or internally
     * failing.
     */
    protected <E> E createObject (Class <E> cls) 
    throws ConfigurationError {
        try {
            return cls.newInstance();
        }
        /* If either of these happen, try the default */
        catch (InstantiationException exception) {
            throw new ConfigurationError(
                "Unable to create object from class " + cls.getName(),
                exception
                );
        }
        catch (IllegalAccessException exception) {
            throw new ConfigurationError(
                "Unable to access default constructor for class " +
                cls.getName(), exception
                );
        }
    }

    /**
     * Find a class by name.
     *
     * <p>Given a class name, this causes class not found as a fail-fast system
     * error.  The SPI is explicitly designed <em>NOT</em> to recover cleanly
     * from service providers which misregister themselves.</p>
     *
     * @param name The name of a class.
     * @return An instance of the given class name.
     */
    protected Class <?> getClass (String name) 
    throws ConfigurationError {
        try {
            return Class.forName(name);
        }
        catch (ClassNotFoundException exception) {
            throw new ConfigurationError( 
                "Unable to find class \"" + name + "\"", exception 
                );
        }
    }

    /**
     * Find an object of the given class based on the configuration of thsi
     * factory finder.
     *
     * <p>Creates an object of the given class (tested, and casted) based on
     * the configuration of this factory finder.  Checks for a registered
     * service provider in the following places (in the following order):
     *
     * <ol>
     *  <li>${java.home}/lib/${configFilename}</li>
     *  <li>System property</li>
     *  <li>Classpath file in "services/"</li>
     * </ol>
     *
     * @throws ConfigurationError If any objects misrepresent themselves or
     * configurations point to classes which are unavailable, protected, or
     * contain errors.
     *
     * @return An object of the given type, or null if none was available.
     */
    protected T find ()
    throws ConfigurationError {
        if (initializer.isRequired()) {
            String filename = String.format(
                "%s%s%s%s%s", properties.getProperty("java.home"),
                File.separator, "lib", File.separator, configFilename);

            File file = new File(filename);

            if (file.exists()) {
                try {
                    InputStream stream = new FileInputStream(file);

                    try {
                        properties.load(stream);
                    }
                    finally {
                        stream.close();
                    }
                }
                catch (IOException exception) {
                    /* Just continue... */
                }
            }

            ClassLoader loader = AbstractFactoryFinder.class.getClassLoader();

            String className = properties.getProperty(name);

            if (className == null) {
                InputStream stream = loader.getResourceAsStream(
                    String.format("META-INF/services/%s", name)
                    );

                if (stream != null) {
                    try {
                        BufferedReader reader = new BufferedReader(
                            new InputStreamReader(stream, "UTF-8"));

                        try {
                            className = reader.readLine();
                        }
                        finally {
                            reader.close();
                        }
                    }
                    catch (IOException exception) {
                        throw new ConfigurationError(
                            "Unexpected I/O error while reading from file",
                            exception);
                    }
                }
            }

            if (className != null) {
                factoryClass = getClass(className);
            }
            else if (fallback != null) {
                factoryClass = getClass(fallback);
            }

            initializer.complete();
        }

        if (factoryClass != null) {
            if (type.isAssignableFrom(factoryClass)) {
                Object factory = createObject(factoryClass);

                /* Unable to create the class for some reason, try the default
                 */
                if (factory == null &&
                    !factoryClass.getName().equals(fallback)) 
                {
                    factory = createObject(getClass(fallback));
                }

                return type.cast(factory);
            }
            else {
                String message = String.format(
                    "The located type %s is not a subclass of expected type %s",
                    factoryClass.getName(), type.getName()
                    );

                throw new ConfigurationError(message);
            }
        }

        return null;
    }

    /**
     * A configuration error occured with the Factory Finder SPI.
     *
     * <p>This error is thrown when a service provider misrepresents or
     * misregisters itself through classpath resources.  It is
     * non-recoverable.</p>
     *
     * @author <a href="mailto:ssmccoy@blisted.org">Scott S. McCoy</a>    
     */
    public static class ConfigurationError
    extends Error {
        ConfigurationError (String message) {
            super(message);
        }

        ConfigurationError (String message, Throwable cause) {
            super(message, cause);
        }
    }
}
