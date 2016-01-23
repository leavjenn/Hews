package com.leavjenn.hews.ui.widget;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v4.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;

import com.leavjenn.hews.R;
import com.leavjenn.hews.misc.SharedPrefsManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PopupFloatingWindow implements View.OnClickListener {
    private boolean isShowing;
    private Context mContext;
    private PopupWindow mWindow;
    private View mRootView;
    private String nameOfShowingActivity;
    private Drawable mBackground = null;
    // private WindowManager mWindowManager;
    private Spinner mSpinnerFont;
    private ImageButton mBtnFontLarge, mBtnFontSmall, mBtnLineNarrow, mBtnLineWide;
    Button mBtnDone;
    private SharedPreferences prefs;
    View mAttachView;


    public PopupFloatingWindow(Context context, View attachView) {
        isShowing = false;
        mContext = context;
        mAttachView = attachView;
        //mWindow = new PopupWindow(context);
        // mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        nameOfShowingActivity = mContext.getClass().getSimpleName();
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        //setContentView(R.layout.popup_window);
        setView(R.layout.popup_window);
        setUpButtons();
        mSpinnerFont = (Spinner) mRootView.findViewById(R.id.spinner_font);
        setUpFontSpinner(mSpinnerFont);
    }

    private void setUpButtons() {
        mBtnFontLarge = (ImageButton) mRootView.findViewById(R.id.imgbtn_font_large);
        mBtnFontLarge.setOnClickListener(this);
        mBtnFontSmall = (ImageButton) mRootView.findViewById(R.id.imgbtn_font_small);
        mBtnFontSmall.setOnClickListener(this);
        mBtnLineNarrow = (ImageButton) mRootView.findViewById(R.id.imgbtn_line_height_narrow);
        mBtnLineNarrow.setOnClickListener(this);
        mBtnLineWide = (ImageButton) mRootView.findViewById(R.id.imgbtn_line_height_wide);
        mBtnLineWide.setOnClickListener(this);
        mBtnDone = (Button) mRootView.findViewById(R.id.btn_done);
        mBtnDone.setOnClickListener(this);
    }

    private void setUpFontSpinner(Spinner spinner) {
        final Map<String, Typeface> fontMap = new ArrayMap<>();
        final List<String> fontList = new ArrayList<>();
        List<String> fontsName;
        if (nameOfShowingActivity.equals("MainActivity")) {
            fontsName = new ArrayList<>(Arrays.asList(SharedPrefsManager.getPostFontsList()));
        } else {
            fontsName = new ArrayList<>(Arrays.asList(SharedPrefsManager.getCommentFontList()));
        }

        for (String fontName : fontsName) {
            fontMap.put(fontName,
                Typeface.createFromAsset(mContext.getAssets(), fontName + ".ttf"));
            fontList.add(fontName);
        }

        spinner.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return fontList.size();
            }

            @Override
            public Object getItem(int position) {
                return fontList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return fontList.get(position).hashCode();
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(mContext)
                        .inflate(android.R.layout.simple_list_item_1, parent, false);
                }
                ((TextView) convertView).setTypeface(fontMap.get(fontList.get(position)));
                ((TextView) convertView).setText(fontList.get(position));
                ((TextView) convertView).setPaintFlags(((TextView) convertView).getPaintFlags()
                    | Paint.SUBPIXEL_TEXT_FLAG);
                return convertView;
            }
        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (nameOfShowingActivity.equals("MainActivity")) {
                    SharedPrefsManager.setPostFont(prefs, fontList.get(position));
                } else {
                    SharedPrefsManager.setCommentFont(prefs, fontList.get(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        if (nameOfShowingActivity.equals("MainActivity")) {
            spinner.setSelection(fontList.indexOf(SharedPrefsManager.getPostFont(prefs)));
        } else {
            spinner.setSelection(fontList.indexOf(SharedPrefsManager.getCommentFont(prefs)));
        }
    }

    /**
     * On pre show
     */
    protected void preShow() {
        if (mRootView == null)
            throw new IllegalStateException("setContentView was not called with a view to display.");

        if (mBackground == null)
            mWindow.setBackgroundDrawable(mContext.getResources().getDrawable(R.color.grey_400));
        else
            mWindow.setBackgroundDrawable(mBackground);

//        mWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
//        mWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        // mWindow.setContentView(mRootView);
        mWindow.setTouchable(true);
        //mWindow.setFocusable(true);
        //mWindow.setOutsideTouchable(true);

    }

    public void show() {
        preShow();
        //Error
        // android.view.WindowManager$BadTokenException:
        // Unable to add window -- token android.view.ViewRootImpl$W@2274902e is not valid;
        // is your activity running?
        // Added android:spinnerMode="dialog" in xml
        // mWindow.showAtLocation(((Activity) mContext).getWindow().
        //getDecorView().findViewById(android.R.id.content), Gravity.NO_GRAVITY, 20, 200);
        mWindow.showAsDropDown(mAttachView, 0, 0);
        isShowing = true;
    }

//    /**
//     * On dismiss
//     */
//    protected void onDismiss() {
//    }

    /**
     * Set background drawable.
     *
     * @param background Background drawable
     */
    public void setBackgroundDrawable(Drawable background) {
        mBackground = background;
    }

    void setView(int layoutID) {
        LayoutInflater layoutInflater =
            (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRootView = layoutInflater.inflate(layoutID, null);
        //mWindow = new PopupWindow(mContext);
        //mWindow.setContentView(mRootView);

        mWindow = new PopupWindow(mRootView, ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    /**
     * Set content view.
     *
     * @param layoutResID Resource id
     */
    public void setContentView(int layoutResID) {
        LayoutInflater inflator = (LayoutInflater) mContext
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRootView = inflator.inflate(layoutResID, null);
        mWindow.setContentView(mRootView);
    }

    /**
     * Dismiss the popup window.
     */
    public void dismiss() {
        mWindow.dismiss();
        isShowing = false;
    }

    public boolean isWindowShowing() {
        return isShowing;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imgbtn_font_small:
                if (nameOfShowingActivity.equals("MainActivity")) {
                    SharedPrefsManager.setPostFontSize(prefs, -0.5f);
                } else {
                    SharedPrefsManager.setCommentFontSize(prefs, -0.5f);
                }
                break;
            case R.id.imgbtn_font_large:
                if (nameOfShowingActivity.equals("MainActivity")) {
                    SharedPrefsManager.setPostFontSize(prefs, 0.5f);
                } else {
                    SharedPrefsManager.setCommentFontSize(prefs, 0.5f);
                }
                break;
            case R.id.imgbtn_line_height_narrow:
                if (nameOfShowingActivity.equals("MainActivity")) {
                    SharedPrefsManager.setPostLineHeight(prefs, -0.1f);
                } else {
                    SharedPrefsManager.setCommentLineHeight(prefs, -0.1f);
                }
                break;
            case R.id.imgbtn_line_height_wide:
                if (nameOfShowingActivity.equals("MainActivity")) {
                    SharedPrefsManager.setPostLineHeight(prefs, 0.1f);
                } else {
                    SharedPrefsManager.setCommentLineHeight(prefs, 0.1f);
                }
                break;
            case R.id.btn_done:
                this.dismiss();
                break;
        }
    }
}
