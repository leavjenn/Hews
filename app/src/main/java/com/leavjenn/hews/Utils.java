package com.leavjenn.hews;

import android.content.Context;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.webkit.URLUtil;
import android.widget.Toast;

public class Utils {
    public static CharSequence formatTime(long timeStamp) {
        timeStamp = timeStamp * 1000;
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(timeStamp,
                System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
        return timeAgo;
    }

    public static float convertPixelsToDp(float px, Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return px / (metrics.densityDpi / 160f);
    }

    public static int convertDpToPixels(int dp, Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }


    public static int getScreenHeight(Context context) {
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

    public static void showOfflineToast(Context context) {
        Toast.makeText(context, "No connection:)", Toast.LENGTH_LONG).show();
    }

    public static void showLongToast(Context context, CharSequence text) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    // uri scheme can only lower case char
    public static Uri validateAndParseUri(String uriString, long postId) {
        if (URLUtil.isValidUrl(uriString)) {
//        if (Patterns.WEB_URL.matcher(uriString).matches()) {
            if (uriString.startsWith("Http")) {
                Log.i("validateAndParseUri", "http" + uriString.substring(4));
                return Uri.parse("http" + uriString.substring(4));
            } else { // TODO other conditions
                return Uri.parse(uriString);
            }
        } else {
            return Uri.parse("https://news.ycombinator.com/item?id=" + postId);
        }
//        }
    }
}
