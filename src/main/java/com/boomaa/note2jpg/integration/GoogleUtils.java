package com.boomaa.note2jpg.integration;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;

public class GoogleUtils {
    private static final String APPLICATION_NAME = "Note2JPG";
    private static final String ACCOUNT_ID = FileUtil.fileToString("GoogleSvcAcctID.conf");
    private static Drive DRIVE_SERVICE;
    private static Map<String, File> NOTE_LIST;

    static {
        try {
            DRIVE_SERVICE = GoogleUtils.getDriveService();
            NOTE_LIST = new HashMap<>();
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
            .setServiceAccountScopes(Collections.singletonList(DriveScopes.DRIVE))
            .setServiceAccountPrivateKeyFromP12File(new java.io.File("GoogleSvcAcctPrivateKey.p12"))
            .build();
    }

    private static Drive getDriveService() throws IOException, GeneralSecurityException {
        return new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(),
            GoogleUtils.authorize()).setApplicationName(APPLICATION_NAME).build();
    }

    public static void retrieveNoteList() {
        try {
            //TODO figure out how to include shared files
            List<File> notes = DRIVE_SERVICE.files().list()
                .setQ("mimeType = 'application/x-zip'")
                .setFields("files")
                .setOrderBy("modifiedTime desc")
                .execute().getFiles();
            for (File temp : notes) {
                NOTE_LIST.put(temp.getName(), temp);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isFilenameMatch(String filename) {
        return NOTE_LIST.isEmpty() || NOTE_LIST.containsKey(filename);
    }

    public static void downloadNote(String filename) {
        try {
            java.io.File outputFile = new java.io.File(filename + ".note");
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }
            OutputStream outputStream = new FileOutputStream(outputFile);
            DRIVE_SERVICE.files().get(NOTE_LIST.get(filename).getId())
                .executeMediaAndDownloadTo(outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static File uploadImage(String filename) {
        //TODO figure out the NEO AWS uploader and use that instead of drive
        try {
            File fileMetadata = new File();
            fileMetadata.setName(filename);
            fileMetadata.setMimeType("image/jpeg");

            FileContent mediaContent = new FileContent("image/jpeg", new java.io.File(filename + ".jpg"));
            File f = DRIVE_SERVICE.files().create(fileMetadata, mediaContent).execute();
            insertPermissions(f.getId());
            return f;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Permission insertPermissions(String fileId) {
        try {
            Permission newPermission = new Permission();
            newPermission.setType("anyone");
            newPermission.setRole("reader");
            return DRIVE_SERVICE.permissions().create(fileId, newPermission).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getEmbedUrl(String fileId) {
        return "https://drive.google.com/open?id=" + fileId;
    }
}