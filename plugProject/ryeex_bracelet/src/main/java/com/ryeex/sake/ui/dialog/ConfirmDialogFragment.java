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
import android.widget.TextView;

import com.ryeex.groot.lib.common.util.DisplayUtil;
import com.ryeex.sake.R;

public class ConfirmDialogFragment extends DialogFragment {
    TextView mTitleView;
    TextView mContentView;
    TextView mConfirmTextView;
    TextView mCancelTextView;

    String mTitle;
    String mContent;
    boolean mCancelable = true;

    String mConfirmText;
    String mCancelText;

    View.OnClickListener mConfirmListener;
    View.OnClickListener mCancelListener;

    boolean mIsConfirmed = false;
    boolean mIsCanceled = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_fragment_confirm, container, false);

        mTitleView = (TextView) rootView.findViewById(R.id.title);
        mContentView = (TextView) rootView.findViewById(R.id.content);

        if (TextUtils.isEmpty(mTitle)) {
            mContentView.setTextColor(getResources().getColor(R.color.baseui_class_text_35));
        } else {
            mTitleView.setText(mTitle);
            mTitleView.setVisibility(View.VISIBLE);
        }

        mContentView.setText(mContent);

        mConfirmTextView = (TextView) rootView.findViewById(R.id.confirm);
        if (TextUtils.isEmpty(mConfirmText)) {
            mConfirmTextView.setText(getString(R.string.confirm_dialog_fragment_confirm));
        } else {
            mConfirmTextView.setText(mConfirmText);
        }
        mConfirmTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();

                mIsConfirmed = true;

                if (mConfirmListener != null) {
                    mConfirmListener.onClick(v);
                }
            }
        });

        mCancelTextView = (TextView) rootView.findViewById(R.id.cancel);
        if (TextUtils.isEmpty(mCancelText)) {
            mCancelTextView.setText(getString(R.string.confirm_dialog_fragment_cancel));
        } else {
            mCancelTextView.setText(mCancelText);
        }
        mCancelTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();

                mIsCanceled = true;

                if (mCancelListener != null) {
                    mCancelListener.onClick(v);
                }
            }
        });

        if (mCancelable) {
            mCancelTextView.setVisibility(View.VISIBLE);
        } else {
            mCancelTextView.setVisibility(View.GONE);
        }

        return rootView;
    }

    public void setTitle(String title) {
        if (TextUtils.isEmpty(title)) {
            return;
        }

        mTitle = title;

        if (mTitleView != null) {
            mTitleView.setText(title);
        }
    }

    public void setContent(String content) {
        if (TextUtils.isEmpty(content)) {
            mContent = "";
        } else {
            mContent = content;
        }

        if (mContentView != null) {
            mContentView.setText(mContent);
        }
    }

    public void setConfirmListener(String confirmText, View.OnClickListener confirmListener) {
        mConfirmText = confirmText;
        mConfirmListener = confirmListener;
    }

    public void setCancelListener(String cancelText, View.OnClickListener cancelListener) {
        mCancelText = cancelText;
        mCancelListener = cancelListener;
    }

    public void setCancelable(boolean cancelable) {
        super.setCancelable(cancelable);

        mCancelable = cancelable;
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
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        if (!mIsConfirmed && !mIsCanceled) {
            if (mCancelListener != null) {
                mCancelListener.onClick(mCancelTextView);
            }
        }
    }
}
