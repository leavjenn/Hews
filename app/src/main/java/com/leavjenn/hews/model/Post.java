package com.leavjenn.hews.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class Post extends HNItem implements Parcelable {
    private int index;
    private String by;
    private long descendants;
    private long id;
    private ArrayList<Long> kids;
    private long score;
    private String text;
    private long time;
    private String title;
    private String type;
    private String url;
    private String prettyUrl;
    private String summary;

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.by);
        dest.writeValue(this.descendants);
        dest.writeValue(this.id);
        dest.writeSerializable(this.kids);
        dest.writeValue(this.score);
        dest.writeString(this.text);
        dest.writeValue(this.time);
        dest.writeString(this.title);
        dest.writeString(this.type);
        dest.writeString(this.url);
    }

    public static final Parcelable.Creator<Post> CREATOR = new Parcelable.Creator<Post>() {

        @Override
        public Post createFromParcel(Parcel source) {
            return new Post(source);
        }

        @Override
        public Post[] newArray(int size) {
            return new Post[size];
        }
    };

    private Post(Parcel in) {
        this.by = in.readString();
        this.descendants = (Long) in.readValue(Long.class.getClassLoader());
        this.id = (Long) in.readValue(Long.class.getClassLoader());
        this.kids = (ArrayList<Long>) in.readSerializable();
        this.score = (Long) in.readValue(Long.class.getClassLoader());
        this.text = in.readString();
        this.time = (Long) in.readValue(Long.class.getClassLoader());
        this.title = in.readString();
        this.type = in.readString();
        this.url = in.readString();
    }
}
