
package com.ryeex.groot.lib.common.util;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Looper;
import android.text.TextUtils;

import java.util.HashMap;

/**
 * Created by chenhao on 17/1/6.
 */

public class FontUtil {
    private static final HashMap<String, Typeface> mFontMap = new HashMap<String, Typeface>();

    /*
     * This function should be called from main thread
     */
    public static Typeface getFontFromAsset(Context context, String fontName) {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw new IllegalStateException("Method call should happen from the main thread.");
        }

        Typeface typeFace = Typeface.defaultFromStyle(Typeface.NORMAL);

        if (!TextUtils.isEmpty(fontName)) {
            Typeface tmpTypeface = mFontMap.get(fontName);
            if (tmpTypeface == null) {
                try {
                    tmpTypeface = Typeface.createFromAsset(context.getAssets(), fontName);
                } catch (Exception e) {
                }

                if (tmpTypeface != null) {
                    mFontMap.put(fontName, tmpTypeface);
                }
            }

            if (tmpTypeface != null) {
                typeFace = tmpTypeface;
            }
        }

        return typeFace;
    }
}
