package com.boomaa.note2jpg.dependencies;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DownloadDeps {
    private static String DEPENDENCIES_URL = "https://raw.githubusercontent.com/Boomaa23/Note2JPG/master/src/main/resources/dependencies.conf";
    private static String LIBRARY_FOLDER = "lib/";
    private static List<MavenDependency> dependencyList = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        File libDir = new File(LIBRARY_FOLDER);
        if (!libDir.exists()) {
            libDir.mkdir();
        }

        BufferedInputStream in = new BufferedInputStream(new URL(DEPENDENCIES_URL).openStream());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] dataBuffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
            out.write(dataBuffer, 0, bytesRead);
        }

        String depsString = out.toString();
        List<String> depsPrelimList = new ArrayList<>();
        int lastIndex = 0;
        for (int i = 0;i < depsString.length();i++) {
            if (depsString.charAt(i) == '\n') {
                String depSingle = depsString.substring(lastIndex, i).replaceAll("[\\\\+\\ \\|\\n]", "");
                if (depSingle.charAt(0) == '-') {
                    depSingle = depSingle.substring(1);
                }
                depsPrelimList.add(depSingle);
                lastIndex = i;
            }
        }
        depsPrelimList.remove(0);

        for (String dep : depsPrelimList) {
            String[] parts = dep.split(":");
            dependencyList.add(new MavenDependency(parts[0], parts[1], parts[3]));
        }

        long originalFolderSize = FileUtil.folderSize(new File(LIBRARY_FOLDER));
        int downloadCounter = 0;
        for (MavenDependency dependency : dependencyList) {
            if (!new File(LIBRARY_FOLDER + dependency.getJarName()).exists()) {
                System.out.println("Downloading " + dependency);
                downloadDependency(dependency);
                downloadCounter++;
            }
        }
        if (!Arrays.asList(args).contains("--nogsdll64")) {
            downloadFile("https://s3.amazonaws.com/s3.edu20.org/files/2796766/gsdll64.dll", "gsdll64.dll");
        }
        if (downloadCounter > 0) {
            System.out.println();
        }

        String folderSize = FileUtil.humanReadable(FileUtil.folderSize(new File(LIBRARY_FOLDER)) - originalFolderSize);
        System.out.println("Downloaded " + downloadCounter + " dependencies to " +
            "/" + LIBRARY_FOLDER + " (" + folderSize + ")");
    }

    private static String getMavenCentralUrl(MavenDependency dependency) {
        return "https://repo1.maven.org/maven2/" +
            dependency.getGroupId().replace(".", "/") + "/" +
            dependency.getArtifactId()  + "/" + dependency.getVersion()  + "/" +
            dependency.getJarName();
    }

    private static void downloadDependency(MavenDependency dependency) {
        downloadFile(getMavenCentralUrl(dependency), dependency.getJarName());
    }

    private static void downloadFile(String url, String filename) {
        try {
            BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(LIBRARY_FOLDER + filename);
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
