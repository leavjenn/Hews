package com.leavjenn.hews.data.local.table;

public class CommentTable {
    public static final String TABLE = "comment";
    public static final String COLUMN_COMMENT_ID = "comment_id";
    public static final String COLUMN_INDEX = "_index";
    public static final String COLUMN_PARENT = "parent";
    public static final String COLUMN_LEVEL = "level";
    public static final String COLUMN_BY = "by";
    public static final String COLUMN_DELETED = "deleted";
    public static final String COLUMN_KIDS = "kids";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_TEXT = "text";


    public static String getCreateTableQuery() {
        return "CREATE TABLE " + TABLE + "("
            + COLUMN_COMMENT_ID + " INTEGER NOT NULL PRIMARY KEY, "
            + COLUMN_INDEX + " INTEGER NOT NULL, "
            + COLUMN_PARENT + " INTEGER NOT NULL, "
            + COLUMN_LEVEL + " INTEGER, "
            + COLUMN_BY + " TEXT, "
            + COLUMN_DELETED + " TEXT, "
            + COLUMN_KIDS + " TEXT, "
            + COLUMN_TIME + " TEXT, "
            + COLUMN_TEXT + " TEXT"
            + ");";
    }
}
