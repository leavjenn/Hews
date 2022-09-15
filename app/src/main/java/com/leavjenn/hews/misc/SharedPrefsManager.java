package com.leavjenn.hews.misc;

import android.content.Context;
import android.content.SharedPreferences;

import com.leavjenn.hews.R;

public class SharedPrefsManager implements SharedPrefsContract {
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
    public static final String KEY_POST_IS_READ = "key_post_is_read";
    public static final String KEY_POST_IS_BOOKMARKED = "key_post_is_bookmarked";
    public static final String KEY_COMMENTS_ARE_BOOKMARKED = "key_comments_are_bookmarked";
    public static final String THEME_LIGHT = "0";
    public static final String THEME_SEPIA = "1";
    public static final String THEME_DARK = "2";
    public static final String THEME_AMOLED_BLACK = "3";
    public static final String SCROLL_MODE = "fabkey";
    public static final String SCROLL_MODE_DISABLE = "0";
    public static final String SCROLL_MODE_BUTTON = "1";
    public static final String SCROLL_MODE_FAB_DRAG = "2";
    public static final String SCROLL_MODE_FAB_HOLD = "3";
    public static final String KEY_SHOW_POST_SUMMARY = "key_show_post_summary";
    public static final String KEY_IS_NEW_VERSION_PROMPT_SHOWED = "key_is_new_version_prompt_showed";

    static String[] fontsForComment = {"PT Sans", "Roboto", "Lato",
        "Open Sans", "Muli", "Slabo 27px", "Crimson Text", "Roboto Slab",
        "Vollkorn", "Merriweather"};
    static String[] fontsForPost = {"Open Sans", "Dosis SemiBold", "Roboto Slab", "Merriweather",
        "RobotoMono"};

    private Context mContext;
    private SharedPreferences mPreferences;

    public SharedPrefsManager(Context context, SharedPreferences preferences) {
        mContext = context;
        mPreferences = preferences;
    }

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
        return sp.getString(KEY_COMMENT_FONT, "Open Sans");
    }

    public static void setCommentFont(SharedPreferences sp, String fontName) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(SharedPrefsManager.KEY_COMMENT_FONT, fontName);
        editor.apply();
    }

    public static float getCommentFontSize(SharedPreferences sp) {
        return sp.getFloat(KEY_COMMENT_FONT_SIZE, 15);
    }

    public static void setCommentFontSize(SharedPreferences sp, float add) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat(SharedPrefsManager.KEY_COMMENT_FONT_SIZE, getCommentFontSize(sp) + add);
        editor.apply();
    }

    public static float getCommentLineHeight(SharedPreferences sp) {
        return sp.getFloat(KEY_COMMENT_LINE_HEIGHT, 1.0f);
    }

    public static void setCommentLineHeight(SharedPreferences sp, float add) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat(SharedPrefsManager.KEY_COMMENT_LINE_HEIGHT, getCommentLineHeight(sp) + add);
        editor.apply();
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

    public static void setPostRead(SharedPreferences sp, long postId) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(KEY_POST_IS_READ + postId, true);
        editor.apply();
    }

    public static boolean isPostRead(SharedPreferences sp, long postId) {
        return sp.getBoolean(KEY_POST_IS_READ + postId, false);
    }

    public static void setPostBookmarked(SharedPreferences sp, long postId) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(KEY_POST_IS_BOOKMARKED + postId, true);
        editor.apply();
    }

    public static void setPostUnbookmarked(SharedPreferences sp, long postId) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(KEY_POST_IS_BOOKMARKED + postId, false);
        editor.apply();
    }

    public static boolean isPostBookmarked(SharedPreferences sp, long postId) {
        return sp.getBoolean(KEY_POST_IS_BOOKMARKED + postId, false);
    }

    public static void setCommentsBookmarked(SharedPreferences sp, long postId) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(KEY_COMMENTS_ARE_BOOKMARKED + postId, true);
        editor.apply();
    }

    public static void setCommentsUnbookmarked(SharedPreferences sp, long postId) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(KEY_COMMENTS_ARE_BOOKMARKED + postId, false);
        editor.apply();
    }

    public static boolean areCommentsBookmarked(SharedPreferences sp, long postId) {
        return sp.getBoolean(KEY_COMMENTS_ARE_BOOKMARKED + postId, false);
    }

    //Settings
    public static String getTheme(SharedPreferences sp) {
        return sp.getString(KEY_THEME, THEME_LIGHT);
    }

    public static String getScrollMode(SharedPreferences sp) {
        return sp.getString(SCROLL_MODE, SCROLL_MODE_FAB_HOLD);
    }

    public static Boolean getShowPostSummary(SharedPreferences sp, Context context) {
        return sp.getBoolean(context.getResources().getString(R.string.pref_key_show_post_summary),
            false);
    }

    public static Boolean getIsOpenLinkInApp(SharedPreferences sp, Context context) {
        return sp.getBoolean(context.getResources().getString(R.string.pref_key_open_link_option), true);
    }

    @Override
    public boolean isPostRead(long postId) {
        return mPreferences.getBoolean(KEY_POST_IS_READ + postId, false);
    }

    @Override
    public boolean isShowPostSummary() {
        return mPreferences.getBoolean(mContext.getResources()
            .getString(R.string.pref_key_show_post_summary), false);
    }

    @Override
    public boolean isPostBookmarked(long postId) {
        return mPreferences.getBoolean(KEY_POST_IS_BOOKMARKED + postId, false);
    }

    @Override
    public boolean areCommentsBookmarked(long postId) {
        return mPreferences.getBoolean(KEY_COMMENTS_ARE_BOOKMARKED + postId, false);
    }

    @Override
    public void setPostBookmarked(long postId) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(KEY_POST_IS_BOOKMARKED + postId, true);
        editor.apply();
    }

    @Override
    public void setCommentsBookmarked(long postId) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(KEY_COMMENTS_ARE_BOOKMARKED + postId, true);
        editor.apply();
    }

    @Override
    public void setPostUnbookmarked(long postId) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(KEY_POST_IS_BOOKMARKED + postId, false);
        editor.apply();
    }

    @Override
    public void setCommentsUnbookmarked(long postId) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(KEY_COMMENTS_ARE_BOOKMARKED + postId, false);
        editor.apply();
    }

    public static boolean isNewVersionPromptShowed(SharedPreferences sp, Context context) {
        return sp.getBoolean(context.getResources()
            .getString(R.string.pref_key_is_new_version_prompt_showed), false);
    }

    public static void setNewVersionPromptShowed(SharedPreferences sp) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(KEY_IS_NEW_VERSION_PROMPT_SHOWED, true);
        editor.apply();
    }
}
