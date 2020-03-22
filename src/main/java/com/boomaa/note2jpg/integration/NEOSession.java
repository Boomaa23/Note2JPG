package com.boomaa.note2jpg.integration;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Map;

public class NEOSession {
    private final String classId;
    private String urlBase = "https://neo.sbunified.org";
    private String authCookieName = "secure_lmssessionkey2";
    private String authCookieValue = null;

    public NEOSession(String classId) {
        this.classId = classId;
    }

    public final NEOSession login(char[] username, char[] password) {
        try {
            authCookieValue = Jsoup.connect(getLoginUrl(username, password))
                .ignoreContentType(true).execute().cookie(authCookieName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public final Document get(String url) {
        try {
            return getConnection(url).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public final Document post(Map<String, String> data, String url) {
        try {
            return getConnection(url).data(data).post();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Connection getConnection(String url) {
        return Jsoup.connect(urlBase + url).cookie(authCookieName, authCookieValue);
    }

    private String getLoginUrl(char[] username, char[] password) {
        StringBuilder sb = new StringBuilder(urlBase + "/log_in/submit_from_portal?from=%2Fstudent_assignments%2Flist%2F");
        sb.append(classId);
        sb.append("&userid=");
        sb.append(username);
        sb.append("&password=");
        sb.append(password);
        return sb.toString();
    }

    public NEOSession setUrlBase(String urlBase) {
        this.urlBase = urlBase;
        return this;
    }

    public String getUrlBase() {
        return urlBase;
    }

    public final String getClassId() {
        return classId;
    }

    public final String getAuth() {
        return authCookieValue;
    }
}
