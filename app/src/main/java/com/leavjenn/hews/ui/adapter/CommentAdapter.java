package com.leavjenn.hews.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.leavjenn.hews.Constants;
import com.leavjenn.hews.R;
import com.leavjenn.hews.SharedPrefsManager;
import com.leavjenn.hews.Utils;
import com.leavjenn.hews.model.Comment;
import com.leavjenn.hews.model.HNItem;
import com.leavjenn.hews.model.Post;
import com.leavjenn.hews.ui.widget.ListDialogFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_COMMENT = 1;
    private static final int VIEW_TYPE_FOOTER = 2;

    Context mContext;
    RecyclerView mRecyclerView;
    ArrayList<HNItem> mItemList;
    Map<Long, List<Comment>> mCollapsedChildrenCommentsIndex;
    Map<Long, List<Comment>> mCollapsedOlderCommentsIndex;
    static SharedPreferences prefs;
    Typeface mFont;
    float mTextSize, mLineHeight;
    int mLoadingState = 0;
    int mCommentIndentColorOrange, mCommentIndentColorBg, mCommentIndentWidth;

    public CommentAdapter(Context context, RecyclerView recyclerView) {
        mContext = context;
        mRecyclerView = recyclerView;
        mItemList = new ArrayList<>();
        mCollapsedChildrenCommentsIndex = new HashMap<>();
        mCollapsedOlderCommentsIndex = new HashMap<>();
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        updateCommentPrefs();
        setCommentIndentStyle();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v;
        RecyclerView.ViewHolder viewHolder = null;
        switch (viewType) {
            case VIEW_TYPE_COMMENT:
                v = LayoutInflater.from(viewGroup.getContext()).inflate(
                        R.layout.list_item_comment,
                        viewGroup, false);
                CommentViewHolder vh = new CommentViewHolder(v,
                        new CommentViewHolder.ViewHolderClickListener() {
                            @Override
                            public void onClick(int position) {
                                if (isInBetweenCommentsCollapsed(position)) {
                                    expandInBetweenComments(position);
                                } else {
                                    collapseInBetweenComments(position);
                                }
                            }

                            @Override
                            public void onClickComment(int position) {
                                if (isChildrenCommentsCollapsed(position)) {
                                    expandChildrenComments(position);
                                } else {
                                    collapseChildrenComment(position);
                                }
                            }

                            @Override
                            public void onLongClick(int position) {
                                final Comment comment = (Comment) mItemList.get(position);
                                final Post post = (Post) mItemList.get(0);
                                ListDialogFragment dialog = new ListDialogFragment();
                                dialog.show(((FragmentActivity) mContext)
                                        .getSupportFragmentManager(), "ListDialog");
                                dialog.setOnListDialogClickListner
                                        (new ListDialogFragment.OnListDialogClickListner() {
                                            @Override
                                            public void onReplyViaBrowserClick(DialogFragment dialog) {
                                                Intent urlIntent = new Intent(Intent.ACTION_VIEW);
                                                String url = "https://news.ycombinator.com/reply?id="
                                                        + comment.getId()
                                                        + "&goto=item?id="
                                                        + post.getId();
                                                urlIntent.setData(Uri.parse(url));
                                                mContext.startActivity(urlIntent);
                                            }

                                            @Override
                                            public void onAuthorProfileClick(DialogFragment dialog) {
                                                Intent urlIntent = new Intent(Intent.ACTION_VIEW);
                                                String url = "https://news.ycombinator.com/user?id="
                                                        + comment.getBy();
                                                urlIntent.setData(Uri.parse(url));
                                                mContext.startActivity(urlIntent);
                                            }

                                            @Override
                                            public void onShareClick(DialogFragment dialog) {
                                                Intent sendIntent = new Intent();
                                                sendIntent.setAction(Intent.ACTION_SEND);
                                                String url = "https://news.ycombinator.com/item?id="
                                                        + comment.getId();
                                                sendIntent.putExtra(Intent.EXTRA_TEXT, url);
                                                sendIntent.setType("text/plain");
                                                mContext.startActivity(Intent.createChooser(sendIntent,
                                                        mContext.getString(R.string.share_link_to)));
                                            }

                                            @Override
                                            public void onShareCommentTextToClick(DialogFragment dialog) {
                                                Intent sendIntent = new Intent();
                                                sendIntent.setAction(Intent.ACTION_SEND);
                                                String text = comment.getBy() + ":\n"
                                                        + Html.fromHtml(comment.getText());
                                                sendIntent.putExtra(Intent.EXTRA_TEXT, text);
                                                sendIntent.setType("text/plain");
                                                mContext.startActivity(Intent.createChooser(sendIntent,
                                                        mContext.getString(R.string.send_to)));
                                            }
                                        });
                            }
                        });

                vh.tvComment.setTypeface(mFont);
                vh.tvComment.setPaintFlags(vh.tvComment.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
                vh.tvComment.setTextSize(mTextSize);
                vh.tvComment.setLineSpacing(0, mLineHeight);
                vh.setCommentIndentStripeStyle(mCommentIndentColorOrange,
                        mCommentIndentColorBg, mCommentIndentWidth);
                viewHolder = vh;
                break;
            case VIEW_TYPE_HEADER:
                v = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.list_item_comment_header, viewGroup, false);
                viewHolder = new CommentHeaderViewHolder(v);
                break;
            case VIEW_TYPE_FOOTER:
                v = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.list_item_comment_footer, viewGroup, false);
                viewHolder = new CommentFooterViewHolder(v);
        }
        return viewHolder;
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof CommentViewHolder) {
            // which one is better?
//        if (getItemViewType(position) == VIEW_TYPE_COMMENT) {
            CommentViewHolder commentViewHolder = (CommentViewHolder) viewHolder;
            Comment comment = (Comment) mItemList.get(position);
            commentViewHolder.setCommentIndent(comment.getLevel());
            if (comment.getDeleted()) {
                commentViewHolder.tvComment.setText("[deleted]");
            } else {
                commentViewHolder.tvComment.setText(Html.fromHtml(comment.getText()));
                commentViewHolder.tvAuthor.setText(comment.getBy());
            }
            commentViewHolder.tvTime.setText(Utils.formatTime(comment.getTime()));

            if (mCollapsedChildrenCommentsIndex.containsKey(comment.getId())) {
                commentViewHolder.tvComment.setText(
                        (mCollapsedChildrenCommentsIndex.get(comment.getId()).size() + 1)
                                + " comments collapsed");
                commentViewHolder.tvComment.setMinLines(2);
                commentViewHolder.tvComment.setGravity(Gravity.CENTER);
            } else {
                commentViewHolder.tvComment.setText(Html.fromHtml(comment.getText()));
                commentViewHolder.tvComment.setMinLines(Integer.MIN_VALUE);
                commentViewHolder.tvComment.setGravity(Gravity.LEFT);
            }

            if (mCollapsedOlderCommentsIndex.containsKey(comment.getId())) {
                commentViewHolder.tvCollapseOlderComments.setVisibility(View.VISIBLE);
                commentViewHolder.tvCollapseOlderComments.setText(
                        " +" + mCollapsedOlderCommentsIndex.get(comment.getId()).size() + " ");
            } else {
                commentViewHolder.tvCollapseOlderComments.setVisibility(View.GONE);
            }
        } else if (viewHolder instanceof CommentHeaderViewHolder) {
            CommentHeaderViewHolder commentHeaderViewHolder = (CommentHeaderViewHolder) viewHolder;
            Post post = (Post) mItemList.get(0);
            commentHeaderViewHolder.tvTitle.setText(post.getTitle());
            Typeface postFont = Typeface.createFromAsset(mContext.getAssets(),
                    SharedPrefsManager.getPostFont(prefs) + ".ttf");
            commentHeaderViewHolder.tvTitle.setTypeface(postFont);
            commentHeaderViewHolder.tvTitle
                    .setTextSize(SharedPrefsManager.getCommentFontSize(prefs) * 1.5f);
            commentHeaderViewHolder.tvTitle.setPaintFlags(
                    commentHeaderViewHolder.tvTitle.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
            commentHeaderViewHolder.tvUrl.setText(post.getUrl());
            commentHeaderViewHolder.tvPoints.setText("+" + String.valueOf(post.getScore()));
            commentHeaderViewHolder.tvComments.setText(String.valueOf(post.getDescendants())
                    + (post.getDescendants() > 1 ? " comments" : " comment"));
            commentHeaderViewHolder.tvTime.setText(Utils.formatTime(post.getTime()));
            commentHeaderViewHolder.tvPoster.setText("by: " + post.getBy());
            if (post.getText() != null && !post.getText().isEmpty()) {
                commentHeaderViewHolder.tvContent.setVisibility(View.VISIBLE);
                commentHeaderViewHolder.tvContent.setText(Html.fromHtml(post.getText()));
                commentHeaderViewHolder.tvContent
                        .setTextSize(SharedPrefsManager.getCommentFontSize(prefs));
                Typeface commentFont = Typeface.createFromAsset(mContext.getAssets(),
                        SharedPrefsManager.getCommentFont(prefs) + ".ttf");
                commentHeaderViewHolder.tvContent.setTypeface(commentFont);
                commentHeaderViewHolder.tvContent.
                        setLineSpacing(0, SharedPrefsManager.getCommentLineHeight(prefs));
            }
        } else if (viewHolder instanceof CommentFooterViewHolder) {
            CommentFooterViewHolder commentFooterViewHolder = (CommentFooterViewHolder) viewHolder;
            if (mLoadingState == Constants.LOADING_FINISH
                    || mLoadingState == Constants.LOADING_ERROR) {
                commentFooterViewHolder.progressBar.setVisibility(View.GONE);
                commentFooterViewHolder.tvNoCommentPromt.setVisibility(View.GONE);
            } else if (mLoadingState == Constants.LOADING_PROMPT_NO_CONTENT) {
                commentFooterViewHolder.progressBar.setVisibility(View.GONE);
                commentFooterViewHolder.tvNoCommentPromt.setText(
                        mContext.getResources().getString(R.string.no_comment_promt));
            } else if (mLoadingState == Constants.LOADING_IN_PROGRESS) {
                commentFooterViewHolder.progressBar.setVisibility(View.VISIBLE);
                commentFooterViewHolder.tvNoCommentPromt.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mItemList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mItemList.get(position) instanceof Post) {
            return VIEW_TYPE_HEADER;
        } else if (mItemList.get(position) instanceof HNItem.Footer) {
            return VIEW_TYPE_FOOTER;
        } else {
            return VIEW_TYPE_COMMENT;
        }
    }

    public void add(HNItem item) {
        mItemList.add(item);
    }

    public void addHeader(Post post) {
        mItemList.add(0, post);
        notifyItemInserted(0);
    }

    public void addComment(Comment comment) {
        mItemList.add(mItemList.size() - 1, comment);
        notifyItemInserted(mItemList.indexOf(comment));
    }

    public void addFooter(HNItem.Footer footer) {
        mItemList.add(footer);
        notifyItemInserted(mItemList.size() - 1);
    }

    public void updateFooter(int loadingState) {
        mLoadingState = loadingState;
        notifyItemChanged(mItemList.size() - 1);
    }

    public void clear() {
        mItemList.clear();
    }

    public void collapseChildrenComment(int position) {
        ArrayList<Comment> childrenComments = new ArrayList<>();
        Comment parentComment = (Comment) mItemList.get(position);
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        // if a comment header is not visible when collapsing, scroll to it's header
        if (position != linearLayoutManager.findFirstCompletelyVisibleItemPosition()) {
            linearLayoutManager.scrollToPosition(position);
        }

        for (int curPosition = position + 1;
             (mItemList.get(curPosition) instanceof Comment
                     && ((Comment) mItemList.get(curPosition)).getLevel() > parentComment.getLevel());
             curPosition++) {
            childrenComments.add((Comment) mItemList.get(curPosition));
        }

        if (!childrenComments.isEmpty()) {
            mCollapsedChildrenCommentsIndex.put(parentComment.getId(), childrenComments);
            for (Comment comment : childrenComments) {
                mItemList.remove(comment);
            }
            notifyItemChanged(position);
            notifyItemRangeRemoved(position + 1, childrenComments.size());
        }
    }

    public void expandChildrenComments(int position) {
        Comment parentComment = (Comment) mItemList.get(position);
        List<Comment> collapsedComments =
                mCollapsedChildrenCommentsIndex.get(parentComment.getId());
        int insertPosition = mItemList.indexOf(parentComment) + 1;
        for (Comment comment : collapsedComments) {
            mItemList.add(insertPosition, comment);
            insertPosition++;
        }
        notifyItemRangeInserted(mItemList.indexOf(parentComment) + 1,
                collapsedComments.size());
        notifyItemChanged(position);
        mCollapsedChildrenCommentsIndex.remove(parentComment.getId());
    }

    public boolean isChildrenCommentsCollapsed(int position) {
        Comment Comment = (Comment) mItemList.get(position);
        return mCollapsedChildrenCommentsIndex.containsKey(Comment.getId());
    }

    public void collapseInBetweenComments(int position) {
        ArrayList<Comment> olderComments = new ArrayList<>();
        Comment curComment = (Comment) mItemList.get(position);
        for (int curPosition = position - 1;
             curComment.getLevel() > 0
                     && ((Comment) mItemList.get(curPosition)).getLevel() >= curComment.getLevel()
                     && curPosition > 0;
             curPosition--) {
            olderComments.add(0, (Comment) mItemList.get(curPosition));
        }

        if (!olderComments.isEmpty()) {
            mCollapsedOlderCommentsIndex.put(curComment.getId(), olderComments);
            for (Comment comment : olderComments) {
                mItemList.remove(comment);
            }
            notifyItemChanged(position);
            notifyItemRangeRemoved(position - olderComments.size(), olderComments.size());
        }
    }

    public void expandInBetweenComments(int position) {
        Comment clingedComment = (Comment) mItemList.get(position);
        List<Comment> olderComments = mCollapsedOlderCommentsIndex.get(clingedComment.getId());
        int insertPosition = position;
        for (Comment comment : olderComments) {
            mItemList.add(insertPosition, comment);
            insertPosition++;
        }

        notifyItemChanged(position);
        notifyItemRangeInserted(position, olderComments.size());
        mCollapsedOlderCommentsIndex.remove(clingedComment.getId());

    }

    public boolean isInBetweenCommentsCollapsed(int position) {
        Comment Comment = (Comment) mItemList.get(position);
        return mCollapsedOlderCommentsIndex.containsKey(Comment.getId());
    }

    public void updateCommentPrefs() {
        mFont = Typeface.createFromAsset(mContext.getAssets(),
                SharedPrefsManager.getCommentFont(prefs) + ".ttf");
        mTextSize = SharedPrefsManager.getCommentFontSize(prefs);
        mLineHeight = SharedPrefsManager.getCommentLineHeight(prefs);
    }

    private void setCommentIndentStyle() {
        mCommentIndentWidth = Utils.convertDpToPixels(CommentViewHolder.UNIT_COMMENT_INDENT_DP, mContext);
        if (SharedPrefsManager.getTheme(prefs).equals(SharedPrefsManager.THEME_LIGHT)) {
            mCommentIndentColorOrange = 0xFFffa726; //orange_400
            mCommentIndentColorBg = 0xFFEEEEEE; //grey_200
        } else if (SharedPrefsManager.getTheme(prefs).equals(SharedPrefsManager.THEME_DARK)) {
            mCommentIndentColorOrange = 0xFFe65100; //orange_900
            mCommentIndentColorBg = 0xFF212121; //grey_900
        } else {
            mCommentIndentColorOrange = 0xFFffa726; //orange_400
            mCommentIndentColorBg = 0xFFF4ECD8; //sepia
        }
    }

}

