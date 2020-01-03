package com.ryeex.groot.lib.common.util;

import android.text.TextUtils;

/**
 * Created by chenhao on 2017/8/30.
 */

public class PhoneUtil {
    public static boolean isPhoneNumber(String mobiles) {
        //"[1]"代表第1位为数字1，"[358]"代表第二位可以为3、5、8中的一个，"\\d{9}"代表后面是可以是0～9的数字，有9位。
        String telRegex = "[1][34578]\\d{9}";
        if (TextUtils.isEmpty(mobiles)) {
            return false;
        } else {
            return mobiles.matches(telRegex);
        }
    }
}
