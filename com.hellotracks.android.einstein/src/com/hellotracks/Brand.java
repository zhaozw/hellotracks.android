package com.hellotracks;

public enum Brand {
	trackbook(Brand.Port.TB), hellotracks(Brand.Port.HT), flotalink(
			Brand.Port.FL);

	private static class Port {
		static final int TB = 8765;
		static final int HT = 8766;
		static final int FL = 8767;
	}

	private int port;
	private String applicationName;
	private String host;

	Brand(int port) {
		this.port = port;
		switch (port) {
		case Brand.Port.TB:
			setTrackbook();
			break;
		case Brand.Port.HT:
			setHellotracks();
			break;
		case Brand.Port.FL:
			setFlotaLink();
			break;
		}
	}

	private void setTrackbook() {
		applicationName = "trackbook";
		host = "http://trackbook.net";
	}

	private void setFlotaLink() {
		applicationName = "Flotalink";
		host = "http://flotalink.com";
	}

	private void setHellotracks() {
		applicationName = "hellotracks";
		host = "http://hellotracks.com";
	}

	public int getPort() {
		return port;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public String getHost() {
		return host;
	}

	public static Brand getBrandForPort(int port) {
		switch (port) {
		case Brand.Port.HT:
			return hellotracks;
		case Brand.Port.FL:
			return flotalink;
		case Brand.Port.TB:
			return trackbook;
		}

		return getDefaultBrand();
	}

	public static Brand getDefaultBrand() {
		return hellotracks;
	}

	public static int getLogoImage(String pkg) {
		return R.drawable.hellotracks;
	}

}
