package com.boomaa.note2jpg.integration;

import com.boomaa.note2jpg.config.Parameter;
import com.boomaa.note2jpg.convert.NFields;
import com.boomaa.note2jpg.integration.s3upload.Connections;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NEOExecutor extends NFields {
    private NameIDMap ufAssignments;
    private NameIDMap classList;

    public NEOExecutor() {
        this.classList = parseClasses(Connections.getNeoSession().get("/enrolled_dashboard"));
        this.ufAssignments = new NameIDMap();
    }

    public final String push(String assignName, String imageUrl) {
        Element img = new Element("img");
        img.attr("src", imageUrl.contains("neo.sbunified.org") ?
                imageUrl.substring(imageUrl.indexOf("/files")) : imageUrl);
        img.attr("width", String.valueOf(NFields.iPadWidth));
        img.attr("height", String.valueOf(NFields.heightFinal));
        String assign = ufAssignments.get(assignName);
        if (assign != null) {
            if (assign.contains("&section_id=")) {
                String realAssign = Connections.getNeoSession().get(assign)
                        .getElementsByClass("optionsRibbon").first()
                        .getElementsByAttribute("href").first().attr("href");
                assign = realAssign.substring(realAssign.lastIndexOf('/'));
            }
            // Only works over unsecured HTTP for some reason
            String baseUrl = "http://neo.sbunified.org/student_freeform_assignment/";
            Connections.getNeoSession().post(Collections.singletonMap("answer", img.outerHtml()), baseUrl + "create" + assign, false);
            Connections.getNeoSession().get(baseUrl + "submit" + assign, false);
            return baseUrl + "show" + assign;
        }
        return null;
    }

    public final NEOExecutor pull() {
        getUnfinished(parseAssignments(retrieveAssignDoc(false)));
        if (Parameter.IncludeUnits.inEither()) {
            getUnitClasses(getUnitNums(retrieveAssignDoc(true)));
        }
        return this;
    }

    public final NameIDMap getAssignments() {
        return ufAssignments;
    }

    public final NameIDMap getClassList() {
        return classList;
    }

    private Document retrieveAssignDoc(boolean inUnits) {
        return Connections.getNeoSession().get("/student_" + (inUnits ? "lessons" : "assignments") + "/list/" + Parameter.NEOClassID.getValue());
    }

    private Elements parseAssignments(Document assignmentsDocument) {
        Elements table = assignmentsDocument.body().getElementById("wrapper").getElementById("contentWrap").getElementById("mainContent")
            .getElementById("centreColumn").getElementsByClass("assignmentsTable");
        return table.first().getElementsByTag("tbody").first().getElementsByTag("tr");
    }

    private List<Integer> getUnitNums(Document unitsDocument) {
        List<Integer> unitNums = new ArrayList<>();
        Elements sections = unitsDocument.getElementsByClass("lesson_boxes");
        for (Element sec : sections) {
            String href = sec.getElementsByAttribute("href").attr("href");
            int lessonStart = href.indexOf("lesson_id=");
            int lessonEnd = href.indexOf("&", lessonStart);
            if (lessonStart != -1 && lessonEnd != -1) {
                unitNums.add(Integer.parseInt(href.substring(lessonStart + 10, lessonEnd)));
            }
        }
        return unitNums;
    }

    private void getUnitClasses(List<Integer> unitNums) {
        for (int num : unitNums) {
            Document unitPage = Connections.getNeoSession().get("/student_lesson/show/" + Parameter.NEOClassID.getValue() + "?lesson_id="  + num);
            Elements modules = unitPage.getElementsByClass("module_sections");
            for (Element module : modules) {
                Elements hrefs = module.getElementsByAttribute("href");
                for (Element href : hrefs) {
                    String title = href.getElementsByTag("span").get(2).text();
                    String url = href.attr("href");
                    if (url != null && title != null) {
                        ufAssignments.put(title, url); // Adds a URL instead of an assignment number to minimize HTTP requests
                    }
                }
            }
        }
    }

    private void getUnfinished(Elements table) {
        for (Element e : table) {
            boolean notSubmitted = true;
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
                Elements flagAlt = innerTd.getElementsByClass("textOffScreen");
                notSubmitted = innerTd.text().contains("-") || innerTd.text().isBlank() || (flagAlt.size() != 0 && flagAlt.first().text().contains("Almost due")) || Parameter.AllowSubmitted.inEither();
                isAssignment = !assignName.getElementsByAttributeValueStarting("title", "Online/essay").isEmpty();
            } catch (IndexOutOfBoundsException ignored) {
            }
            if (assignment != null && isAssignment && notSubmitted) {
                ufAssignments.put(assignment, assignmentId);
            }
        }
    }

    private NameIDMap parseClasses(Document document) {
        NameIDMap classesTemp = new NameIDMap();
        Elements classListing = document.getElementsByClass("catalog_boxes").first().children();
        for (Element tile : classListing) {
            String name = tile.getElementsByClass("header").first().getElementsByTag("a").first().getElementsByTag("h2").text();
            classesTemp.put(name, tile.attr("id"));
        }
        return classesTemp;
    }
}
