package com.hellotracks.deprecated;

import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.model.ResultWorker;

public class LabelsScreen extends AbstractScreen {

	private Button labelGreen;
	private Button labelYellow;
	private Button labelOrange;
	private Button labelRed;
	private Button labelViolett;
	private Button labelBlue;

	private String profileString = null;
	private boolean myProfile = false;
	private String account = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_tracklabels);
		profileString = getIntent().getExtras().getString(C.profilestring);
		myProfile = getIntent().getExtras().getBoolean(C.myprofile, false);

		labelGreen = (Button) findViewById(R.id.labelGreen);
		labelYellow = (Button) findViewById(R.id.labelYellow);
		labelOrange = (Button) findViewById(R.id.labelOrange);
		labelRed = (Button) findViewById(R.id.labelRed);
		labelViolett = (Button) findViewById(R.id.labelViolett);
		labelBlue = (Button) findViewById(R.id.labelBlue);

		SharedPreferences prefs = Prefs.get(this);

		try {
			JSONObject obj = new JSONObject(profileString);
			account = obj.getString("account");
			if (account == null)
				account = prefs.getString(Prefs.USERNAME, "");

			labelGreen.setText(prefs.getString(Prefs.TRACKLABEL_GREEN, ""));
			labelYellow.setText(prefs.getString(Prefs.TRACKLABEL_YELLOW, ""));
			labelOrange.setText(prefs.getString(Prefs.TRACKLABEL_ORANGE, ""));
			labelRed.setText(prefs.getString(Prefs.TRACKLABEL_RED, ""));
			labelViolett.setText(prefs.getString(Prefs.TRACKLABEL_VIOLETT, ""));
			labelBlue.setText(prefs.getString(Prefs.TRACKLABEL_BLUE, ""));

		} catch (Exception exc) {
			Log.w(exc);
		}
	}

	public void onGreen(View view) {
		openLabelTextDialog(Prefs.TRACKLABEL_GREEN, labelGreen);
	}
	
	public void onYellow(View view) {
		openLabelTextDialog(Prefs.TRACKLABEL_YELLOW, labelYellow);
	}
	
	public void onOrange(View view) {
		openLabelTextDialog(Prefs.TRACKLABEL_ORANGE, labelOrange);
	}
	
	public void onRed(View view) {
		openLabelTextDialog(Prefs.TRACKLABEL_RED, labelRed);
	}
	
	public void onViolett(View view) {
		openLabelTextDialog(Prefs.TRACKLABEL_VIOLETT, labelViolett);
	}
	
	public void onBlue(View view) {
		openLabelTextDialog(Prefs.TRACKLABEL_BLUE, labelBlue);
	}

	private void openLabelTextDialog(final String key, final Button label) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setMessage("Enter label text");

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		input.setText(Prefs.get(this).getString(key, ""));
		input.setBackgroundColor(Color.GREEN);
		alert.setView(input);

		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				final String value = input.getText().toString();
				try {
					JSONObject obj = prepareObj();
					obj.put(key, value);
					obj.put(C.account, account);
					doAction(ACTION_EDITPROFILE, obj, new ResultWorker() {
						@Override
						public void onResult(String result, Context context) {
							Prefs.get(LabelsScreen.this).edit()
									.putString(key, value);
							label.setText(value);
						}
					});
				} catch (Exception exc) {
					Log.w(exc);
				}
			}
		});

		AlertDialog dialog = alert.create();
		dialog.setCanceledOnTouchOutside(true);
		dialog.show();
	}

}
