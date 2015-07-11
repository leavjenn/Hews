package com.leavjenn.hews.ui.widget;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.leavjenn.hews.R;

public class ListDialogFragment extends DialogFragment {
    OnListDialogClickListner mListener;

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
                        mListener.onReplyViaBrowserClick(ListDialogFragment.this);
                        break;
                    case 1:
                        mListener.onAuthorProfileClick(ListDialogFragment.this);
                        break;
                    case 2:
                        mListener.onShareClick(ListDialogFragment.this);
                        break;
                    case 3:
                        mListener.onShareCommentTextToClick(ListDialogFragment.this);
                        break;
                }
            }
        });
        return builder.create();
    }

    public void setOnListDialogClickListner(OnListDialogClickListner listener) {
        mListener = listener;
    }

    public interface OnListDialogClickListner {
        void onReplyViaBrowserClick(DialogFragment dialog);

        void onShareClick(DialogFragment dialog);

        void onAuthorProfileClick(DialogFragment dialog);

        void onShareCommentTextToClick(DialogFragment dialog);
    }
}
