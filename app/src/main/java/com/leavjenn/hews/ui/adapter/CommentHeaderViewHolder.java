package com.leavjenn.hews.ui.adapter;


import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.leavjenn.hews.R;

public class CommentHeaderViewHolder extends RecyclerView.ViewHolder {
    TextView tvTitle;
    TextView tvContent;
    TextView tvUrl;
    TextView tvPoints;
    TextView tvComments;
    TextView tvTime;
    TextView tvPoster;

    public CommentHeaderViewHolder(View v) {
        super(v);
        tvTitle = (TextView) v.findViewById(R.id.tv_header_title);
        tvUrl = (TextView) v.findViewById(R.id.tv_header_url);
        tvPoints = (TextView) v.findViewById(R.id.tv_header_points);
        tvComments = (TextView) v.findViewById(R.id.tv_header_comments);
        tvTime = (TextView) v.findViewById(R.id.tv_header_time);
        tvPoster = (TextView) v.findViewById(R.id.tv_header_poster);
        tvContent = (TextView) v.findViewById(R.id.tv_header_content);

    }
}
