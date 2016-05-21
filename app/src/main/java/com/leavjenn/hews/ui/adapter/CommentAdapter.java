package com.leavjenn.hews.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.customtabs.CustomTabsIntent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.leavjenn.hews.Constants;
import com.leavjenn.hews.R;
import com.leavjenn.hews.misc.Utils;
import com.leavjenn.hews.misc.ChromeCustomTabsHelper;
import com.leavjenn.hews.misc.HTMLCodeTagHandler;
import com.leavjenn.hews.misc.SharedPrefsManager;
import com.leavjenn.hews.model.Comment;
import com.leavjenn.hews.model.HNItem;
import com.leavjenn.hews.model.Post;
import com.leavjenn.hews.ui.comment.CommentsActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_COMMENT = 1;
    private static final int VIEW_TYPE_FOOTER = 2;

    private int mLoadingState;
    private int mCommentIndentColorOrange, mCommentIndentColorBg, mCommentIndentWidth;
    private Typeface mFont;
    private float mTextSize, mLineHeight;
    private HTMLCodeTagHandler mHTMLCodeTagHandler;
    private Map<Long, List<Comment>> mCollapsedChildrenCommentsIndex;
    private Map<Long, List<Comment>> mCollapsedOlderCommentsIndex;
    private Context mContext;
    private RecyclerView mRecyclerView;
    private ArrayList<HNItem> mItemList;
    private ArrayList<Comment> mCommentList;
    private SharedPreferences prefs;

    public CommentAdapter(Context context, RecyclerView recyclerView) {
        mContext = context;
        mRecyclerView = recyclerView;
        mItemList = new ArrayList<>();
        mCommentList = new ArrayList<>();
        mHTMLCodeTagHandler = new HTMLCodeTagHandler();
        mCollapsedChildrenCommentsIndex = new HashMap<>();
        mCollapsedOlderCommentsIndex = new HashMap<>();
        prefs = ((CommentsActivity) mContext).getSharedPreferences();
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
                    R.layout.list_item_comment, viewGroup, false);
                CommentViewHolder vh = new CommentViewHolder(v, setupViewHolderClickListener());
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
                    .inflate(R.layout.list_item_footer, viewGroup, false);
                viewHolder = new FooterViewHolder(v);
        }
        return viewHolder;
    }

    private CommentViewHolder.ViewHolderClickListener setupViewHolderClickListener() {
        return new CommentViewHolder.ViewHolderClickListener() {
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
            }
        };
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof CommentViewHolder) {
            // which one is better?
//        if (getItemViewType(position) == VIEW_TYPE_COMMENT) {
            bindCommentViewHolder(viewHolder, position);
        } else if (viewHolder instanceof CommentHeaderViewHolder) {
            bindHeaderViewHolder(viewHolder);
        } else if (viewHolder instanceof FooterViewHolder) {
            bindFooterViewHolder(viewHolder);
        }
    }

    private void bindCommentViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        CommentViewHolder commentViewHolder = (CommentViewHolder) viewHolder;
        Comment comment = (Comment) mItemList.get(position);
        commentViewHolder.setCommentIndent(comment.getLevel());
        commentViewHolder.tvTime.setText(Utils.formatTime(comment.getTime()));
        if (!comment.getDeleted()) {
            commentViewHolder.tvAuthor.setText(comment.getBy());
        }

        if (mCollapsedChildrenCommentsIndex.containsKey(comment.getCommentId())) {
            commentViewHolder.tvComment.setText(mContext.getString(R.string.comments_collapsed_prompt,
                mCollapsedChildrenCommentsIndex.get(comment.getCommentId()).size() + 1));
            commentViewHolder.tvComment.setMinLines(2);
            commentViewHolder.tvComment.setGravity(Gravity.CENTER);
        } else {
            setTextViewHTML(commentViewHolder.tvComment, comment.getText());
            commentViewHolder.tvComment.setMinLines(Integer.MIN_VALUE);
            commentViewHolder.tvComment.setGravity(Gravity.LEFT);
        }

        if (mCollapsedOlderCommentsIndex.containsKey(comment.getCommentId())) {
            commentViewHolder.tvCollapseOlderComments.setVisibility(View.VISIBLE);
            commentViewHolder.tvCollapseOlderComments.setText(
                mContext.getString(R.string.comments_collapsed_prompt,
                    mCollapsedOlderCommentsIndex.get(comment.getCommentId()).size()));
        } else {
            commentViewHolder.tvCollapseOlderComments.setVisibility(View.GONE);
        }
    }

    private void bindHeaderViewHolder(RecyclerView.ViewHolder viewHolder) {
        CommentHeaderViewHolder commentHeaderViewHolder = (CommentHeaderViewHolder) viewHolder;
        Post post = (Post) mItemList.get(0);
        commentHeaderViewHolder.tvTitle.setText(post.getTitle());
        Typeface postFont = Typeface.createFromAsset(mContext.getAssets(),
            SharedPrefsManager.getPostFont(prefs) + ".ttf");
        commentHeaderViewHolder.tvTitle.setTypeface(postFont);
        commentHeaderViewHolder.tvTitle.setTextSize(SharedPrefsManager.getCommentFontSize(prefs) * 1.5f);
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
            commentHeaderViewHolder.tvContent.setTextSize(SharedPrefsManager.getCommentFontSize(prefs));
            Typeface commentFont = Typeface.createFromAsset(mContext.getAssets(),
                SharedPrefsManager.getCommentFont(prefs) + ".ttf");
            commentHeaderViewHolder.tvContent.setTypeface(commentFont);
            commentHeaderViewHolder.tvContent.
                setLineSpacing(0, SharedPrefsManager.getCommentLineHeight(prefs));
        }
    }

    private void bindFooterViewHolder(RecyclerView.ViewHolder viewHolder) {
        FooterViewHolder footerViewHolder = (FooterViewHolder) viewHolder;
        if (mLoadingState == Constants.LOADING_FINISH) {
            footerViewHolder.progressBar.setVisibility(View.GONE);
            footerViewHolder.tvPrompt.setVisibility(View.GONE);
        } else if (mLoadingState == Constants.LOADING_PROMPT_NO_CONTENT) {
            footerViewHolder.progressBar.setVisibility(View.GONE);
            footerViewHolder.tvPrompt.setText(
                mContext.getResources().getString(R.string.prompt_no_comments));
        } else if (mLoadingState == Constants.LOADING_IN_PROGRESS) {
            footerViewHolder.progressBar.setVisibility(View.VISIBLE);
            footerViewHolder.tvPrompt.setVisibility(View.GONE);
        } else if (mLoadingState == Constants.LOADING_ERROR) {
            footerViewHolder.progressBar.setVisibility(View.GONE);
            footerViewHolder.tvPrompt.setText(
                mContext.getResources().getString(R.string.prompt_comments_loading_error));
        }
    }

    private void setTextViewHTML(TextView textView, String string) {
        // trim two trailing blank lines
        CharSequence sequence =
            Html.fromHtml(string.replace("<p>", "<br /><br />").replace("\n", "<br />"), null, mHTMLCodeTagHandler);
        // use Chrome custom tab if it is available
        if (mContext instanceof CommentsActivity
            && ((CommentsActivity) mContext).getChromeCustomTabsHelper() != null) {
            SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
            URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);
            for (URLSpan span : urls) {
                makeLinkClickable(strBuilder, span);
            }
            textView.setText(strBuilder);
        } else {
            textView.setText(sequence);
        }
    }

    private void makeLinkClickable(SpannableStringBuilder strBuilder, final URLSpan span) {
        int start = strBuilder.getSpanStart(span);
        int end = strBuilder.getSpanEnd(span);
        int flags = strBuilder.getSpanFlags(span);
        ClickableSpan clickable = new ClickableSpan() {
            public void onClick(View view) {
                Uri uri = Uri.parse(span.getURL());
                CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder();
                Utils.setupIntentBuilder(intentBuilder, mContext,
                    ((CommentsActivity) mContext).getSharedPreferences());
                ChromeCustomTabsHelper.openCustomTab((Activity) mContext, intentBuilder.build(),
                    uri, null);
            }
        };
        strBuilder.setSpan(clickable, start, end, flags);
        strBuilder.removeSpan(span);
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
        mCommentList.add(comment);
    }

    public void addFooter(HNItem.Footer footer) {
        mItemList.add(footer);
        notifyItemInserted(mItemList.size() - 1);
    }

    public void addAllComments(List<Comment> commentList) {
        mItemList.addAll(mItemList.size() - 1, commentList);
        notifyDataSetChanged();
        mCommentList.addAll(commentList);
    }

    public void updateFooter(int loadingState) {
        mLoadingState = loadingState;
    }

    public ArrayList<Comment> getCommentList() {
        return mCommentList;
    }

    public void clear() {
        mItemList.clear();
        mCommentList.clear();
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
            mCollapsedChildrenCommentsIndex.put(parentComment.getCommentId(), childrenComments);
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
            mCollapsedChildrenCommentsIndex.get(parentComment.getCommentId());
        int insertPosition = mItemList.indexOf(parentComment) + 1;
        for (Comment comment : collapsedComments) {
            mItemList.add(insertPosition, comment);
            insertPosition++;
        }
        notifyItemRangeInserted(mItemList.indexOf(parentComment) + 1,
            collapsedComments.size());
        notifyItemChanged(position);
        mCollapsedChildrenCommentsIndex.remove(parentComment.getCommentId());
    }

    public boolean isChildrenCommentsCollapsed(int position) {
        Comment Comment = (Comment) mItemList.get(position);
        return mCollapsedChildrenCommentsIndex.containsKey(Comment.getCommentId());
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
            mCollapsedOlderCommentsIndex.put(curComment.getCommentId(), olderComments);
            for (Comment comment : olderComments) {
                mItemList.remove(comment);
            }
            notifyItemChanged(position);
            notifyItemRangeRemoved(position - olderComments.size(), olderComments.size());
        }
    }

    public void expandInBetweenComments(int position) {
        Comment clingedComment = (Comment) mItemList.get(position);
        List<Comment> olderComments = mCollapsedOlderCommentsIndex.get(clingedComment.getCommentId());
        int insertPosition = position;
        for (Comment comment : olderComments) {
            mItemList.add(insertPosition, comment);
            insertPosition++;
        }

        notifyItemChanged(position);
        notifyItemRangeInserted(position, olderComments.size());
        mCollapsedOlderCommentsIndex.remove(clingedComment.getCommentId());

    }

    public boolean isInBetweenCommentsCollapsed(int position) {
        Comment Comment = (Comment) mItemList.get(position);
        return mCollapsedOlderCommentsIndex.containsKey(Comment.getCommentId());
    }

        final Comment comment = (Comment) mItemList.get(position);
        // scrollToPosition() not working
        ((LinearLayoutManager) mRecyclerView.getLayoutManager()).scrollToPositionWithOffset(position, 0);
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
        } else if (SharedPrefsManager.getTheme(prefs).equals(SharedPrefsManager.THEME_AMOLED_BLACK)) {
            mCommentIndentColorOrange = 0xFFe65100; //orange_900
            mCommentIndentColorBg = 0xFF000000; //black
        } else {
            mCommentIndentColorOrange = 0xFFffa726; //orange_400
            mCommentIndentColorBg = 0xFFF4ECD8; //sepia
        }
    }
}

