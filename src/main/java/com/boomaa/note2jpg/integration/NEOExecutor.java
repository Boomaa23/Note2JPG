package com.boomaa.note2jpg.integration;

import com.boomaa.note2jpg.function.NFields;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NEOExecutor extends NFields {
    private List<String> unfinishedAssignments;
    private final String classId;
    private final char[] username;
    private final char[] password;

    public NEOExecutor(String classId, char[] username, char[] password) {
        this.classId = classId;
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
            String auth = Jsoup.connect(getLoginUrl()).ignoreContentType(true).execute().cookie("secure_lmssessionkey2");
            return Jsoup.connect("https://neo.sbunified.org/student_assignments/list/" + classId)
                .cookie("secure_lmssessionkey2", auth).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Elements parseAssignments(Document assignmentsDocument) {
        Elements table = assignmentsDocument.body().getElementById("wrapper").getElementById("contentWrap").getElementById("mainContent")
            .getElementById("centreColumn").getElementsByClass("assignmentsTable");
        return table.first().getElementsByTag("tbody").first().getElementsByTag("tr");
    }

    private List<String> getUnfinished(Elements table) {
        List<String> ufAssign = new ArrayList<>();
        for (Element e : table) {
            boolean isSubmitted = true;
            boolean isAssignment = false;
            String assignment = null;
            try {
                assignment = e.getElementsByClass("assignment").get(0)
                    .getElementsByTag("a").first().text();
                Element innerATag = e.getAllElements().get(4);
                isSubmitted = innerATag.getElementsByAttributeValue("title", "Yes").isEmpty();
                isAssignment = !innerATag.getElementsByAttributeValueStarting("title", "Online/essay").isEmpty();
            } catch (IndexOutOfBoundsException ignored) {
            }
            if (assignment != null && isSubmitted && isAssignment) {
                System.out.println(assignment);
                ufAssign.add(assignment);
            }
        }
        return ufAssign;
    }

    public static List<String> parseArgs() {
        if (argsList.contains("--neo")) {
            if (argsList.contains("-f") || argsList.contains("--all")) {
                throw new IllegalArgumentException("Cannot use other selectors with NEO integration");
            }
            int ioNeo = argsList.indexOf("--neo");
            char[] username = argsList.get(ioNeo + 1).toCharArray();
            char[] password = argsList.get(ioNeo + 2).toCharArray();
            if (argsList.contains("--classid")) {
                return new NEOExecutor(argsList.get(argsList.indexOf("--classid") + 1), username, password)
                    .execute().getAssignments();
            }
            return new NEOExecutor(username, password).execute().getAssignments();
        }
        return filenames;
    }
}
