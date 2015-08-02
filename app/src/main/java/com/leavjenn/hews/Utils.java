package com.leavjenn.hews;

import android.content.Context;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class Utils {
    public static CharSequence formatTime(long timeStamp) {
        timeStamp = timeStamp * 1000;
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(timeStamp,
                System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
        return timeAgo;
    }

    public static float convertPixelsToDp(float px, Context context){
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return px / (metrics.densityDpi / 160f);
    }

    public static int convertDpToPixels(int dp, Context context){
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }


    public static int getScreenHeight(Context context){
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.y;
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo() != null;
    }
}
