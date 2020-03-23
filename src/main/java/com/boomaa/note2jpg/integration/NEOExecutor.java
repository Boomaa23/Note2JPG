package com.boomaa.note2jpg.integration;

import com.boomaa.note2jpg.function.NFields;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Collections;

public class NEOExecutor extends NFields {
    private Assignments ufAssignments;
    private NEOSession session;

    public NEOExecutor(String classId, char[] username, char[] password) {
        session = new NEOSession(classId).login(username, password);
    }

    public NEOExecutor(char[] username, char[] password) {
        this(JSONHelper.getNEOClassID(), username, password);
    }

    public final NEOExecutor push(String assignName, String imageUrl) {
        //TODO test this with an open assignment
        Element img = new Element("img");
        img.attr("src", imageUrl);
        img.attr("width", String.valueOf(NFields.iPadWidth));
        img.attr("height", String.valueOf(NFields.heightFinal));
        session.post(Collections.singletonMap("answer", img.outerHtml()), "/student_freeform_assignment/create/" + ufAssignments.get(assignName));
        return this;
    }

    public final NEOExecutor pull() {
        ufAssignments = getUnfinished(parseAssignments(retrieveAssignDoc()));
        return this;
    }

    public final Assignments getAssignments() {
        return ufAssignments;
    }

    private Document retrieveAssignDoc() {
        return session.get("/student_assignments/list/" + session.getClassId());
    }

    private Elements parseAssignments(Document assignmentsDocument) {
        Elements table = assignmentsDocument.body().getElementById("wrapper").getElementById("contentWrap").getElementById("mainContent")
            .getElementById("centreColumn").getElementsByClass("assignmentsTable");
        return table.first().getElementsByTag("tbody").first().getElementsByTag("tr");
    }

    private Assignments getUnfinished(Elements table) {
        Assignments ufAssignTemp = new Assignments();
        for (Element e : table) {
            boolean isSubmitted = true;
            boolean isAssignment = false;
            String assignment = null;
            String assignmentId = null;
            try {
                Element assignName = e.getElementsByClass("assignment").get(0)
                    .getElementsByTag("a").first();
                assignment = assignName.text();
                assignmentId = assignName.attr("href");
                assignmentId = assignmentId.substring(assignmentId.lastIndexOf('/'));
                Element innerATag = e.getAllElements().get(4);
                isSubmitted = innerATag.getElementsByAttributeValue("title", "Yes").isEmpty();
                isAssignment = !innerATag.getElementsByAttributeValueStarting("title", "Online/essay").isEmpty();
            } catch (IndexOutOfBoundsException ignored) {
            }
            if (assignment != null && !isSubmitted && isAssignment) {
                ufAssignTemp.put(assignment, assignmentId);
            }
        }
        return ufAssignTemp;
    }

    public static NEOExecutor parseArgs() {
        int ioNeo = NFields.argsList.indexOf("--neo");
        char[] username = NFields.argsList.get(ioNeo + 1).toCharArray();
        char[] password = NFields.argsList.get(ioNeo + 2).toCharArray();
        if (NFields.argsList.contains("--classid")) {
            return new NEOExecutor(NFields.argsList.get(NFields.argsList.indexOf("--classid") + 1), username, password);
        }
        return new NEOExecutor(username, password);
    }
}
