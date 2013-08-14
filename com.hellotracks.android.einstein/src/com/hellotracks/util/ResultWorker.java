package com.hellotracks.util;

import android.content.Context;
import android.widget.Toast;

import com.hellotracks.R;

public class ResultWorker {

	public static final int STATUS_OK = 0;
	public static final int STATUS_NORESULT = 1001;
	
	public static final int ERROR_FORMAT = -1;
	public static final int ERROR_USERUNKNOWN = -2;
	public static final int ERROR_PASSWORDMISMATCH = -3;
	public static final int ERROR_USERALREADYEXISTS = -4;
	public static final int ERROR_NOPERMISSION = -5;
	public static final int ERROR_UNKNOWN = -99;

	public void onResult(final String result, Context context) {
		Toast.makeText(context, "OK", Toast.LENGTH_SHORT).show();
	}

	public void onFailure(int failure, Context context) {
		int txt = 0;
		switch (failure) {
		case ERROR_FORMAT:
			return;
		case ERROR_USERUNKNOWN:
			txt = R.string.userAlreadyExists;
			break;
		case ERROR_PASSWORDMISMATCH:
			txt = R.string.passwordMismatch;
			break;
		case ERROR_USERALREADYEXISTS:
			txt = R.string.userAlreadyExists;
			break;
		case ERROR_UNKNOWN:
			return;
		case ERROR_NOPERMISSION:
			txt = R.string.NoPermission;
			break;
		}
		if (txt > 0) {
			Toast.makeText(context,
					context.getResources().getString(txt),
					Toast.LENGTH_LONG).show();
		}
	}

}
