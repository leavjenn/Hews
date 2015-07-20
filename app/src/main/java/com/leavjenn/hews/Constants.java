package com.leavjenn.hews;

public final class Constants {
    public static final String KEY_ID = "id";
    public static final String KEY_DELETED = "deleted";
    public static final String KEY_BY = "by";
    public static final String KEY_PARENT = "parent";
    public static final String KEY_KIDS = "kids";
    public static final String KEY_DESC = "descendants";
    public static final String KEY_SCORE = "score";
    public static final String KEY_TIME = "time";
    public static final String KEY_TITLE = "title";
    public static final String KEY_TYPE = "type";
    public static final String KEY_URL = "url";
    public static final String KEY_TEXT = "text";
    public static final String KEY_ERROR = "error";
    public static final String KEY_POST = "post";
    public static final String YCOMBINATOR_ITEM_URL = "https://news.ycombinator.com/item?id=";
    public static final String KEY_TOP_STORIES_URL
            = "https://hacker-news.firebaseio.com/v0/topstories";
    public static final String KEY_NEW_STORIES_URL
            = "https://hacker-news.firebaseio.com/v0/newstories";
    public static final String KEY_ASK_HN_URL
            = "https://hacker-news.firebaseio.com/v0/askstories";
    public static final String KEY_SHOW_HN_URL
            = "https://hacker-news.firebaseio.com/v0/showstories";
    public static final String KEY_API_URL = "https://hacker-news.firebaseio.com/v0";
    public static final String KEY_ITEM_URL = "https://hacker-news.firebaseio.com/v0/item/";
	
	public static final String SEARCH_BASE_URL = "https://hn.algolia.com/api/v1/";
	
    public static final String TYPE_STORY = "story";
    public static final String TYPE_COMMENT = "comment";
    public static final String TYPE_POLL = "poll";
    public static final String TYPE_POLLOUT = "pollout";

    public final static int COMMENTS_LOADING_IN_PROGRESS = 0;
    public final static int COMMENTS_LOADING_FINISH = 1;
    public final static int COMMENTS_LOADING_ERROR = 2;
    public final static int COMMENTS_LOADING_NO_COMMENT = 3;

}
