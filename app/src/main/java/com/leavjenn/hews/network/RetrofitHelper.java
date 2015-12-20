package com.leavjenn.hews.network;

import com.google.gson.GsonBuilder;
import com.leavjenn.hews.Constants;

import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

public class RetrofitHelper {

    public HackerNewsService getHackerNewsService() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(Constants.KEY_API_URL)
                .setConverter(new GsonConverter(new GsonBuilder().create()))
                .setLogLevel(RestAdapter.LogLevel.NONE)
                .build();

        return restAdapter.create(HackerNewsService.class);
    }

    public HackerNewsService getSearchService() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(Constants.SEARCH_BASE_URL)
                .setConverter(new GsonConverter(new GsonBuilder().create()))
                .setLogLevel(RestAdapter.LogLevel.NONE)
                .build();

        return restAdapter.create(HackerNewsService.class);
    }
}
