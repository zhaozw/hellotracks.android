package com.hellotracks.map;

import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.hellotracks.Mode;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.util.Ui;

public class ModeHolder {
    public View view;
    public RadioButton radio;
    public Mode mode;

    public ModeHolder(final Activity activity, RadioGroup group, final Mode m) {
        this.mode = m;
        this.view = Ui.inflateAndReturnInflatedView(activity.getLayoutInflater(), R.layout.include_mode, group);
        this.radio = (RadioButton) view.findViewById(R.id.radioButton);

        final TextView title = (TextView) this.view.findViewById(R.id.textTitle);
        final TextView right = (TextView) this.view.findViewById(R.id.textRight);
        final TextView desc = (TextView) this.view.findViewById(R.id.textDesc);
        if (mode == Mode.sport) {
            title.setText(R.string.ModeOutdoor);
            right.setText(R.string.Outdoor);
            desc.setText(R.string.OutdoorShortDesc);
        } else if (mode == Mode.transport) {
            title.setText(R.string.ModeTransport);
            right.setText(R.string.Transport);
            desc.setText(R.string.TransportShortDesc);
        } else {
            title.setText(R.string.ModeRough);
            right.setText(R.string.LiveLocation);
            desc.setText(R.string.FuzzyShortDesc);
        }
        this.view.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ModeHolder.this.radio.setChecked(true);
                Prefs.get(activity).edit().putString(Prefs.MODE, m.toString()).commit();
            }
        });
    }
    
    public void setShowDesc(boolean show) {
        TextView desc = (TextView) this.view.findViewById(R.id.textDesc);
        desc.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public RadioButton getRadio() {
        return radio;
    }

    public View getView() {
        return view;
    }
}