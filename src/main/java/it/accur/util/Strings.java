package it.accur.util;

import java.util.regex.Pattern;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Utility functions for String objects.
 *
 * <p>This is a utility class which provides some helper functions for use with
 * {@link String} objects.</p>
 */
public final class Strings {
    private static final Pattern CSVWS = Pattern.compile(
        "\\s*(?<!\\\\),\\s*"
        );
    private static final Pattern EQWS  = Pattern.compile(
        "\\s*(?<!\\\\)=\\s*"
        );

    private static final String CSVESC = "\\,";
    private static final String CSV    = ",";
    private static final String EQESC  = "\\=";
    private static final String EQ     = "=";

    /**
     * Disallow creation.
     * <p>In line with {@link java.util.Arrays} and similar primative
     * pluralisms, this class is a static utility class.  It may not be
     * instantiated or overridden.</p>
     */
    private Strings () {}

    /**
     * Determine if the given string is all digits.
     *
     * <p>Determine if all characters in the sequence digit characters.  A
     * digit character is a character in the range of between <code>0</code>
     * and <code>9</code>.  Defers to {@link Character#isDigit}.</p>
     *
     * @return <code>true</code> if all characters in the sequence are numeric,
     * <code>false</code> otherwise.
     */
    public static boolean isDigits (CharSequence characters) {
        int length = characters.length();

        for (int i = 0; i < length; i++) {
            if (!Character.isDigit(characters.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Determine if the given character sequence contains a number value.
     *
     * <p>This method will test the given character string to ensure it
     * contains only <code>0<code>-<code>9</code>, the character <code>.</code>
     * and optionally begins with the character <code>-</code>.</p>
     *
     * <p>Character sequences which pass this test should always successfully
     * pass <code>parseInt</code>, <code>parseLong</code>,
     * <code>parseDouble</code> and <code>parseFloat</code>.</p>
     *
     * @param characters The character sequence.
     *
     * @return <code>true</code> if this string is a number, <code>false</code>
     * otherwise.
     */
    public static boolean isNumber (CharSequence characters) {
        int     length      = characters.length();
        boolean decimalSeen = false;

        for (int i = 0; i < length; i++) {
            char c = characters.charAt(i);

            if (!Character.isDigit(c)) {
                if (c == '.') {
                    if (decimalSeen) {
                        return false;
                    }
                    else {
                        decimalSeen = true;
                    }
                }
                else if (!(i == 0 && c == '-')) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Determine if the given character sequence has content.
     *
     * @return <code>true</code> if the character sequence is not
     * <code>null</code>, and not zero length.
     */
    public static boolean isEmpty (CharSequence characters) {
        return characters == null || characters.length() == 0;
    }


    /**
     * Create a list of strings from a comma-separated input.
     *
     * <p>This simple utility method creates a list from an input string of
     * comma-separated values.  Neither this method, nor {@link #asSet(String)}
     * support quotes or escaping.</p>
     *
     * @param input The comma separated list ("1, 2, 3").
     * @return A new list (e.g. {1,2,3}).
     */
    public static List <String> asList (final String input) {
        String[] result = CSVWS.split(input.trim());

        for (int i = 0; i < result.length; i++) {
            result[i] = result[i].replace(CSVESC, CSV);
        }

        return Arrays.asList(result);
    }

    /**
     * Create a map of strings from a comma-separated list of key value pairs.
     *
     * <p>Given a comma-seprated list of key value pairs (denoted by an
     * <code>=</code>), construct a hash map of the input.  Escaped commas and
     * equals signs are represented properly.</p>
     *
     * @param input A list of pairs, <code>one=1,two=2</code>.
     * @return A new map (e.g. {two=2,one=1}).
     */
    public static Map <String, String> asMap (final String input) {
        Map <String, String> result = new HashMap <String, String>();

        for (String each : asList(input)) {
            String[] pair = EQWS.split(each.trim());

            result.put(pair[0].replace(EQESC, EQ),
                       pair[1].replace(EQESC, EQ));
        }

        return result;
    }

    /**
     * Create a set of strings from a comma-separated word list.
     *
     * <p>This static factory method creates a hash set from a comma separated
     * value list.</p>
     *
     * @param input The comma separated list, <code>1, 2, 3</code>
     *
     * @return A new hash set (e.g. {1,2,3}).
     */
    public static Set <String> asSet (final String input) {
        return new HashSet(asList(input));
    }

    /**
     * Compare two strings which might be null.
     *
     * <p>Compare two strings which may be null for equality.</p>
     *
     * @return <code>true</code> if both strings are null, or neither are null
     * and {@link String.equals(Object)} is <code>true</code>.
     */
    public static boolean isEqual (final String a, final String b) {
        if (a != null) {
            return a.equals(b);
        }
        else {
            return b == null;
        }
    }
}
