package com.hellotracks.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.hellotracks.Log;
import com.hellotracks.types.GPS;

public class Buffer {

	private static Buffer instance;

	private static final int MAX_SIZE = 3000;

	private List<GPS> entries = Collections
			.synchronizedList(new LinkedList<GPS>());

	private long lastTime = 0;

	private Buffer() {
		instance = this;
	}

	public static Buffer getDefault() {
		if (instance == null)
			instance = new Buffer();
		return instance;
	}

	public List<GPS> getEntries() {
		return entries;
	}

	public synchronized void push(GPS gps) {
		Log.i("pushing");
		if (gps != null) {
			if (Math.abs(gps.ts - lastTime) < 6000) {
				Log.i("nope");
				return;
			} else {
				Log.i("yes");
				lastTime = gps.ts;
			}

			while (entries.size() > MAX_SIZE) {
				int rndIndex = (int) (Math.random() * MAX_SIZE);
				entries.remove(rndIndex);
			}

			entries.add(gps);

			Log.d("pushing new location (buffersize=" + entries.size() + ")");
		}
	}

	public GPS[] get(int max) {
		LinkedList<GPS> list = new LinkedList<GPS>();
		synchronized (entries) {
			int c = 0;
			for (GPS entry : entries) {
				if (c > max)
					break;
				else
					list.add(entry);
			}
		}
		return list.toArray(new GPS[list.size()]);
	}

	public void remove(GPS gps) {
		entries.remove(gps);
	}

	public int size() {
		return entries.size();
	}
}
