package com.ryeex.groot.lib.common.util;

import android.net.Uri;

import java.util.HashMap;
import java.util.Map;

public class UriUtil {
    public static Parameter parseUri(Uri uri) {
        Parameter parameter = new Parameter();
        parameter.path = uri.getPath();
        parameter.mQueryStrings = new HashMap<>();
        for (String key : uri.getQueryParameterNames()) {
            parameter.mQueryStrings.put(key, uri.getQueryParameter(key));
        }

        return parameter;
    }

    public static class Parameter {
        public String path;
        public Map<String, String> mQueryStrings = new HashMap<>();
    }
}
