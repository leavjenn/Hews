package com.leavjenn.hews.model;

import org.parceler.Parcel;

import java.util.ArrayList;

@Parcel
public class Post extends HNItem {
    int index;
    String by;
    long descendants;
    long id;
    ArrayList<Long> kids;
    long score;
    String text;
    long time;
    String title;
    String type;
    String url;
    String prettyUrl;
    String summary;

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
}
