package com.hellotracks.einstein;

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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.activities.AbstractScreen;
import com.hellotracks.activities.RegisterPlaceScreen;
import com.hellotracks.model.ResultWorker;
import com.hellotracks.util.ImageCache;
import com.hellotracks.util.ImageCache.ImageCallback;
import com.hellotracks.util.lazylist.LazyAdapter;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;

public class NetworkScreen extends BasicAbstractScreen {

	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			clearAndRefill();
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

	@Override
	protected LazyAdapter createAdapter(JSONArray array) {
		final LazyAdapter adapter = new LazyAdapter(this, array) {

			public int getCount() {
				return data.size() + 4;
			}

			@Override
			public View getView(final int index, View convertView,
					ViewGroup parent) {

				int count = getCount();
				if (index == count - 4) {
					final View map = inflater.inflate(
							R.layout.list_item_search, null);
					final TextView searchField = (TextView) map
							.findViewById(R.id.searchField);
					searchField.setFocusable(false);
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(searchField.getWindowToken(), 0);
					searchField.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View v) {
							openSearchDialog(map);
						}
					});
					return map;
				} else if (index == count - 3) {
					View map = inflater.inflate(R.layout.list_item_more, null);
					Button button = (Button) map.findViewById(R.id.loadButton);
					button.setText(R.string.CreateNewPlace);
					button.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(final View v) {
							startActivityForResult(new Intent(
									NetworkScreen.this,
									RegisterPlaceScreen.class),
									C.REQUESTCODE_CONTACT);
						}
					});
					return map;
				} else if (index == count - 2) {
					View map = inflater.inflate(R.layout.list_item_more, null);
					Button button = (Button) map.findViewById(R.id.loadButton);
					button.setText(R.string.NearbyMe);
					button.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(final View view) {
							openFindDialog();
						}
					});
					return map;
				} else if (index == count - 1) {
					View map = inflater.inflate(R.layout.list_item_more, null);
					Button button = (Button) map.findViewById(R.id.loadButton);
					button.setText(R.string.InviteContact);
					button.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(final View v) {
							openInviteDialog();
						}
					});
					return map;
				}

				final View vi = inflater.inflate(R.layout.list_item_mynetwork,
						null);

				try {
					JSONObject node = data.get(index);
					int type = node.has("type") ? node.getInt("type") : 0;

					TextView title = (TextView) vi.findViewById(R.id.nameText);
					TextView info = (TextView) vi
							.findViewById(R.id.messageText);
					TextView time = (TextView) vi.findViewById(R.id.timeText);
					time.setVisibility(View.VISIBLE);

					time.setText(node.has("time") ? node.getString("time") : "");
					title.setText(node.getString("title"));
					info.setText(node.getString("info"));

					final ImageView icon = (ImageView) vi
							.findViewById(R.id.icon);
					ImageCache cache = ImageCache.getInstance();

					String url = node.getString("url");

					if (url != null) {
						Bitmap bm = cache.loadFromCache(url);
						if (bm != null) {
							icon.setImageBitmap(bm);
						} else {
							icon.setImageBitmap(null);
							cache.loadAsync(url, new ImageCallback() {

								@Override
								public void onImageLoaded(final Bitmap img,
										String url) {
									if (img != null) {
										activity.runOnUiThread(new Runnable() {

											@Override
											public void run() {
												icon.setImageBitmap(img);
											}

										});
									}
								}
							}, vi.getContext());
						}
					} else {
						icon.setVisibility(View.GONE);
					}

					if (type == TYPE_INVITATION) {
						info.setBackgroundColor(getResources().getColor(
								R.color.orange));
						title.setVisibility(View.INVISIBLE);
					} else if (type == TYPE_RECOMMENDATION) {
						info.setBackgroundColor(getResources().getColor(
								R.color.violett));
						title.setVisibility(View.INVISIBLE);
					} else {
						info.setBackgroundDrawable(null);
						title.setVisibility(View.VISIBLE);
					}

					View ignore = vi.findViewById(R.id.ignore);
					if ((type & TYPE_EXTERNAL) > 0) {
						time.setVisibility(View.GONE);
						ignore.setVisibility(View.VISIBLE);
						ignore.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								try {
									JSONObject obj = prepareObj();
									long id = getId(index);
									Log.w("not interrested in id=" + id);
									obj.put("id", id);

									doAction(
											AbstractScreen.ACTION_NOTINTERESTED,
											obj,
											getResources().getString(
													R.string.SendNow),
											new ResultWorker() {
												@Override
												public void onResult(
														String result,
														Context context) {
													NetworkScreen.this.adapter
															.remove(index);
												}
											});

								} catch (Exception exc) {
									Log.w(exc);
								}
							}
						});
					} else {
						ignore.setVisibility(View.GONE);
					}
				} catch (Exception exc) {
					Log.w(exc);
				}
				return vi;
			}

			@Override
			protected int getListItemLayoutFor(int index) {
				return R.layout.list_item_network;
			}
		};
		return adapter;
	}

	@Override
	protected String getAction() {
		return action;
	}

	@Override
	protected Map<String, Object> getParams() {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(C.type, type);
		if (ACTION_MYNETWORK.equals(action)) {
			String uid = account == null ? Prefs.get(this).getString(
					Prefs.USERNAME, "") : account;
			params.put(C.account, uid);
			params.put("include", TYPE_INVITATION | TYPE_RECOMMENDATION);
		} else {
			params.put("cnt", 25);
		}
		if (ACTION_SEARCH.equals(action)) {
			params.put("search", search);
		}
		return params;
	}

	@Override
	protected int getContentView() {
		return R.layout.screen_network;
	}

	@Override
	protected int getEmptyMessage() {
		return R.string.NoEntries;
	}

	private String action;
	private String type = "";
	private String search;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		action = getIntent().getStringExtra(C.action);
		type = getIntent().getStringExtra(C.type);
		search = getIntent().getStringExtra(C.search);
		if (action == null)
			action = ACTION_MYNETWORK;
		account = getIntent().getStringExtra(C.account);

		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> ad, View view, int pos,
					long id) {
				Intent intent = new Intent(NetworkScreen.this,
						ProfileScreen.class);
				intent.putExtra(C.account, adapter.getAccount(pos));
				intent.putExtra(C.name, adapter.getString(pos, "title"));
				startActivityForResult(intent, C.REQUESTCODE_CONTACT);
			}
		});

		TextView nameView = (TextView) findViewById(R.id.name);
		Typeface tf = Typeface.createFromAsset(getAssets(), C.FortuneCity);
		nameView.setTypeface(tf);
		nameView.setText(R.string.ContactsAndPlaces);

		refill();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == C.REQUESTCODE_CONTACT) {
			clearAndRefill();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public void onAdd(final View view) {
		ActionItem findItem = new ActionItem(this, R.string.NearbyMe);
		ActionItem newPlaceItem = new ActionItem(this, R.string.CreateNewPlace);
		ActionItem inviteItem = new ActionItem(this, R.string.InviteContact);
		ActionItem searchItem = new ActionItem(this,
				R.string.SearchForPeopleOrPlaces);
		QuickAction mQuickAction = new QuickAction(this);
		mQuickAction.addActionItem(searchItem);
		mQuickAction.addActionItem(newPlaceItem);
		mQuickAction.addActionItem(findItem);
		mQuickAction.addActionItem(inviteItem);
		mQuickAction
				.setOnActionItemClickListener(new OnActionItemClickListener() {

					@Override
					public void onItemClick(QuickAction source, int pos,
							int actionId) {
						switch (pos) {
						case 0:
							openSearchDialog(view);
							break;
						case 1:
							startActivityForResult(new Intent(
									NetworkScreen.this,
									RegisterPlaceScreen.class),
									C.REQUESTCODE_CONTACT);
							break;
						case 2:
							openFindDialog();
							break;
						case 3:
							openInviteDialog();
						}
					}
				});
		mQuickAction.show(view);
	}

	protected void openSearchDialog(final View view) {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setMessage(R.string.EnterSearch);
		final EditText input = new EditText(this);
		input.setHint(R.string.NameOrPlace);
		alert.setView(input);
		alert.setPositiveButton(getResources().getString(R.string.Search),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString().trim();
						if (value.length() >= 2) {
							action = ACTION_SEARCH;
							type = "search";
							search = value;
							refill();
						}
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

	private void clearAndRefill() {
		type = "";
		action = ACTION_MYNETWORK;
		refill();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			onBack(null);
			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			onAdd(findViewById(R.id.addButton));
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void openFindDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(
				NetworkScreen.this);
		builder.setTitle(R.string.NearbyMe);
		Resources r = getResources();
		String[] names = new String[] { r.getString(R.string.People),
				r.getString(R.string.Places) };
		builder.setItems(names, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				if (item == 0)
					type = "person";
				else
					type = "place";
				action = ACTION_FIND;
				refill();
			}
		});
		AlertDialog dialog = builder.create();
		dialog.setCanceledOnTouchOutside(true);
		dialog.show();
	}

	private void openInviteDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(
				NetworkScreen.this);
		builder.setTitle(R.string.InviteContact);
		Resources r = getResources();
		String[] names = new String[] {
				r.getString(R.string.InviteContactByEmail),
				r.getString(R.string.InviteContactBySms) };
		builder.setItems(names, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				if (item == 0) {
					onInviteContactByEmail(null);
				} else {
					onInviteContactBySms(null);
				}
			}
		});
		AlertDialog dialog = builder.create();
		dialog.setCanceledOnTouchOutside(true);
		dialog.show();
	}

}