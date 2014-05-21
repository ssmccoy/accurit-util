package it.accur.util;

import java.security.SignatureException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility methods for generating signature strings.
 */
public class Signatures {
    private Signatures () {}

    /**
     * Create an RFC 2401-compliant HMAC signature.
     *
     * <p>Given a specified algorithm, payload and secret create a signature in
     * HMAC format.</p>
     * 
     * @param secret The shared secret to use for signing.
     * @param payload The payload to sign.
     * @param algorithm The algorithm to use in signing.
     *
     * @return A hexadecimal encoded RFC 2104-compliant HMAC signature.  
     * @throws SignatureException when signature generation fails
     */
    public static String calculateSignature (final byte[] secret,
                                             final byte[] payload,
                                             final String algorithm)
    throws SignatureException {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(
                secret, algorithm);

            Mac mac = Mac.getInstance(algorithm);

            mac.init(signingKey);

            byte[] rawHmac = mac.doFinal(payload);

            return Bytes.encodeHexString(rawHmac);
        }
        catch (Exception exception) {
            throw new SignatureException("Failed to generate HMAC: " +
                                         exception.getMessage(), exception);
        }
    }

    /**
     * Create an RFC 2401-compliant HMAC signature.
     *
     * <p>Given a specified algorithm, payload and secret create a signature in
     * HMAC format.</p>
     * 
     * @param secret The shared secret to use for signing.
     * @param payload The payload to sign.
     * @param algorithm The algorithm to use in signing.
     *
     * @return A hexadecimal encoded RFC 2104-compliant HMAC signature.  
     * @throws SignatureException when signature generation fails
     */
    public static String calculateSignature (final String secret,
                                             final String payload,
                                             final String algorithm)
    throws SignatureException {
        return calculateSignature(secret.getBytes(),
                                  payload.getBytes(),
                                  algorithm);
    }

    /**
     * Computes RFC 2104-compliant HMAC SHA-1 signature.
     *
     * @param payload The contents to base the signature from.
     * @param secret The secret key to use in generating the signature.
     *
     * @return A hexadecimal encoded RFC 2104-compliant HMAC signature.  
     * @throws SignatureException when signature generation fails
     */
    public static String calculateHMACSHA1(String secret, String payload)
    throws SignatureException {
        return calculateSignature(secret, payload, "HmacSHA1");
    }
}
