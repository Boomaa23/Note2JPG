package com.boomaa.note2jpg.integration;

import com.google.gson.JsonParser;
import org.jsoup.nodes.Document;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AWSExecutor {
    private final NEOSession session;

    protected AWSExecutor(NEOSession session) {
        this.session = session;
    }

    public final String execute(String filename) {
        String neoFilename = registerNeoFilename(filename);
        Map<String, String> credVars = getAWSCredentials();
        uploadImage(credVars, neoFilename, filename);
        return session.getBaseUrl() + "/" + credVars.get("aws_location") + "/" + neoFilename;
    }

    private String registerNeoFilename(String filename) {
        Map<String, String> data = new HashMap<>();
        data.put("file_type", "image/jpg");
        //TODO figure out if this is needed
        data.put("file_size", "5380");
        data.put("file_name", filename + ".jpg");

        String neoFilename = filename;
        try {
            Document registeredJson = session.getConnection("/upload/file")
                .ignoreContentType(true)
                .data(data)
                .post();
            neoFilename = JsonParser.parseString(registeredJson.text()).getAsJsonObject().get("name").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return neoFilename;
    }

    private Map<String, String> getAWSCredentials() {
        Document awsCredSrc = session.get("/uploader?data_json=%7B%22controller%22%3A%22locker%22%2C%22action%22%3A%22list%22%2C%22resource%22%3A%22File%22%2C%22location%22%3A%22ClassResource%22%2C%22to_controller%22%3A%22locker%22%2C%22to_id%22%3Anull%2C%22auto%22%3Atrue%2C%22type%22%3A%22FileResource%22%2C%22new%22%3A%22true%22%7D");
        String returnedVarString = awsCredSrc.getElementsByTag("script").get(0).html().replaceAll("\\ +", "");
        List<String> lineSortedVars = new ArrayList<>();
        int lastIndex = 0;
        for (int i = 0; i < returnedVarString.length(); i++) {
            if (returnedVarString.charAt(i) == '\n') {
                String credVarSingle = returnedVarString.substring(lastIndex, i).replaceAll("[\\n]", "");
                if (credVarSingle.trim().length() >= 3 && credVarSingle.substring(0, 3).equals("var")) {
                    lineSortedVars.add(credVarSingle.substring(3));
                }
                lastIndex = i;
            }
        }

        Map<String, String> awsCredMap = new HashMap<>();
        for (int i = 0;i < lineSortedVars.size();i++) {
            String currVar = lineSortedVars.get(i);
            int ioEquals = currVar.indexOf("=");
            int ioEnd = currVar.lastIndexOf("\";");
            if (ioEnd == -1 || ioEquals == -1) {
                continue;
            }
            awsCredMap.put(currVar.substring(0, ioEquals), currVar.substring(ioEquals + 2, ioEnd));
        }
        return awsCredMap;
    }

    private Document uploadImage(Map<String, String> credVarMap, String neoFilename, String filename) {
        filename += ".jpg";
        Map<String, String> awsFormData = new HashMap<>();
        awsFormData.put("AWSAccessKeyId", credVarMap.get("aws_key"));
        awsFormData.put("acl", credVarMap.get("aws_acl"));
        awsFormData.put("key", credVarMap.get("aws_location") + "/" + neoFilename);
        awsFormData.put("policy", credVarMap.get("aws_policy"));
        awsFormData.put("signature", credVarMap.get("aws_signature"));
        awsFormData.put("Content-Type", "image/jpg");
        awsFormData.put("name", neoFilename);
        awsFormData.put("filename", neoFilename);
        awsFormData.put("utf8", "true");

        try {
            return session.getConnection("https://s3.amazonaws.com/s3.edu20.org/", false)
                .data(awsFormData).data("file", filename, new FileInputStream(filename))
                .header("Content-Type", "multipart/form-data")
                .ignoreHttpErrors(true).ignoreContentType(true)
                .post();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
