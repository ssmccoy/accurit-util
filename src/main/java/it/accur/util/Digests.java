package it.accur.util;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Digests {
    private static final String DEFAULT_ALGORITHM = "SHA-1";

    private Digests () {}

    /**
     * Calculate the digest of the given payload using the given algorithm.
     *
     * <p>Given a payload as a byte array and the name of a supported hashing
     * algorithm, calculate it's digest and return a hex-encoded string of the
     * message digest.</p>
     *
     * @param message The message to create a digest of.
     * @param algorithm The algorithm to use.
     *
     * @return A hex-encoded string.
     *
     * @throws IllegalStateException If the supplied algorithm is not
     * supported.
     * @see MessageDigest
     * @see java.security.Security#getProviders
     */
    public static String calculateDigest (final byte[] message,
                                          final String algorithm)
    throws IllegalStateException {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);

            byte[] hash = digest.digest(message);

            return Bytes.encodeHexString(hash);
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                "Algorithm not supported", exception);
        }
    }

    /**
     * Calculate a SHA-1 digest for the given string.
     *
     * <p>Returns the SHA-1 hash as a hexadecimal string.</p>
     *
     * @param message The message to create a digest of.
     * 
     * @return A hex-encoded string.
     *
     * @throws IllegalStateException If the supplied algorithm is not
     * supported.
     */
    public static String calculateDigest (final String message) {
        return calculateDigest(message.getBytes(), DEFAULT_ALGORITHM);
    }
}
