package com.leavjenn.hews.model;

import com.google.gson.annotations.SerializedName;
import com.leavjenn.hews.data.table.CommentTable;
import com.pushtorefresh.storio.sqlite.annotations.StorIOSQLiteColumn;
import com.pushtorefresh.storio.sqlite.annotations.StorIOSQLiteType;

import java.util.ArrayList;

@StorIOSQLiteType(table = "comment")
public class Comment extends HNItem {

    @StorIOSQLiteColumn(name = CommentTable.COLUMN_COMMENT_ID, key = true)
    @SerializedName("id")
    long commentId;

    @StorIOSQLiteColumn(name = CommentTable.COLUMN_INDEX)
    int _index;

    @StorIOSQLiteColumn(name = CommentTable.COLUMN_BY)
    String by;

    @StorIOSQLiteColumn(name = CommentTable.COLUMN_DELETED)
    boolean deleted;

    @StorIOSQLiteColumn(name = CommentTable.COLUMN_PARENT)
    long parent;

    ArrayList<Long> kids;

    @StorIOSQLiteColumn(name = CommentTable.COLUMN_TIME)
    long time;

    @StorIOSQLiteColumn(name = CommentTable.COLUMN_TEXT)
    String text;

    @StorIOSQLiteColumn(name = CommentTable.COLUMN_LEVEL)
    int level;

    public ArrayList<Comment> comments;

    //FIXME necessary?
    public String error;

    public Comment() {
        comments = new ArrayList<>();
    }

    public Comment(long commentId, String by, long parent, ArrayList<Long> kids, long time, String text) {
        this.commentId = commentId;
        this.by = by;
        this.parent = parent;
        this.kids = kids;
        this.time = time;
        this.text = text;
    }

    public int getIndex() {
        return _index;
    }

    public void setIndex(int _index) {
        this._index = _index;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getBy() {
        return by;
    }

    public void setBy(String by) {
        this.by = by;
    }

    public long getCommentId() {
        return commentId;
    }

    public void setCommentId(long commentId) {
        this.commentId = commentId;
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