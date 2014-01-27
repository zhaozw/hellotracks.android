package com.hellotracks.deprecated;

import java.util.HashMap;

import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.hellotracks.Logger;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.C;

public class EditBillingAddressScreen extends AbstractScreen {

	private TextView address;
	private TextView postalCode;
	private TextView city;
	private TextView vatNumber;
	private TextView attention;
	private TextView legalName;
	private Button country;

	private String account;
	private String country_code = "";

	private static final String[] iso3166 = new String[] { "AFGHANISTAN", "AF",
			"ÅLAND ISLANDS", "AX", "ALBANIA", "AL", "ALGERIA", "DZ",
			"AMERICAN SAMOA", "AS", "ANDORRA", "AD", "ANGOLA", "AO",
			"ANGUILLA", "AI", "ANTARCTICA", "AQ", "ANTIGUA AND BARBUDA", "AG",
			"ARGENTINA", "AR", "ARMENIA", "AM", "ARUBA", "AW", "AUSTRALIA",
			"AU", "AUSTRIA", "AT", "AZERBAIJAN", "AZ", "BAHAMAS", "BS",
			"BAHRAIN", "BH", "BANGLADESH", "BD", "BARBADOS", "BB", "BELARUS",
			"BY", "BELGIUM", "BE", "BELIZE", "BZ", "BENIN", "BJ", "BERMUDA",
			"BM", "BHUTAN", "BT", "BOLIVIA", "BO", "BOSNIA AND HERZEGOVINA",
			"BA", "BOTSWANA", "BW", "BOUVET ISLAND", "BV", "BRAZIL", "BR",
			"BRITISH INDIAN OCEAN TERRITORY", "IO", "BRUNEI DARUSSALAM", "BN",
			"BULGARIA", "BG", "BURKINA FASO", "BF", "BURUNDI", "BI",
			"CAMBODIA", "KH", "CAMEROON", "CM", "CANADA", "CA", "CAPE VERDE",
			"CV", "CAYMAN ISLANDS", "KY", "CENTRAL AFRICAN REPUBLIC", "CF",
			"CHAD", "TD", "CHILE", "CL", "CHINA", "CN", "CHRISTMAS ISLAND",
			"CX", "COCOS (KEELING) ISLANDS", "CC", "COLOMBIA", "CO", "COMOROS",
			"KM", "CONGO", "CG", "CONGO, THE DEMOCRATIC REPUBLIC OF THE", "CD",
			"COOK ISLANDS", "CK", "COSTA RICA", "CR", "CÔTE D'IVOIRE", "CI",
			"CROATIA", "HR", "CUBA", "CU", "CYPRUS", "CY", "CZECH REPUBLIC",
			"CZ", "DENMARK", "DK", "DJIBOUTI", "DJ", "DOMINICA", "DM",
			"DOMINICAN REPUBLIC", "DO", "ECUADOR", "EC", "EGYPT", "EG",
			"EL SALVADOR", "SV", "EQUATORIAL GUINEA", "GQ", "ERITREA", "ER",
			"ESTONIA", "EE", "ETHIOPIA", "ET", "FALKLAND ISLANDS (MALVINAS)",
			"FK", "FAROE ISLANDS", "FO", "FIJI", "FJ", "FINLAND", "FI",
			"FRANCE", "FR", "FRENCH GUIANA", "GF", "FRENCH POLYNESIA", "PF",
			"FRENCH SOUTHERN TERRITORIES", "TF", "GABON", "GA", "GAMBIA", "GM",
			"GEORGIA", "GE", "GERMANY", "DE", "GHANA", "GH", "GIBRALTAR", "GI",
			"GREECE", "GR", "GREENLAND", "GL", "GRENADA", "GD", "GUADELOUPE",
			"GP", "GUAM", "GU", "GUATEMALA", "GT", "GUERNSEY", "GG", "GUINEA",
			"GN", "GUINEA-BISSAU", "GW", "GUYANA", "GY", "HAITI", "HT",
			"HEARD ISLAND AND MCDONALD ISLANDS", "HM",
			"HOLY SEE (VATICAN CITY STATE)", "VA", "HONDURAS", "HN",
			"HONG KONG", "HK", "HUNGARY", "HU", "ICELAND", "IS", "INDIA", "IN",
			"INDONESIA", "ID", "IRAN, ISLAMIC REPUBLIC OF", "IR", "IRAQ", "IQ",
			"IRELAND", "IE", "ISLE OF MAN", "IM", "ISRAEL", "IL", "ITALY",
			"IT", "JAMAICA", "JM", "JAPAN", "JP", "JERSEY", "JE", "JORDAN",
			"JO", "KAZAKHSTAN", "KZ", "KENYA", "KE", "KIRIBATI", "KI",
			"KOREA, DEMOCRATIC PEOPLE'S REPUBLIC OF", "KP",
			"KOREA, REPUBLIC OF", "KR", "KUWAIT", "KW", "KYRGYZSTAN", "KG",
			"LAO PEOPLE'S DEMOCRATIC REPUBLIC", "LA", "LATVIA", "LV",
			"LEBANON", "LB", "LESOTHO", "LS", "LIBERIA", "LR",
			"LIBYAN ARAB JAMAHIRIYA", "LY", "LIECHTENSTEIN", "LI", "LITHUANIA",
			"LT", "LUXEMBOURG", "LU", "MACAO", "MO",
			"MACEDONIA, THE FORMER YUGOSLAV REPUBLIC OF", "MK", "MADAGASCAR",
			"MG", "MALAWI", "MW", "MALAYSIA", "MY", "MALDIVES", "MV", "MALI",
			"ML", "MALTA", "MT", "MARSHALL ISLANDS", "MH", "MARTINIQUE", "MQ",
			"MAURITANIA", "MR", "MAURITIUS", "MU", "MAYOTTE", "YT", "MEXICO",
			"MX", "MICRONESIA, FEDERATED STATES OF", "FM",
			"MOLDOVA, REPUBLIC OF", "MD", "MONACO", "MC", "MONGOLIA", "MN",
			"MONTENEGRO", "ME", "MONTSERRAT", "MS", "MOROCCO", "MA",
			"MOZAMBIQUE", "MZ", "MYANMAR", "MM", "NAMIBIA", "NA", "NAURU",
			"NR", "NEPAL", "NP", "NETHERLANDS", "NL", "NETHERLANDS ANTILLES",
			"AN", "NEW CALEDONIA", "NC", "NEW ZEALAND", "NZ", "NICARAGUA",
			"NI", "NIGER", "NE", "NIGERIA", "NG", "NIUE", "NU",
			"NORFOLK ISLAND", "NF", "NORTHERN MARIANA ISLANDS", "MP", "NORWAY",
			"NO", "OMAN", "OM", "PAKISTAN", "PK", "PALAU", "PW",
			"PALESTINIAN TERRITORY, OCCUPIED", "PS", "PANAMA", "PA",
			"PAPUA NEW GUINEA", "PG", "PARAGUAY", "PY", "PERU", "PE",
			"PHILIPPINES", "PH", "PITCAIRN", "PN", "POLAND", "PL", "PORTUGAL",
			"PT", "PUERTO RICO", "PR", "QATAR", "QA", "REUNION", "RE",
			"ROMANIA", "RO", "RUSSIAN FEDERATION", "RU", "RWANDA", "RW",
			"SAINT BARTHÉLEMY", "BL", "SAINT HELENA", "SH",
			"SAINT KITTS AND NEVIS", "KN", "SAINT LUCIA", "LC", "SAINT MARTIN",
			"MF", "SAINT PIERRE AND MIQUELON", "PM",
			"SAINT VINCENT AND THE GRENADINES", "VC", "SAMOA", "WS",
			"SAN MARINO", "SM", "SAO TOME AND PRINCIPE", "ST", "SAUDI ARABIA",
			"SA", "SENEGAL", "SN", "SERBIA", "RS", "SEYCHELLES", "SC",
			"SIERRA LEONE", "SL", "SINGAPORE", "SG", "SLOVAKIA", "SK",
			"SLOVENIA", "SI", "SOLOMON ISLANDS", "SB", "SOMALIA", "SO",
			"SOUTH AFRICA", "ZA",
			"SOUTH GEORGIA AND THE SOUTH SANDWICH ISLANDS", "GS", "SPAIN",
			"ES", "SRI LANKA", "LK", "SUDAN", "SD", "SURINAME", "SR",
			"SVALBARD AND JAN MAYEN", "SJ", "SWAZILAND", "SZ", "SWEDEN", "SE",
			"SWITZERLAND", "CH", "SYRIAN ARAB REPUBLIC", "SY",
			"TAIWAN, PROVINCE OF CHINA", "TW", "TAJIKISTAN", "TJ",
			"TANZANIA, UNITED REPUBLIC OF", "TZ", "THAILAND", "TH",
			"TIMOR-LESTE", "TL", "TOGO", "TG", "TOKELAU", "TK", "TONGA", "TO",
			"TRINIDAD AND TOBAGO", "TT", "TUNISIA", "TN", "TURKEY", "TR",
			"TURKMENISTAN", "TM", "TURKS AND CAICOS ISLANDS", "TC", "TUVALU",
			"TV", "UGANDA", "UG", "UKRAINE", "UA", "UNITED ARAB EMIRATES",
			"AE", "UNITED KINGDOM", "GB", "UNITED STATES", "US",
			"UNITED STATES MINOR OUTLYING ISLANDS", "UM", "URUGUAY", "UY",
			"UZBEKISTAN", "UZ", "VANUATU", "VU", "VENEZUELA", "VE", "VIET NAM",
			"VN", "VIRGIN ISLANDS, BRITISH", "VG", "VIRGIN ISLANDS, U.S.",
			"VI", "WALLIS AND FUTUNA", "WF", "WESTERN SAHARA", "EH", "YEMEN",
			"YE", "ZAMBIA", "ZM", "ZIMBABWE", "ZW" };

