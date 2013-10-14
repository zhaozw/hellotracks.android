package com.hellotracks.base;

import java.util.HashMap;
import java.util.Map;
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
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.util.ResultWorker;
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

	protected Map<String, Object> getParams() {
		if (account == null)
			return super.getParams();

		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put(C.account, account);
		map.put("fromts", System.currentTimeMillis());
		map.put("count", 10);
		return map;
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

	private String account = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		account = getIntent().getStringExtra(C.account);

		super.onCreate(savedInstanceState);
		count = 10;

		Typeface tf = Typeface.createFromAsset(getAssets(), C.FortuneCity);
		TextView name = (TextView) findViewById(R.id.name);
		name.setTypeface(tf);
		
		View more = getLayoutInflater().inflate(R.layout.list_item_more, null);
        Button button = (Button) more.findViewById(R.id.loadButton);
        button.setOnClickListener(new OnClickListener() {

            protected long fromTS = System.currentTimeMillis() * 2;

            @Override
            public void onClick(final View v) {
                HashMap<String, Object> map = new HashMap<String, Object>();
                if (getParams() != null)
                    map.putAll(getParams());
                map.put("fromts", fromTS - 1);
                map.put("count", 10);
                refill(map, new ResultWorker() {
                    @Override
                    public void onResult(final String result, Context context) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    adapter.addData(new JSONArray(result));
                                    adapter.notifyDataSetChanged();
                                    if (adapter.getCount() > 0)
                                        fromTS = Math.min(adapter.getLong(adapter.getCount() - 1, "ts") - 1, fromTS);
                                } catch (Exception exc) {
                                }
                            }
                        });
                    }
                });

            }
        });
        list.addFooterView(more);


		list.setOnItemClickListener(new OnItemClickListener() {

			private int ACTION_REMOVE = 2;
			private int ACTION_TRACK = 3;

			@Override
			public void onItemClick(AdapterView<?> ad, final View view,
					final int pos, long id) {
				QuickAction action = new QuickAction(ActivitiesScreen.this);
				boolean any = false;
				if ((adapter.getInt(pos, "actions") & MAY_DELETE) > 0) {
					ActionItem removeItem = new ActionItem(
							ActivitiesScreen.this, R.string.RemoveNotification);
					removeItem.setActionId(ACTION_REMOVE);
					action.addActionItem(removeItem);
					any = true;
				}
				if (!any) {
					return;
				}
				action.setOnActionItemClickListener(new OnActionItemClickListener() {

					@Override
					public void onItemClick(QuickAction source, int pos,
							int actionId) {
						if (actionId == ACTION_REMOVE) {
							try {
								JSONObject obj = prepareObj();
								obj.put(C.id, adapter.getId(pos));
								doAction(ACTION_REMOVEOBJECT, obj,
										new ResultWorker() {
											@Override
											public void onResult(String result,
													Context context) {
												refill();
											}
										});
							} catch (Exception exc) {
								Log.w(exc);
							}
						}
					}
				});

				action.show(view);

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
		return account != null ? ACTION_PERSONALACTIVITIES : ACTION_ACTIVITIES;
	}

	@Override
	protected int getEmptyMessage() {
		return R.string.NoEntries;
	}

	@Override
	protected LazyAdapter createAdapter(JSONArray array) {
		return new LazyAdapter(this, array) {

		    @Override
		    protected void setup() {
		        everyListItemOwnLayout = true;
		    }
		   
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