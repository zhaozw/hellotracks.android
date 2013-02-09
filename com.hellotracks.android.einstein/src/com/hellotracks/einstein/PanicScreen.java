package com.hellotracks.einstein;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hellotracks.Log;
import com.hellotracks.R;
import com.hellotracks.activities.AbstractScreen;
import com.hellotracks.model.ResultWorker;

public class PanicScreen extends AbstractScreen {

	private boolean running = true;

	private String message = "";
	private String[] receivers;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_panic);

		message = getIntent().getStringExtra("message");
		receivers = getIntent().getStringArrayExtra("receivers");

		final TextView text = (TextView) findViewById(R.id.text);
		try {
			final Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			v.vibrate(30);
		} catch (Exception exc) {
		}

		new Thread() {
			@Override
			public void run() {
				for (int i = 10; i >= 0; i--) {
					if (!running)
						break;
					final String count = " " + i + " ";
					if (i == 0) {
						panic();
						finish();
					} else {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								text.setText(count);
							}
						});
					}
					try {
						Thread.sleep(1000);
					} catch (Exception exc) {
					}
				}
			}
		}.start();
	}

	public void onCancel(View view) {
		running = false;
		finish();
	}

	private void panic() {
		try {
			JSONObject obj = prepareObj();
			obj.put("msg", message);
			JSONArray array = new JSONArray();
			for (String r : receivers) {
				array.put(r);
			}
			obj.put("receivers", array);
			doAction(AbstractScreen.ACTION_SENDMSG, obj, null,
					new ResultWorker() {
						@Override
						public void onResult(String result, Context context) {
							Toast.makeText(getApplicationContext(),
									R.string.OK, Toast.LENGTH_LONG).show();
						}
					});

		} catch (Exception exc) {
			Log.w(exc);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			onCancel(null);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
