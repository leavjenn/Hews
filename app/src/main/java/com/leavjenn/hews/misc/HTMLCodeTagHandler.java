package com.leavjenn.hews.misc;

import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.style.TypefaceSpan;

import org.xml.sax.XMLReader;

public class HTMLCodeTagHandler implements Html.TagHandler {
    @Override
    public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
        if (tag.equalsIgnoreCase("code")) {
            processCodeTag(opening, output);
        }
    }

    private void processCodeTag(boolean opening, Editable output) {
        int len = output.length();
        if (opening) {
            output.setSpan(new TypefaceSpan("monospace"), len, len, Spannable.SPAN_MARK_MARK);
        } else {
            Object obj = getLast(output, TypefaceSpan.class);
            int where = output.getSpanStart(obj);
            output.removeSpan(obj);
            if (where != len) {
                output.setSpan(new TypefaceSpan("monospace"), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private Object getLast(Editable text, Class kind) {
        Object[] objs = text.getSpans(0, text.length(), kind);
        if (objs.length == 0) {
            return null;
        } else {
            for (int i = objs.length; i > 0; i--) {
                if (text.getSpanFlags(objs[i - 1]) == Spannable.SPAN_MARK_MARK) {
                    return objs[i - 1];
                }
            }
            return null;
        }
    }
}
