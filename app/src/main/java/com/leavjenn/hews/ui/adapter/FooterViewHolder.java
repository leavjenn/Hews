package com.leavjenn.hews.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.leavjenn.hews.R;

public class FooterViewHolder extends RecyclerView.ViewHolder {
    ProgressBar progressBar;
    TextView tvPrompt;

    public FooterViewHolder(View v) {
        super(v);
        progressBar = (ProgressBar) v.findViewById(R.id.progress_bar);
        tvPrompt = (TextView) v.findViewById(R.id.tv_prompt);
    }
}
