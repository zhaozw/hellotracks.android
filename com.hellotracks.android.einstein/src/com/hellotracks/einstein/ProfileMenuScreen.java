package com.hellotracks.einstein;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.hellotracks.R;
import com.hellotracks.activities.AbstractScreen;
import com.hellotracks.activities.WebScreen;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;

public class ProfileMenuScreen extends AbstractScreen {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_profilemenu);

		TextView nameView = (TextView) findViewById(R.id.name);
		Button businessButton = (Button) findViewById(R.id.business);
		Typeface tf = Typeface.createFromAsset(getAssets(), C.FortuneCity);
		nameView.setTypeface(tf);
		businessButton.setTypeface(tf);
		businessButton.setText("@hellotracks Business");
	}

	public void onRate(View view) {
		openMarketDialog(getResources().getString(R.string.RateNow));
	}

	private void onFindNearby(final String type) {
		Intent intent = new Intent(ProfileMenuScreen.this, ContactsScreen.class);
		intent.putExtra(C.type, type);
		intent.putExtra(C.action, ACTION_FIND);
		startActivityForResult(intent, C.REQUESTCODE_CONTACT);
	}

	public void onAbout(View view) {
		Intent intent = new Intent(ProfileMenuScreen.this, AboutScreen.class);
		startActivity(intent);
	}

	public void onBusiness(View view) {
		Intent intent = new Intent(ProfileMenuScreen.this, BusinessScreen.class);
		startActivityForResult(intent, C.REQUESTCODE_CREATE_COMPANY);
	}

	public void onFind(final View view) {
		ActionItem finPeopleItem = new ActionItem(this,
				R.string.FindPeopleNearby);
		ActionItem findPlacesItem = new ActionItem(this,
				R.string.FindPlacesNearby);
		ActionItem searchItem = new ActionItem(this,
				R.string.SearchForPeopleOrPlaces);
		QuickAction mQuickAction = new QuickAction(this);
		mQuickAction.addActionItem(finPeopleItem);
		mQuickAction.addActionItem(findPlacesItem);
		mQuickAction.addActionItem(searchItem);
		mQuickAction
				.setOnActionItemClickListener(new OnActionItemClickListener() {

					@Override
					public void onItemClick(QuickAction source, int pos,
							int actionId) {
						switch (pos) {
						case 0:
							onFindNearby("person");
							break;
						case 1:
							onFindNearby("place");
							break;
						case 2:
							onSearch(view);
						}
					}
				});
		mQuickAction.show(view);
	}

	private void onSearch(View view) {
		Intent intent = new Intent(ProfileMenuScreen.this, ContactsScreen.class);
		intent.putExtra(C.action, C.search);
		startActivityForResult(intent, C.REQUESTCODE_CONTACT);
	}

	private void onFAQ(View view) {
		if (!isOnline(true))
			return;

		Intent intent = new Intent(this, WebScreen.class);
		intent.putExtra("url", "http://www.hellotracks.com/faq");
		startActivity(intent);
	}

	public void onHelp(final View view) {
		if (!isOnline(true))
			return;

		ActionItem infoItem = new ActionItem(this, R.string.Information);
		ActionItem faqItem = new ActionItem(this, R.string.FAQ);
		ActionItem questionFeedbackItem = new ActionItem(this,
				R.string.QuestionOrFeedback);
		QuickAction quick = new QuickAction(this);
		quick.addActionItem(infoItem);
		quick.addActionItem(faqItem);
		quick.addActionItem(questionFeedbackItem);
		quick.setOnActionItemClickListener(new OnActionItemClickListener() {

			@Override
			public void onItemClick(QuickAction source, int pos, int actionId) {
				switch (pos) {
				case 0:
					startActivity(new Intent(ProfileMenuScreen.this,
							HelpScreen.class));
					break;
				case 1:
					onFAQ(view);
					break;
				case 2:
					startActivity(new Intent(ProfileMenuScreen.this,
							ContactScreen.class));
					break;
				}
			}
		});
		quick.show(view);
	}

	public void onBlog(View view) {
		Intent intent = new Intent(ProfileMenuScreen.this, WebScreen.class);
		intent.putExtra("url", "http://www.hellotracks.com/blog");
		startActivity(intent);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode < 0) {
			setResult(resultCode);
			finish();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public void onSettings(View view) {
		if (isOnline(false)) {
			startActivityForResult(
					new Intent(this, ProfileSettingsScreen.class),
					C.REQUESTCODE_CONTACT);
		} else {
			openDialog();
		}
	}

	public void onActivities(View view) {
		startActivity(new Intent(this, ActivitiesScreen.class));
		finish();
	}

	public void onMessages(View view) {
		startActivity(new Intent(this, ConversationsScreen.class));
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
}
