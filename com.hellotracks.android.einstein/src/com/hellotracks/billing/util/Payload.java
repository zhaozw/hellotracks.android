package com.hellotracks.billing.util;

import android.content.Context;

import com.hellotracks.Prefs;

public class Payload {

    public static final String chunk1 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqS+pn+4v6rxogdPo97mkKQgSUduDXcyDYxvW/u/MlwILN1Ec97lsGpGji1OBLxI8nn8EDDuERJbNX0ahx";
    public static final String chunk2 = "VKY32jt1pwSZbHWOldK8pXfu5MPtq55pL8CSxM3YGMdBnDL7iU/Sc46ZVDuQpSRomR6gu1+mZPC9c6y7qD78qDY1tYnVVmay4I2KzNw4WSoc2RTSc8K8FIdQpbxC/";
    public static final String chunk3 = "mZNJrIz//CD8nRIs7jdG8MgLQG+ez0wvsXV8WRXIQ2ZPV81y7ce0LeAJhLXIGsjn560xsDyCu0B";
    public static final String chunk4 = "j/u7zIvfxz1MXPTVdodoB4LrHBygPfKf4qAi0jlXIh0FMfuUdRj5cQTtxBYRQIDAQAB";

    public static String createPayload(Context context) {
        String username = Prefs.get(context).getString(Prefs.USERNAME, null);
        if (username == null)
            return null;

        String base = Base64.encode(username.getBytes());
        return base;
    }

    public static boolean verifyPayload(Context context, String devPayload) {
        String userPayload = createPayload(context);
        return devPayload.equals(userPayload);
    }

    public static String disjunk() {
        return chunk1 + chunk2 + chunk3 + chunk4;
    }
}
