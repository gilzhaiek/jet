package com.example.hudmetricsclient;

import android.widget.TextView;

public class MetricView {
    TextView mCurrentTextView;
    TextView mPreviousTextView;

    public MetricView(TextView currentTextView, TextView previousTextView) {
        mCurrentTextView = currentTextView;
        mPreviousTextView = previousTextView;
    }

    public void addValue(float value) {
        mPreviousTextView.setText(mCurrentTextView.getText());
        mCurrentTextView.setText(String.format("%.2f", value));
    }
}
