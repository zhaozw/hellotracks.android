package com.hellotracks.einstein;

import org.json.JSONObject;

import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;

import com.hellotracks.R;
import com.hellotracks.activities.AbstractScreen;
import com.hellotracks.activities.TrackListScreen;

public class CompanyPermissionsScreen extends AbstractScreen {

	private String account;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_company_permissions);

		int permissions = getIntent().getIntExtra(C.permissions, 0);
		account = getIntent().getStringExtra(C.account);
		
		RadioButton networkYes = (RadioButton) findViewById(R.id.networkYes);
		RadioButton networkNo = (RadioButton) findViewById(R.id.networkNo);
		if ((permissions & (1 << 0)) > 0)
			networkYes.setChecked(true);
		else
			networkNo.setChecked(true);
		
		RadioButton mapYes = (RadioButton) findViewById(R.id.mapYes);
		RadioButton mapNo = (RadioButton) findViewById(R.id.mapNo);
		if ((permissions & (1 << 1)) > 0)
			mapYes.setChecked(true);
		else
			mapNo.setChecked(true);
		
		RadioButton tracksActivitiesNone = (RadioButton) findViewById(R.id.tracksActivitiesNone);
		RadioButton tracksActivitiesOwn = (RadioButton) findViewById(R.id.tracksActivitiesOwn);
		RadioButton tracksActivitiesAll = (RadioButton) findViewById(R.id.tracksActivitiesAll);
	
		if ((permissions & (1 << 3)) > 0)
			tracksActivitiesAll.setChecked(true);
		else if ((permissions & (1 << 2)) > 0)
			tracksActivitiesOwn.setChecked(true);
		else
			tracksActivitiesNone.setChecked(true);
		
	}

	public void onNetworkYes(View view) {
		write("user_network", true);
	}

	public void onNetworkNo(View view) {
		write("user_network", false);
	}

	public void onMapYes(View view) {
		write("user_map", true);
	}

	public void onMapNo(View view) {
		write("user_map", false);
	}

	public void onTracksActivitiesNone(View view) {
		write("user_mytracks", false);
		write("user_activities", false);
	}
	
	public void onTracksActivitiesOwn(View view) {
		write("user_mytracks", true);
		write("user_activities", false);
	}
	
	public void onTracksActivitiesAll(View view) {
		write("user_mytracks", true);
		write("user_activities", true);
	}

	private void write(String key, boolean value) {
		try {
			JSONObject obj = prepareObj();
			obj.put(key, value);
			obj.put(C.account, account);
			doAction(ACTION_EDITPROFILE, obj, null);
		} catch (Exception exc) {
		}
	}

}
