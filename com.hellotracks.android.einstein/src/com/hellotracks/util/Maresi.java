package com.hellotracks.util;

public class Maresi {

	public static String createMaresi(String username, String password) {
		StringBuilder sb = new StringBuilder();
		int hashCode = Math.abs((username + password).hashCode());
		String hash = String.valueOf(hashCode > 100000000 ? hashCode
				: Integer.MAX_VALUE - hashCode);

		int first = Integer.parseInt(hash.substring(0, 2));
		int second = Integer.parseInt(hash.substring(2, 4));
		int third = Integer.parseInt(hash.substring(4, 6));
		int forth = Integer.parseInt(hash.substring(6, 8));

		sb.append(consonant(first).toUpperCase());
		sb.append(vowel(first));
		sb.append(consonant(second));
		sb.append(vowel(second));
		sb.append(consonant(third));
		sb.append(vowel(third));
		sb.append(consonant(forth));
		sb.append(vowel(forth));
		return sb.toString();
	}

	private static String vowel(int v) {
		if (v < 20)
			return "a";
		else if (v < 40)
			return "e";
		else if (v < 60)
			return "i";
		else if (v < 80)
			return "o";
		else
			return "u";
	}

	private static String consonant(int a) {
		switch (a % 18) {
		case 0:
			return "b";
		case 1:
			return "c";
		case 2:
			return "d";
		case 3:
			return "f";
		case 4:
			return "g";
		case 5:
			return "h";
		case 6:
			return "j";
		case 7:
			return "k";
		case 8:
			return "l";
		case 9:
			return "m";
		case 10:
			return "n";
		case 11:
			return "p";
		case 12:
			return "r";
		case 13:
			return "s";
		case 14:
			return "t";
		case 15:
			return "v";
		case 16:
			return "w";
		default:
			return "z";
		}
	}
}
