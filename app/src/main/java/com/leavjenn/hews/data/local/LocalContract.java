package com.leavjenn.hews.data.local;

import com.leavjenn.hews.model.Comment;
import com.leavjenn.hews.model.Post;
import com.pushtorefresh.storio.sqlite.operations.delete.DeleteResult;
import com.pushtorefresh.storio.sqlite.operations.put.PutResult;
import com.pushtorefresh.storio.sqlite.operations.put.PutResults;

import java.util.List;

import rx.Observable;

public interface LocalContract {
    Observable<PutResult> putPostToDb(Post post);

    Observable<List<Post>> getPostFromDb(long postId);

    Observable<List<Post>> getAllPostsFromDb();

    Observable<DeleteResult> deletePostFromDb(Post post);

    Observable<PutResults<Comment>> putCommentsToDb(List<Comment> commentList);

    Observable<List<Comment>> getStoryCommentsFromDb(long postId);

    Observable<DeleteResult> deleteStoryCommentsFromDb(long postId);

}
