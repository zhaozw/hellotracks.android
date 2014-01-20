package com.hellotracks.deprecated;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.MailTo;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.network.RegisterCompanyScreen;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;

public class BusinessScreen extends AbstractScreen {

	private class PageWebViewClient extends WebViewClient {

		private Context context;

		public PageWebViewClient(Context context) {
			this.context = context;
		}

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
			return true;
		}
	}
	
	private WebView webview;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.deprecated_screen_business);
		
		Toast.makeText(this, R.string.JustASecond, Toast.LENGTH_LONG).show();

		TextView nameView = (TextView) findViewById(R.id.name);
		Typeface tf = Typeface.createFromAsset(getAssets(), C.FortuneCity);
		nameView.setTypeface(tf);
		nameView.setText("@Business");

		Button createCompanyButton = (Button) findViewById(R.id.createCompany);
		createCompanyButton.setTypeface(tf);
		createCompanyButton.setText(R.string.SignUpHellotracksBusiness);

		String url = "http://manual.hellotracks.com/en/business-account?headless=true";

		webview = (WebView) findViewById(R.id.webView);
		webview.getSettings().setJavaScriptEnabled(true);
		webview.setWebViewClient(new PageWebViewClient(this));
		webview.loadUrl(url);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == C.REQUESTCODE_CREATE_COMPANY) {
			finish();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public void onCreateCompany(View view) {
		Intent intent = new Intent(this, RegisterCompanyScreen.class);
		startActivityForResult(intent, C.REQUESTCODE_CREATE_COMPANY);
	}
	
	public void onMenu(View view) {
		QuickAction mQuickAction = new QuickAction(this);
		final String[][] otherUrls = new String[][] {
				{ getResources().getString(R.string.BusinessAccount), "http://www.hellotracks.com/business/business-account?headless=true"},
				{ getResources().getString(R.string.PricingAsItShouldBe), "http://www.hellotracks.com/business/pricing-as-it-should-be?headless=true"},
				{ getResources().getString(R.string.TheBusinessPackage), "http://www.hellotracks.com/business/business-only-features?headless=true"},
		};
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


}
