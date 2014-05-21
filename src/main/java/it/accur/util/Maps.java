package it.accur.util;
import java.util.HashMap;
import java.util.Map;

/**
 * A set of utilities for creating and manipulating maps.
 */
public class Maps {
    private Maps () {}

    /**
     * Build a map from a balanced array of objects.
     *
     * <p>Given a <em>balanced</em> array (having an even number of elements),
     * use every even element, beginning at zero, as the key and every odd
     * element, beginning at one, as a corresponding value in a hashset.</p>
     *
     * <p>For entries which don't have type key as value, use {@link
     * build(Pair)}</p>
     *
     * @param objects A balanced array of objects.
     * @return A map built from the provided array.
     */
    public static <E> Map <E, E> build (final E ... objects) {
        if (objects.length % 2 != 0) {
            throw new IllegalArgumentException(
                "Can only build a map from a balanced array"
                );
        }

        Map <E, E> result = new HashMap <E, E> ();

        for (int i = 0; i < objects.length; i += 2) {
            result.put(objects[i], objects[i + 1]);
        }

        return result;
    }

    /**
     * Build a map from a balanced array of objects.
     *
     * <p>Given an array of {@link Pair} values, return a similarly typed
     * {@link Map} constructed using the left value of each {@link Pair} as a
     * key, and the right value as a value.</p>
     *
     * @param objects A balanced array of key-value pairs.
     * @return A map built from the provided array.
     */
    public static <K, V> Map <K, V> build (final Pair <K, V> ... pairs) {
        Map <K, V> result = new HashMap <K, V> ();

        for (Pair <K, V> pair : pairs) {
            result.put(pair.left(), pair.right());
        }

        return result;
    }
}
