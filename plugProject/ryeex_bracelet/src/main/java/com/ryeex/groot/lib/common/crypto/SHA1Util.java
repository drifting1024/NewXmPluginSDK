
package com.ryeex.groot.lib.common.crypto;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by chenhao on 15/11/17.
 */
public class SHA1Util {
    public static String getSHA1Digest(final String pInput) {
        try {
            final MessageDigest lDigest = MessageDigest.getInstance("SHA1");
            lDigest.update(getBytes(pInput));
            final BigInteger lHashInt = new BigInteger(1, lDigest.digest());
            return String.format("%1$040X", lHashInt);
        } catch (final NoSuchAlgorithmException lException) {
            throw new RuntimeException(lException);
        }
    }

    public static byte[] getBytes(final String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (final UnsupportedEncodingException e) {
            return s.getBytes();
        }
    }
}
