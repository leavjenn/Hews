package com.leavjenn.hews.ui.search;

import com.leavjenn.hews.R;
import com.leavjenn.hews.RxSchedulersOverrideRule;
import com.leavjenn.hews.misc.SharedPrefsContract;
import com.leavjenn.hews.misc.UtilsContract;
import com.leavjenn.hews.model.HNItem;
import com.leavjenn.hews.data.remote.DataManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rx.Observable;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SearchPresenterTest {
    private SearchPresenter mPresenter;
    @Mock
    private SearchView mSearchView;
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
        mPresenter = new SearchPresenter(mSearchView, mDataManager, mPrefsManager, mUtils);
    }

    @After
    public void tearDown() throws Exception {
        mPresenter.unsubscribe();
        mPresenter.destroy();
    }

    @Test
    public void refreshWhenNoParams() {
        mPresenter.refresh();
        verify(mSearchView).hideSwipeRefresh();
    }

    @Test
    public void refreshWhenOffline() {
        String mKeyword = "keyword";
        String mDateRange = "01234567890";
        boolean isSortByDate = true;
        when(mUtils.isOnline()).thenReturn(false);
        mPresenter.refresh(mKeyword, mDateRange, isSortByDate);
        verify(mSearchView).hideSwipeRefresh();
        verify(mSearchView).showOfflineSnackBar();
        verify(mSearchView,never()).resetAdapter();
    }

//    @Test
//    public void refreshNormal() {
//        String mKeyword = "keyword";
//        String mDateRange = "01234567890";
//        boolean isSortByDate = true;
//        when(mUtils.isOnline()).thenReturn(true);
//        mPresenter.refresh(mKeyword, mDateRange, isSortByDate);
//        verify(mSearchView).showSwipeRefresh();
//        verify(mSearchView).hideOfflineSnackBar();
//        verify(mSearchView).resetAdapter();
//        verify(mSearchView).updateListFooter(Constants.LOADING_IDLE);
//    }

    @Test
    public void getSearchResultsEmpty() {
        HNItem.SearchResult result = new HNItem.SearchResult();
        result.setHits(new HNItem.SearchHit[]{});
        when(mDataManager.getSearchResult("1", "created_at_i>" + "0123456789"
            + "," + "created_at_i<" + "0", 1, true))
            .thenReturn(Observable.just(result));
        mPresenter.loadPostIdListBySearch("1", "01234567890", 1, true);
        verify(mSearchView).hideSwipeRefresh();
        verify(mSearchView).updatePrompt(R.string.no_search_result_prompt);
    }
}