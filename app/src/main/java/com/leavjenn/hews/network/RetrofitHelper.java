package com.leavjenn.hews.network;

import com.google.gson.GsonBuilder;
import com.leavjenn.hews.Constants;
import com.squareup.okhttp.OkHttpClient;

import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;

/**
 * Created by Jenn on 2015/4/15.
 */
public class RetrofitHelper {

    public HackerNewsService getHackerNewsService() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(Constants.KEY_API_URL)
                .setClient(new OkClient(new OkHttpClient()))
                .setConverter(new GsonConverter(new GsonBuilder().create()))
                .setLogLevel(RestAdapter.LogLevel.NONE)
                .build();

        return restAdapter.create(HackerNewsService.class);
    }

    public HackerNewsService getSearchService() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(Constants.SEARCH_BASE_URL)
                .setClient(new OkClient(new OkHttpClient()))
                .setConverter(new GsonConverter(new GsonBuilder().create()))
                .setLogLevel(RestAdapter.LogLevel.NONE)
                .build();

        return restAdapter.create(HackerNewsService.class);
    }
}
