package it.accur.util;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collections;

public final class Sets {
    private Sets () {} 

    /**
     * Return a set difference of the given sets.
     *
     * <p>For the given sets <em>u</em> and <em>a</em>, return a set of all
     * members of <em>u</em> which are <u>not</u> members of <em>a</em>.</p>
     *
     * @param u A set.
     * @param a The complement set.
     *
     * @return The set u \ a.
     */
    public static <E> Set <E> difference (Set <E> u, Set <E> a) {
        Set <E> result = new HashSet <E> ();

        for (E e : u) {
            if (!a.contains(e)) {
                result.add(e);
            }
        }

        return result;
    }

    public static <T> Set <T> fromArray (T ... a) {
        return Collections.unmodifiableSet(new HashSet <T> (Arrays.asList(a)));
    }
}
