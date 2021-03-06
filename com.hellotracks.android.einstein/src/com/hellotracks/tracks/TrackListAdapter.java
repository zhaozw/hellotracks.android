package com.hellotracks.tracks;

import java.text.SimpleDateFormat;

import org.json.JSONArray;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.emilsjolander.components.stickylistheaders.StickyListHeadersAdapter;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.tracks.TrackListScreen.Flag;
import com.hellotracks.util.lazylist.LazyAdapter;

public class TrackListAdapter extends LazyAdapter implements StickyListHeadersAdapter {
    private TrackListScreen mScreen;

    private SimpleDateFormat sdf = new SimpleDateFormat("MMMMMMMMMMMM dd, yyyy");

    public TrackListAdapter(TrackListScreen screen, JSONArray array) {
        super(screen, array);
        this.mScreen = screen;
    }

    @Override
    protected int getListItemLayoutFor(int index) {
        return R.layout.list_item_track;
    }

    @Override
    public View getView(final int index, View convertView, ViewGroup parent) {
        View view = super.getView(index, convertView, parent);

        View layoutMain = view.findViewById(R.id.layoutMain);
        View layoutLabels = view.findViewById(R.id.layoutLabels);
        View layoutPause = view.findViewById(R.id.layoutPause);

        final long id = getId(index);
        boolean isPause = id == -1;

        layoutLabels.setVisibility(isPause ? View.GONE : View.VISIBLE);
        layoutMain.setVisibility(isPause ? View.GONE : View.VISIBLE);
        layoutPause.setVisibility(isPause ? View.VISIBLE : View.GONE);

        if (isPause) {
            TextView button = (TextView) view.findViewById(R.id.time);
            button.setText(getString(index, "text"));
            return view;
        }
        {
            String infoTxt = getString(index, "info");
            int idx = infoTxt.indexOf("\n");
            String timeTxt = infoTxt.substring(0, idx);
            TextView textBottom = (TextView) view.findViewById(R.id.textBottom);
            textBottom.setText(timeTxt);

            String metaTxt = infoTxt.substring(idx + 2);
            TextView info = (TextView) view.findViewById(R.id.info);
            info.setText(metaTxt);
        }
        {
            String msg = getString(index, AbstractScreen.MESSAGE);
            TextView textTop = (TextView) view.findViewById(R.id.textTop);
            int idx = msg.indexOf("B: ");
            if (idx > 0) {
                String a = msg.substring(0, idx);
                String b = msg.substring(idx);
                int i1 = findSecondComma(a);
                int i2 = findSecondComma(b);
                if (i1 > 0) {
                    a = a.substring(0, i1);
                }
                if (i2 > 0) {
                    b = b.substring(0, i2);
                }
                msg = a + "\n" + b;
            }
            textTop.setText(msg);
        }

        view.findViewById(R.id.buttonTouch).setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                mScreen.openTrackInfo(index, id);
                return true;
            }
        });

        view.findViewById(R.id.buttonTouch).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mScreen.gaSendButtonPressed("show_track");
                String url = getString(index, AbstractScreen.URL);
                String comments = getString(index, "comments");
                int labels = getInt(index, "labels");
                int actions = getInt(index, "actions");
                String text = getString(index, "info");
                mScreen.showTrack(v, text, id, url, comments, labels, actions);
            }
        });

        JSONArray comments = getArray(index, "comments");
        if (comments != null && comments.length() > 0) {
            TextView bubble = (TextView) view.findViewById(R.id.bubble);
            bubble.setVisibility(View.VISIBLE);
            bubble.setText(" " + comments.length() + " ");
            bubble.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    mScreen.openTrackInfo(index, id);
                }
            });
        } else {
            view.findViewById(R.id.bubble).setVisibility(View.GONE);
        }

        int labels = getInt(index, "labels");
        view.findViewById(R.id.label_green).setVisibility((labels & Flag.GREEN) > 0 ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.label_yellow).setVisibility((labels & Flag.YELLOW) > 0 ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.label_orange).setVisibility((labels & Flag.ORANGE) > 0 ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.label_red).setVisibility((labels & Flag.RED) > 0 ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.label_violett).setVisibility((labels & Flag.VIOLETT) > 0 ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.label_blue).setVisibility((labels & Flag.BLUE) > 0 ? View.VISIBLE : View.GONE);

        ImageButton edit = (ImageButton) view.findViewById(R.id.buttonEdit);
        edit.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mScreen.openTrackInfo(index, id);
            }
        });
        return view;

    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        HeaderViewHolder holder;
        if (convertView == null) {
            holder = new HeaderViewHolder();
            convertView = inflater.inflate(R.layout.list_item_tracks_header, parent, false);
            holder.text = (TextView) convertView.findViewById(R.id.text);
            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }
        long ts = getLong(position, "ts");
        if (ts < 0) {
            ts = getLong(position - 1, "ts");
        }
        String title = sdf.format(ts);
        holder.text.setText(title);
        return convertView;
    }

    @Override
    public long getHeaderId(int position) {
        long ts = getLong(position, "ts");
        if (ts <= 0) {
            ts = getLong(position - 1, "ts");
        }
        String s = sdf.format(ts);
        return s.hashCode();
    }

    class HeaderViewHolder {
        TextView text;
    }

    class ViewHolder {
        TextView text;
    }
}
