package com.leavjenn.hews.ui;

import android.os.Bundle;

public abstract class BasePresenter {
    public abstract void setup();

    public abstract void restoreState(Bundle savedInstanceState);

    public abstract void saveState(Bundle outState);

    public abstract void destroy();

    public abstract void unsubscribe();
}