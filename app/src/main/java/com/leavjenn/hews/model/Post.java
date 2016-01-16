package com.leavjenn.hews.model;

import org.parceler.Parcel;

import com.pushtorefresh.storio.sqlite.annotations.StorIOSQLiteColumn;
import com.pushtorefresh.storio.sqlite.annotations.StorIOSQLiteType;

import java.util.ArrayList;

@Parcel
@StorIOSQLiteType(table = "post")
public class Post extends HNItem {

    @StorIOSQLiteColumn(name = "id", key = true)
    long id;

    int index;

    @StorIOSQLiteColumn(name = "by")
    String by;

    @StorIOSQLiteColumn(name = "descendants")
    long descendants;

    ArrayList<Long> kids;

    @StorIOSQLiteColumn(name = "score")
    long score;

    @StorIOSQLiteColumn(name = "text")
    String text;

    @StorIOSQLiteColumn(name = "time")
    long time;

    @StorIOSQLiteColumn(name = "title")
    String title;

    @StorIOSQLiteColumn(name = "type")
    String type;

    @StorIOSQLiteColumn(name = "url")
    String url;

    @StorIOSQLiteColumn(name = "prettyUrl")
    String prettyUrl;

    @StorIOSQLiteColumn(name = "summary")
    String summary;

    @StorIOSQLiteColumn(name = "isBookmarked")
    boolean isBookmarked;

    public Post() {
    }

    public Post(Long id) {
        this.id = id;
    }

    public Post(Long id, int index) {
        this.id = id;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getBy() {
        return by;
    }

    public void setBy(String by) {
        this.by = by;
    }

    public long getDescendants() {
        return descendants;
    }

    public void setDescendants(long descendants) {
        this.descendants = descendants;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ArrayList<Long> getKids() {
        return kids;
    }

    public void setKids(ArrayList<Long> kids) {
        this.kids = kids;
    }

    public long getScore() {
        return score;
    }

    public void setScore(long score) {
        this.score = score;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPrettyUrl() {
        return prettyUrl;
    }

    public void setPrettyUrl(String prettyUrl) {
        this.prettyUrl = prettyUrl;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public boolean isBookmarked() {
        return isBookmarked;
    }

    public void setIsBookmarked(boolean isBookmarked) {
        this.isBookmarked = isBookmarked;
    }
}
