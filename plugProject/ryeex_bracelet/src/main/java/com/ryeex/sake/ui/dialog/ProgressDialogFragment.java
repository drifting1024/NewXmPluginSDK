package com.ryeex.sake.ui.dialog;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ryeex.groot.lib.common.util.DisplayUtil;
import com.ryeex.sake.R;

public class ProgressDialogFragment extends DialogFragment {
    TextView mTitleView;
    ProgressBar mProgressBar;
    TextView mProgressTextView;
    TextView mCancelTextView;

    String mTitle;
    boolean mCancelable = false;

    OnCancelListener mOnCancelListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_fragment_progress, container, false);

        mTitleView = (TextView) rootView.findViewById(R.id.title);

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progress_bar);
        mProgressTextView = (TextView) rootView.findViewById(R.id.progress_value);
        mCancelTextView = (TextView) rootView.findViewById(R.id.cancel);

        if (!TextUtils.isEmpty(mTitle)) {
            mTitleView.setText(mTitle);
            mTitleView.setVisibility(View.VISIBLE);
        }

        if (mCancelable) {
            mCancelTextView.setVisibility(View.VISIBLE);
        } else {
            mCancelTextView.setVisibility(View.GONE);
        }
        mCancelTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();

                if (mOnCancelListener != null) {
                    mOnCancelListener.onCancel();
                }
            }
        });

        return rootView;
    }

    public void setTitle(String title) {
        mTitle = title;

        if (mTitleView != null) {
            mTitleView.setText(title);
        }
    }

    public void setProgress(float progress, String progressStr) {
        if (mProgressBar != null) {
            mProgressBar.setProgress((int) progress);
        }

        if (mProgressTextView != null) {
            if (TextUtils.isEmpty(progressStr)) {
                mProgressTextView.setText((int) progress + "%");
            } else {
                mProgressTextView.setText(progressStr);
            }
        }
    }

    public void setCancelable(boolean cancelable) {
        super.setCancelable(cancelable);

        mCancelable = cancelable;
    }

    public void setOnCancelListener(OnCancelListener cancelListener) {
        mOnCancelListener = cancelListener;
    }

    @Override
    public void onStart() {
        super.onStart();

        Window window = getDialog().getWindow();

        // 一定要设置Background，如果不设置，window属性设置无效
        window.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.transparent)));

        WindowManager.LayoutParams params = window.getAttributes();
        params.gravity = Gravity.CENTER;
        params.width = DisplayUtil.dip2px(getActivity(), 300);
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        window.setAttributes(params);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        if (mOnCancelListener != null) {
            mOnCancelListener.onCancel();
        }
    }

    public interface OnCancelListener {
        void onCancel();
    }
}
