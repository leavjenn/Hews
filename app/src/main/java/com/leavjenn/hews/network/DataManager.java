package com.leavjenn.hews.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Html;
import android.util.Log;

import com.leavjenn.hews.Constants;
import com.leavjenn.hews.data.StorIOHelper;
import com.leavjenn.hews.data.table.CommentTable;
import com.leavjenn.hews.data.table.PostTable;
import com.leavjenn.hews.misc.SharedPrefsManager;
import com.leavjenn.hews.model.Comment;
import com.leavjenn.hews.model.HNItem;
import com.leavjenn.hews.model.Post;
import com.pushtorefresh.storio.sqlite.operations.delete.DeleteResult;
import com.pushtorefresh.storio.sqlite.operations.put.PutResult;
import com.pushtorefresh.storio.sqlite.operations.put.PutResults;
import com.pushtorefresh.storio.sqlite.queries.DeleteQuery;
import com.pushtorefresh.storio.sqlite.queries.Query;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

public class DataManager {
    public static final String HACKER_NEWS_BASE_URL = "https://news.ycombinator.com/";
    public static final String HACKER_NEWS_ITEM_URL = "https://news.ycombinator.com/item?id=";
    private static final int MINIMUM_STRING = 20;
    HackerNewsService mHackerNewsService, mSearchService;

    public DataManager() {
        mHackerNewsService = new RetrofitHelper().getHackerNewsService();
        mSearchService = new RetrofitHelper().getSearchService();
    }

    public Observable<List<Long>> getPostList(String type) {
        return mHackerNewsService.getStories(type);
    }

    public Observable<Post> getPost(Long postId) {
        return mHackerNewsService.getStory(String.valueOf(postId));
    }

    public Observable<Post> getPosts(List<Long> postIds) {
        return Observable.from(postIds)
                .flatMap(new Func1<Long, Observable<Post>>() {
                    @Override
                    public Observable<Post> call(Long aLong) {
                        return mHackerNewsService.getStory(String.valueOf(aLong));
                    }
                })
                .onErrorReturn(new Func1<Throwable, Post>() {
                    @Override
                    public Post call(Throwable throwable) {
                        Log.e("getPosts", throwable.toString());
                        return null;
                    }
                })
                .filter(new Func1<Post, Boolean>() {
                    @Override
                    public Boolean call(Post post) {
                        return post != null && post.getTitle() != null;
                    }
                });
    }

    public Observable<Comment> getSummary(List<Long> commentIds) {
        return Observable.from(commentIds)
                .flatMap(new Func1<Long, Observable<Comment>>() {
                    @Override
                    public Observable<Comment> call(Long aLong) {
                        return mHackerNewsService.getComment(aLong);
                    }
                }).filter(new Func1<Comment, Boolean>() {
                    @Override
                    public Boolean call(Comment comment) {
                        if (comment != null
                                && comment.getBy() != null && !comment.getBy().trim().isEmpty()
                                && comment.getText() != null && !comment.getText().trim().isEmpty()
                                && comment.getText().length() > MINIMUM_STRING * 4) {
                            //TODO length num improve needed.
                            String s = Html.fromHtml(comment.getText()).toString().substring(0,
                                    MINIMUM_STRING)
                                    .toLowerCase();
                            return s.contains("summary")
                                    || s.contains("tldr")
                                    || s.contains("tl;dr")
                                    || s.contains("tl; dr");
                        } else {
                            return false;
                        }
                    }
                }).firstOrDefault(null);
    }

    public Observable<List<Comment>> getComments(Post post, int level) {
        List<Long> commentIds = post.getKids();
        long descendants = post.getDescendants();
        if (commentIds.size() > 3 && descendants > 15) {
            Log.i("---getComments", "kids > 3");
            return Observable.concat(getCommentsByBranches(commentIds.subList(0, 3), level),
                    getCommentsAllAtOnce(commentIds.subList(3, commentIds.size()), level));
        } else if (descendants / commentIds.size() > 15) {
            Log.i("---getComments", "few kids, big branch");
            return getCommentsByBranches(commentIds, level);
        } else {
            Log.i("---getComments", "other");
            return getCommentsAllAtOnce(commentIds, level);
        }
    }

    public Observable<List<Comment>> getCommentsByBranches(List<Long> commentIds, final int level) {
        Log.i("---", "getCommentsByBranches");
        return Observable.from(commentIds)
                .flatMap(new Func1<Long, Observable<List<Comment>>>() {
                    @Override
                    public Observable<List<Comment>> call(Long commentId) {
                        return getOneBranchComments(commentId, level);
                    }
                });
    }

