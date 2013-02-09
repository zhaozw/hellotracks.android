package com.hellotracks.types;

import android.os.BatteryManager;

public class Battery {

	public static int level = -1;
	public static int status = -1;

	public static boolean isLevelHigherThan20Percent() {
		return level > 20;
	}

	public static boolean isCharging() {
		return status == BatteryManager.BATTERY_STATUS_CHARGING
				|| status == BatteryManager.BATTERY_STATUS_FULL;
	}
}
