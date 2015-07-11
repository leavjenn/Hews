package com.leavjenn.hews.ui.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.leavjenn.hews.R;
import com.leavjenn.hews.SharedPrefsManager;
import com.leavjenn.hews.model.Post;

import java.util.ArrayList;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {
    final static int UNREAD_ITEM_LEFT_FOR_RELOADING = 10;
    static SharedPreferences prefs;
    ArrayList<Post> mPostArrayList;
    static Context mContext;
    int mMaxRead;
    Typeface mFont;
    float mTextSize, mLineHeight;
    OnReachBottomListener mOnReachBottomListener;
    OnItemClickListener mOnItemClickListener;

    public PostAdapter(Context context, OnReachBottomListener onReachBottomListener,
                       OnItemClickListener onItemClickListener) {
        mContext = context;
        mMaxRead = 0;
        mPostArrayList = new ArrayList<>();
        mOnReachBottomListener = onReachBottomListener;
        mOnItemClickListener = onItemClickListener;
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        updatePostPrefs();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_post,
                viewGroup, false);
        ViewHolder vh = new ViewHolder(v);
        vh.tvTitle.setTypeface(mFont);
        vh.tvTitle.setTextSize(mTextSize);
        vh.tvTitle.setLineSpacing(0, mLineHeight);
        vh.tvTitle.setPaintFlags(vh.tvTitle.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int i) {
        if (mMaxRead < i) {
            mMaxRead = i;
            if (mMaxRead == getItemCount() - UNREAD_ITEM_LEFT_FOR_RELOADING) {
                mOnReachBottomListener.OnReachBottom();
            }
        }
        final Post currentPost = mPostArrayList.get(i);
        viewHolder.tvTitle.setText(currentPost.getTitle());

        String s = currentPost.getDescendants() > 1 ? " comments" : " comment";
        viewHolder.tvDescendants.setText(String.valueOf(currentPost.getDescendants()) + s);
        viewHolder.tvScore.setText("+ " + String.valueOf(currentPost.getScore()));
        viewHolder.tvPrettyUrl.setText(currentPost.getPrettyUrl());
        if (currentPost.getSummary() != null) {
            viewHolder.tvSummary.setVisibility(View.VISIBLE);
            viewHolder.tvSummary.setText(currentPost.getSummary());
        }else {
            viewHolder.tvSummary.setVisibility(View.GONE);
        }
        viewHolder.layoutFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnItemClickListener.onItemClick(mPostArrayList.get(i));
            }
        });
    }

    @Override
    public int getItemCount() {
        return mPostArrayList.size();
    }

    public void add(Post item) {
        mPostArrayList.add(item);
        notifyItemInserted(mPostArrayList.size());
    }


    public void clear() {
        mPostArrayList.clear();
        mMaxRead = 0;
    }

    public void updatePostPrefs() {
        mFont = Typeface.createFromAsset(mContext.getAssets(),
                SharedPrefsManager.getPostFont(prefs) + ".ttf");
        mTextSize = SharedPrefsManager.getPostFontSize(prefs);
        mLineHeight = SharedPrefsManager.getPostLineHeight(prefs);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout layoutFrame;
        TextView tvTitle;
        TextView tvScore;
        TextView tvDescendants;
        TextView tvPrettyUrl;
        TextView tvSummary;

        public ViewHolder(View v) {
            super(v);
            layoutFrame = (RelativeLayout) v.findViewById(R.id.layout_post_frame);
            tvTitle = (TextView) v.findViewById(R.id.tv_post_title);
            tvScore = (TextView) v.findViewById(R.id.tv_post_point);
            tvDescendants = (TextView) v.findViewById(R.id.tv_post_comments);
            tvPrettyUrl = (TextView) v.findViewById(R.id.tv_post_pretty_url);
            tvSummary = (TextView) v.findViewById(R.id.tv_post_summary);
        }
    }


    public interface OnReachBottomListener {
        void OnReachBottom();
    }

    public interface OnItemClickListener {
        void onItemClick(Post post);
    }

}
