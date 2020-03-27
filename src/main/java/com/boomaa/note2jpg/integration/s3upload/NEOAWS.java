package com.boomaa.note2jpg.integration.s3upload;

public class NEOAWS {
    public static NEOSession NEO_SESSION;
    public static AWSExecutor AWS_EXECUTOR;

    public static void create(String username, String password) {
        NEO_SESSION = new NEOSession(username, password);
        AWS_EXECUTOR = new AWSExecutor(NEO_SESSION);
    }
}
