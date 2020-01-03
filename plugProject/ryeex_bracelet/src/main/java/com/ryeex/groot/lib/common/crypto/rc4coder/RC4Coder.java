
package com.ryeex.groot.lib.common.crypto.rc4coder;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class RC4Coder {
    public static final String RC4_ALGORITHM = "RC4";

    private SecretKeySpec keySpec;

    public RC4Coder(byte[] rc4Key) throws SecurityException {
        if (isEmpty(rc4Key)) {
            throw new SecurityException("rc4 key is null");
        }

        if (rc4Key.length != 16) {
            throw new IllegalArgumentException("rc4Key is invalid");
        }

        keySpec = new SecretKeySpec(rc4Key, "RC4");
    }

    private static boolean isEmpty(byte[] byteArr) {
        return byteArr == null || byteArr.length == 0;
    }

    /**
     * 解密
     * 
     * @return
     * @throws SecurityException
     */
    public byte[] decrypt(byte[] cipherData) throws SecurityException {
        try {
            Cipher cipher = Cipher.getInstance(RC4_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            if (null == cipherData) {
                throw new IllegalBlockSizeException("no block data");
            }
            byte[] rawData = cipher.doFinal(cipherData);
            return rawData;
        } catch (NoSuchAlgorithmException e) {
            throw new SecurityException(e);
        } catch (NoSuchPaddingException e) {
            throw new SecurityException(e);
        } catch (InvalidKeyException e) {
            throw new SecurityException(e);
        } catch (IllegalBlockSizeException e) {
            throw new SecurityException(e);
        } catch (BadPaddingException e) {
            throw new SecurityException(e);
        }
    }

    /**
     * 加密
     * 
     * @return
     * @throws SecurityException
     */
    public byte[] encrypt(byte[] rawData) throws SecurityException {
        try {
            Cipher cipher = Cipher.getInstance(RC4_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return cipher.doFinal(rawData);
        } catch (NoSuchAlgorithmException e) {
            throw new SecurityException(e);
        } catch (NoSuchPaddingException e) {
            throw new SecurityException(e);
        } catch (InvalidKeyException e) {
            throw new SecurityException(e);
        } catch (IllegalBlockSizeException e) {
            throw new SecurityException(e);
        } catch (BadPaddingException e) {
            throw new SecurityException(e);
        }
    }

}
