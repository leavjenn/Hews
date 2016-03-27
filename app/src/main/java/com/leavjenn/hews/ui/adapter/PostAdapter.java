package com.leavjenn.hews.ui.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.leavjenn.hews.Constants;
import com.leavjenn.hews.R;
import com.leavjenn.hews.Utils;
import com.leavjenn.hews.misc.SharedPrefsManager;
import com.leavjenn.hews.model.HNItem;
import com.leavjenn.hews.model.Post;

import java.util.ArrayList;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int UNREAD_ITEM_LEFT_FOR_RELOADING = 10;
    private static final int VIEW_TYPE_POST = 0;
    private static final int VIEW_TYPE_FOOTER = 1;

    private int mMaxRead;
    private int mLoadingState;
    private int mResPrompt;
    private Typeface mFont;
    private float mTextSize, mLineHeight;
    private SharedPreferences prefs;
    private ArrayList<HNItem> mPostList;
    private Context mContext;
    private OnReachBottomListener mOnReachBottomListener;
    private OnItemClickListener mOnItemClickListener;

    public PostAdapter(Context context, OnReachBottomListener onReachBottomListener,
                       OnItemClickListener onItemClickListener) {
        mContext = context;
        mPostList = new ArrayList<>();
        mOnReachBottomListener = onReachBottomListener;
        mOnItemClickListener = onItemClickListener;
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        updatePostPrefs();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v;
        RecyclerView.ViewHolder viewHolder = null;
        if (viewType == VIEW_TYPE_FOOTER) {
            v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_footer,
                viewGroup, false);
            viewHolder = new FooterViewHolder(v);
        } else if (viewType == VIEW_TYPE_POST) {
            v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_post,
                viewGroup, false);
            PostViewHolder vh = new PostViewHolder(v);
            vh.tvTitle.setTypeface(mFont);
            vh.tvTitle.setTextSize(mTextSize);
            vh.tvTitle.setLineSpacing(0, mLineHeight);
            vh.tvTitle.setPaintFlags(vh.tvTitle.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
            vh.tvSummary.setTypeface(Typeface.createFromAsset(mContext.getAssets(), "Open Sans.ttf"));
            viewHolder = vh;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof PostViewHolder) {
            bindPostViewHolder(viewHolder, position);
        } else if (viewHolder instanceof FooterViewHolder) {
            bindFooterViewHolder(viewHolder);
        }
    }

    private void bindPostViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (mMaxRead < position) {
            mMaxRead = position;
            if (mMaxRead >= getItemCount() - UNREAD_ITEM_LEFT_FOR_RELOADING) {
                mOnReachBottomListener.OnReachBottom();
            }
        }
        final Post currentPost = (Post) mPostList.get(position);
        PostViewHolder postVH = (PostViewHolder) viewHolder;
        postVH.tvTitle.setText(currentPost.getTitle());
        // set title color based on has read or not
        TypedValue titleColor = new TypedValue();
        if (SharedPrefsManager.isPostRead(prefs, currentPost.getId())) {
            mContext.getTheme().resolveAttribute(R.attr.text_title_color_inverse, titleColor, true);
        } else {
            mContext.getTheme().resolveAttribute(android.R.attr.textColor, titleColor, true);
        }
        postVH.tvTitle.setTextColor(titleColor.data);

        String s = currentPost.getDescendants() > 1 ? " comments" : " comment";
        postVH.tvScore.setText("+ " + String.valueOf(currentPost.getScore()));
        postVH.tvDescendants.setText(String.valueOf(currentPost.getDescendants()) + s);
        postVH.tvTime.setText(String.valueOf(Utils.formatTime(currentPost.getTime())));
        postVH.tvPrettyUrl.setText(currentPost.getPrettyUrl());
        if (currentPost.getSummary() != null) {
            postVH.tvSummary.setVisibility(View.VISIBLE);
            postVH.tvSummary.setText(currentPost.getSummary());
        } else {
            postVH.tvSummary.setVisibility(View.GONE);
        }
    }

    private void bindFooterViewHolder(RecyclerView.ViewHolder viewHolder) {
        FooterViewHolder footerViewHolder = (FooterViewHolder) viewHolder;
        if (mLoadingState == Constants.LOADING_IDLE
            || mLoadingState == Constants.LOADING_FINISH
            || mLoadingState == Constants.LOADING_ERROR) {
            footerViewHolder.progressBar.setVisibility(View.GONE);
            footerViewHolder.tvPrompt.setVisibility(View.GONE);
        } else if (mLoadingState == Constants.LOADING_PROMPT_NO_CONTENT) {
            footerViewHolder.progressBar.setVisibility(View.GONE);
            footerViewHolder.tvPrompt.setVisibility(View.VISIBLE);
            if (mResPrompt != 0) {
                footerViewHolder.tvPrompt.setText(mContext.getResources().getString(mResPrompt));
            }
        } else if (mLoadingState == Constants.LOADING_IN_PROGRESS) {
            footerViewHolder.progressBar.setVisibility(View.VISIBLE);
            footerViewHolder.tvPrompt.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mPostList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mPostList.get(position) instanceof HNItem.Footer) {
            return VIEW_TYPE_FOOTER;
        } else {
            return VIEW_TYPE_POST;
        }
    }

    public void addPost(Post post) {
        mPostList.add(mPostList.size() - 1, post);
        notifyItemInserted(mPostList.size() - 1);
    }

    public void addFooter(HNItem.Footer footer) {
        mPostList.add(footer);
        notifyItemInserted(mPostList.size() - 1);
    }

    public void addAllPosts(List<? extends HNItem> hnItemList) {
        mPostList.addAll(mPostList.size() - 1, hnItemList);
        notifyDataSetChanged();
    }

    public void clear() {
        mPostList.clear();
        notifyDataSetChanged();
        mMaxRead = 0;
    }

    public void updateFooter(int loadingState) {
        mLoadingState = loadingState;
    }

    public void updatePrompt(int resPrompt) {
        mResPrompt = resPrompt;
    }

    public ArrayList<Post> getPostList() {
        ArrayList<Post> postList = new ArrayList<>();
        for (HNItem hnItem : mPostList) {
            if (hnItem instanceof Post) {
                postList.add((Post) hnItem);
            }
        }
        return postList;
    }

    public void updatePostPrefs() {
        mFont = Typeface.createFromAsset(mContext.getAssets(),
            SharedPrefsManager.getPostFont(prefs) + ".ttf");
        mTextSize = SharedPrefsManager.getPostFontSize(prefs);
        mLineHeight = SharedPrefsManager.getPostLineHeight(prefs);
    }

    public class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvScore;
        TextView tvDescendants;
        TextView tvTime;
        TextView tvPrettyUrl;
        TextView tvSummary;

        public PostViewHolder(View v) {
            super(v);
            tvTitle = (TextView) v.findViewById(R.id.tv_post_title);
            tvScore = (TextView) v.findViewById(R.id.tv_post_point);
            tvTime = (TextView) v.findViewById(R.id.tv_post_time);
            tvDescendants = (TextView) v.findViewById(R.id.tv_post_comments);
            tvPrettyUrl = (TextView) v.findViewById(R.id.tv_post_pretty_url);
            tvSummary = (TextView) v.findViewById(R.id.tv_post_summary);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getAdapterPosition() == RecyclerView.NO_POSITION) {
                        return;
                    }
                    if (mPostList.get(getAdapterPosition()) instanceof Post) {
                        Post post = (Post) mPostList.get(getAdapterPosition());
                        SharedPrefsManager.setPostRead(prefs, post.getId());
                        notifyItemChanged(getAdapterPosition());
                        mOnItemClickListener.onOpenComment(post);
                    }
                }
            });
            v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (getAdapterPosition() == RecyclerView.NO_POSITION) {
                        return true;
                    }
                    if (mPostList.get(getAdapterPosition()) instanceof Post) {
                        mOnItemClickListener.onOpenLink((Post) mPostList.get(getAdapterPosition()));
                    }
                    return true;
                }
            });
        }
    }


    public interface OnReachBottomListener {
        void OnReachBottom();
    }

    public interface OnItemClickListener {
        void onOpenComment(Post post);

        void onOpenLink(Post post);
    }

}