    public Observable<List<Comment>> getOneBranchComments(final long commentId, final int level) {
        return mHackerNewsService.getComment(commentId)
                .onErrorReturn(new Func1<Throwable, Comment>() {
                    @Override
                    public Comment call(Throwable throwable) {
                        Log.e("getOneBranchComments", throwable.toString());
                        return null;
                    }
                })
                .filter(new Func1<Comment, Boolean>() {
                    @Override
                    public Boolean call(Comment comment) {
                        return (comment != null) && !comment.getDeleted() && comment.getText() != null;
                    }
                })
                .flatMap(new Func1<Comment, Observable<Comment>>() {
                    @Override
                    public Observable<Comment> call(Comment comment) {
                        Log.i("---getOneBranchComments", String.valueOf(comment.getCommentId()));
                        return getInnerComments(comment, level);
                    }
                })
                .toList()
                .map(new Func1<List<Comment>, List<Comment>>() {
                    @Override
                    public List<Comment> call(List<Comment> allComments) {
                        List<Long> firstLevelCommentAsList = new ArrayList<>();
                        firstLevelCommentAsList.add(commentId);
                        return sortComments(firstLevelCommentAsList, allComments);
                    }
                });
    }

    public Observable<List<Comment>> getCommentsAllAtOnce(final List<Long> firstLevelCommentIds, final int level) {
        Log.i("---", "getCommentsAllAtOnce");
        return Observable.from(firstLevelCommentIds)
                .flatMap(new Func1<Long, Observable<Comment>>() {
                    @Override
                    public Observable<Comment> call(Long commentId) {
                        return mHackerNewsService.getComment(commentId)
                                .onErrorReturn(new Func1<Throwable, Comment>() {
                                    @Override
                                    public Comment call(Throwable throwable) {
                                        Log.e("getCommentsAllAtOnce", throwable.toString());
                                        return null;
                                    }
                                });
                    }
                })
                .filter(new Func1<Comment, Boolean>() {
                    @Override
                    public Boolean call(Comment comment) {
                        return (comment != null) && !comment.getDeleted() && comment.getText() != null;
                    }
                })
                .flatMap(new Func1<Comment, Observable<Comment>>() {
                    @Override
                    public Observable<Comment> call(Comment comment) {
                        return getInnerComments(comment, level);
                    }
                })
                .toList()
                .map(new Func1<List<Comment>, List<Comment>>() {
                    @Override
                    public List<Comment> call(List<Comment> allComments) {
                        return sortComments(firstLevelCommentIds, allComments);
                    }
                });
    }

    private Observable<Comment> getInnerComments(Comment comment, final int level) {
        if (comment == null || comment.getDeleted() || comment.getText() == null) {
            return null;
        }
        comment.setLevel(level);
        if (comment.getKids() != null && !comment.getKids().isEmpty()) {
            return Observable.just(comment)
                    .mergeWith(Observable.from(comment.getKids())
                            .flatMap(new Func1<Long, Observable<Comment>>() {
                                @Override
                                public Observable<Comment> call(Long commentId) {
                                    return mHackerNewsService.getComment(commentId)
                                            .onErrorReturn(new Func1<Throwable, Comment>() {
                                                @Override
                                                public Comment call(Throwable throwable) {
                                                    Log.e("getInnerComments", throwable.toString());
                                                    return null;
                                                }
                                            });
                                }
                            })
                            .filter(new Func1<Comment, Boolean>() {
                                @Override
                                public Boolean call(Comment comment) {
                                    return (comment != null) && !comment.getDeleted()
                                            && comment.getText() != null;
                                }
                            })
                            .flatMap(new Func1<Comment, Observable<Comment>>() {
                                @Override
                                public Observable<Comment> call(Comment comment) {
                                    return getInnerComments(comment, level + 1);
                                }
                            })
                    );
        }
        return Observable.just(comment);
    }

