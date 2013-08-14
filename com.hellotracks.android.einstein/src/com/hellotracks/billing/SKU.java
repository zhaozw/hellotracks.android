package com.hellotracks.billing;

import java.util.LinkedList;
import java.util.List;

public enum SKU {
    single_monthly, team10_monthly, crew20_monthly, fleet50_monthly;

    public static String[] names() {
        SKU[] skus = SKU.values();
        String[] names = new String[skus.length];
        for (int i = 0; i < skus.length; i++) {
            names[i] = skus[i].name();
        }
        return names;
    }
    
    public static List<String> asList() {
        SKU[] skus = SKU.values();
        List<String> names = new LinkedList<String>();
        for (int i = 0; i < skus.length; i++) {
            names.add(skus[i].name());
        }
        return names;
    }
    

}
