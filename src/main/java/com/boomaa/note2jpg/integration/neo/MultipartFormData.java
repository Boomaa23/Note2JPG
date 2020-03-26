package com.boomaa.note2jpg.integration.neo;

import java.io.InputStream;

public class MultipartFormData {
    private final String key;
    private final String filename;
    private final InputStream inputStream;

    private MultipartFormData(String key, String filename, InputStream inputStream) {
        this.key = key;
        this.filename = filename;
        this.inputStream = inputStream;
    }

    public String getKey() {
        return key;
    }

    public String getFilename() {
        return filename;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public static class DefaultBuilder {
        private String key;
        private String filename;
        private InputStream inputStream;

        public DefaultBuilder(String filename) {
            this.filename = filename + ".jpg";
            this.key = "file";
        }

        public DefaultBuilder setInputStream(InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        public MultipartFormData build() {
            return new MultipartFormData(key, filename, inputStream);
        }
    }
}
