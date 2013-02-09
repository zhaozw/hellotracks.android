package com.hellotracks.einstein;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.TextView;

import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.activities.AbstractScreen;
import com.hellotracks.activities.RegisterPlaceScreen;
import com.hellotracks.model.ResultWorker;
import com.hellotracks.util.lazylist.LazyAdapter;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;

public class ContactsScreen extends BasicAbstractScreen {

	@Override
	protected LazyAdapter createAdapter(JSONArray array) {
		final LazyAdapter adapter = new LazyAdapter(this, array) {
			@Override
			public View getView(final int index, View convertView,
					ViewGroup parent) {
				View vi = super.getView(index, convertView, parent);
				int type = getInt(index, "type");

				TextView title = (TextView) vi.findViewById(R.id.title);
				TextView info = (TextView) vi.findViewById(R.id.info);
				if (type == TYPE_INVITATION || type == TYPE_RECOMMENDATION) {
					info.setBackgroundDrawable(getResources().getDrawable(
							R.drawable.custom_button_insta_one));
					title.setVisibility(View.GONE);
				} else {
					info.setBackgroundDrawable(null);
					title.setVisibility(View.VISIBLE);
				}

				View ignore = vi.findViewById(R.id.ignore);
				if ((type & TYPE_EXTERNAL) > 0) {
					ignore.setVisibility(View.VISIBLE);
					ignore.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							try {
								JSONObject obj = prepareObj();
								obj.put("id", getId(index));

								doAction(
										AbstractScreen.ACTION_NOTINTERESTED,
										obj,
										getResources().getString(
												R.string.SendNow),
										new ResultWorker() {
											@Override
											public void onResult(String result,
													Context context) {
												ContactsScreen.this.adapter
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
		if (ACTION_NETWORK.equals(action)) {
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
	private String type;
	private String search;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		action = getIntent().getStringExtra(C.action);
		if (action == null)
			action = ACTION_NETWORK;
		type = getIntent().getStringExtra(C.type);
		account = getIntent().getStringExtra(C.account);

		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> ad, View view, int pos,
					long id) {
				Intent intent = new Intent();
				intent.putExtra(C.account, adapter.getAccount(pos));
				intent.putExtra(C.name, adapter.getString(pos, "title"));
				setResult(1, intent);
				finish();
			}
		});

		TextView nameView = (TextView) findViewById(R.id.name);
		Typeface tf = Typeface.createFromAsset(getAssets(), C.FortuneCity);
		nameView.setTypeface(tf);
		String type = getIntent().getExtras().getString(C.type);
		if ("person".equals(type)) {
			nameView.setText(R.string.Contacts);
		} else if ("place".equals(type)) {
			nameView.setText(R.string.Places);
		} else if ("members".equals(type)) {
			nameView.setText(R.string.Members);
		} else if ("present".equals(type)) {
			nameView.setText(R.string.Present);
			findViewById(R.id.addButton).setVisibility(View.GONE);
		} else if ("search".equals(type)) {
			nameView.setText(R.string.Search);
		}

		if ("search".equals(action)) {
			openSearchDialog(nameView);
		} else {
			refill();
		}
	}

	private void onAddPerson(final View view) {
		ActionItem findItem = new ActionItem(this, R.string.FindPeopleNearby);
		ActionItem searchItem = new ActionItem(this,
				R.string.SearchForPeopleOrPlaces);
		QuickAction mQuickAction = new QuickAction(this);
		mQuickAction.addActionItem(findItem);
		mQuickAction.addActionItem(searchItem);
		mQuickAction
				.setOnActionItemClickListener(new OnActionItemClickListener() {

					@Override
					public void onItemClick(QuickAction source, int pos,
							int actionId) {
						switch (pos) {
						case 0:
							type = "person";
							action = ACTION_FIND;
							refill();
							break;
						case 1:
							type = "search";
							openSearchDialog(view);
						}
					}
				});
		mQuickAction.show(view);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == C.REQUESTCODE_CONTACT) {
			setResult(resultCode, data);
			finish();
		}
	}

	private void onCreateMember(View view) {
		ActionItem newMemberItem = new ActionItem(this, R.string.CreateMember);
		QuickAction mQuickAction = new QuickAction(this);
		mQuickAction.addActionItem(newMemberItem);
		mQuickAction
				.setOnActionItemClickListener(new OnActionItemClickListener() {

					@Override
					public void onItemClick(QuickAction source, int pos,
							int actionId) {
						Intent intent = new Intent(getApplicationContext(),
								RegisterMemberScreen.class);
						intent.putExtra(C.owner, account);
						startActivityForResult(intent, C.REQUESTCODE_CONTACT);
					}
				});
		mQuickAction.show(view);
	}

	public void onAddPlace(final View view) {
		ActionItem newPlaceItem = new ActionItem(this, R.string.CreateNewPlace);
		ActionItem findPlacesItem = new ActionItem(this,
				R.string.FindPlacesNearby);
		ActionItem searchItem = new ActionItem(this,
				R.string.SearchForPeopleOrPlaces);
		QuickAction mQuickAction = new QuickAction(this);
		mQuickAction.addActionItem(newPlaceItem);
		mQuickAction.addActionItem(findPlacesItem);
		mQuickAction.addActionItem(searchItem);
		mQuickAction
				.setOnActionItemClickListener(new OnActionItemClickListener() {

					@Override
					public void onItemClick(QuickAction source, int pos,
							int actionId) {
						switch (pos) {
						case 0:
							startActivityForResult(new Intent(ContactsScreen.this,
									RegisterPlaceScreen.class), C.REQUESTCODE_CONTACT);
							break;
						case 1:
							type = "place";
							action = ACTION_FIND;
							refill();
							break;
						case 2:
							type = "search";
							openSearchDialog(view);
						}
					}
				});
		mQuickAction.show(view);
	}

	public void onAdd(final View view) {
		String type = getIntent().getExtras().getString(C.type);
		if ("members".equals(type)) {
			onCreateMember(view);
		} else if ("place".equals(type)) {
			onAddPlace(view);
		} else {
			onAddPerson(view);
		}
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

}