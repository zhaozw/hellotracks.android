package com.hellotracks.base;

import com.hellotracks.OldTrackingService;

public class C {

    public static final String BROADCAST_PARKING = "anagog.pd.service.intent.PARKING_UPDATE";
	public static final String BROADCAST_ADDTRACKTOMAP = "ht.addTrackToMap";
	public static final String BROADCAST_SHOWMAP = "ht.showMap";
	public static final String BROADCAST_ACTIVITYRECOGNIZED = "ht.activityRecognized";
	
	public static int REQUESTCODE_CONTACT() {
	    return 105;
	}
	public static final int REQUESTCODE_TRACKS = 109;
	public static final int REQUESTCODE_EDIT = 105;
	public static final int REQUESTCODE_PICK_CONTACT = 106;
	public static final int REQUESTCODE_PICK_PLACE_FB = 112;
	public static final int REQUESTCODE_CREATE_COMPANY = 107;
	public static final int REQUESTCODE_MSG = 108;	
    public static final int REQUESTCODE_LOGIN = 109;
    public static final int REQUESTCODE_SIGNUP = 110;
    public static final int REQUESTCODE_INAPPBILLING = 1001;
    public static final int REQUESTCODE_GOOGLEPLACE = 200;

    public static final int REQCODE_GOOGLEPLAYSERVICES = 1122;
	public static final int RESULTCODE_SHOWTRACK = 101;
	public static final int RESULTCODE_CLOSEAPP = -1000;
	public static final int RESULTCODE_LOGIN_SUCCESS = 672038743;
	
	public static final String FortuneCity = "FortuneCity.ttf";
	public static final String LaBelle = "LaBelleAurore.ttf";
	
	public static final String URL_TERMS = "http://hellotracks.com/eula_hellotracks_en.txt";
	
	public static final String profilestring = "profilestring";
	public static final String account = "account";
	public static final String data = "data";
	public static final String email = "email";
	public static final String name = "name";
	public static final String myprofile = "myprofile";
	public static final String type = "type";
	public static final String action = "action";
	public static final String count = "cnt";
	public static final String permissions = "permissions";
	public static final String notify_email = "notify_email";
	public static final String owner = "owner";
	public static final String url = "url";
	public static final String usr = "usr";
	public static final String id = "id";
	public static final String search = "search";
	public static final String errortext = "errortext";
	
	public static final String person = "person";
	public static final String place = "place";
	
	public static final String OPEN_SCREEN = "open_screen";
	
	
	public static final String GCM_CMD_STARTOUTDOOR = "@!startoutdoor";
	public static final String GCM_CMD_STARTTRANSPORT = "@!starttransport";
	public static final String GCM_CMD_STARTTRACKINGSERVICE = "@!starttrackingservice";
	
	public static final String GCM_CMD_STOPOUTDOOR = "@!stopoutdoor";
    public static final String GCM_CMD_STOPTRANSPORT = "@!stoptransport";
    public static final String GCM_CMD_STOPTRACKINGSERVICE = "@!stoptrackingservice";
    
    public static final String GCM_CMD_PLAYSTORE = "@!playstore";
    public static final String GCM_CMD_URI = "@!uri";
	
	public static final Class trackingServiceClass = OldTrackingService.class;
    
}
