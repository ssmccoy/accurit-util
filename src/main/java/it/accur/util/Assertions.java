package it.accur.util;

/**
 * Static assertion methods.
 *
 * <p>This static class contains basic methods for performing frequently
 * required assertions, such as those used for validating arguments.</p>
 */
public class Assertions {
    private Assertions () {
    }

    /**
     * Assert the given object is not null.
     *
     * @param object The object in question.
     * @param type A suitable name for use in the error message.
     *
     * @throws IllegalArgumentException When the given value is null, with a
     * message <code>type + " is a required argument"</code>
     */
    public static void notNullArgument (final Object object, 
                                        final String type)
    throws IllegalArgumentException {
        if (object == null) {
            throw new IllegalArgumentException(
                type + " is a required argument");
        }
    }
}
