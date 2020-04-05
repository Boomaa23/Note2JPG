package com.boomaa.note2jpg.integration.s3upload;

import com.google.gson.JsonParser;
import org.jsoup.nodes.Document;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    public AWSExecutor(NEOSession session) {
        this.session = session;
    }

    public String[] remove(String filename) {
        Extension ext = Extension.getFromFilename(filename);
        Map<String, String> credVars = getAWSCredentials();
        MultipartFormData fileData = new MultipartFormData.DefaultBuilder(filename)
                .setInputStream(new ByteArrayInputStream(new byte[0]))
                .setExtension(ext).build();
        uploadImage(getAWSFormData(credVars, filename, ext.mimeType), fileData);
        return getMultiUrl(credVars, filename);
    }

    public String[] uploadFile(String filename) {
        return uploadFile(filename, false);
    }

    public String[] uploadFile(String filename, boolean registerFilename) {
        return upload(filename, registerFilename, () -> {
            try {
                return new FileInputStream(filename);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.exit(1);
            }
            return null;
        });
    }

    public String[] upload(String filename, boolean registerFilename, Supplier<InputStream> input) {
        Extension ext = Extension.getFromFilename(filename);
        if (registerFilename) {
            filename = registerNeoFilename(filename);
        }
        Map<String, String> credVars = getAWSCredentials();
        MultipartFormData fileData = new MultipartFormData.DefaultBuilder(filename)
            .setInputStream(input.get()).setExtension(ext).build();
        uploadImage(getAWSFormData(credVars, filename, ext.mimeType), fileData);
        return getMultiUrl(credVars, filename);
    }

    public String[] getMultiUrl(Map<String, String> credVars, String filename) {
        String addUrl = credVars.get("aws_location") + "/" + URLEncoder.encode(filename, StandardCharsets.UTF_8);
        return new String[] { AWS_BUCKET_URL + addUrl, session.getBaseUrl() + "/" + addUrl };
    }

    private String registerNeoFilename(String filename) {
        Map<String, String> data = new HashMap<>();
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
//        TODO see if this breaks it
        awsFormData.put("Content-Type", mimeType);
        awsFormData.put("name", neoFilename);
        awsFormData.put("filename", neoFilename);
        awsFormData.put("utf8", "true");
        return awsFormData;
    }
}
