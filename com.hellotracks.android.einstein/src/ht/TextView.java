package ht;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;

import com.hellotracks.R;

public class TextView extends android.widget.TextView {
    
    private static Typeface roboto;
    
    public TextView(Context context, AttributeSet attrs) {
        super(context, attrs);

//        TypedArray styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.TypefacedTextView);
//        String fontName = styledAttrs.getString(R.styleable.TypefacedTextView_typeface);
//        styledAttrs.recycle();

        if (roboto == null) {
            roboto = Typeface.createFromAsset(context.getAssets(), "Roboto-Regular.ttf");
        }

        //if (fontName != null) {

        setTypeface(roboto);
    }
}
