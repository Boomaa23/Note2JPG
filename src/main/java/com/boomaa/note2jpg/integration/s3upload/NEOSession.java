package com.boomaa.note2jpg.integration.s3upload;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Map;

public final class NEOSession {
    private String baseUrl = "https://neo.sbunified.org";
    private String authCookieName = "secure_lmssessionkey2";
    private String authCookieValue = null;

    public NEOSession(String username, String password) {
        try {
            authCookieValue = Jsoup.connect(getLoginUrl(username, password))
                .ignoreContentType(true).execute().cookie(authCookieName);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public final Connection getConnection(String url) {
        return getConnection(url, true);
    }

    public final Connection getConnection(String url, boolean useBaseUrl) {
        return Jsoup.connect((useBaseUrl ? baseUrl : "") + url).cookie(authCookieName, authCookieValue);
    }

    private String getLoginUrl(String username, String password) {
        StringBuilder sb = new StringBuilder(baseUrl + "/log_in/submit_from_portal");
        sb.append("?userid=");
        sb.append(username);
        sb.append("&password=");
        sb.append(password);
        return sb.toString();
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
}
