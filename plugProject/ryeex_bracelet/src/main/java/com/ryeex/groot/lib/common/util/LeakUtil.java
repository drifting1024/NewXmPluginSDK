package com.ryeex.groot.lib.common.util;

import android.app.Activity;
import android.content.Context;
import android.os.IBinder;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

/**
 * Created by chenhao on 2017/9/20.
 */

public class LeakUtil {
    public static void fixInputMethodManagerLeak(Activity destContext) {
        if (destContext == null) {
            return;
        }

        final InputMethodManager imm = (InputMethodManager) destContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) {
            return;
        }

        final ReflectorUtil.TypedObject windowToken = new ReflectorUtil.TypedObject(destContext.getWindow().getDecorView().getWindowToken(), IBinder.class);

        ReflectorUtil.invokeMethodExceptionSafe(imm, "windowDismissed", windowToken);

        final ReflectorUtil.TypedObject view = new ReflectorUtil.TypedObject(null, View.class);

        ReflectorUtil.invokeMethodExceptionSafe(imm, "startGettingWindowFocus", view);

        String[] arr = new String[]{
                "mCurRootView", "mServedView", "mNextServedView"
        };
        Field f;
        Object obj_get;
        for (int i = 0; i < arr.length; i++) {
            String param = arr[i];
            try {
                f = imm.getClass().getDeclaredField(param);
                if (f.isAccessible() == false) {
                    f.setAccessible(true);
                }
                obj_get = f.get(imm);
                if (obj_get != null && obj_get instanceof View) {
                    View v_get = (View) obj_get;
                    if (v_get.getContext() == destContext) { // 被InputMethodManager持有引用的context是想要目标销毁的
                        f.set(imm, null); // 置空，破坏掉path to gc节点
                    } else {
                        // // 不是想要目标销毁的，即为又进了另一层界面了，不要处理，避免影响原逻辑,也就不用继续for循环了
                        break;
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public static void fixTextLineCacheLeak() {
        Field textLineCached = null;
        try {
            textLineCached = Class.forName("android.text.TextLine").getDeclaredField("sCached");
            textLineCached.setAccessible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (textLineCached == null) return;

        Object cached = null;
        try {
            // Get reference to the TextLine sCached array.
            cached = textLineCached.get(null);
        } catch (Exception ex) {
            //
        }
        if (cached != null) {
            // Clear the array.
            for (int i = 0, size = Array.getLength(cached); i < size; i++) {
                Array.set(cached, i, null);
            }
        }
    }
}
