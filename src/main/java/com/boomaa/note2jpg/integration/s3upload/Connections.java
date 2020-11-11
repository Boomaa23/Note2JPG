package com.boomaa.note2jpg.integration.s3upload;

import com.boomaa.note2jpg.integration.GoogleUtils;

public class Connections {
    private static NEOSession NEO_SESSION;
    private static AWSExecutor AWS_EXECUTOR;

    public static void create(String username, String password) {
        NEO_SESSION = NEOSession.getInstance(username, password);
        AWS_EXECUTOR = new AWSExecutor(NEO_SESSION);
    }

    public static NEOSession getNeoSession() {
        return NEO_SESSION;
    }

    public static AWSExecutor getAwsExecutor() {
        return AWS_EXECUTOR;
    }

    public static GoogleUtils getGoogleUtils() {
        return GoogleUtils.getInstance();
    }
}
