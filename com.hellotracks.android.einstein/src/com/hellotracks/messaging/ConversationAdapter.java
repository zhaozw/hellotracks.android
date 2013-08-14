package com.hellotracks.messaging;

import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.hellotracks.Log;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.util.ImageCache;
import com.hellotracks.util.ImageCache.ImageCallback;
import com.hellotracks.util.lazylist.LazyAdapter;
import com.squareup.picasso.Picasso;

public class ConversationAdapter extends LazyAdapter {

	private String myUrl = null;
	private String myName = null;
	private String myAccount = null;
	private String otherUrl = null;
	private String otherName = null;
	private String otherAccount = null;

	public ConversationAdapter(Activity a, JSONObject dataObject)
			throws Exception {
		super(a, dataObject.getJSONArray("conversation"));

		JSONObject me = dataObject.getJSONObject("me");
		myUrl = me.getString("url");
		myName = me.getString("name");
		myAccount = me.getString("account");

		JSONObject other = dataObject.getJSONObject("other");
		otherUrl = other.getString("url");
		otherName = other.getString("name");
		otherAccount = other.getString("account");
	}

	public View getView(int index, View convertView, ViewGroup parent) {

		try {
			JSONObject node = data.get(index);

			boolean in = node.getBoolean("in");

			final View vi = inflater.inflate(in ? R.layout.list_item_msg
					: R.layout.list_item_msg2, null);

			TextView timeField = (TextView) vi.findViewById(R.id.title);
			TextView messageField = (TextView) vi.findViewById(R.id.info);

			final ImageView icon = (ImageView) vi.findViewById(R.id.icon);

			timeField.setText(node.getString("time"));

			String msg = node.getString("msg");
			int text = msg.indexOf("text:");
			if (text > 0)
				messageField.setText(msg.substring(text + 5));
			else
				messageField.setText(msg);

			if (msg.contains("http") || msg.contains("www.")
					|| msg.contains("geo:") || msg.contains("navigation:")) {
				vi.findViewById(R.id.pin).setVisibility(View.VISIBLE);
			} else {
				vi.findViewById(R.id.pin).setVisibility(View.GONE);
			}

			String url = in ? otherUrl : myUrl;
			if (url != null) {
			    Picasso.with(activity).load(url).into(icon);				
			} else {
				icon.setVisibility(View.GONE);
			}
			return vi;
		} catch (Exception exc) {
			Log.w(exc);
		}

		return new View(inflater.getContext());
	}

	public void removeAll() {
		data.clear();
	}

	@Override
	protected int getListItemLayoutFor(int index) {
		return 0;
	}

}