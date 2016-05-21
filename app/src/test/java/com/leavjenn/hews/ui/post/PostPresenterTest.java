package com.leavjenn.hews.ui.post;

import com.leavjenn.hews.Constants;
import com.leavjenn.hews.RxSchedulersOverrideRule;
import com.leavjenn.hews.misc.SharedPrefsContract;
import com.leavjenn.hews.misc.UtilsContract;
import com.leavjenn.hews.model.Post;
import com.leavjenn.hews.data.remote.DataManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import rx.Observable;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PostPresenterTest {

    private PostPresenter mPresenter;
    @Mock
    private PostView mPostView;
    @Mock
    private DataManager mDataManager;
    @Mock
    private SharedPrefsContract mPrefsManager;
    @Mock
    private UtilsContract mUtils;

    @Rule
    // Must be added to every test class targets app code that uses RxJava
    public final RxSchedulersOverrideRule mOverrideSchedulersRule = new RxSchedulersOverrideRule();

    @Before
    public void setUp() throws Exception {
        mPresenter = new PostPresenter(mPostView, mDataManager, mPrefsManager, mUtils);
    }

    @After
    public void tearDown() {
        mPresenter.unsubscribe();
        mPresenter.destroy();
    }

    @Test
    public void refreshWhenOffline() {
        when(mUtils.isOnline()).thenReturn(false);
        mPresenter.refresh();
        verify(mPostView).hideSwipeRefresh();
        verify(mPostView).showOfflineSnackBar();
    }

    @Test
    public void testLoadPostIdList() throws Exception {
        when(mDataManager.getPostList("test")).thenReturn(Observable.just(makePostIdList(5)));
        mPresenter.loadPostIdList("test");
        verify(mPostView).hideSwipeRefresh();
    }

    private List<Long> makePostIdList(int numIds) {
        List<Long> idList = new ArrayList<>();
        for (int i = 0; i < numIds; i++) {
            idList.add(new Random().nextLong());
        }
        return idList;
    }

    @Test
    public void loadPostsSuccess() {
        List<Post> postList = makePosts(25);
        when(mDataManager.getPosts(null)).thenReturn(Observable.from(postList));
        when(mPostView.getAllPostList()).thenReturn(postList);
        mPresenter.loadPosts(null, true);
        verify(mPostView, times(25)).hideSwipeRefresh();
        verify(mPostView).updateListFooter(Constants.LOADING_IN_PROGRESS);
        for (Post post : postList) {
            verify(mPostView).showPost(post);
        }
        verify(mPostView).updateListFooter(Constants.LOADING_IDLE);
        verify(mPostView).getAllPostList();
        assertEquals(25, mPresenter.getCachedPosts().size());
    }

    private List<Post> makePosts(int numPosts) {
        List<Post> posts = new ArrayList<>();
        for (int i = 0; i < numPosts; i++) {
            posts.add(makePost());
        }
        return posts;
    }

    private Post makePost() {
        Post post = new Post();
        post.setBy("sdf");
        post.setId(234234);
        post.setTitle("dfg");
        post.setDescendants(23);
        post.setUrl("346");
        return post;
    }
}