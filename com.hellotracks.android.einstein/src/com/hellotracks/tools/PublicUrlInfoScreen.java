package com.hellotracks.tools;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hellotracks.Logger;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.TrackingSender;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.util.Ui;

public class PublicUrlInfoScreen extends AbstractScreen {

    private StringBuilder sb = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_publicurl_info);

        TextView textURL = (TextView) findViewById(R.id.textURL);

        String user = Prefs.get(this).getString(Prefs.USERNAME, "");
        String pwd = TrackingSender.md5("0", Prefs.get(this).getString(Prefs.PASSWORD, ""));
        try {
            sb.append("http://hellotracks.com/live.html?usr=").append(URLEncoder.encode(user, "UTF-8")).append("&pwd=")
                    .append(URLEncoder.encode(pwd, "UTF-8")).append("&tok=0");
        } catch (UnsupportedEncodingException exc) {
            Logger.e(exc);
        }
        textURL.setText(sb.toString());
    }

    public void onOK(View view) {
        finish();
    }

    public void onOpen(View view) {
        Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse(sb.toString()));
        startActivity(open);
    }

    @SuppressLint({ "NewApi", "ServiceCast" })
    public void onCopy(View view) {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(sb.toString());
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("hellotracks URL", sb.toString());
            clipboard.setPrimaryClip(clip);
        }
        Ui.makeText(this, R.string.UrlCopiedToClipboard, Toast.LENGTH_LONG).show();
    }

}