    private List<Comment> sortComments(List<Long> firstLevelCommentIds, List<Comment> allComments) {
        HashMap<Long, Comment> allCommentsMap = new HashMap<>();
        for (Comment childComment : allComments) {
            allCommentsMap.put(childComment.getCommentId(), childComment);
        }
        List<Comment> validFirstLevelCommentList = new ArrayList<>();
        for (Long id : firstLevelCommentIds) {
            Comment firstLevelComment = allCommentsMap.get(id);
            if (firstLevelComment != null && !firstLevelComment.getDeleted()
                    && firstLevelComment.getText() != null) {
                validFirstLevelCommentList.add(firstLevelComment);
            }
        }
        return sortAllComments(validFirstLevelCommentList, allCommentsMap);
    }

    private List<Comment> sortAllComments(List<Comment> commentList, HashMap<Long, Comment> allCommentsMap) {
        List<Comment> sortedList = new ArrayList<>();
        for (Comment comment : commentList) {
            sortedList.add(comment);
            if (comment.getKids() != null && comment.getKids().size() > 0) {
                List<Comment> validChildCommentList = new ArrayList<>();
                for (long id : comment.getKids()) {
                    Comment childComment = allCommentsMap.get(id);
                    if (childComment != null && !childComment.getDeleted() && childComment.getText() != null) {
                        validChildCommentList.add(childComment);
                    }
                }
                sortedList.addAll(sortAllComments(validChildCommentList, allCommentsMap));
            }
        }
        return sortedList;
    }

    public Observable<HNItem.SearchResult> getPopularPosts(String startTime, int page) {
        return mSearchService.searchPopularity(startTime, page, Constants.NUM_LOADING_ITEM);
    }

    public Observable<HNItem.SearchResult> getSearchResult(String keyword, String timeRange,
                                                           int page, boolean isSortByDate) {
        if (isSortByDate) {
            return mSearchService.searchByDate(keyword, timeRange, page, Constants.NUM_LOADING_ITEM);
        } else {
            return mSearchService.search(keyword, timeRange, page, Constants.NUM_LOADING_ITEM);
        }
    }

