package com.hellotracks.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.MailTo;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.hellotracks.R;
import com.hellotracks.einstein.C;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;

public class WebScreen extends Activity {

	private class PageWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if (url.startsWith("mailto:")) {
				MailTo mt = MailTo.parse(url);
				Intent i = new Intent(Intent.ACTION_SEND);
				i.setType("text/plain");
				i.putExtra(Intent.EXTRA_EMAIL, new String[] { mt.getTo() });
				i.putExtra(Intent.EXTRA_SUBJECT, mt.getSubject());
				i.putExtra(Intent.EXTRA_CC, mt.getCc());
				i.putExtra(Intent.EXTRA_TEXT, mt.getBody());
				startActivity(i);
				view.reload();
				return false;
			} else {
				if (!url.endsWith("?headless=true"))
					url += "?headless=true";
			}
			view.loadUrl(url);
			return false;
		}
	}

	private String[][] otherUrls = null;

	private WebView webview;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_web);
		
		TextView nameView = (TextView) findViewById(R.id.name);
		nameView.setText("@hellotracks");
		Typeface tf = Typeface.createFromAsset(getAssets(), C.FortuneCity);
		nameView.setTypeface(tf);
		
		String url = getIntent().getExtras().getString("url");

		String[] names = getIntent().getExtras().getStringArray("urls");
		if (names != null) {
			otherUrls = new String[names.length][];
			for (int i = 0; i < names.length; i++) {
				otherUrls[i] = new String[] { names[i],
						getIntent().getExtras().getString(names[i]) };
			}
		} else {
			findViewById(R.id.button_menu).setVisibility(View.GONE);
		}

		webview = (WebView) findViewById(R.id.webView);
		webview.getSettings().setBuiltInZoomControls(true);
		webview.getSettings().setJavaScriptEnabled(true);
		webview.setWebViewClient(new PageWebViewClient());
		webview.getSettings().setLoadWithOverviewMode(true);
		webview.loadUrl(url);
	}

	public void onMenu(View view) {
		QuickAction mQuickAction = new QuickAction(this);
		for (int i = 0; i < otherUrls.length; i++) {
			ActionItem item = new ActionItem(this, otherUrls[i][0]);
			item.setActionId(i);
			mQuickAction.addActionItem(item);
		}
		mQuickAction
				.setOnActionItemClickListener(new OnActionItemClickListener() {

					@Override
					public void onItemClick(QuickAction source, int pos,
							int actionId) {
						webview.loadUrl(otherUrls[actionId][1]);
					}
				});
		mQuickAction.show(view);
	}

	public void onBack(View view) {
		finish();
	}
}
