package com.leavjenn.hews.network;

import com.leavjenn.hews.model.Comment;
import com.leavjenn.hews.model.HNItem;
import com.leavjenn.hews.model.Post;

import java.util.List;

import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;
import rx.Observable;

public interface HackerNewsService {

    @GET("/topstories.json")
    Observable<List<Long>> getTopStories();

    @GET("/newstories.json")
    Observable<List<Long>> getNewStories();

    @GET("/askstories.json")
    Observable<List<Long>> getAskStories();

    @GET("/showstories.json")
    Observable<List<Long>> getShowStories();

    @GET("/item/{itemId}.json")
    Observable<Post> getStory(@Path("itemId") String itemId);

    @GET("/item/{itemId}.json")
    Observable<Comment> getComment(@Path("itemId") long itemId);

    // popularity by time range
    @GET("/search?tags=story")
    Observable<HNItem.SearchResult> searchPopularity(@Query("numericFilters") String timeRange,
                                                     @Query("page") int page,
                                                     @Query("hitsPerPage") int hitsPerPage);

    @GET("/search?tags=story&typoTolerance=false")
    Observable<HNItem.SearchResult> search(@Query("query") String keyword,
                                                 @Query("numericFilters") String timeRange,
                                                 @Query("page") int page,
                                                 @Query("hitsPerPage") int hitsPerPage);

    @GET("/search_by_date?tags=story&typoTolerance=false")
    Observable<HNItem.SearchResult> searchByDate(@Query("query") String keyword,
                                                 @Query("numericFilters") String timeRange,
                                                 @Query("page") int page,
                                                 @Query("hitsPerPage") int hitsPerPage);

}
