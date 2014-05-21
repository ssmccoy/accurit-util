package it.accur.util;
import java.util.Iterator;

public final class Iterables {
    private Iterables () {}

    /**
     * Concatenate a given iterable collection by a delimiter.
     *
     * <p>Given a string delimiter and a list of objects, concatenate the list
     * into a single string of the string representation of each item in the
     * list, separated by the delimiter, with no trailing delimiter at the
     * end.</p>
     * 
     * <pre>
     * "foo,bar,baz".equals(Lists.join(Arrays.asList("foo", "bar", "baz", ",")))
     * </pre>
     *
     * @param list The list to join.
     * @param delimiter The delimiter.
     * 
     * @return The joined list as a string.
     */
    public static <T> String join (final Iterable <T> iterable,
                                   final String delimiter)
    {
        StringBuilder builder = new StringBuilder();

        Iterator <T> iterator = iterable.iterator();

        boolean incomplete = iterator.hasNext();

        while (incomplete) {
            T element = iterator.next();
            
            if (element != null) {
                builder.append(element.toString());
            }

            incomplete = iterator.hasNext();

            if (incomplete) {
                builder.append(delimiter);
            }
        }

        return builder.toString();
    }
}
