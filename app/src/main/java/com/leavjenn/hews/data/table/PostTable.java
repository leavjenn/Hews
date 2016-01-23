package com.leavjenn.hews.data.table;

public class PostTable {

    public static final String TABLE = "post";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_BY = "by";
    public static final String COLUMN_DESCENDANTS = "descendants";
    public static final String COLUMN_KIDS = "kids";
    public static final String COLUMN_SCORE = "score";
    public static final String COLUMN_TEXT = "text";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_URL = "url";
    public static final String COLUMN_PRETTY_URL = "prettyUrl";
    public static final String COLUMN_SUMMARY = "summary";
    public static final String COLUMN_ISBOOKMARKED = "isBookmarked";


    public static String getCreateTableQuery() {
        return "CREATE TABLE " + TABLE + "("
            + COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY, "
            + COLUMN_BY + " TEXT, "
            + COLUMN_DESCENDANTS + " TEXT, "
            + COLUMN_KIDS + " TEXT, "
            + COLUMN_SCORE + " TEXT, "
            + COLUMN_TEXT + " TEXT, "
            + COLUMN_TIME + " TEXT, "
            + COLUMN_TITLE + " TEXT, "
            + COLUMN_TYPE + " TEXT, "
            + COLUMN_URL + " TEXT, "
            + COLUMN_PRETTY_URL + " TEXT, "
            + COLUMN_SUMMARY + " TEXT, "
            + COLUMN_ISBOOKMARKED + " INTEGER"
            + ");";
    }
}
