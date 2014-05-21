package it.accur.util;

/**
 * Utility class for dealing with objects.
 *
 * <p>This is a generic utility class abstract operations which sufficent for
 * all object types.</p>
 */
public final class Objects {
    private Objects () {}

    /**
     * Determine if two values are the same.
     *
     * <p>This test will return <code>true</code> if two objects are the same
     * instance, or if they are both not null and their {@link
     * Object#equals(Object)} method returns <code>true</code></p>
     *
     * @return <code>true</code> if they are the same or equivilent objects,
     * <code>false</code> if one of them is null or they are not equivilent.
     */
    public static boolean equals (final Object a, final Object b) {
        return (a == b || (a != null && a.equals(b)));
    }
}
