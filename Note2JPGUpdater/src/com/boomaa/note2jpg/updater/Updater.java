package com.boomaa.note2jpg.updater;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Updater {
    private static JFrame FRAME;
    private static boolean OUTPUT_DONE = false;
    private static String DEPENDENCIES_URL = "https://raw.githubusercontent.com/Boomaa23/Note2JPG/master/src/main/resources/dependencies.conf";
    private static String LIBRARY_FOLDER = "lib/";
    private static List<MavenDependency> dependencyList = new ArrayList<>();

    static {
        FRAME = new JFrame("Note2JPG Updater");
        try {
            FRAME.setIconImage(ImageIO.read(new URL("https://www.gingerlabs.com/images/notability-logo.png"))
                    .getSubimage(0, 0, 104, 104).getScaledInstance(32, 32, Image.SCALE_SMOOTH));
        } catch (IOException e) {
            e.printStackTrace();
        }
        FRAME.setSize(400, 300);
        FRAME.setResizable(false);
        FRAME.setLocationRelativeTo(null);
        FRAME.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        var jt = new JTextArea();
        jt.setEditable(false);
        jt.setAutoscrolls(true);
        jt.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (OUTPUT_DONE) {
                    FRAME.dispose();
                }
            }
        });
        var scr = new JScrollPane(jt);
        scr.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scr.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        var taos = new PrintStream(new ConsolePrintOut(jt));
        System.setOut(taos);
        System.setErr(taos);
        FRAME.getContentPane().add(scr);
        FRAME.setVisible(true);
    }

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
        for (int i = 0; i < depsString.length(); i++) {
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

        if (!Arrays.asList(args).contains("--nogsdll64")
                && !new File(LIBRARY_FOLDER + "/gsdll64.dll").exists()) {
            System.out.println("Downloading gsdll64.dll");
            downloadFile("https://s3.amazonaws.com/s3.edu20.org/files/2796766/gsdll64.dll", "gsdll64.dll");
            downloadCounter++;
        }
        //TODO implement some sort of version checking
        if (!Arrays.asList(args).contains("--noappjar")) {
            System.out.println("Downloading Note2JPG.jar");
            downloadFile("https://raw.githubusercontent.com/Boomaa23/Note2JPG/master/Note2JPG.jar", "Note2JPG.jar", false);
            downloadCounter++;
        }
        if (Arrays.asList(args).contains("--launcher")) {
            System.out.println("Downloading Note2JPG.cmd");
            downloadFile("https://raw.githubusercontent.com/Boomaa23/Note2JPG/master/Note2JPG.cmd", "Note2JPG.cmd", false);
            downloadCounter++;
        }
        if (downloadCounter > 0) {
            System.out.println();
        }

        String folderSize = FileUtil.humanReadable(FileUtil.folderSize(new File(LIBRARY_FOLDER)) - originalFolderSize);
        System.out.println("Downloaded " + downloadCounter + " dependencies to " +
            "/" + LIBRARY_FOLDER + " (" + folderSize + ")");
        System.out.println("Press any key to quit...");
        OUTPUT_DONE = true;
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
        downloadFile(url, filename, true);
    }

    private static void downloadFile(String url, String filename, boolean useBase) {
        try {
            String location = filename;
            if (useBase) {
                location = LIBRARY_FOLDER + location;
            }
            BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(location);
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
