package com.leavjenn.hews;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

public class AuthTask extends AsyncTask<Void, Void, Void> {
    Context context;
    SharedPreferences sp;
    long postId;


    public AuthTask(Context context, SharedPreferences prefs, long id) {
        this.context = context;
        sp = prefs;
        postId = id;

    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            Connection connection = Jsoup.connect("https://news.ycombinator.com/item?id=" + postId);
            connection.cookie("user", SharedPrefsManager.getLoginCookie(sp));
            Document document = connection.get();
            Elements element = document.select("a[id^=\"up_" + postId + "\"]");
            Log.i("get element", element.attr("abs:href"));

            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(element.attr("abs:href"))
                    .addHeader("cookie", "user=" + SharedPrefsManager.getLoginCookie(sp)).build();
            Response response = client.newCall(request).execute();
            Log.i("code", String.valueOf(response.code()));
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("IOException", e.toString());
        }
        //return element != null ? element.attr("abs:href") : null;
        return null;
    }


//    @Override
//    protected void onPostExecute(String href) {
//        super.onPostExecute(href);
//    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        Toast.makeText(context, "upvote success", Toast.LENGTH_LONG).show();
    }
}
