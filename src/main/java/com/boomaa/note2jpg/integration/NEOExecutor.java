package com.boomaa.note2jpg.integration;

import com.boomaa.note2jpg.convert.NFields;
import com.boomaa.note2jpg.integration.s3upload.Connections;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Collections;

public class NEOExecutor extends NFields {
    private final String classId;
    private Assignments ufAssignments;

    public NEOExecutor(String classId) {
        this.classId = classId;
    }

    public final String push(String assignName, String imageUrl) {
        Element img = new Element("img");
        if (imageUrl.contains("neo.sbunified.org")) {
            img.attr("src", imageUrl.substring(imageUrl.indexOf("/files")));
        } else {
            img.attr("src", imageUrl);
        }
        img.attr("width", String.valueOf(NFields.iPadWidth));
        img.attr("height", String.valueOf(NFields.heightFinal));
        String assign = ufAssignments.get(assignName);
        if (assign != null) {
            // Only works over unsecured HTTP for some reason
            String baseUrl = "http://neo.sbunified.org/student_freeform_assignment/";
            Connections.getNeoSession().post(Collections.singletonMap("answer", img.outerHtml()), baseUrl + "create" + assign, false);
            Connections.getNeoSession().get(baseUrl + "submit" + assign, false);
            return baseUrl + "show" + assign;
        }
        return null;
    }

    public final NEOExecutor pull() {
        ufAssignments = getUnfinished(parseAssignments(retrieveAssignDoc()));
        return this;
    }

    public final Assignments getAssignments() {
        return ufAssignments;
    }

    private Document retrieveAssignDoc() {
        return Connections.getNeoSession().get("/student_assignments/list/" + classId);
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
                Element innerTd = e.getElementsByTag("td").get(5);
                isSubmitted = !innerTd.text().contains("-");
                isAssignment = !assignName.getElementsByAttributeValueStarting("title", "Online/essay").isEmpty();
            } catch (IndexOutOfBoundsException ignored) {
            }
            if (assignment != null && !isSubmitted && isAssignment) {
                ufAssignTemp.put(assignment, assignmentId);
            }
        }
        return ufAssignTemp;
    }
}
