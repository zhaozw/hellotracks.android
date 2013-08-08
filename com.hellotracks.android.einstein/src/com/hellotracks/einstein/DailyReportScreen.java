package com.hellotracks.einstein;

import org.json.JSONObject;

import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;

import com.hellotracks.R;
import com.hellotracks.activities.AbstractScreen;

public class DailyReportScreen extends AbstractScreen {

	private String account;

	private static final int N_ArrivesAtPlace = 1;
	private static final int N_LeavesFromPlace = 2;
	private static final int N_NewTrack = 4;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_dailyreport);

		int permissions = getIntent().getIntExtra(C.notify_email, 0);
		account = getIntent().getStringExtra(C.account);

		RadioButton tracksYes = (RadioButton) findViewById(R.id.tracksYes);
		RadioButton tracksNo = (RadioButton) findViewById(R.id.tracksNo);
		if ((permissions & (1 << N_NewTrack)) > 0)
			tracksYes.setChecked(true);
		else
			tracksNo.setChecked(true);

		RadioButton arrivalsYes = (RadioButton) findViewById(R.id.arrivalsYes);
		RadioButton arrivalsNo = (RadioButton) findViewById(R.id.arrivalsNo);
		if ((permissions & (1 << N_ArrivesAtPlace)) > 0)
			arrivalsYes.setChecked(true);
		else
			arrivalsNo.setChecked(true);

		RadioButton departuresYes = (RadioButton) findViewById(R.id.departuresYes);
		RadioButton departuresNo = (RadioButton) findViewById(R.id.departuresNo);
		if ((permissions & (1 << N_LeavesFromPlace)) > 0)
			departuresYes.setChecked(true);
		else
			departuresNo.setChecked(true);
		
		setupActionBar(R.string.Profile);
	}
	
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            break;
        }
        return true;
    };


	public void onTracksYes(View view) {
		add(1 << N_NewTrack);
	}

	public void onArrivalsYes(View view) {
		add(1 << N_ArrivesAtPlace);
	}

	public void onDeparturesYes(View view) {
		add(1 << N_LeavesFromPlace);
	}

	public void onTracksNo(View view) {
		remove(1 << N_NewTrack);
	}

	public void onArrivalsNo(View view) {
		remove(1 << N_ArrivesAtPlace);
	}

	public void onDeparturesNo(View view) {
		remove(1 << N_LeavesFromPlace);
	}

	private void add(int value) {
		try {
			JSONObject obj = prepareObj();
			obj.put("add_notifyemail", value);
			obj.put(C.account, account);
			doAction(ACTION_EDITPROFILE, obj, null);
		} catch (Exception exc) {
		}
	}

	private void remove(int value) {
		try {
			JSONObject obj = prepareObj();
			obj.put("remove_notifyemail", value);
			obj.put(C.account, account);
			doAction(ACTION_EDITPROFILE, obj, null);
		} catch (Exception exc) {
		}
	}

}