	private HashMap<String, String> codeToName = new HashMap<String, String>();
	private HashMap<String, String> nameToCode = new HashMap<String, String>();
	private String[] names = new String[iso3166.length / 2];

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_billing_address);

		TextView nameView = (TextView) findViewById(R.id.name);
		Typeface tf = Typeface.createFromAsset(getAssets(), C.FortuneCity);
		nameView.setTypeface(tf);

		for (int i = 0; i < iso3166.length; i += 2) {
			codeToName.put(iso3166[i + 1], iso3166[i]);
			nameToCode.put(iso3166[i], iso3166[i + 1]);
			names[i / 2] = iso3166[i];
		}

		try {
			address = ((TextView) findViewById(R.id.address));
			postalCode = ((TextView) findViewById(R.id.postalCode));
			city = ((TextView) findViewById(R.id.city));
			country = ((Button) findViewById(R.id.countryButton));
			vatNumber = ((TextView) findViewById(R.id.vatNumber));
			legalName = ((TextView) findViewById(R.id.legalName));
			attention = ((TextView) findViewById(R.id.attention));

			JSONObject obj = new JSONObject(getIntent().getStringExtra(
					C.profilestring));
			account = obj.getString("account");

			address.setText(obj.getString("address"));
			postalCode.setText(obj.getString("postalcode"));
			city.setText(obj.getString("city"));

			country_code = obj.has("country_code") ? obj.getString("country_code")
					: "";
			String countryName = codeToName.get(country_code);
			if (countryName != null)
				country.setText(countryName);
			
			vatNumber.setText(obj.getString("vat_number"));

			try {
				attention.setText(obj.getString("attention"));
				legalName.setText(obj.getString("legal_name"));
			} catch (Exception exc) {
			}
			if (legalName.getText().length() == 0) {
				legalName.setText(obj.getString("name"));
			}
		} catch (Exception exc) {
			Logger.w(exc);
		}

	}

	public void onCountry(View view) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.Country);
		builder.setItems(names, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				country_code = nameToCode.get(names[item]);
				country.setText(names[item]);
			}
		});
		AlertDialog alert = builder.create();
		alert.setCanceledOnTouchOutside(true);
		alert.show();
	}

	public void onBack(View view) {
		try {
			JSONObject obj = prepareObj();
			obj.put("address", address.getText().toString());
			obj.put("postalcode", postalCode.getText().toString());
			obj.put("city", city.getText().toString());
			obj.put("country_code", country_code);
			obj.put("vat_number", vatNumber.getText().toString());
			obj.put("legal_name", legalName.getText().toString());
			obj.put("attention", attention.getText().toString());
			obj.put("account", account);
			doAction(ACTION_EDITPROFILE, obj, null);
		} catch (Exception exc) {
			Logger.w(exc);
		}
		setResult(0);
		finish();
	}

}
