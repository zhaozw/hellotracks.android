package com.hellotracks.einstein;

import java.util.HashMap;

import org.json.JSONArray;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.hellotracks.R;
import com.hellotracks.model.ResultWorker;
import com.hellotracks.util.lazylist.LazyAdapter;

public abstract class MoreLazyAdapter extends LazyAdapter {

	private BasicAbstractScreen screen;

	public MoreLazyAdapter(BasicAbstractScreen a, JSONArray array) {
		super(a, array);
		this.screen = a;
		this.everyListItemOwnLayout = true;
	}

	protected long fromTS = System.currentTimeMillis() * 2;

	@Override
	public View getView(int index, View convertView, ViewGroup parent) {
		int count = getCount();
		if (index == count - 1) {
			View map = inflater.inflate(R.layout.list_item_more, null);
			Button button = (Button) map.findViewById(R.id.loadButton);
			button.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(final View v) {
					HashMap<String, Object> map = new HashMap<String, Object>();
					if (screen.getParams() != null)
						map.putAll(screen.getParams());
					map.put("fromts", fromTS - 1);
					map.put("count", 10);
					screen.refill(map, new ResultWorker() {
						@Override
						public void onResult(final String result,
								Context context) {
							screen.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									try {
										addData(new JSONArray(result));
										notifyDataSetChanged();
									} catch (Exception exc) {
									}
								}
							});
						}
					});

				}
			});
			return map;
		}

		try {
			fromTS = Math.min(data.get(index).getLong("ts") - 1, fromTS);
		} catch (Exception exc) {
		}
		return super.getView(index, convertView, parent);
	}

	@Override
	public int getCount() {
		return super.getCount() + 1;
	}
}
