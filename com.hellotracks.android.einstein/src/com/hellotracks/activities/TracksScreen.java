package com.hellotracks.activities;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.einstein.BasicAbstractScreen;
import com.hellotracks.einstein.C;
import com.hellotracks.einstein.ContactsScreen;
import com.hellotracks.einstein.MoreLazyAdapter;
import com.hellotracks.einstein.TrackScreen;
import com.hellotracks.model.ResultWorker;
import com.hellotracks.util.lazylist.LazyAdapter;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;

public class TracksScreen extends BasicAbstractScreen {

	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			account = Prefs.get(TracksScreen.this)
					.getString(Prefs.USERNAME, "");
			nameView.setText(R.string.MyTracks);
			refill();
		}
	};

	protected void onResume() {
		registerReceiver(mIntentReceiver, new IntentFilter(
				Prefs.TAB_TRACKS_INTENT));
		super.onResume();
	};

	@Override
	protected void onPause() {
		unregisterReceiver(mIntentReceiver);
		super.onPause();
	}

	private TextView nameView;

	private Typeface tf;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		count = 1;
		account = getIntent().getStringExtra(C.account);
		if (account == null) {
			account = Prefs.get(this).getString(Prefs.USERNAME, "");
		}
		super.onCreate(savedInstanceState);

		nameView = (TextView) findViewById(R.id.name);
		tf = Typeface.createFromAsset(getAssets(), C.FortuneCity);
		nameView.setTypeface(tf);

		String name = getIntent().getStringExtra(C.name);
		if (name != null) {
			nameView.setText(name);
		} 

		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> ad, final View view,
					final int pos, long id) {
				Intent intent = new Intent(TracksScreen.this, TrackScreen.class);
				long trackId = adapter.getId(pos);
				if (trackId > 0) {
					intent.putExtra("track", trackId);
					intent.putExtra("labels", adapter.getInt(pos, "labels"));
					intent.putExtra("url", adapter.getString(pos, "url"));
					intent.putExtra("text", adapter.getString(pos, "info"));
					intent.putExtra("link", adapter.getString(pos, "link"));
					intent.putExtra("actions", adapter.getInt(pos, "actions"));
					try {
						intent.putExtra("comments",
								adapter.getArray(pos, "comments").toString());
					} catch (Exception exc) {
					}
					startActivityForResult(intent, 0);
				} else if (trackId == -1) {
					final AlertDialog.Builder alert = new AlertDialog.Builder(
							TracksScreen.this);
					alert.setTitle(R.string.MergeTracks);
					alert.setMessage(R.string.MergeText);
					alert.setPositiveButton(
							getResources().getString(R.string.MergeNow),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									try {
										JSONObject data = prepareObj();
										data.put("track1",
												adapter.getLong(pos, "track1"));
										data.put("track2",
												adapter.getLong(pos, "track2"));
										doAction(ACTION_MERGETRACKS, data,
												new ResultWorker() {
													@Override
													public void onResult(
															String result,
															Context context) {
														refill();
													}
												});
										Toast.makeText(TracksScreen.this,
												R.string.OK, Toast.LENGTH_SHORT)
												.show();
									} catch (Exception exc) {
									}
								}
							});
					alert.setNegativeButton(
							getResources().getString(R.string.Cancel),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

								}
							});
					AlertDialog dlg = alert.create();
					dlg.setCanceledOnTouchOutside(true);
					dlg.show();
				}
			}
		});
		
		refill();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (data != null && data.getStringExtra(C.account) != null) {
			account = data.getStringExtra(C.account);
			String name = data.getStringExtra(C.name);
			if (name != null) {
				nameView.setText(name);
			}
		}
		refill();
	}

	@Override
	protected int getEmptyMessage() {
		return R.string.CurrentlyNoTracksAvailable;
	}

	public void onBack(View view) {
		finish();
	}

	public static class Flag {
		public static final int GREEN = 1 << 1;
		public static final int YELLOW = 1 << 2;
		public static final int ORANGE = 1 << 3;
		public static final int RED = 1 << 4;
		public static final int VIOLETT = 1 << 5;
		public static final int BLUE = 1 << 6;
		public static final int NONE = 0;
	}

	@Override
	protected LazyAdapter createAdapter(JSONArray array) {
		MoreLazyAdapter adapter = new MoreLazyAdapter(this, array) {
			@Override
			protected int getListItemLayoutFor(int index) {
				return R.layout.list_item_track;
			}

			@Override
			public View getView(int index, View convertView, ViewGroup parent) {
				final long id = getId(index);
				if (id == -1) {
					View map = inflater.inflate(R.layout.list_item_pause, null);
					TextView button = (TextView) map.findViewById(R.id.time);
					button.setText(getString(index, "text"));
					return map;
				}
				View view = super.getView(index, convertView, parent);
				if (index < super.getCount() - 1) {

					view.findViewById(R.id.image).setOnClickListener(
							new OnClickListener() {

								@Override
								public void onClick(View v) {
									showTrack(v, id);
								}
							});

					JSONArray comments = getArray(index, "comments");
					if (comments != null && comments.length() > 0) {
						TextView bubble = (TextView) view
								.findViewById(R.id.bubble);
						bubble.setVisibility(View.VISIBLE);
						bubble.setText(" " + comments.length() + " ");
					} else {
						view.findViewById(R.id.bubble).setVisibility(View.GONE);
					}

					int labels = getInt(index, "labels");
					view.findViewById(R.id.label_green).setVisibility(
							(labels & Flag.GREEN) > 0 ? View.VISIBLE
									: View.GONE);
					view.findViewById(R.id.label_yellow).setVisibility(
							(labels & Flag.YELLOW) > 0 ? View.VISIBLE
									: View.GONE);
					view.findViewById(R.id.label_orange).setVisibility(
							(labels & Flag.ORANGE) > 0 ? View.VISIBLE
									: View.GONE);
					view.findViewById(R.id.label_red).setVisibility(
							(labels & Flag.RED) > 0 ? View.VISIBLE : View.GONE);
					view.findViewById(R.id.label_violett).setVisibility(
							(labels & Flag.VIOLETT) > 0 ? View.VISIBLE
									: View.GONE);
					view.findViewById(R.id.label_blue)
							.setVisibility(
									(labels & Flag.BLUE) > 0 ? View.VISIBLE
											: View.GONE);
				}
				return view;

			}

		};
		return adapter;
	}

	@Override
	protected int getContentView() {
		return R.layout.screen_tracks;
	}

	@Override
	protected Map<String, Object> getParams() {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("pauses", true);
		return params;
	}

	@Override
	protected String getAction() {
		return ACTION_TRACKS;
	}

	public void onMenu(View view) {
		QuickAction mQuickAction = new QuickAction(this);
		mQuickAction
				.setOnActionItemClickListener(new OnActionItemClickListener() {

					@Override
					public void onItemClick(QuickAction source, int pos,
							int actionId) {
						if (pos == 0) {
							Intent intent = new Intent(TracksScreen.this,
									ContactsScreen.class);
							intent.putExtra(C.type, "person");
							intent.putExtra(C.account, account);
							startActivityForResult(intent,
									C.REQUESTCODE_CONTACT);
						} else {
							account = Prefs.get(TracksScreen.this).getString(
									Prefs.USERNAME, "");
							nameView.setText(R.string.MyTracks);
							refill();
						}
					}
				});

		ActionItem item = new ActionItem(this, R.string.Contacts);
		mQuickAction.addActionItem(item);
		ActionItem item2 = new ActionItem(this, R.string.MyTracks);
		mQuickAction.addActionItem(item2);
		mQuickAction.show(view);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			onBack(null);
			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			onMenu(findViewById(R.id.button_menu));
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

}