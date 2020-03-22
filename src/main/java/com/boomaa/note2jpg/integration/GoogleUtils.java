package com.boomaa.note2jpg.integration;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import com.boomaa.note2jpg.function.FileUtil;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

public class GoogleUtils {
    private static final String APPLICATION_NAME = "Note2JPG";
    private static final String ACCOUNT_ID = FileUtil.fileToString("GoogleSvcAcctID.conf");
    private static Drive DRIVE_SERVICE;
    private static Drive.Files.List NOTE_LIST;

    static {
        try {
            DRIVE_SERVICE = GoogleUtils.getDriveService();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    private static Credential authorize() throws IOException, GeneralSecurityException {
        //TODO use Google Auth Library (ServiceAccountCredentials) for authentication instead of Google API
        return new GoogleCredential.Builder()
            .setTransport(GoogleNetHttpTransport.newTrustedTransport())
            .setJsonFactory(new GsonFactory())
            .setServiceAccountId(ACCOUNT_ID)
            //TODO sort out this scopes issues
            .setServiceAccountScopes(Collections.singletonList(DriveScopes.DRIVE_FILE))
            .setServiceAccountPrivateKeyFromP12File(new java.io.File("GoogleSvcAcctPrivateKey.p12"))
            .build();
    }

    private static Drive getDriveService() throws IOException, GeneralSecurityException {
        return new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(),
            GoogleUtils.authorize()).setApplicationName(APPLICATION_NAME).build();
    }

    public static void retrieveNoteList() {
        try {
            NOTE_LIST = DRIVE_SERVICE.files().list()
                .setQ("mimeType = 'application/x-zip'")
                .setOrderBy("modifiedTime desc");
//            System.out.println(NOTE_LIST.executeUsingHead().getContent());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isFilenameMatch(String filename) {
        //TODO [IMPORTANT] make this actually work
        return false;
    }

    public static void downloadNote(String filename) {
        //TODO [IMPORTANT] make this actually work
    }

    public static String uploadImage(String filename) {
        //TODO figure out the NEO AWS uploader and use that instead of drive
        String url = "";
        try {
            //TODO get this to work and get a direct display URL for the uploaded image from it
            Drive.Files.List fileList = DRIVE_SERVICE.files().list();
            System.out.println(fileList.getDriveId());
            File fileMetadata = new File();
            fileMetadata.setName(filename);
            fileMetadata.setMimeType("image/jpeg");

            FileContent mediaContent = new FileContent("image/jpeg", new java.io.File(filename + ".jpg"));
            File file = DRIVE_SERVICE.files().create(fileMetadata, mediaContent)
                .execute();
            System.out.println(file.getCreatedTime());
            System.out.println(file.toPrettyString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return url;
    }
}