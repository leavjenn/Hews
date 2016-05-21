package com.leavjenn.hews.data.local;

import android.content.Context;

import com.leavjenn.hews.data.local.table.CommentTable;
import com.leavjenn.hews.data.local.table.PostTable;
import com.leavjenn.hews.model.Comment;
import com.leavjenn.hews.model.Post;
import com.pushtorefresh.storio.sqlite.operations.delete.DeleteResult;
import com.pushtorefresh.storio.sqlite.operations.put.PutResult;
import com.pushtorefresh.storio.sqlite.operations.put.PutResults;
import com.pushtorefresh.storio.sqlite.queries.DeleteQuery;
import com.pushtorefresh.storio.sqlite.queries.Query;

import java.util.List;

import rx.Observable;

public class LocalDataManager implements LocalContract {
    private Context mContext;

    public LocalDataManager(Context context) {
        mContext = context;
    }

    @Override
    public Observable<PutResult> putPostToDb(Post post) {
        return StorIOHelper.getStorIOSQLite(mContext)
            .put()
            .object(post)
            .prepare()
            .asRxObservable();
    }

    @Override
    public Observable<List<Post>> getPostFromDb(long postId) {
        return StorIOHelper.getStorIOSQLite(mContext)
            .get()
            .listOfObjects(Post.class)
            .withQuery(Query.builder()
                .table(PostTable.TABLE)
                .where(PostTable.COLUMN_ID + " = " + postId)
                .build())
            .prepare()
            .asRxObservable();
    }

    @Override
    public Observable<List<Post>> getAllPostsFromDb() {
        return StorIOHelper.getStorIOSQLite(mContext)
            .get()
            .listOfObjects(Post.class)
            .withQuery(Query.builder().table(PostTable.TABLE).build())
            .prepare()
            .asRxObservable().onBackpressureBuffer();
    }

    @Override
    public Observable<DeleteResult> deletePostFromDb(Post post) {
        return StorIOHelper.getStorIOSQLite(mContext)
            .delete()
            .object(post)
            .prepare()
            .asRxObservable();
    }

    @Override
    public Observable<PutResults<Comment>> putCommentsToDb(List<Comment> commentList) {
        return StorIOHelper.getStorIOSQLite(mContext)
            .put()
            .objects(commentList)
            .prepare()
            .asRxObservable().onBackpressureBuffer();
    }

    @Override
    public Observable<List<Comment>> getStoryCommentsFromDb(long postId) {
        return StorIOHelper.getStorIOSQLite(mContext)
            .get()
            .listOfObjects(Comment.class)
            .withQuery(Query.builder()
                .table(CommentTable.TABLE)
                .where(CommentTable.COLUMN_PARENT + " = " + postId)
                .orderBy(CommentTable.COLUMN_INDEX + " ASC")
                .build())
            .prepare()
            .asRxObservable().onBackpressureBuffer();
    }

    @Override
    public Observable<DeleteResult> deleteStoryCommentsFromDb(long postId) {
        return StorIOHelper.getStorIOSQLite(mContext)
            .delete()
            .byQuery(DeleteQuery.builder()
                .table(CommentTable.TABLE)
                .where(CommentTable.COLUMN_PARENT + "=" + postId)
                .build())
            .prepare()
            .asRxObservable().onBackpressureBuffer();
    }
}
