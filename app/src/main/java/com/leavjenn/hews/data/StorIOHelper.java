package com.leavjenn.hews.data;

import android.content.Context;

import com.leavjenn.hews.model.Post;
import com.leavjenn.hews.model.PostStorIOSQLiteDeleteResolver;
import com.leavjenn.hews.model.PostStorIOSQLiteGetResolver;
import com.leavjenn.hews.model.PostStorIOSQLitePutResolver;
import com.pushtorefresh.storio.sqlite.SQLiteTypeMapping;
import com.pushtorefresh.storio.sqlite.StorIOSQLite;
import com.pushtorefresh.storio.sqlite.impl.DefaultStorIOSQLite;

public class StorIOHelper {
    private static StorIOSQLite mStorIOSQLite;

    public static StorIOSQLite getStorIOSQLite(Context context) {
        if (mStorIOSQLite != null) {
            return mStorIOSQLite;
        }
        mStorIOSQLite = DefaultStorIOSQLite.builder()
                .sqliteOpenHelper(new DbOpenHelper(context))
                .addTypeMapping(Post.class, SQLiteTypeMapping.<Post>builder()
                        // object that knows how to perform Put Operation (insert or update)
                        .putResolver(new PostStorIOSQLitePutResolver())
                                // object that knows how to perform Get Operation
                        .getResolver(new PostStorIOSQLiteGetResolver())
                                // object that knows how to perform Delete Operation
                        .deleteResolver(new PostStorIOSQLiteDeleteResolver())
                        .build())
                .build();

        return mStorIOSQLite;
    }
}
