package com.leavjenn.hews.model;

import com.pushtorefresh.storio.sqlite.annotations.StorIOSQLiteColumn;
import com.pushtorefresh.storio.sqlite.annotations.StorIOSQLiteType;

import java.util.ArrayList;

@StorIOSQLiteType(table = "comment")
public class Comment extends HNItem {

    @StorIOSQLiteColumn(name = "id", key = true)
    long id;

    @StorIOSQLiteColumn(name = "index")
    int index;

    @StorIOSQLiteColumn(name = "by")
    String by;

    @StorIOSQLiteColumn(name = "deleted")
    boolean deleted;

    @StorIOSQLiteColumn(name = "parent")
    long parent;

    ArrayList<Long> kids;

    @StorIOSQLiteColumn(name = "time")
    long time;

    @StorIOSQLiteColumn(name = "text")
    String text;

    @StorIOSQLiteColumn(name = "level")
    int level;

    public ArrayList<Comment> comments;

    //FIXME necessary?
    public String error;

    public Comment() {
        comments = new ArrayList<>();
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