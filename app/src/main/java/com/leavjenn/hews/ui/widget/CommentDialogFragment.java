package com.leavjenn.hews.ui.widget;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.leavjenn.hews.R;

public class CommentDialogFragment extends DialogFragment {
    OnCommentDialogClickListener mListener;

    public CommentDialogFragment() {
        // Required empty public constructor
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
                        mListener.onUpVote();
                        break;
                    case 1:
                        mListener.onDownVote();
                        break;
                    case 2:
                        mListener.onReply();
                        break;
                    case 3:
                        mListener.onAuthorProfile();
                        break;
                    case 4:
                        mListener.onShare();
                        break;
                    case 5:
                        mListener.onShareCommentTextTo();
                        break;
                }
            }
        });
        return builder.create();
    }

    public void setOnListDialogClickListener(OnCommentDialogClickListener listener) {
        mListener = listener;
    }

    public interface OnCommentDialogClickListener {
        void onUpVote();

        void onDownVote();

        void onReply();

        void onAuthorProfile();

        void onShare();

        void onShareCommentTextTo();
    }
}
