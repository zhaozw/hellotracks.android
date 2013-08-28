package com.hellotracks.deprecated;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.ActivitiesScreen;
import com.hellotracks.base.C;
import com.hellotracks.messaging.ConversationListScreen;
import com.hellotracks.network.NetworkScreen;
import com.hellotracks.tools.PanicInfoScreen;
import com.hellotracks.tracks.TrackListScreen;

public class MenuScreen extends AbstractScreen {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_menu);

		TextView nameView = (TextView) findViewById(R.id.name);
		Typeface tf = Typeface.createFromAsset(getAssets(), C.FortuneCity);
		nameView.setTypeface(tf);
	}

	public void onRate(View view) {
		FlurryAgent.logEvent("MainMenu-Rate");
		openMarketDialog(getResources().getString(R.string.RateNow));
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode < 0) {
			setResult(resultCode);
			finish();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public void onActivities(View view) {
		FlurryAgent.logEvent("MainMenu-Activities");
		startActivity(new Intent(this, ActivitiesScreen.class));
		finish();
	}

	public void onMessages(View view) {
		FlurryAgent.logEvent("MainMenu-Messages");
		startActivity(new Intent(this, ConversationListScreen.class));
		finish();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	public void onEmergency(View view) {
	    startActivity(new Intent(this, PanicInfoScreen.class));
	    finish();
	}

	protected void openDialog() {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setMessage(R.string.InternetConnectionNeeded);
		alert.setPositiveButton(getResources().getString(R.string.logout),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						setResult(-1);
						finish();
					}
				});
		alert.setNegativeButton(getResources().getString(R.string.Cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.cancel();
					}
				});
		alert.show();
	}

	public void onCockpit(View view) {
		FlurryAgent.logEvent("MainMenu-Cockpit");
		Intent intent = new Intent(this, CockpitScreen.class);
		startActivity(intent);
	}

	public void onNetwork(View view) {
		FlurryAgent.logEvent("MainMenu-Network");
		Intent intent = new Intent(this, NetworkScreen.class);
		startActivity(intent);
	}

	public void onTracks(View view) {
		FlurryAgent.logEvent("MainMenu-Tracks");
		Intent intent = new Intent(this, TrackListScreen.class);
		startActivity(intent);
	}

	
}
