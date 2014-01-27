package com.hellotracks.messaging;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.hellotracks.Logger;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.util.lazylist.LazyAdapter;
import com.squareup.picasso.Picasso;

public class ConversationsAdapter extends LazyAdapter {

    private Runnable clickListener;

    public ConversationsAdapter(Activity activity, JSONArray array, Runnable clickListener) {
        super(activity, array);
        this.clickListener = clickListener;
    }

    private HashMap<Integer, Boolean> selectionMap = new HashMap<Integer, Boolean>();

    public Collection<String> getSelectedAccounts() {
        Collection<String> list = new HashSet<String>();
        for (int index : selectionMap.keySet().toArray(new Integer[0])) {
            if (selectionMap.get(index)) {
                try {
                    list.add(data.get(index).getString(AbstractScreen.ACCOUNT));
                } catch (Exception exc) {
                }
            }
        }
        return list;
    }

    public Collection<String> getSelectedNames() {
        Collection<String> list = new HashSet<String>();
        for (int index : selectionMap.keySet().toArray(new Integer[0])) {
            if (selectionMap.get(index)) {
                try {
                    list.add(data.get(index).getString("name"));
                } catch (Exception exc) {
                }
            }
        }
        return list;
    }

    public View getView(final int index, View convertView, ViewGroup parent) {

        try {
            JSONObject node = data.get(index);

            final View vi = convertView == null ? inflater.inflate(R.layout.list_item_conversations, null)
                    : convertView;

            final RadioButton check = (RadioButton) vi.findViewById(R.id.check);
            boolean checked = false;
            if (selectionMap.containsKey(index))
                checked = selectionMap.get(index);
            check.setChecked(checked);
            check.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    boolean checked = false;
                    if (selectionMap.containsKey(index))
                        checked = selectionMap.get(index);
                    selectionMap.put(index, !checked);
                    check.setChecked(!checked);
                    clickListener.run();
                }
            });

            TextView timeField = (TextView) vi.findViewById(R.id.timeText);
            long ts = node.getLong("ts");
            timeField.setText(node.getString("time"));

            TextView messageField = (TextView) vi.findViewById(R.id.messageText);
            String msg = node.getString("msg");

            int text = msg.indexOf("text:");
            if (text > 0)
                msg = msg.substring(text + 5);

            if (msg.length() > 30)
                msg = msg.substring(0, 27) + "...";
            messageField.setText(msg);
            if (data.get(index).has("unread")) {
                messageField.setTextColor(0xFF000000);
                vi.setBackgroundResource(R.color.lightSelection);
            } else {
                messageField.setTextColor(0xFF666666);
                vi.setBackgroundResource(R.color.transparent);
            }
            TextView nameField = (TextView) vi.findViewById(R.id.nameText);
            nameField.setText(node.getString("name"));

            final ImageView icon = (ImageView) vi.findViewById(R.id.icon);

            String url = node.getString("url");

            if (url != null) {
                Picasso.with(activity).load(url).into(icon);
            } else {
                icon.setVisibility(View.GONE);
            }
            return vi;
        } catch (Exception exc) {
            Logger.w(exc);
        }

        return new View(inflater.getContext());
    }

    @Override
    protected int getListItemLayoutFor(int index) {
        return R.layout.list_item_network;
    }

}