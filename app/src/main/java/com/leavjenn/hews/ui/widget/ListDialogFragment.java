package com.leavjenn.hews.ui.widget;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.leavjenn.hews.R;

public class ListDialogFragment extends DialogFragment {
    OnListDialogClickListener mListener;

    public ListDialogFragment() {
        // Required empty public constructor
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setItems(R.array.dialog, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        mListener.onUpvote();
                        break;
                    case 1:
                        mListener.onReply();
                        break;
                    case 2:
                        mListener.onAuthorProfile();
                        break;
                    case 3:
                        mListener.onShare();
                        break;
                    case 4:
                        mListener.onShareCommentTextTo();
                        break;
                }
            }
        });
        return builder.create();
    }

    public void setOnListDialogClickListener(OnListDialogClickListener listener) {
        mListener = listener;
    }

    public interface OnListDialogClickListener {
        void onUpvote();

        void onReply();

        void onAuthorProfile();

        void onShare();

        void onShareCommentTextTo();
    }
}
