package com.boomaa.note2jpg.integration.s3upload;

import com.google.gson.JsonParser;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class AWSExecutor {
    private final String NEO_CRED_URL = "/uploader?data_json=%7B%22controller%22%3A%22locker%22%2C%22action%22%3A%22list%22%2C%22" +
        "resource%22%3A%22File%22%2C%22location%22%3A%22ClassResource%22%2C%22to_controller%22%3A%22locker%22%2C%22to_id%22%3An" +
        "ull%2C%22auto%22%3Atrue%2C%22type%22%3A%22FileResource%22%2C%22new%22%3A%22true%22%7D";
    private final String AWS_BUCKET_URL = "https://s3.amazonaws.com/s3.edu20.org/";
    private final NEOSession session;
    private Elements registeredFileRows;

    public AWSExecutor(NEOSession session) {
        this.session = session;
    }

    public String[] remove(String filename) {
        Extension ext = Extension.getFromFilename(filename);
        Map<String, String> credVars = getAWSCredentials();
        MultipartFormData fileData = new MultipartFormData(filename, ext, new ByteArrayInputStream(new byte[0]));
        uploadImage(getAWSFormData(credVars, filename, ext.mimeType), fileData);
        return getMultiUrl(credVars, filename);
    }

    public String[] uploadFile(String uploadFilename, String localPath, boolean registerFilename) {
        return upload(uploadFilename, registerFilename, () -> {
            try {
                return new FileInputStream(localPath + uploadFilename);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.exit(1);
            }
            return null;
        });
    }

    public String[] upload(String filename, boolean registerFilename, Supplier<InputStream> input) {
        Extension ext = Extension.getFromFilename(filename);
        if (registerFilename || !isRegistered(filename)) {
            filename = registerNeoFilename(filename);
        }
        Map<String, String> credVars = getAWSCredentials();
        MultipartFormData fileData = new MultipartFormData(filename, ext, input.get());
        uploadImage(getAWSFormData(credVars, filename, ext.mimeType), fileData);
        return getMultiUrl(credVars, filename);
    }

    public String[] getMultiUrl(Map<String, String> credVars, String filename) {
        String addUrl = credVars.get("aws_location") + "/" + filename;
        return new String[] { AWS_BUCKET_URL + addUrl, session.getBaseUrl() + "/" + addUrl };
    }

    private String registerNeoFilename(String filename) {
        Map<String, String> data = new HashMap<>();
        data.put("file_type", "image/jpg");
        //TODO figure out if this is needed
        data.put("file_size", "2050");
        data.put("file_name", filename);

        String neoFilename = filename;
        try {
            Document registeredJson = session.getConnection("/upload/file")
                .data(data).ignoreContentType(true).post();
            neoFilename = JsonParser.parseString(registeredJson.text()).getAsJsonObject().get("name").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return neoFilename;
    }

    private Map<String, String> getAWSCredentials() {
        String returnedVarString = session.get(NEO_CRED_URL).getElementsByTag("script")
            .get(0).html().replaceAll("\\ +", "");
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
        for (String currVar : lineSortedVars) {
            int ioEquals = currVar.indexOf("=");
            int ioEnd = currVar.lastIndexOf("\";");
            if (ioEnd == -1 || ioEquals == -1) {
                continue;
            }
            awsCredMap.put(currVar.substring(0, ioEquals), currVar.substring(ioEquals + 2, ioEnd));
        }
        return awsCredMap;
    }

    private Document uploadImage(Map<String, String> awsFormData, MultipartFormData fileData) {
        try {
            return session.getConnection(AWS_BUCKET_URL, false)
                .data(awsFormData).data(fileData.getKey(), fileData.getFilename(), fileData.getInputStream())
                .header("Content-Type", "multipart/form-data")
                .ignoreHttpErrors(true).ignoreContentType(true)
                .post();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Map<String, String> getAWSFormData(Map<String, String> credVarMap, String neoFilename, String mimeType) {
        Map<String, String> awsFormData = new HashMap<>();
        awsFormData.put("AWSAccessKeyId", credVarMap.get("aws_key"));
        awsFormData.put("acl", credVarMap.get("aws_acl"));
        awsFormData.put("key", credVarMap.get("aws_location") + "/" + neoFilename);
        awsFormData.put("policy", credVarMap.get("aws_policy"));
        awsFormData.put("signature", credVarMap.get("aws_signature"));
        // TODO see if this breaks it
        awsFormData.put("Content-Type", mimeType);
        awsFormData.put("name", neoFilename);
        awsFormData.put("filename", neoFilename);
        awsFormData.put("utf8", "true");
        return awsFormData;
    }

    private boolean isRegistered(String filename) {
        if (registeredFileRows == null) {
            registeredFileRows = session.get("/uploaded_files/list?ajax_request=true&limit=10000").getElementsByTag("tr");
        }
        for (Element row : registeredFileRows) {
            Elements hrefs = row.getElementsByAttribute("href");
            if (!row.hasClass("sort-icon") && hrefs.size() == 1
                    && hrefs.first().text().equals(filename)) {
                return true;
            }
        }
        return false;
    }
}
