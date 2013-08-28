package com.hellotracks.base;

import android.content.Intent;
import android.net.MailTo;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.hellotracks.R;

public class WebScreen extends AbstractScreen {

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

	private WebView webview;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_web);
		
		String url = getIntent().getExtras().getString("url");


		webview = (WebView) findViewById(R.id.webView);
		webview.getSettings().setBuiltInZoomControls(true);
		webview.getSettings().setJavaScriptEnabled(true);
		webview.setWebViewClient(new PageWebViewClient());
		webview.getSettings().setLoadWithOverviewMode(true);
		webview.loadUrl(url);
		
		setupActionBar(R.string.Back);
	}

	public void onBack(View view) {
		finish();
	}
}
