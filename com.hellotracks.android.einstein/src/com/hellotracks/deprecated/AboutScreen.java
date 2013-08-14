package com.hellotracks.deprecated;

import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.TextView;

import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.C;

public class AboutScreen extends AbstractScreen {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_about);

		TextView nameView = (TextView) findViewById(R.id.name);
		Typeface tf = Typeface.createFromAsset(getAssets(), C.FortuneCity);
		nameView.setTypeface(tf);
		nameView.setText("@hellotracks");
		
		TextView copyrightView = (TextView) findViewById(R.id.copyright);
		copyrightView.setTypeface(tf);
		copyrightView.setText("Copyright © 2012 Mario Bertschler");

		try {
			TextView versionView = (TextView) findViewById(R.id.version);
			versionView.setTypeface(tf);
			versionView.setText("Version " + this.getPackageManager().getPackageInfo(
					this.getPackageName(), 0).versionName);
		} catch (Exception exc) {
		}
	}

}
