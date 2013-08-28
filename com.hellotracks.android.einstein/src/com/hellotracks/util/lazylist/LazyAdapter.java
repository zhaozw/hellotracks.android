package com.hellotracks.util.lazylist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hellotracks.Log;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.util.ImageCache;
import com.hellotracks.util.ImageCache.ImageCallback;
import com.hellotracks.util.Time;
import com.hellotracks.util.Ui;
import com.squareup.picasso.Picasso;

public abstract class LazyAdapter extends BaseAdapter {

    protected Activity activity;
    protected LayoutInflater inflater = null;
    public ImageLoader imageLoader;
    protected ArrayList<JSONObject> data = new ArrayList<JSONObject>();
    private boolean hideBigImage = false;
    public boolean everyListItemOwnLayout = false;

    public LazyAdapter(Activity a, JSONArray array) {
        this(a, array, false);
    }

    public LazyAdapter(Activity a, JSONArray array, boolean hideBigImage) {
        this.hideBigImage = hideBigImage;

        activity = a;

        addData(array);
        inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        imageLoader = new ImageLoader(activity.getApplicationContext());
        setup();
    }

    protected void setup() {
        // may be overwritten and filled
    }

    public void addData(JSONArray array) {
        for (int i = 0; i < array.length(); i++) {
            try {
                data.add(array.getJSONObject(i));
            } catch (JSONException e) {
            }
        }
    }

    public String getString(int position, String key) {
        try {
            return data.get(position).getString(key);
        } catch (Exception exc) {
            return null;
        }
    }

    public JSONArray getArray(int position, String key) {
        try {
            JSONObject obj = data.get(position);
            return obj.getJSONArray(key);
        } catch (Exception exc) {
            return null;
        }
    }

    public Collection<Long> getAllIds() {
        Collection<Long> set = new HashSet<Long>();
        for (int i = 0; i < data.size(); i++) {
            set.add(getId(i));
        }
        return set;
    }

    public void remove(int position) {
        data.remove(position);
        notifyDataSetChanged();
    }

    public int getInt(int position, String key) {
        try {
            return data.get(position).getInt(key);
        } catch (Exception exc) {
            return 0;
        }
    }

    public long getLong(int position, String key) {
        try {
            return data.get(position).getLong(key);
        } catch (Exception exc) {
            return Long.MIN_VALUE;
        }
    }

    public long getId(int position) {
        try {
            return data.get(position).getLong(AbstractScreen.ID);
        } catch (Exception exc) {
            return Integer.MIN_VALUE;
        }
    }

    public String getAccount(int position) {
        try {
            return data.get(position).getString(AbstractScreen.ACCOUNT);
        } catch (Exception exc) {
            return null;
        }
    }

    public int getCount() {
        return data.size();
    }

    public Object getItem(int position) {
        try {
            return data.get(position);
        } catch (Exception exc) {
            Log.w(exc);
            return null;
        }
    }

    public long getItemId(int position) {
        return getId(position);
    }

    public View getView(int index, View convertView, ViewGroup parent) {
        final View vi = convertView == null || everyListItemOwnLayout ? inflater.inflate(getListItemLayoutFor(index),
                null) : convertView;

        TextView title = (TextView) vi.findViewById(R.id.title);
        TextView info = (TextView) vi.findViewById(R.id.info);

        final ImageView image = (ImageView) vi.findViewById(R.id.image);
        final ImageView icon = (ImageView) vi.findViewById(R.id.icon);

        try {
            JSONObject node = data.get(index);
            if (!node.has("title"))
                return vi;

            if (title != null && node.has("ts")) {
                long ts = node.getLong("ts");
                String passed = Time.formatTimePassed(activity, ts);
                title.setText(passed);
            } else if (title != null) {
                title.setText(node.getString(AbstractScreen.TITLE));
            }

            info.setText(node.getString(AbstractScreen.INFO));

            if (node.has(AbstractScreen.MESSAGE)) {
                TextView message = (TextView) vi.findViewById(R.id.message);
                if (message != null) {
                    message.setVisibility(View.VISIBLE);
                    String msg = node.getString(AbstractScreen.MESSAGE);
                    int idx = msg.indexOf("B: ");
                    if (idx > 0) {
                        msg = msg.substring(0, idx) + "\n" + msg.substring(idx);
                    }
                    message.setText(msg);
                }
            }

            String url = node.has(AbstractScreen.URL) ? node.getString(AbstractScreen.URL) : null;
            if (!hideBigImage && url != null) {
                int idx1 = url.indexOf("size=");
                int idx2 = url.indexOf("&", idx1);
                if (idx1 > 0 && idx2 > 0) {
                    int h = (int) Ui.convertDpToPixel(100, activity);
                    int w = 4*h;
                    url = url.substring(0, idx1) + "size=" + w + "x" + h + url.substring(idx2); 
                    Log.i(url);
                }

                image.setVisibility(View.VISIBLE);
 
                Picasso.with(activity).load(url).into(image);
            } else {
                image.setVisibility(View.GONE);
            }
            if (icon != null) {
                String url2 = node.has(AbstractScreen.URL2) ? node.getString(AbstractScreen.URL2) : null;
                if (url2 != null) {
                    icon.setVisibility(View.VISIBLE);
                    Picasso.with(activity).load(url2).into(icon);
                } else {
                    icon.setVisibility(View.GONE);
                }
            }
        } catch (Exception exc) {
            Log.w(exc);
        }

        return vi;
    }

    protected abstract int getListItemLayoutFor(int index);
}