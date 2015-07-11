package com.leavjenn.hews.model;

import java.util.ArrayList;

public class Comment extends HNItem {
    private int index;
    private String by;
    private long id;
    private boolean deleted;
    private long parent;
    private ArrayList<Long> kids;
    private long time;
    private String text;

    public ArrayList<Comment> comments;
    private int level = 0;
    public boolean isTopLevelComment;
    public String error;


    public Comment() {
        comments = new ArrayList<>();
        isTopLevelComment = false;

    }

    public Comment(int index, String by, long id, long parent, ArrayList<Long> kids, long time, String text) {
        this.index = index;
        this.by = by;
        this.id = id;
        this.parent = parent;
        this.kids = kids;
        this.time = time;
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean getDeleted() {
        return deleted;
    }

    public long getParent() {
        return parent;
    }

    public void setParent(long parent) {
        this.parent = parent;
    }

    public ArrayList<Long> getKids() {
        return kids;
    }

    public void setKids(ArrayList<Long> kids) {
        this.kids = kids;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}