package com.ryeex.groot.lib.common.util;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;

/**
 * Created by chenhao on 2017/9/20.
 */

public class ViewUtil {
    public static void clearAllActivityViews(Activity activity) {
        try {
            View view = activity.getWindow().getDecorView().findViewById(android.R.id.content);

            clearViewResource(view);

        } catch (Exception e) {
        }
    }

    private static void clearViewResource(View view) {
        if (view == null) {
            return;
        }

        if (view instanceof ImageView) {
            ((ImageView) view).setImageBitmap(null);
        } else if (view instanceof WebView) {
            WebView webview = (WebView) view;
            webview.setTag(null);
            webview.stopLoading();
            webview.clearHistory();
            try {
                webview.removeAllViews();
            } catch (Exception e) {

            }
            webview.clearView();

            try {
                //android 5.0 之后webview需要先detach, 再destroy, 防止内存泄露
                ((ViewGroup) webview.getParent()).removeView(webview);
            } catch (Exception e) {
            }

            webview.destroy();
            webview = null;
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            int childCount = group.getChildCount();

            for (int i = 0; i < childCount; i++) {
                View child = group.getChildAt(i);
                clearViewResource(child);
            }

            try {
                group.removeAllViews();
            } catch (Exception e) {

            }

        }
    }

    public static boolean hitTest(View v, int x, int y) {
        final int tx = (int) (v.getTranslationX() + 0.5f);
        final int ty = (int) (v.getTranslationY() + 0.5f);
        final int left = v.getLeft() + tx;
        final int right = v.getRight() + tx;
        final int top = v.getTop() + ty;
        final int bottom = v.getBottom() + ty;

        return (x >= left) && (x <= right) && (y >= top) && (y <= bottom);
    }
}
