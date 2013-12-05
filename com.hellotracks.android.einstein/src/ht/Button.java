package ht;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

public class Button extends android.widget.Button {

    private static Typeface roboto;

    public Button(Context context, AttributeSet attrs) {
        super(context, attrs);

        //        TypedArray styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.TypefacedTextView);
        //        String fontName = styledAttrs.getString(R.styleable.TypefacedTextView_typeface);
        //        styledAttrs.recycle();

        if (roboto == null) {
            roboto = Typeface.createFromAsset(context.getAssets(), "Roboto-Regular.ttf");
        }

        //if (fontName != null) {

        setTypeface(roboto);
        //}
    }
}
