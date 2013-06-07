package com.hellotracks;
public enum Mode {
	transport, sport, fuzzy;

	public static boolean isTransport(String mode) {
		return "transport".equals(mode) || "intelligent".equals(mode);
	}

	public static boolean isOutdoor(String mode) {
		return "sport".equals(mode) || "outdoor".equals(mode);
	}

	public static boolean isFuzzy(String mode) {
		return "fuzzy".equals(mode);
	}

	public static Mode fromString(String mode) {
		if (isTransport(mode)) {
			return transport;
		} else if (isFuzzy(mode)) {
			return fuzzy;
		}
		return sport;
	}

}