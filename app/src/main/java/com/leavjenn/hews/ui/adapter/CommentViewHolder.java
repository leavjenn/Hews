package com.leavjenn.hews.ui.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.support.v7.widget.RecyclerView;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.leavjenn.hews.R;

public class CommentViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
        View.OnLongClickListener {
    public TextView ivIndent;
    public TextView tvComment;
    public TextView tvAuthor;
    public TextView tvTime;
    public TextView tvCollapsedChildrenCommentsCount;
    public TextView tvCollapseOlderComments;
    ViewHolderClickListener mListener;
    final static int UNIT_COMMENT_INDENT_PIXEL = 15;


    public CommentViewHolder(View v, ViewHolderClickListener listener) {
        super(v);
        ivIndent = (TextView) v.findViewById(R.id.iv_indent);
        ivIndent.setBackground(getStripeDrawable());
        tvComment = (TextView) v.findViewById(R.id.tv_comment);
        // enable link clicking. If not setOnClickListener, itemView ClickListener will not work
        tvComment.setOnClickListener(this);
        tvComment.setMovementMethod(LinkMovementMethod.getInstance());
        tvComment.setOnLongClickListener(this);
        tvAuthor = (TextView) v.findViewById(R.id.tv_author);
        tvTime = (TextView) v.findViewById(R.id.tv_time);
        tvCollapsedChildrenCommentsCount =
                (TextView) v.findViewById(R.id.tv_collapsed_children_comments_count);
        tvCollapseOlderComments = (TextView) v.findViewById(R.id.tv_collapse_older_comments);
        v.setOnClickListener(this);
        v.setOnLongClickListener(this);
        mListener = listener;
    }

    public Drawable getStripeDrawable() {
        ShapeDrawable bg = new ShapeDrawable(new RectShape());
        int[] pixels = new int[UNIT_COMMENT_INDENT_PIXEL];
        for (int i = 0; i < 5; i++) {
            pixels[i] = 0xFFffa726;
            pixels[i + 5] = 0xFFffa726;
            pixels[UNIT_COMMENT_INDENT_PIXEL - 1 - i] = 0xFFEEEEEE;
        }
        Bitmap bm = Bitmap.createBitmap(pixels, UNIT_COMMENT_INDENT_PIXEL, 1, Bitmap.Config.ARGB_8888);
        Shader shader = new BitmapShader(bm,
                Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        bg.getPaint().setShader(shader);
        return bg;
    }

    public void setCommentIndent(int level) {
        ivIndent.getLayoutParams().width = level * UNIT_COMMENT_INDENT_PIXEL;
        ivIndent.requestLayout();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_comment) {
            mListener.onClickComment(getPosition());
        } else {
            mListener.onClick(getPosition());
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (mListener != null) {
            mListener.onLongClick(getPosition());
        }
        return true;
    }

    public interface ViewHolderClickListener {
        void onClick(int position);

        void onClickComment(int position);

        void onLongClick(int position);
    }
}
