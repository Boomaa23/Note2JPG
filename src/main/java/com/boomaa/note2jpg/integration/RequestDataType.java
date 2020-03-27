package com.boomaa.note2jpg.integration;

public enum RequestDataType {
    NOTE(".note", "mimeType = 'application/x-zip'"),
    JPG(".jpg", "mimeType = 'image/jpeg'");

    public final String ext;
    public final String q;

    RequestDataType(String ext, String q) {
        this.ext = ext;
        this.q = q;
    }
}
