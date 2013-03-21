package com.hellotracks.activities;

import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;
import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.einstein.C;
import com.hellotracks.einstein.HelpScreen;
import com.hellotracks.einstein.HomeMapScreen;
import com.hellotracks.model.ResultWorker;
import com.hellotracks.types.LatLng;

public class WelcomeScreen extends RegisterScreen {

	private int[] images= { R.drawable.gallery1, R.drawable.gallery2,
			R.drawable.gallery3, R.drawable.gallery4, R.drawable.gallery7 };

	public class ImageAdapter extends BaseAdapter {
		private Context myContext;

		private View[] views = new View[images.length];

		public ImageAdapter(Context c) {
			this.myContext = c;
		}

		int count = 0;

		public void randomAnim() {
			final int p = count++ % images.length;
			try {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						views[p].startAnimation(animation);
					}
				});
			} catch (Exception exc) {
				Log.w(exc);
			}
		}

		public int getCount() {
			return images.length;
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				ImageView iv = new ImageView(this.myContext);
				iv.setImageResource(images[position]);
				iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
				iv.setLayoutParams(new GridView.LayoutParams(
						LayoutParams.MATCH_PARENT, 100));
				views[position] = iv;
			}
			return views[position];
		}
	}

	private Animation animation;
	private Timer timer;
	private ImageAdapter imageAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_welcome);

		animation = AnimationUtils.loadAnimation(this, R.anim.fade);

		final GridView grid = (GridView) findViewById(R.id.grid);
		grid.setAdapter(imageAdapter = new ImageAdapter(this));
		grid.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> ad, View v, int pos, long id) {
				final ImageView iv = new ImageView(WelcomeScreen.this);
				iv.setImageResource(images[pos]);
				iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
				iv.setLayoutParams(new GridView.LayoutParams(
						LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
				final PopupWindow pw = new PopupWindow(iv, 200, 400, true);
				pw.setAnimationStyle(R.style.Animations_PopDownMenu_Center);
				pw.showAtLocation(grid, Gravity.CENTER, 0, 0);
				new Thread() {
					public void run() {
						try {
							Thread.sleep(3000);
							runOnUiThread(new Runnable() {

								@Override
								public void run() {
									pw.dismiss();
								}

							});
						} catch (InterruptedException e) {
						}

					};
				}.start();
			}
		});

		Typeface tf = Typeface.createFromAsset(getAssets(), C.LaBelle);
		TextView slogan = (TextView) findViewById(R.id.slogan);
		slogan.setTypeface(tf);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onPause() {
		timer.cancel();
		super.onPause();
	}

	public void onDirect(View view) {
		try {
			if (!isOnline(true)) {
				return;
			}
			String s = Secure
					.getString(getContentResolver(), Secure.ANDROID_ID);
			final String u = "+" + s.substring(4, 5) + s.substring(12, 16);
			final String p = s.substring(1, 4) + s.substring(8, 11);

			Prefs.get(this).edit().putString(Prefs.USERNAME, u)
					.putString(Prefs.PASSWORD, p).commit();

			JSONObject data = AbstractScreen.prepareObj(this);
			data.put("man", Build.MANUFACTURER);
			data.put("mod", Build.MODEL);
			data.put("os", "Android " + Build.VERSION.RELEASE);
			data.put(
					"ver",
					this.getPackageManager().getPackageInfo(
							this.getPackageName(), 0).versionCode);
			data.put(
					"vername",
					this.getPackageManager().getPackageInfo(
							this.getPackageName(), 0).versionName);

			AbstractScreen.doAction(this, AbstractScreen.ACTION_LOGIN, data,
					null, new ResultWorker() {

						@Override
						public void onResult(final String result,
								Context context) {
							startActivity(new Intent(WelcomeScreen.this,
									HomeMapScreen.class));
							finish();
						}

						@Override
						public void onFailure(final int status,
								final Context context) {
							try {
								JSONObject registerObj = new JSONObject();
								registerObj.put("accounttype", "person");
								registerObj.put("name", getResources().getString(R.string.ThatsMe));
								registerObj.put("username", u);
								registerObj.put("password", p);
								Locale locale = Locale.getDefault();
								TimeZone timezone = TimeZone.getDefault();
								registerObj.put("language",
										locale.getLanguage());
								registerObj.put("country", locale.getCountry());
								registerObj.put("timezone", timezone.getID());
								LatLng ll = getLastLocation();
								if (ll.lat + ll.lng != 0) {
									registerObj.put("latitude", ll.lat);
									registerObj.put("longitude", ll.lng);
								}
								sendRegistration(registerObj, u, p, false);
							} catch (Exception exc) {
								Log.w(exc);
								Toast.makeText(WelcomeScreen.this,
										R.string.SomethingWentWrong,
										Toast.LENGTH_LONG).show();
							}
						}
					});

		} catch (Exception exc) {
			Log.w(exc);
			Toast.makeText(WelcomeScreen.this, R.string.DoesNotWorkWithThisPhone,
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onResume() {
		timer = new Timer();

		SharedPreferences prefs = Prefs.get(this);
		String username = prefs.getString(Prefs.USERNAME, "");
		String password = prefs.getString(Prefs.PASSWORD, "");
		boolean autologin = prefs.getBoolean(Prefs.AUTOLOGIN, true);
		boolean tracking = prefs.getBoolean(Prefs.STATUS_ONOFF, false);
		if ((tracking || autologin) && username.length() > 0
				&& password.length() > 0) {
			startActivity(new Intent(this, HomeMapScreen.class));
			finish();
		}

		super.onResume();
	}

	public void onSignUp(View view) {
		startActivity(new Intent(this, SignUpScreen.class));
		finish();
	}

	public void onLogIn(View view) {
		startActivity(new Intent(this, LoginScreen.class));
		finish();
	}

	public void onInfo(View view) {
		startActivity(new Intent(this, HelpScreen.class));
	}

}
