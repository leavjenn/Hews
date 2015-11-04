package com.leavjenn.hews;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsManager {
    public static final String KEY_POST_FONT = "key_post_font";
    public static final String KEY_POST_FONT_SIZE = "key_post_font_size";
    public static final String KEY_POST_LINE_HEIGHT = "key_post_line_height";
    public static final String KEY_COMMENT_FONT = "key_comment_font";
    public static final String KEY_COMMENT_FONT_SIZE = "key_comment_font_size";
    public static final String KEY_COMMENT_LINE_HEIGHT = "key_comment_line_height";
    public static final String KEY_THEME = "themekey";
    public static final String KEY_USERNAME = "user_name";
    public static final String KEY_LOGIN_COOKIE = "login_cookie";
    public static final String KEY_REPLY_TEXT = "replying_text";
    public static final String THEME_DARK = "0";
    public static final String THEME_SEPIA = "1";
    public static final String THEME_LIGHT = "2";
    public static final String KEY_FAB_MODE = "fabkey";
    public static final String FAB_DISABLE = "0";
    public static final String FAB_DRAG_SCROLL_DOWN = "1";
    public static final String FAB_PRESS_SCROLL_DOWN = "2";
    public static final String KEY_SHOW_POST_SUMMARY = "key_show_post_summary";

    static String[] fontsForComment = {"PT Sans", "Roboto", "Lato",
            "Open Sans", "Muli", "Slabo 27px", "Crimson Text", "Roboto Slab",
            "Vollkorn", "Merriweather"};
    static String[] fontsForPost = {"Open Sans", "Dosis SemiBold", "Roboto Slab", "Merriweather",
            "RobotoMono"};

    public static String[] getPostFontsList() {
        return fontsForPost;
    }

    public static String[] getCommentFontList() {
        return fontsForComment;
    }

    public static String getPostFont(SharedPreferences sp) {
        return sp.getString(KEY_POST_FONT, "Roboto Slab");
    }

    public static void setPostFont(SharedPreferences sp, String fontName) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(SharedPrefsManager.KEY_POST_FONT, fontName);
        editor.apply();
    }

    public static float getPostFontSize(SharedPreferences sp) {
        return sp.getFloat(KEY_POST_FONT_SIZE, 16);
    }

    public static void setPostFontSize(SharedPreferences sp, float add) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat(SharedPrefsManager.KEY_POST_FONT_SIZE, getPostFontSize(sp) + add);
        editor.apply();
    }

    public static float getPostLineHeight(SharedPreferences sp) {
        return sp.getFloat(KEY_POST_LINE_HEIGHT, 1.2f);
    }

    public static void setPostLineHeight(SharedPreferences sp, float add) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat(SharedPrefsManager.KEY_POST_LINE_HEIGHT, getPostLineHeight(sp) + add);
        editor.apply();
    }

    public static String getCommentFont(SharedPreferences sp) {
        return sp.getString(KEY_COMMENT_FONT, "Roboto");
    }

    public static void setCommentFont(SharedPreferences sp, String fontName) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(SharedPrefsManager.KEY_COMMENT_FONT, fontName);
        editor.apply();
    }

    public static float getCommentFontSize(SharedPreferences sp) {
        return sp.getFloat(KEY_COMMENT_FONT_SIZE, 14);
    }

    public static void setCommentFontSize(SharedPreferences sp, float add) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat(SharedPrefsManager.KEY_COMMENT_FONT_SIZE, getCommentFontSize(sp) + add);
        editor.apply();
    }

    public static float getCommentLineHeight(SharedPreferences sp) {
        return sp.getFloat(KEY_COMMENT_LINE_HEIGHT, 1.2f);
    }

    public static void setCommentLineHeight(SharedPreferences sp, float add) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat(SharedPrefsManager.KEY_COMMENT_LINE_HEIGHT, getCommentLineHeight(sp) + add);
        editor.apply();
    }

    //Settings
    public static String getTheme(SharedPreferences sp) {
        return sp.getString(KEY_THEME, THEME_LIGHT);
    }

    public static String getFabMode(SharedPreferences sp) {
        return sp.getString(KEY_FAB_MODE, FAB_PRESS_SCROLL_DOWN);
    }

    public static Boolean getShowPostSummary(SharedPreferences sp, Context context) {
        return sp.getBoolean(context.getResources().getString(R.string.pref_key_show_post_summary),
                false);
    }

    public static Boolean getIsOpenLinkInBrowser(SharedPreferences sp, Context context) {
        return sp.getBoolean(context.getResources().getString(R.string.pref_key_open_link), false);
    }

    public static void setLoginCookie(SharedPreferences sp, String cookie) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(SharedPrefsManager.KEY_LOGIN_COOKIE, cookie);
        editor.apply();
    }

    public static String getLoginCookie(SharedPreferences sp) {
        return sp.getString(KEY_LOGIN_COOKIE, "");
    }

    public static void setUsername(SharedPreferences sp, String username) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(SharedPrefsManager.KEY_USERNAME, username);
        editor.apply();
    }

    public static String getUsername(SharedPreferences sp, Context context) {
        return sp.getString(KEY_USERNAME, context.getResources().getString(R.string.nav_logout));
    }

    public static void setReplyText(SharedPreferences sp, String replyText) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(SharedPrefsManager.KEY_REPLY_TEXT, replyText);
        editor.apply();
    }

    public static String getReplyText(SharedPreferences sp) {
        return sp.getString(KEY_REPLY_TEXT, "");
    }
}
