
package com.ryeex.groot.lib.common.crypto.rc4coder;

import com.ryeex.groot.lib.common.crypto.Base64Coder;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import javax.crypto.IllegalBlockSizeException;

/**
 * RC4drop 1024 key should be 256bit
 */
public class RC4DropCoder {

    private static final int DropLength = 1024;
    private static final byte[] dropBytes = new byte[DropLength];

    static {
        Arrays.fill(dropBytes, (byte) 0);
    }

    RC4 rc4instance;

    public RC4DropCoder(byte[] rc4Key) throws SecurityException {
        if (isEmpty(rc4Key)) {
            throw new SecurityException("rc4 key is null");
        }

        if (rc4Key.length != 32) {
            throw new IllegalArgumentException("rc4Key length is invalid");
        }

        rc4instance = new RC4(rc4Key);
        decrypt(dropBytes); // 这就是被扔掉的前面的数据
    }

    public RC4DropCoder(String rc4Key) throws SecurityException {
        this(Base64Coder.decode(rc4Key));
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
            if (null == cipherData) {
                throw new IllegalBlockSizeException("no block data");
            }
            rc4instance.encrypt(cipherData);
            return cipherData;
        } catch (IllegalBlockSizeException e) {
            throw new SecurityException(e);
        }
    }

    public String decrypt(String cipherBase64String) {
        try {
            return new String(decrypt(Base64Coder.decode(cipherBase64String)), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new SecurityException(e);
        }
    }

    /**
     * RC4 加密解密都是它
     * 
     * @return
     * @throws SecurityException
     */
    public byte[] encrypt(byte[] rawData) throws SecurityException {
        try {
            if (null == rawData) {
                throw new IllegalBlockSizeException("no block data");
            }
            rc4instance.encrypt(rawData);
            return rawData;
        } catch (IllegalBlockSizeException e) {
            throw new SecurityException(e);
        }
    }

    public String encrypt(String rawData) {
        byte[] rawDataBytes = null;
        try {
            rawDataBytes = rawData.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // never happened
        }
        return String.valueOf(Base64Coder.encode(encrypt(rawDataBytes)));
    }

}
