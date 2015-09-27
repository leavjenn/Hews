package com.leavjenn.hews.ui.widget;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.leavjenn.hews.R;
import com.leavjenn.hews.Utils;

public class LoginDialogFragment extends DialogFragment {
    OnLoginListener mListener;
    String mNote;
    TextInputLayout tiLayoutName;
    TextInputLayout tiLayoutPassword;
    ProgressBar progress;
    TextView tvHint;

    public LoginDialogFragment() {
        // Required empty public constructor
    }

    public static LoginDialogFragment newInstance(OnLoginListener mOnLoginListener, String note) {
        LoginDialogFragment fragment = new LoginDialogFragment();
        fragment.mListener = mOnLoginListener;
        fragment.mNote = note;
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_login, null);

        tiLayoutName = (TextInputLayout) v.findViewById(R.id.tilayout_user_name);
        tiLayoutPassword = (TextInputLayout) v.findViewById(R.id.tilayout_password);
        progress = (ProgressBar) v.findViewById(R.id.progressbar_login);
        tvHint = (TextView) v.findViewById(R.id.tv_hint);
        builder.setView(v)
                .setPositiveButton("Login", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
//                                String username = tiLayoutName.getEditText().getText().toString();
//                                String password = tiLayoutPassword.getEditText().getText().toString();
//                                if (Utils.isOnline(LoginDialogFragment.this.getActivity())) {
//                                    if (!username.isEmpty() && password.length() >= 8) {
//
//                                        ProgressDialog progressDialog = new ProgressDialog(getActivity());
//                                        progressDialog.setMessage("Logging in...");
//                                        progressDialog.show();
//                                        mListener.onLogin(username, password);
//                                        tiLayoutName.setVisibility(View.GONE);
//                                        tiLayoutPassword.setVisibility(View.GONE);
//                                        progress.setVisibility(View.VISIBLE);
//                                    }
//                                } else {
//                                    Toast.makeText(LoginDialogFragment.this.getActivity(),
//                                            "No connection:)", Toast.LENGTH_LONG).show();
//                                }
                            }
                        }
                )
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }
                )
                .setTitle("Login");

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        final AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            Button positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String username = tiLayoutName.getEditText().getText().toString();
                    String password = tiLayoutPassword.getEditText().getText().toString();
                    if (Utils.isOnline(LoginDialogFragment.this.getActivity())) {
                        if (!username.isEmpty() && password.length() >= 8) {
                            mListener.onLogin(username, password);
                            tiLayoutName.setVisibility(View.GONE);
                            tiLayoutPassword.setVisibility(View.GONE);
                            progress.setVisibility(View.VISIBLE);
                            tvHint.setText(R.string.login_hint_logging_in);
                        } else if (username.isEmpty()) {
                            tvHint.setText(R.string.login_hint_error_empty_username);
                        } else if (password.length() < 8) {
                            tvHint.setText(R.string.login_hint_error_short_password);
                        }
                    } else {
                        tvHint.setText(R.string.login_hint_error_offline);
                    }
                }
            });
        }
    }

    public void resetLogin() {
        tiLayoutName.setVisibility(View.VISIBLE);
        tiLayoutPassword.setVisibility(View.VISIBLE);
        progress.setVisibility(View.GONE);
        tvHint.setText(R.string.login_hint_error_wrong_info);
    }

    public void setListener(OnLoginListener onLoginListener) {
        mListener = onLoginListener;
    }

    public interface OnLoginListener {
        void onLogin(String username, String password);
    }
}
