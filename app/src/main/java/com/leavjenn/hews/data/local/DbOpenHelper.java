package com.leavjenn.hews.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.leavjenn.hews.data.local.table.CommentTable;
import com.leavjenn.hews.data.local.table.PostTable;

public class DbOpenHelper extends SQLiteOpenHelper {

    public DbOpenHelper(Context context) {
        super(context, "hews_db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(PostTable.getCreateTableQuery());
        db.execSQL(CommentTable.getCreateTableQuery());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