    public Observable<String> login(final String username, final String password) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                try {
                    Connection login = Jsoup.connect(HACKER_NEWS_BASE_URL + "login");
                    login.header("Accept-Encoding", "gzip")
                            .data("go_to", "news")
                            .data("acct", username)
                            .data("pw", password)
                            .header("Origin", "https://news.ycombinator.com")
                            .followRedirects(true)
                            .referrer(HACKER_NEWS_BASE_URL + "login?go_to=news")
                            .method(Connection.Method.POST);
                    Connection.Response response = login.execute();
                    String cookie = response.cookie("user");
                    if (cookie == null) {
                        subscriber.onNext("");
                    } else {
                        subscriber.onNext(cookie);
                    }
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public Observable<Integer> vote(final long itemId, final SharedPreferences sp) {
        return Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                String cookieLogin = SharedPrefsManager.getLoginCookie(sp);
                if (cookieLogin.isEmpty()) {
                    subscriber.onNext(Constants.OPERATE_ERROR_NO_COOKIE);
                    subscriber.onCompleted();
                }
                try {
                    Connection vote = Jsoup.connect(HACKER_NEWS_ITEM_URL + itemId)
                            .header("Accept-Encoding", "gzip")
                            .cookie("user", cookieLogin);
                    Document commentsDocument = vote.get();
                    /*
                    votable element:
                    <a id="up_10276091"
                     href="vote?for=10276091&dir=up&auth=3ecc4be748d7cc412223ea906b559ce72ccdc262&goto=news"
                     onclick="return vote(this)">

                     url:
                    https://news.ycombinator.com/vote?for=10276091&dir=up
                     &auth=3ecc4be748d7cc412223ea906b559ce72ccdc262&goto=news

                    logout element:
                    <a id="up_10276091" href="vote?for=10276091&dir=up&goto=item%3Fid%3D10276091">

                    url:
                    https://news.ycombinator.com/vote?for=10276091&dir=up&goto=item%3Fid%3D10276091
                    */
                    Elements links = commentsDocument.select("a[id=up_" + itemId + "]");
                    if (links.size() == 0) {
                        subscriber.onNext(Constants.OPERATE_ERROR_HAVE_VOTED);
                        subscriber.onCompleted();
                    } else {
                        Element voteElement = links.get(0).select("a[href^=vote]").first();
                        if (!voteElement.attr("href").contains("auth=")) {
                            subscriber.onNext(Constants.OPERATE_ERROR_COOKIE_EXPIRED);
                            subscriber.onCompleted();
                        }
                        if (voteElement.attr("href").contains("auth=")) {
                            String url = (voteElement.attr("href"));
                            Request voteRequest = new Request.Builder()
                                    .addHeader("cookie", "user=" + cookieLogin)
                                    .url(HACKER_NEWS_BASE_URL + url)
                                    .build();
                            Response response = new OkHttpClient().newCall(voteRequest).execute();
                            if (response.code() == 200) {
                                if (response.body() == null) {
                                    subscriber.onNext(Constants.OPERATE_ERROR_UNKNOWN);
                                } else {
                                    subscriber.onNext(Constants.OPERATE_SUCCESS);
                                }
                            } else {
                                subscriber.onNext(Constants.OPERATE_ERROR_UNKNOWN);
                            }
                        }
                    }
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public Observable<Integer> reply(final long itemId, final String replyText, final String cookieLogin) {
        return Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                if (cookieLogin.isEmpty()) {
                    subscriber.onNext(Constants.OPERATE_ERROR_NO_COOKIE);
                    subscriber.onCompleted();
                }
                try {
                    Connection reply = Jsoup.connect(HACKER_NEWS_ITEM_URL + itemId)
                            .header("Accept-Encoding", "gzip")
                            .cookie("user", cookieLogin);
                    Document replyDocument = reply.get();
                    Element element = replyDocument.select("input[name=hmac]").first();
                    if (element != null) {
                        String replyHmac = element.attr("value");
                        RequestBody requestBody = (new FormEncodingBuilder())
                                .add("parent", String.valueOf(itemId))
                                .add("goto", (new StringBuilder()).append("item?id=").append(itemId).toString())
                                .add("hmac", replyHmac)
                                .add("text", replyText)
                                .build();
                        Request request = new Request.Builder()
                                .addHeader("cookie", "user=" + cookieLogin)
                                .url(HACKER_NEWS_BASE_URL + "comment")
                                .post(requestBody)
                                .build();

                        Response response = new OkHttpClient().newCall(request).execute();
                        if (response.code() == 200) {
                            subscriber.onNext(Constants.OPERATE_SUCCESS);
                        } else {
                            subscriber.onNext(Constants.OPERATE_ERROR_UNKNOWN);
                        }
                    } else {
                        subscriber.onNext(Constants.OPERATE_ERROR_COOKIE_EXPIRED);
                    }
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public Observable<PutResult> putPostToDb(Context context, Post post) {
        return StorIOHelper.getStorIOSQLite(context)
                .put()
                .object(post)
                .prepare()
                .createObservable();
    }

    public Observable<List<Post>> getPostFromDb(Context context, long postId) {
        return StorIOHelper.getStorIOSQLite(context)
                .get()
                .listOfObjects(Post.class)
                .withQuery(Query.builder()
                        .table(PostTable.TABLE)
                        .where(PostTable.COLUMN_ID + " = " + postId)
                        .build())
                .prepare()
                .createObservable();
    }

    public Observable<List<Post>> getAllPostsFromDb(Context context) {
        return StorIOHelper.getStorIOSQLite(context)
                .get()
                .listOfObjects(Post.class)
                .withQuery(Query.builder().table(PostTable.TABLE).build())
                .prepare()
                .createObservable();
    }

    public Observable<DeleteResult> deletePostFromDb(Context context, Post post) {
        return StorIOHelper.getStorIOSQLite(context)
                .delete()
                .object(post)
                .prepare()
                .createObservable();
    }

    public Observable<PutResults<Comment>> putCommentsToDb(Context context, List<Comment> commentList) {
        return StorIOHelper.getStorIOSQLite(context)
                .put()
                .objects(commentList)
                .prepare()
                .createObservable();
    }

    public Observable<List<Comment>> getStoryCommentsFromDb(Context context, long postId) {
        return StorIOHelper.getStorIOSQLite(context)
                .get()
                .listOfObjects(Comment.class)
                .withQuery(Query.builder()
                        .table(CommentTable.TABLE)
                        .where(CommentTable.COLUMN_PARENT + " = " + postId)
                        .orderBy(CommentTable.COLUMN_INDEX + " ASC")
                        .build())
                .prepare()
                .createObservable();
    }

    public Observable<DeleteResult> deleteStoryCommentsFromDb(Context context, long postId) {
        return StorIOHelper.getStorIOSQLite(context)
                .delete()
                .byQuery(DeleteQuery.builder()
                        .table(CommentTable.TABLE)
                        .where(CommentTable.COLUMN_PARENT + "=" + postId)
                        .build())
                .prepare()
                .createObservable();
    }
}
