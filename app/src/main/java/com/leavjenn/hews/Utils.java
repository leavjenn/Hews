package com.leavjenn.hews;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.support.annotation.StringRes;
import android.support.customtabs.CustomTabsIntent;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.webkit.URLUtil;
import android.widget.Toast;

import com.leavjenn.hews.misc.ShareBroadcastReceiver;
import com.leavjenn.hews.misc.SharedPrefsManager;

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

    public static void showLongToast(Context context, CharSequence text) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    public static void showLongToast(Context context, @StringRes int ResId) {
        Toast.makeText(context, ResId, Toast.LENGTH_LONG).show();
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

    public static void setupIntentBuilder(CustomTabsIntent.Builder intentBuilder, Context context, SharedPreferences prefs) {
        if (SharedPrefsManager.getTheme(prefs).equals(SharedPrefsManager.THEME_DARK)) {
            intentBuilder.setToolbarColor(context.getResources().getColor(R.color.grey_900));
        } else if (SharedPrefsManager.getTheme(prefs).equals(SharedPrefsManager.THEME_AMOLED_BLACK)) {
            intentBuilder.setToolbarColor(context.getResources().getColor(android.R.color.black));
        } else {
            // use darker orange color here so chrome toolbar will fit dark theme
            intentBuilder.setToolbarColor(context.getResources().getColor(R.color.orange_800));
        }
        intentBuilder.enableUrlBarHiding();
        intentBuilder.setShowTitle(true);
        intentBuilder.setCloseButtonIcon(
                BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_arrow_back));
        // share link option
        String menuItemTitle = context.getString(R.string.share_link_to);
        Intent actionIntent = new Intent(context.getApplicationContext(), ShareBroadcastReceiver.class);
        PendingIntent menuItemPendingIntent =
                PendingIntent.getBroadcast(context.getApplicationContext(), 0, actionIntent, 0);
        intentBuilder.addMenuItem(menuItemTitle, menuItemPendingIntent);
        // TODO Use this way, it will show share to option when long click
//            String shareLabel = getString(R.string.share_link_to);
//            Intent shareIntent = new Intent();
//            shareIntent.setAction(Intent.ACTION_SEND);
//            String postUrl = post.getUrl();
//            shareIntent.putExtra(Intent.EXTRA_TEXT, postUrl);
//            shareIntent.setType("text/plain");
//            shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            Intent chooserIntent = Intent.createChooser(shareIntent, getString(R.string.share_link_to));
//            startActivity(chooserIntent);
//            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
//                    shareIntent, 0);
//            intentBuilder.addMenuItem(shareLabel, pendingIntent);
    }
}
