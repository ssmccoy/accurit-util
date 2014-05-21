package it.accur.util;

import java.util.Date;

/**
 * A collection of utility functions for {@link Date}.
 *
 * <p>This namespace contains a collection of general utility functions and
 * constants for manipulating {@link Date} objects.</p>
 */
public final class Dates {
    /** 
     * The last date representable by the java date system. 
     *
     * <p>Guaranteed to be <code>&gt;&eq;</code> all other dates.</p>
     */
    public static final Date END_OF_TIME = new Date(Long.MAX_VALUE);

    private Dates () {}
}
