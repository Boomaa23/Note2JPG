package com.boomaa.note2jpg.integration;

import com.boomaa.note2jpg.config.Parameter;
import com.boomaa.note2jpg.integration.s3upload.Extension;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GoogleUtils {
    private static final GoogleUtils INSTANCE = new GoogleUtils();
    private static final String APPLICATION_NAME = "Note2JPG";
    private static final String PRIVATE_KEY_NAME = "GoogleSvcAcctPrivateKey.json";
    private static final String CONFIG_FILE_NAME = "googleclient.conf";
    private static final String REFRESH_TOKEN_FN = "oauthrefresh.token";
    private String clientId;
    private String clientSecret;
    private String refreshToken;
    private HttpTransport httpTransport;
    private JsonFactory jsonFactory;
    private Drive driveService;
    private Map<String, File> noteList; //Keys are Google file names
    private Map<String, File> imageList; //Keys are Google file IDs

    private GoogleUtils() {
        try {
            readClientConfig();
            readRefreshToken();
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            jsonFactory = JacksonFactory.getDefaultInstance();
            driveService = this.getDriveService();
            noteList = new LinkedHashMap<>();
            imageList = new LinkedHashMap<>();
            retrieveData(Extension.note, Parameter.LimitDriveNotes.getValueInt());
            retrieveData(Extension.jpg, Parameter.LimitDriveNotes.getValueInt());
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    private void readClientConfig() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/" + CONFIG_FILE_NAME)));
        clientId = reader.readLine();
        clientSecret = reader.readLine();
        reader.close();
    }

    private void readRefreshToken() {
        try {
            refreshToken = Files.readString(Paths.get(REFRESH_TOKEN_FN));
        } catch (IOException ignored) {
        }
    }

    private HttpRequestInitializer authorize() throws IOException {
        try {
            if (Parameter.GoogleSvcAcct.inEither()) {
                return new HttpCredentialsAdapter(
                        GoogleCredentials.fromStream(new FileInputStream(PRIVATE_KEY_NAME))
                                .createScoped(DriveScopes.DRIVE));
            } else if (Parameter.GoogleRelog.inEither() || refreshToken == null) {
                    AuthorizationCodeFlow flow = new AuthorizationCodeFlow.Builder(
                            BearerToken.authorizationHeaderAccessMethod(), httpTransport, jsonFactory,
                            new GenericUrl(GoogleOAuthConstants.TOKEN_SERVER_URL),
                            new ClientParametersAuthentication(clientId, clientSecret),
                            clientId, GoogleOAuthConstants.AUTHORIZATION_SERVER_URL)
                            .setScopes(Collections.singleton(DriveScopes.DRIVE_READONLY)).build();
                    LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(5818).build();
                    Credential cred = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
                    Files.writeString(Paths.get(REFRESH_TOKEN_FN), cred.getRefreshToken());
                    return cred;
            } else {
                GoogleTokenResponse tokenResp = new GoogleRefreshTokenRequest(httpTransport, jsonFactory, refreshToken, clientId, clientSecret)
                        .setScopes(Collections.singleton(DriveScopes.DRIVE_READONLY)).setGrantType("refresh_token").execute();
                Date expireDate = Date.from(Instant.now().plusSeconds(tokenResp.getExpiresInSeconds()));
                return new HttpCredentialsAdapter(GoogleCredentials.create(new AccessToken(tokenResp.getAccessToken(), expireDate))
                        .createScoped(Collections.singleton(DriveScopes.DRIVE_READONLY)).toBuilder().build());
            }
        } catch (FileNotFoundException e) {
            System.err.println("Cannot find Google private key. Add a key or do not specify Google integration parameters.");
            e.printStackTrace();
            System.exit(1);
        } catch (GoogleJsonResponseException ignored) {
            System.exit(1);
        }
        return null;
    }

    private Drive getDriveService() throws IOException {
        return new Drive.Builder(httpTransport, jsonFactory, this.authorize())
                .setApplicationName(APPLICATION_NAME).build();
    }

    public void retrieveData(Extension extension, int maxRetrNum) {
        List<File> notes = new ArrayList<>();

        int noteCtr = 0;
        String nextPageToken = "";
        int reqNum = Math.min(maxRetrNum, 100);
        while (noteCtr < maxRetrNum && nextPageToken != null) {
            FileList dlFiles = doDataRequest(extension, nextPageToken, reqNum);
            noteCtr += reqNum;
            nextPageToken = dlFiles.getNextPageToken();
            notes.addAll(dlFiles.getFiles());
        }

        for (File note : notes) {
            String noteName = note.getName();
            int noExtEnd = noteName.length() - extension.name().length();
            if (noteName.substring(noExtEnd).equals(extension.name())) {
                if (extension == Extension.note) {
                    noteList.put(noteName.substring(0, noExtEnd - 1), note);
                } else if (extension == Extension.jpg) {
                    imageList.put(note.getId(), note);
                }
            }
        }
    }

    public FileList doDataRequest(Extension extension, String pageToken, int reqNum) {
        try {
            return driveService.files().list()
                    .setQ(extension.mimeType)
                    .setFields("files")
                    .setPageSize(reqNum)
                    .setPageToken(pageToken)
                    .setOrderBy("modifiedTime desc")
                    .setFields("files,nextPageToken")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteAllMatchingImages(String filename) {
        for (Map.Entry<String, File> entry : imageList.entrySet()) {
            File file = entry.getValue();
            if (file != null && file.getName().equals(filename + ".jpg")) {
                try {
                    driveService.files().delete(file.getId()).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean isNoteMatch(String filename) {
        return noteList.containsKey(filename);
    }

    public String getEmbedUrl(String fileId) {
        return "https://drive.google.com/open?id=" + fileId;
    }

    public void downloadNote(String gdriveFilename, String outputFilename) {
        try {
            java.io.File outputFile = new java.io.File(outputFilename);
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }
            OutputStream outputStream = new FileOutputStream(outputFile);
            driveService.files().get(noteList.get(gdriveFilename).getId())
                .executeMediaAndDownloadTo(outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File uploadImage(String filename) {
        try {
            File imageMetadata = new File();
            imageMetadata.setName(filename);
            imageMetadata.setMimeType("image/jpeg");

            FileContent imageContent = new FileContent("image/jpeg", new java.io.File(filename));
            File f = driveService.files().create(imageMetadata, imageContent).execute();
            insertPermissions(f.getId());
            return f;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Permission insertPermissions(String fileId) {
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

    public List<String> getNoteNameList() {
        return new ArrayList<>(noteList.keySet());
    }

    public List<String> getImageNameList() {
        return new ArrayList<>(imageList.keySet());
    }

    public static GoogleUtils getInstance() {
        return INSTANCE;
    }
}