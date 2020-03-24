package com.boomaa.note2jpg.integration;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoogleUtils {
    private static final String APPLICATION_NAME = "Note2JPG";
    private static final String PRIVATE_KEY_NAME = "GoogleSvcAcctPrivateKey.json";
    private static Drive driveService;
    private static Map<String, File> noteList;

    static {
        try {
            driveService = GoogleUtils.getDriveService();
            noteList = new HashMap<>();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    private static HttpRequestInitializer authorize() throws IOException {
        return new HttpCredentialsAdapter(
            GoogleCredentials.fromStream(
                new FileInputStream(PRIVATE_KEY_NAME))
                .createScoped(DriveScopes.DRIVE));
    }

    private static Drive getDriveService() throws IOException, GeneralSecurityException {
        return new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(),
            GoogleUtils.authorize()).setApplicationName(APPLICATION_NAME).build();
    }

    public static void retrieveNoteList() {
        try {
            List<File> notes = driveService.files().list()
                .setQ("mimeType = 'application/x-zip'")
                .setFields("files")
                .setOrderBy("modifiedTime desc")
                .execute().getFiles();
            for (File note : notes) {
                noteList.put(note.getName(), note);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isFilenameMatch(String filename) {
        return noteList.containsKey(filename + ".note");
    }

    public static String getEmbedUrl(String fileId) {
        return "https://drive.google.com/open?id=" + fileId;
    }

    public static void downloadNote(String filename) {
        try {
            filename += ".note";
            java.io.File outputFile = new java.io.File(filename);
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }
            OutputStream outputStream = new FileOutputStream(outputFile);
            driveService.files().get(noteList.get(filename).getId())
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
            File f = driveService.files().create(fileMetadata, mediaContent).execute();
            insertPermissions(f.getId());
            return f;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Permission insertPermissions(String fileId) {
        try {
            Permission newPermission = new Permission();
            newPermission.setType("anyone");
            newPermission.setRole("reader");
            return driveService.permissions().create(fileId, newPermission).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}