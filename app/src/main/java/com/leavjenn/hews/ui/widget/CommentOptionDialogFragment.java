package com.leavjenn.hews.ui.widget;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.leavjenn.hews.R;
import com.leavjenn.hews.model.Comment;

public class CommentOptionDialogFragment extends DialogFragment {
    OnSelectCommentListener mListener;
    Comment mComment;

    public CommentOptionDialogFragment() {
        // Required empty public constructor
    }

    public static CommentOptionDialogFragment newInstance(OnSelectCommentListener listener, Comment comment) {
        CommentOptionDialogFragment fragment = new CommentOptionDialogFragment();
        fragment.mListener = listener;
        fragment.mComment = comment;
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setItems(R.array.dialog_comment, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        mListener.onUpVote(mComment);
                        break;
                    case 1:
                        mListener.onDownVote(mComment);
                        break;
                    case 2:
                        mListener.onReply(mComment);
                        break;
                    case 3:
                        mListener.onAuthorProfile(mComment);
                        break;
                    case 4:
                        mListener.onShare(mComment);
                        break;
                    case 5:
                        mListener.onShareCommentTextTo(mComment);
                        break;
                }
            }
        });
        return builder.create();
    }

    public void setOnSelectCommentListener(OnSelectCommentListener listener) {
        mListener = listener;
    }

    public void setSelectedComment(Comment comment) {
        mComment = comment;
    }

    public interface OnSelectCommentListener {
        void onUpVote(Comment comment);

        void onDownVote(Comment comment);

        void onReply(Comment comment);

        void onAuthorProfile(Comment comment);

        void onShare(Comment comment);

        void onShareCommentTextTo(Comment comment);
    }
}
