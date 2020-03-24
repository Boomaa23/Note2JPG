package com.boomaa.note2jpg.dependencies;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Download {
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

        for (String dep : depsPrelimList) {
            String[] parts = dep.split(":");
            dependencyList.add(new MavenDependency(parts[0], parts[1], parts[3]));
        }

        for (MavenDependency dependency : dependencyList) {
            System.out.println("Downloading " + dependency);
            downloadDependency(dependency);
        }
    }

    private static String getMavenUrl(MavenDependency dependency) {
        return "https://repo1.maven.org/maven2/" +
            dependency.getGroupId().replace(".", "/") + "/" +
            dependency.getArtifactId()  + "/" + dependency.getVersion()  + "/" +
            getMavenJarName(dependency);
    }

    private static String getMavenJarName(MavenDependency dependency) {
        return dependency.getArtifactId() + "-" + dependency.getVersion() + ".jar";
    }

    private static void downloadDependency(MavenDependency dependency) {
        downloadFile(getMavenUrl(dependency), getMavenJarName(dependency));
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
