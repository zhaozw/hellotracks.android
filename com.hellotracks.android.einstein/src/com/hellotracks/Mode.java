package com.hellotracks;

public enum Mode {
    transport, sport, fuzzy, automatic;

    public static boolean isTransport(String mode) {
        return "transport".equals(mode) || "intelligent".equals(mode);
    }

    public static boolean isOutdoor(String mode) {
        return "sport".equals(mode) || "outdoor".equals(mode);
    }

    public static boolean isFuzzy(String mode) {
        return "fuzzy".equals(mode);
    }

    public static boolean isAutomatic(String mode) {
        return "automatic".equals(mode);
    }

    public static Mode fromString(String mode) {
        if (isAutomatic(mode)) {
            return automatic;
        } else if (isTransport(mode)) {
            return transport;
        } else if (isFuzzy(mode)) {
            return fuzzy;
        }
        return sport;
    }

    public Class getTrackingServiceClass() {
        if (automatic == this) {
            return BestTrackingService.class;
        } else if (fuzzy == this) {
            return NewTrackingService.class;
        } else {
            return OldTrackingService.class;
        }
    }

    public Class[] getOtherServiceClasses() {
        if (automatic == this) {
            return new Class[] { NewTrackingService.class, OldTrackingService.class };
        } else if (fuzzy == this) {
            return new Class[] { BestTrackingService.class, OldTrackingService.class };
        } else {
            return new Class[] { BestTrackingService.class, NewTrackingService.class };
        }
    }

}