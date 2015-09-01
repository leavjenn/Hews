package com.leavjenn.hews.ui.widget;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.DatePicker;

import com.leavjenn.hews.R;

import java.util.Calendar;
import java.util.TimeZone;

public class DateRangeDialogFragment extends DialogFragment {
    onDateSetListener mListener;
    private int startYear, startMonth, startDay, endYear, endMonth, endDay;

    public DateRangeDialogFragment() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Inflate your custom layout containing 2 DatePickers
        View customView = getActivity().getLayoutInflater().inflate(R.layout.dialog_time_range, null);
        builder.setView(customView); // Set the view of the dialog to your custom layout
        // Define your date pickers
        final DatePicker dpStart = (DatePicker) customView.findViewById(R.id.dpStartDate);
        final DatePicker dpEnd = (DatePicker) customView.findViewById(R.id.dpEndDate);
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
        long curTime = c.getTimeInMillis();
        dpStart.setMaxDate(curTime);
        dpEnd.setMaxDate(curTime);
        // 1160418110000 = 10/10/2006, the time when the first post has been submitted
        dpStart.setMinDate(1160418110000l);
        dpEnd.setMinDate(1160418110000l);
        builder.setTitle("Select start and end date");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startYear = dpStart.getYear();
                startMonth = dpStart.getMonth();
                startDay = dpStart.getDayOfMonth();
                endYear = dpEnd.getYear();
                endMonth = dpEnd.getMonth();
                endDay = dpEnd.getDayOfMonth();
                mListener.onDateSet(startYear, startMonth, startDay, endYear, endMonth, endDay);
                dialog.dismiss();
            }
        });
        return builder.create();
    }

    public void setOnDateSetListner(onDateSetListener listener) {
        mListener = listener;
    }

    public interface onDateSetListener {
        void onDateSet(int startYear, int startMonth, int startDay, int endYear, int endMonth, int endDay);
    }
}
