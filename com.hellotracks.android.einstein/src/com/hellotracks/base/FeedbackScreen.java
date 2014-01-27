package com.hellotracks.base;

import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.hellotracks.Logger;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.util.ResultWorker;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;

public class FeedbackScreen extends AbstractScreen {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_feedback);
        setupActionBar(R.string.Cancel);
    }

    public void onOK(View view) {
        try {
            String fullname = ((TextView) findViewById(R.id.fullName)).getText().toString();
            if (fullname.trim().length() == 0) {
                ActionItem resetItem = new ActionItem(this, R.string.PleaseEnterNameFirst);
                QuickAction quick = new QuickAction(this);
                quick.addActionItem(resetItem);
                quick.show(findViewById(R.id.fullName));
                return;
            }

            String message = ((TextView) findViewById(R.id.message)).getText().toString();
            if (message.trim().length() == 0) {
                ActionItem resetItem = new ActionItem(this, R.string.PleaseEnterMessageFirst);
                QuickAction quick = new QuickAction(this);
                quick.addActionItem(resetItem);
                quick.show(findViewById(R.id.message));
                return;
            }

            String name = ((TextView) findViewById(R.id.fullName)).getText().toString();
            String subject = ((TextView) findViewById(R.id.subject)).getText().toString();
            String company = ((TextView) findViewById(R.id.companyName)).getText().toString();

            final boolean feedback = ((RadioGroup) findViewById(R.id.radiogroup)).getCheckedRadioButtonId() == R.id.radioFeedback;

            SharedPreferences prefs = Prefs.get(this);

            StringBuilder sb = new StringBuilder();
            sb.append("*** ");
            sb.append(feedback ? "Feedback" : "Question");
            sb.append(" ***");
            sb.append("\n\n");
            sb.append("\nName: " + name + "(" + prefs.getString(Prefs.NAME, "") + ")");
            sb.append("\nUsername: " + prefs.getString(Prefs.USERNAME, ""));
            sb.append("\nAccount: " + prefs.getString(Prefs.ACCOUNT, ""));
            sb.append("\nMode: " + prefs.getString(Prefs.MODE, ""));
            sb.append("\nStatus On/Off: " + prefs.getBoolean(Prefs.STATUS_ONOFF, false));
            if (prefs.getString(Prefs.PLAN_ORDER, null) != null) {
                sb.append("\nOrder Id: " + prefs.getString(Prefs.PLAN_ORDER, ""));
                sb.append("\nItem Type: " + prefs.getString(Prefs.PLAN_PRODUCT, ""));
                sb.append("\nState: " + prefs.getString(Prefs.PLAN_STATUS, ""));
            }
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

            JSONObject obj = prepareObj();
            obj.put("msg", sb.toString());
            doAction(ACTION_FEEDBACK, obj, new ResultWorker() {
                @Override
                public void onResult(String result, Context context) {
                    if (feedback) {
                        Toast.makeText(FeedbackScreen.this, R.string.ThankYourForYourFeedback, Toast.LENGTH_LONG)
                                .show();
                    } else {
                        Toast.makeText(FeedbackScreen.this, R.string.ThankYourForYourQuestion, Toast.LENGTH_LONG)
                                .show();
                    }
                    finish();
                }
            });
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

}
