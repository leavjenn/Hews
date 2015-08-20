package com.leavjenn.hews.network;

import android.text.Html;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.leavjenn.hews.Constants;
import com.leavjenn.hews.model.Comment;
import com.leavjenn.hews.model.HNItem;
import com.leavjenn.hews.model.Post;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Func1;

public class DataManager {
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

    public Scheduler getScheduler() {
        return mScheduler;
    }
}
