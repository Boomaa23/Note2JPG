package com.boomaa.note2jpg.integration.s3upload;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Map;

public final class NEOSession {
    private static NEOSession INSTANCE;
    private String baseUrl = "https://neo.sbunified.org";
    private String authCookieName = "secure_lmssessionkey2";
    private String authCookieValue = null;

    private NEOSession(String username, String password) {
        try {
            authCookieValue = Jsoup.connect(getLoginUrl(username, password))
                .ignoreContentType(true).execute().cookie(authCookieName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public final Document get(String url) {
        return get(url, true);
    }
    
    public final Document get(String url, boolean useBaseUrl) {
        try {
            return getConnection(url, useBaseUrl).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public final Document post(Map<String, String> data, String url) {
        return post(data, url, true);
    }

    public final Document post(Map<String, String> data, String url, boolean useBaseUrl) {
        try {
            return getConnection(url, useBaseUrl).data(data).post();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public final Connection getConnection(String url) {
        return getConnection(url, true);
    }

    public final Connection getConnection(String url, boolean useBaseUrl) {
        return Jsoup.connect((useBaseUrl ? baseUrl : "") + url).cookie(authCookieName, authCookieValue);
    }

    private String getLoginUrl(String username, String password) {
        return baseUrl + "/log_in/submit_from_portal?userid=" + username + "&password=" + password;
    }

    public NEOSession setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public final String getAuth() {
        return authCookieValue;
    }

    public static NEOSession getInstance() {
        return getInstance(null, null);
    }

    public static NEOSession getInstance(String username, String password) {
        if (INSTANCE == null && username != null && password != null) {
            INSTANCE = new NEOSession(username, password);
        }
        return INSTANCE;
    }
}
