package it.accur.util;
import java.net.URLEncoder;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.io.UnsupportedEncodingException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Arrays;

/**
 * A collection of utility routines for building and working with {@link URL}
 * objects.
 */
public class URLs {
    /** No-op constructor. */
    private URLs () {}

    private static void buildQuery (final StringBuilder builder,
                                    final String ... params)
    {
        try {
            Iterator <String> iterator = Arrays.asList(params).iterator();

            /* This should currently be guaranteed to have an even number of
             * items
             */
            int param = 0;

            while (iterator.hasNext()) {
                String key   = iterator.next();
                String value = iterator.next();

                param++;

                if (key == null) {
                    throw new IllegalArgumentException(
                        "Unable to create a parameter with a null key: " +
                        "parameter " + param
                        );
                }

                if (value != null) {
                    builder.append(URLEncoder.encode(key, "UTF-8"));
                    builder.append("=");
                    builder.append(URLEncoder.encode(value, "UTF-8"));

                    if (iterator.hasNext()) {
                        builder.append("&");
                    }
                }
            }
        }
        catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("Platform does not support UTF-8",
                                            exception);
        }
    }

    public static URL build (final String url) {
        /* I don't believe all exceptions should be runtime exceptions, but
         * this one definitely should be.  Nothing says for sure we'll get
         * invalid input, input can be validated independently by the caller...
         */
        try {
            return new URL(url);
        }
        catch (MalformedURLException exception) {
            throw new IllegalArgumentException(
                "The given URL was invalid"
                );
        }
    }

    public static String buildQuery (final String ... params) {
        if (params.length % 2 != 0) {
            throw new IllegalArgumentException(
                "URLs.buildQuery() requires a balanced list of parameters"
                );
        }

        StringBuilder result = new StringBuilder();

        buildQuery(result, params);
        
        return result.toString();
    }

    public static URLConnection postForm (final URL url,
                                          final String ... parameters)
    throws IOException {
        if (parameters.length % 2 != 0) {
            throw new IllegalArgumentException(
                "Method expects a balanced list of parameters"
                );
        }

        String postBody = buildQuery(parameters);

        URLConnection connection = url.openConnection();

        if (connection instanceof HttpURLConnection) {
            HttpURLConnection http = (HttpURLConnection) connection;

            http.setRequestMethod("POST");
            http.setRequestProperty(
                "Content-Type", "application/x-www-form-urlencoded");

            http.setDoOutput(true);

            OutputStream stream = http.getOutputStream();
            Writer       writer = new OutputStreamWriter(stream);

            try {
                writer.write(postBody);
            }
            finally {
                writer.close();
            }

            return connection;
        }

        throw new IllegalArgumentException(
            "Cannot post to a non-http URL"
            );
    }

    public static URL build (final String url,
                             final String ... params)
    {
        if (params.length % 2 != 0) {
            throw new IllegalArgumentException(
                "URLs.build() requires a balanced list of parameters"
                );
        }

        StringBuilder builder = new StringBuilder(url);

        if (url.contains("?")) {
            builder.append("&");
        }
        else {
            builder.append("?");
        }

        buildQuery(builder, params);

        try {
            return new URL(builder.toString());
        }
        catch (MalformedURLException exception) {
            throw new IllegalArgumentException("Not a valid URL: " + url,
                                               exception);
        }
    }

    /* TODO 2011-04-21T21:56:45Z-0700
     * This will need to be removed, it no longer makes sense now that I've
     * realized I misunderstood the java.net.URL API
     */
    public static URL build (final URL url,
                             final String ... params)
    {
        return build(url.toString(), params);
    }
}
