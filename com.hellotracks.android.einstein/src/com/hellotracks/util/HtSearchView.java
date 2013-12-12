package com.hellotracks.util;

import android.content.Context;
import android.support.v7.widget.SearchView;
import android.util.AttributeSet;

/**
 * SearchView that won't iconify and doesn't show an X icon when there's no text
 * (it used to be scrazier, but it got refactored)
 */
public class HtSearchView extends SearchView {

    public HtSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HtSearchView(Context context) {
        super(context);
        init();
    }

    private void init() {
        super.setIconified(false);
        onActionViewExpanded();
        setOnCloseListener(new OnCloseListener() {

            @Override
            public boolean onClose() {
                clearFocus();
                return true;
            }
        });
    }
}
