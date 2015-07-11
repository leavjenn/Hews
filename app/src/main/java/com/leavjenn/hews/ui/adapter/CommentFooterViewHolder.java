package com.leavjenn.hews.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.leavjenn.hews.R;

public class CommentFooterViewHolder extends RecyclerView.ViewHolder {
    ProgressBar progressBar;
    TextView tvNoCommentPromt;

    public CommentFooterViewHolder(View v) {
        super(v);
        progressBar = (ProgressBar) v.findViewById(R.id.progress_bar);
        tvNoCommentPromt = (TextView) v.findViewById(R.id.tv_no_comment_promt);
    }
}
