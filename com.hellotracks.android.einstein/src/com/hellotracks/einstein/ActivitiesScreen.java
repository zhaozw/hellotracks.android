package com.hellotracks.einstein;

import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.model.ResultWorker;
import com.hellotracks.util.lazylist.LazyAdapter;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;

public class ActivitiesScreen extends BasicAbstractScreen {


	class UpdateTimeTask extends TimerTask {

		public void run() {
			refill();
		}
	}

	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			refill();
		}
	};

	private Timer timer;

	protected void onResume() {
		registerReceiver(mIntentReceiver, new IntentFilter(
				Prefs.TAB_ACTIVITIES_INTENT));
		timer = new Timer();
		timer.schedule(new UpdateTimeTask(), 30000, 30000);
		super.onResume();
	};

	@Override
	protected void onPause() {
		unregisterReceiver(mIntentReceiver);
		if (timer != null)
			timer.cancel();
		super.onPause();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		count = 10;

		Typeface tf = Typeface.createFromAsset(getAssets(), C.FortuneCity);
		TextView name = (TextView) findViewById(R.id.name);
		name.setTypeface(tf);

		list.setOnItemClickListener(new OnItemClickListener() {

			private int ACTION_REMOVE = 2;
			private int ACTION_TRACK = 3;

			@Override
			public void onItemClick(AdapterView<?> ad, final View view,
					final int pos, long id) {
				QuickAction mQuickAction = new QuickAction(
						ActivitiesScreen.this);
				boolean any = false;
				if ((adapter.getInt(pos, "actions") & MAY_DELETE) > 0) {
					ActionItem removeItem = new ActionItem(
							ActivitiesScreen.this, R.string.RemoveNotification);
					removeItem.setActionId(ACTION_REMOVE);
					mQuickAction.addActionItem(removeItem);
					any = true;
				}
				final int track = adapter.getInt(pos, TRACK);
				if (track > 0) {
					ActionItem trackItem = new ActionItem(
							ActivitiesScreen.this, R.string.ShowTrackInMap);
					trackItem.setActionId(ACTION_TRACK);
					mQuickAction.addActionItem(trackItem);
					any = true;
				}
				if (!any) {
					return;
				}

				mQuickAction
						.setOnActionItemClickListener(new OnActionItemClickListener() {

							@Override
							public void onItemClick(QuickAction source,
									int pos, int actionId) {
								if (actionId == ACTION_REMOVE) {
									try {
										JSONObject obj = prepareObj();
										obj.put(C.id, adapter.getId(pos));
										doAction(ACTION_REMOVEOBJECT, obj,
												new ResultWorker() {
													@Override
													public void onResult(
															String result,
															Context context) {
														refill();
													}
												});
									} catch (Exception exc) {
										Log.w(exc);
									}
								} else {
									showTrack(view, track);
								}
							}
						});

				mQuickAction.show(view);

			}
		});

		refill();
	}

	@Override
	protected int getContentView() {
		return R.layout.screen_activities;
	}

	@Override
	protected String getAction() {
		return ACTION_ACTIVITIES;
	}

	@Override
	protected int getEmptyMessage() {
		return R.string.NoEntries;
	}

	@Override
	protected LazyAdapter createAdapter(JSONArray array) {
		return new MoreLazyAdapter(this, array) {

		
			@Override
			protected int getListItemLayoutFor(int index) {
				final int track = adapter.getInt(index, TRACK);
				if (track > 0) {
					return R.layout.list_item_activities_track;
				} else {
					return R.layout.list_item_activities;
				}
				
			}
		};
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			onBack(null);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}