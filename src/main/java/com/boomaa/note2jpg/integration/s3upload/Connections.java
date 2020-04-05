package com.boomaa.note2jpg.integration.s3upload;

public class Connections {
    private static NEOSession NEO_SESSION;
    private static AWSExecutor AWS_EXECUTOR;

    public static void create(String username, String password) {
        NEO_SESSION = new NEOSession(username, password);
        AWS_EXECUTOR = new AWSExecutor(NEO_SESSION);
    }

    public static NEOSession getNeoSession() {
        return NEO_SESSION;
    }

    public static AWSExecutor getAwsExecutor() {
        return AWS_EXECUTOR;
    }
}
