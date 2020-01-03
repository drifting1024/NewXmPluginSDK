package com.ryeex.groot.lib.common.util;

import java.util.List;

/**
 * Created by chenhao on 2017/12/20.
 */

public class ListUtil {
    public static boolean isEmpty(List<?> list) {
        return list == null || list.size() <= 0;
    }
}
