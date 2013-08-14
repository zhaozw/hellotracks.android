package com.hellotracks.base;

import org.json.JSONObject;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.hellotracks.Log;
import com.hellotracks.R;
import com.hellotracks.model.ResultWorker;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;

public class ContactScreen extends AbstractScreen {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_contact);

		TextView nameView = (TextView) findViewById(R.id.name);
		Typeface tf = Typeface.createFromAsset(getAssets(), C.FortuneCity);
		nameView.setTypeface(tf);
	}

	public void onOK(View view) {
		String fullname = ((TextView) findViewById(R.id.fullName)).getText()
				.toString();
		if (fullname.trim().length() == 0) {
			ActionItem resetItem = new ActionItem(this,
					R.string.PleaseEnterNameFirst);
			QuickAction mQuickAction = new QuickAction(this);
			mQuickAction.addActionItem(resetItem);
			mQuickAction.show(findViewById(R.id.fullName));
			return;
		}

		String message = ((TextView) findViewById(R.id.message)).getText()
				.toString();
		if (message.trim().length() == 0) {
			ActionItem resetItem = new ActionItem(this,
					R.string.PleaseEnterMessageFirst);
			QuickAction mQuickAction = new QuickAction(this);
			mQuickAction.addActionItem(resetItem);
			mQuickAction.show(findViewById(R.id.message));
			return;
		}

		String name = ((TextView) findViewById(R.id.fullName)).getText()
				.toString();
  		String subject = ((TextView) findViewById(R.id.subject)).getText()
				.toString();
		String company = ((TextView) findViewById(R.id.companyName)).getText()
				.toString();

		final boolean feedback = ((RadioGroup) findViewById(R.id.radiogroup))
				.getCheckedRadioButtonId() == R.id.radioFeedback;

		StringBuilder sb = new StringBuilder();
		sb.append("*** ");
		sb.append(feedback ? "Feedback" : "Question");
		sb.append(" ***");
		sb.append("\n\n");
		sb.append("Name: " + name);
		sb.append("\n\n");
		if (company.length() > 0) {
			sb.append("Company: " + company);
			sb.append("\n\n");
		}
		if (subject.length() > 0) {
			sb.append("Subject: " + subject);
			sb.append("\n\n");
		}
		sb.append("\n\n");
		sb.append(message);
		try {
			JSONObject obj = prepareObj();
			obj.put("msg", sb.toString());
			doAction(ACTION_FEEDBACK, obj, new ResultWorker() {
				@Override
				public void onResult(String result, Context context) {
					if (feedback) {
						Toast.makeText(ContactScreen.this,
								R.string.ThankYourForYourFeedback,
								Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(ContactScreen.this,
								R.string.ThankYourForYourQuestion,
								Toast.LENGTH_LONG).show();
					}
					finish();
				}
			});
		} catch (Exception exc) {
			Log.w(exc);
		}
	}

}
