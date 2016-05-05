package com.leavjenn.hews.ui.widget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.leavjenn.hews.R;

public class FeedbackDialogFragment extends DialogFragment {
    OnFeedbackListClickListener mListener;

    public FeedbackDialogFragment() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        return builder.setTitle(R.string.feedback_dialog_title)
            .setItems(R.array.dialog_feedback, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mListener != null) {
                        switch (which) {
                            case 0:
                                mListener.onSelectTwitter();
                                break;
                            case 1:
                                mListener.onSelectGooglePlus();
                                break;
                            case 2:
                                mListener.onSelectEmail();
                                break;
                            case 3:
                                mListener.onSelectGooglePlayReview();
                                break;
                        }
                    }
                }
            }).create();
    }

    public void setOnFeedbackListClickListener(OnFeedbackListClickListener listener) {
        mListener = listener;
    }

    public interface OnFeedbackListClickListener {
        void onSelectTwitter();

        void onSelectGooglePlus();

        void onSelectEmail();

        void onSelectGooglePlayReview();
    }
}
