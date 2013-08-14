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
import com.hellotracks.account.AccountSettingsScreen;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.ActivitiesScreen;
import com.hellotracks.base.C;
import com.hellotracks.base.ContactScreen;
import com.hellotracks.base.WebScreen;
import com.hellotracks.messaging.ConversationsScreen;
import com.hellotracks.network.NetworkScreen;
import com.hellotracks.panic.PanicInfoScreen;
import com.hellotracks.profile.ProfileSettingsScreen;
import com.hellotracks.tracks.TrackListScreen;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;

public class MainMenuScreen extends AbstractScreen {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_mainmenu);

		TextView nameView = (TextView) findViewById(R.id.name);
		Typeface tf = Typeface.createFromAsset(getAssets(), C.FortuneCity);
		nameView.setTypeface(tf);

		setupUI(false);
	}

	public void onRate(View view) {
		FlurryAgent.logEvent("MainMenu-Rate");
		openMarketDialog(getResources().getString(R.string.RateNow));
	}

	public void onAbout(View view) {
		FlurryAgent.logEvent("MainMenu-About");
		Intent intent = new Intent(MainMenuScreen.this, AboutScreen.class);
		startActivity(intent);
	}

	public void onFAQ(View view) {
		FlurryAgent.logEvent("MainMenu-FAQ");

		if (!isOnline(true))
			return;

		Intent intent = new Intent(this, WebScreen.class);
		intent.putExtra("url", "http://www.hellotracks.com/faq");
		startActivity(intent);
	}

	public void onFeedback(View view) {
		startActivity(new Intent(MainMenuScreen.this, ContactScreen.class));
	}

	public void onHelp(final View view) {
		FlurryAgent.logEvent("MainMenu-Help");

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
					startActivity(new Intent(MainMenuScreen.this,
							HelpScreen.class));
					break;
				case 1:
					onFAQ(view);
					break;
				case 2:
					startActivity(new Intent(MainMenuScreen.this,
							ContactScreen.class));
					break;
				}
			}
		});
		quick.show(view);
	}

	public void onBlog(View view) {
		Intent intent = new Intent(MainMenuScreen.this, WebScreen.class);
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

	public void onProfileSettings(View view) {
		if (isOnline(true)) {
			startActivityForResult(
					new Intent(this, ProfileSettingsScreen.class),
					C.REQUESTCODE_CONTACT);
		}
	}

	public void onActivities(View view) {
		FlurryAgent.logEvent("MainMenu-Activities");
		startActivity(new Intent(this, ActivitiesScreen.class));
		finish();
	}

	public void onMessages(View view) {
		FlurryAgent.logEvent("MainMenu-Messages");
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

	public void onAccountSettings(View view) {
		FlurryAgent.logEvent("MainMenu-Account");
		Intent intent = new Intent(this, AccountSettingsScreen.class);
		startActivity(intent);
		finish();
	}

	public void onTracks(View view) {
		FlurryAgent.logEvent("MainMenu-Tracks");
		Intent intent = new Intent(this, TrackListScreen.class);
		startActivity(intent);
	}

	public void onMore(View view) {
		setupUI(true);
	}
	
	private void setupUI(boolean more) {
	
	    int visMain = more ? View.GONE : View.VISIBLE;
	    int visMore = more ? View.VISIBLE : View.GONE;
	    
	    findViewById(R.id.cockpit).setVisibility(visMain);
        findViewById(R.id.messagesButton).setVisibility(visMain);
        findViewById(R.id.profileSettings).setVisibility(visMain);
        findViewById(R.id.tracks).setVisibility(visMain);
        findViewById(R.id.moreButton).setVisibility(visMain);
        
        findViewById(R.id.accountButton).setVisibility(visMore);
        findViewById(R.id.emergencyButton).setVisibility(visMore);
        findViewById(R.id.feedback).setVisibility(visMore);
        findViewById(R.id.faq).setVisibility(visMore);
        findViewById(R.id.rateButton).setVisibility(visMore);
        findViewById(R.id.activitiesButton).setVisibility(visMore);
        findViewById(R.id.networkButton).setVisibility(visMore);      
	}
}
