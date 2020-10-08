package com.boomaa.note2jpg.integration.s3upload;

import java.io.InputStream;

public class MultipartFormData {
    private final String filename;
    private final InputStream inputStream;

    public MultipartFormData(String filename, Extension ext, InputStream inputStream) {
        this.filename = filename + "." + ext.name();
        this.inputStream = inputStream;
    }

    public String getKey() {
        return "file";
    }

    public String getFilename() {
        return filename;
    }

    public InputStream getInputStream() {
        return inputStream;
    }
}
