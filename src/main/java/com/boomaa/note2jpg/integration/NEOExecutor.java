package com.boomaa.note2jpg.integration;

import com.boomaa.note2jpg.function.NFields;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NEOExecutor extends NFields {
    private List<String> unfinishedAssignments;
    private final char[] classId;
    private final char[] username;
    private final char[] password;

    public NEOExecutor(String classId, char[] username, char[] password) {
        this.classId = classId.toCharArray();
        this.username = username;
        this.password = password;
    }

    public NEOExecutor(char[] username, char[] password) {
        // Assume AP Physics 1 Period 1
        this("1543270", username, password);
    }

    public final NEOExecutor execute() {
        unfinishedAssignments = getUnfinished(parseAssignments(getWebContent()));
        return this;
    }

    public final List<String> getAssignments() {
        return unfinishedAssignments;
    }

    private String getLoginUrl() {
        StringBuilder sb = new StringBuilder("https://neo.sbunified.org/log_in/submit_from_portal?from=%2Fstudent_assignments%2Flist%2F");
        sb.append(classId);
        sb.append("&userid=");
        sb.append(username);
        sb.append("&password=");
        sb.append(password);
        return sb.toString();
    }

    private Document getWebContent() {
        try {
            Connection.Response resp = Jsoup.connect(getLoginUrl()).ignoreContentType(true).execute();
            String auth = resp.cookie("secure_lmssessionkey2");
            return Jsoup.connect("https://neo.sbunified.org/student_assignments/list/1543270")
                .cookie("secure_lmssessionkey2", auth).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Elements parseAssignments(Document assignmentsDocument) {
        System.out.println(assignmentsDocument.text());
        Elements table = assignmentsDocument.body().getElementById("wrapper").getElementById("contentWrap").getElementById("mainContent")
            .getElementById("centreColumn").getElementsByClass("assignmentsTable");
        return table.get(0).getElementsByTag("tbody").get(0).getElementsByTag("tr");
    }

    private List<String> getUnfinished(Elements table) {
        List<String> ufAssign = new ArrayList<>();
        for (Element e : table) {
            String assignment = e.getElementsByClass("assignment").get(0).getElementsByTag("a").get(0).text();
            boolean isSubmitted = e.getAllElements().get(4).getElementsByAttributeValue("title", "Yes").isEmpty();
            if (isSubmitted) {
                ufAssign.add(assignment);
            }
        }
        return ufAssign;
    }

    public static List<String> parseArgs() {
        if (argsList.contains("--neo")) {
            int ioNeo = argsList.indexOf("--neo");
            char[] username = argsList.get(ioNeo + 1).toCharArray();
            char[] password = argsList.get(ioNeo + 2).toCharArray();
            return new NEOExecutor(username, password).execute().getAssignments();
        }
        return null;
    }
}
