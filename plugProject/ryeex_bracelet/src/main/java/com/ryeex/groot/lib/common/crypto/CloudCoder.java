
package com.ryeex.groot.lib.common.crypto;

import android.net.Uri;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

public class CloudCoder {
    private static final String RC4_ALGORITHM_NAME = "RC4";
    private static Random random = new SecureRandom();

    /**
     * Compute SHA1 hash value for the string
     *
     * @param plain plain text. It will be encoded to BASE64 before hash
     * @return BASE64 encoded hash value
     */
    public static String hash4SHA1(String plain) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] utf8Bytes = plain.getBytes("UTF-8");
            byte[] sha1Bytes = md.digest(utf8Bytes);
            return String.valueOf(Base64Coder.encode(sha1Bytes));
        } catch (NoSuchAlgorithmException e) {
        } catch (UnsupportedEncodingException e) {
        }
        // this should never be reached
        throw new IllegalStateException("failed to SHA1");
    }

    /**
     * Generate signature for the request.
     *
     * @param method http request method. GET or POST
     * @param requestUrl the full request url. e.g.: http://api.xiaomi.com/getUser?id=123321
     * @param params request params. This should be a TreeMap because the parameters are required to
     *            be in lexicographic order.
     * @param security AES secret key. Must NOT be null.
     * @return hash value for the values provided
     */
    public static String generateSignature(String method, String requestUrl,
            Map<String, String> params, String security) {
        if (TextUtils.isEmpty(security)) {
            throw new InvalidParameterException("security is not nullable");
        }
        List<String> exps = new ArrayList<String>();
        if (method != null) {
            exps.add(method.toUpperCase());
        }
        if (requestUrl != null) {
            Uri uri = Uri.parse(requestUrl);
            exps.add(uri.getEncodedPath());
        }
        if (params != null && !params.isEmpty()) {
            final TreeMap<String, String> sortedParams = new TreeMap<String, String>(params);
            Set<Map.Entry<String, String>> entries = sortedParams.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                exps.add(String.format("%s=%s", entry.getKey(),
                        entry.getValue()));
            }
        }
        exps.add(security);
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (String s : exps) {
            if (!first) {
                sb.append('&');
            }
            sb.append(s);
            first = false;
        }
        return hash4SHA1(sb.toString());
    }

    /**
     * 获取随机nonce值 64是随机数，后面32位是总的分钟数
     *
     * @return
     */
    public static String generateNonce(long diff) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        try {
            dos.writeLong(random.nextLong());
            dos.writeInt((int) ((System.currentTimeMillis() + diff) / (1000 * 60)));
            dos.flush();
        } catch (IOException e) {
            // MyLog.e("never happend", e);
        }
        return String.valueOf(Base64Coder.encode(bos.toByteArray()));
    }
}
