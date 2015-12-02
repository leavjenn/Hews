package com.leavjenn.hews.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Html;
import android.util.Log;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.leavjenn.hews.Constants;
import com.leavjenn.hews.SharedPrefsManager;
import com.leavjenn.hews.data.StorIOHelper;
import com.leavjenn.hews.model.Comment;
import com.leavjenn.hews.model.HNItem;
import com.leavjenn.hews.model.Post;
import com.pushtorefresh.storio.sqlite.operations.put.PutResult;
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
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

public class DataManager {
    public static final String HACKER_NEWS_BASE_URL = "https://news.ycombinator.com/";
    public static final String HACKER_NEWS_ITEM_URL = "https://news.ycombinator.com/item?id=";
    private static final int MINIMUM_STRING = 20;
    HackerNewsService mHackerNewsService, mSearchService;
    private Scheduler mScheduler;

    public DataManager(Scheduler scheduler) {
        mHackerNewsService = new RetrofitHelper().getHackerNewsService();
        mSearchService = new RetrofitHelper().getSearchService();
        mScheduler = scheduler;
    }


    public Observable<Post> getAllPostFromFirebaseAndRetro(final String type) {
        return Observable.create(new Observable.OnSubscribe<DataSnapshot>() {
            @Override
            public void call(final Subscriber<? super DataSnapshot> subscriber) {

                Firebase storiesRef = new Firebase(type);
                storiesRef.addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        subscriber.onNext(dataSnapshot);
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        subscriber.onError(firebaseError.toException());
                    }
                });
            }
        }).map(new Func1<DataSnapshot, List<Long>>() {
            @Override
            public List<Long> call(DataSnapshot dataSnapshot) {
                return (List<Long>) dataSnapshot.getValue();
            }
        }).flatMap(new Func1<List<Long>, Observable<Post>>() {
            @Override
            public Observable<Post> call(List<Long> longs) {
                return getPosts(longs);
            }
        });
    }

    public Observable<List<Long>> getPostListFromFirebase(final String type) {
        return Observable.create(new Observable.OnSubscribe<DataSnapshot>() {
            @Override
            public void call(final Subscriber<? super DataSnapshot> subscriber) {

                Firebase storiesRef = new Firebase(type);
                storiesRef.addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        subscriber.onNext(dataSnapshot);
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        subscriber.onError(firebaseError.toException());
                    }
                });
            }
        }).map(new Func1<DataSnapshot, List<Long>>() {
            @Override
            public List<Long> call(DataSnapshot dataSnapshot) {
                return (List<Long>) dataSnapshot.getValue();
            }
        });
    }

    public Observable<Post> getPostFromList(final List<Long> list) {
        return Observable.from(list)
                .flatMap(new Func1<Long, Observable<Post>>() {
                    @Override
                    public Observable<Post> call(Long aLong) {
                        return getPostFromFirebase(aLong);
                    }
                });
    }

    public Observable<Post> getPostFromListByOrder(final List<Long> list) {
        return Observable.from(list)
                .concatMap(new Func1<Long, Observable<Post>>() {
                    @Override
                    public Observable<Post> call(Long aLong) {
                        return getPostFromFirebase(aLong);
                    }
                });
    }

    public Observable<Post> getPostFromFirebase(final Long postId) {
        return Observable.create(new Observable.OnSubscribe<Post>() {
            @Override
            public void call(final Subscriber<? super Post> subscriber) {

                Firebase itemRef = new Firebase(Constants.KEY_ITEM_URL + postId);
                itemRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Post post = mapToPost(dataSnapshot);
                        //if (post != null) {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(post);
                        }
                        subscriber.onCompleted();
                        //}
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        subscriber.onError(firebaseError.toException());
                    }
                });
            }
        });
    }

    Post mapToPost(DataSnapshot snapshot) {
        HashMap<String, Object> item = (HashMap<String,
                Object>) snapshot.getValue();
        Post post = null;
        if (item != null && item.get(Constants.KEY_TITLE) != null) {
            post = new Post((Long) item.get(Constants.KEY_ID));
            post.setTitle((String) item.get(Constants.KEY_TITLE));
            post.setKids((ArrayList<Long>) item.get(Constants.KEY_KIDS));
            post.setScore((Long) item.get(Constants.KEY_SCORE));
            post.setBy((String) item.get(Constants.KEY_BY));
            if (item.get(Constants.KEY_DESC) != null) {
                post.setDescendants((Long) item.get(Constants.KEY_DESC));
            } else {
                // Jobs or what
                post.setDescendants(0);
            }
            if (item.get(Constants.KEY_TEXT) != null) {
                post.setText((String) item.get(Constants.KEY_TEXT));
            }
            post.setTime((Long) item.get(Constants.KEY_TIME));
            post.setType((String) item.get(Constants.KEY_TYPE));

            String url = (String) item.get(Constants.KEY_URL);
            if (url == null || url.isEmpty()) {
                url = Constants.YCOMBINATOR_ITEM_URL + post.getId();
            }
            //TODO change self URL
            post.setUrl(url);

            String[] splitUrl = url.split("/");
            if (splitUrl.length > 2) {
                url = splitUrl[2];
                post.setPrettyUrl(url);
            }
        }
        return post;
    }

    public Observable<Post> getPosts(List<Long> postIds) {
        return Observable.from(postIds)
                .concatMap(new Func1<Long, Observable<Post>>() {
                    @Override
                    public Observable<Post> call(Long aLong) {
                        return mHackerNewsService.getStory(String.valueOf(aLong));
                    }
                }).filter(new Func1<Post, Boolean>() {
                    @Override
                    public Boolean call(Post post) {
                        return post != null && post.getTitle() != null;
                    }
                });
    }

    public Observable<Comment> getSummary(List<Long> commentIds) {
        Observable<Comment> c = Observable.from(commentIds)
                .concatMap(new Func1<Long, Observable<Comment>>() {
                    @Override
                    public Observable<Comment> call(Long aLong) {
                        return getComment(aLong);
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
        return c;
    }

    public Observable<Comment> getComments(final List<Long> commentIds, final int level) {
        return Observable.from(commentIds)
                .concatMap(new Func1<Long, Observable<Comment>>() {
                    @Override
                    public Observable<Comment> call(Long aLong) {
                        return mHackerNewsService.getComment(String.valueOf(aLong));
                    }
                }).concatMap(new Func1<Comment, Observable<Comment>>() {
                    @Override
                    public Observable<Comment> call(Comment comment) {
                        if (comment != null && comment.getText() != null) {
                            comment.setLevel(level);
                            if (comment.getKids() == null || comment.getKids().isEmpty()) {
                                return Observable.just(comment);
                            } else {
                                return Observable.just(comment)
                                        .mergeWith(getComments(comment.getKids(), level + 1));
                            }
                        }
                        return Observable.just(null);

                    }
                }).filter(new Func1<Comment, Boolean>() {
                    @Override
                    public Boolean call(Comment comment) {
                        return (comment != null
                                && comment.getBy() != null && !comment.getBy().trim().isEmpty()
                                && comment.getText() != null && !comment.getText().trim().isEmpty());
                    }
                });
    }

    public Observable<Comment> getCommentsFromFirebase(final List<Long> commentIds, final int level) {
        return Observable.from(commentIds)
                .concatMap(new Func1<Long, Observable<? extends Comment>>() {
                    @Override
                    public Observable<? extends Comment> call(Long aLong) {
                        return getComment(aLong);
                    }
                }).concatMap(new Func1<Comment, Observable<Comment>>() {
                    @Override
                    public Observable<Comment> call(Comment comment) {
                        if (comment != null && comment.getText() != null) {
                            comment.setLevel(level);
                            if (comment.getKids() == null || comment.getKids().isEmpty()) {
                                return Observable.just(comment);
                            } else {
                                return Observable.just(comment)
                                        .mergeWith(getCommentsFromFirebase(comment.getKids(), level + 1));
                            }
                        }
                        return Observable.just(null);

                    }
                }).filter(new Func1<Comment, Boolean>() {
                    @Override
                    public Boolean call(Comment comment) {
                        return (comment != null
                                && comment.getBy() != null && !comment.getBy().trim().isEmpty()
                                && comment.getText() != null && !comment.getText().trim().isEmpty());
                    }
                });
    }

    public Observable<Comment> getComment(final Long id) {
        return Observable.create(new Observable.OnSubscribe<Comment>() {
            @Override
            public void call(final Subscriber<? super Comment> subscriber) {

                Firebase itemRef = new Firebase(Constants.KEY_ITEM_URL + id);
                itemRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Comment comment = mapToComment(dataSnapshot);
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(comment);
                        }
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        subscriber.onError(firebaseError.toException());
                    }
                });
            }
        });
    }

    private Comment mapToComment(DataSnapshot snapshot) {
        HashMap<String, Object> item = (HashMap<String, Object>) snapshot.getValue();
        Comment comment = null;
        if (item != null && item.get(Constants.KEY_TEXT) != null) {
            comment = new Comment();
            comment.setId((Long) item.get(Constants.KEY_ID));
            comment.setKids((ArrayList<Long>) item.get(Constants.KEY_KIDS));
            comment.setBy((String) item.get(Constants.KEY_BY));
            comment.setText((String) item.get(Constants.KEY_TEXT));
            comment.setTime((Long) item.get(Constants.KEY_TIME));
        }
        return comment;

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

    public Observable<PutResult> putPostBookmark(Context context, Post post) {
        return StorIOHelper.getStorIOSQLite(context)
                .put()
                .object(post)
                .prepare()
                .createObservable();
    }

    public Observable<List<Post>> getAllPostBookmarks(Context context) {
        return StorIOHelper.getStorIOSQLite(context)
                .get()
                .listOfObjects(Post.class)
                .withQuery(Query.builder().table("post").build())
                .prepare()
                .createObservable();
    }

    public Scheduler getScheduler() {
        return mScheduler;
    }
}
