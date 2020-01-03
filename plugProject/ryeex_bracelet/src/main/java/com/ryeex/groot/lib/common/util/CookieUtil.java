
package com.ryeex.groot.lib.common.util;

import android.text.TextUtils;

import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by chenhao on 17/5/22.
 */

public class CookieUtil {
    public static HttpCookie getCookie(CookieManager cookieManager, String name) {
        if (cookieManager == null || TextUtils.isEmpty(name)) {
            return null;
        }

        CookieStore store = cookieManager.getCookieStore();
        List<HttpCookie> cookies = store.getCookies();
        for (HttpCookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie;
            }
        }

        return null;
    }

    public static HttpCookie getCookie(CookieManager cookieManager, String name, String domain) {
        if (cookieManager == null || TextUtils.isEmpty(name) || TextUtils.isEmpty(domain)) {
            return null;
        }

        CookieStore store = cookieManager.getCookieStore();
        List<HttpCookie> cookies = store.getCookies();
        for (HttpCookie cookie : cookies) {
            if (name.equals(cookie.getName()) && domain.equals(cookie.getDomain())) {
                return cookie;
            }
        }

        return null;
    }

    public static HttpCookie getCookie(CookieManager cookieManager, String name, String domain,
            String path) {
        if (cookieManager == null || TextUtils.isEmpty(name) || TextUtils.isEmpty(domain)
                || TextUtils.isEmpty(path)) {
            return null;
        }

        CookieStore store = cookieManager.getCookieStore();
        List<HttpCookie> cookies = store.getCookies();
        for (HttpCookie cookie : cookies) {
            if (name.equals(cookie.getName()) && domain.equals(cookie.getDomain())
                    && path.equals(cookie.getPath())) {
                return cookie;
            }
        }

        return null;
    }

    public static void addCookie(CookieManager cookieManager, String url, String name, String value,
            String domain, String path) {
        if (cookieManager == null) {
            return;
        }

        HttpCookie cookie = new HttpCookie(name, value);
        cookie.setDomain(domain);
        cookie.setPath(path);
        try {
            cookieManager.getCookieStore().add(new URI(url), cookie);
        } catch (URISyntaxException e) {
        }
    }

    public static void clearAllCookie(CookieManager cookieManager) {
        if (cookieManager == null) {
            return;
        }
        cookieManager.getCookieStore().removeAll();
    }
}
