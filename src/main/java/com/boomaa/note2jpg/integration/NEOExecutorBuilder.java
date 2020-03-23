package com.boomaa.note2jpg.integration;

import com.boomaa.note2jpg.config.Parameter;

public class NEOExecutorBuilder {
    private char[] username;
    private char[] password;
    private String classID;

    public NEOExecutorBuilder(String username, String password) {
        this.username = username.toCharArray();
        this.password = password.toCharArray();
        this.classID = Parameter.NEOClassID.getPriority();
    }

    public NEOExecutorBuilder setUsername(String username) {
        this.username = username.toCharArray();
        return this;
    }

    public NEOExecutorBuilder setPassword(String password) {
        this.password = password.toCharArray();
        return this;
    }

    public NEOExecutorBuilder setClassID(String classID) {
        this.classID = classID;
        return this;
    }

    public NEOExecutor build() {
        return new NEOExecutor(classID, username, password);
    }
}
