package com.boomaa.note2jpg.updater;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Updater {
    private static JFrame MAIN_FRAME;
    private static JFrame CONFIG_FRAME;
    private static DefaultTableModel TABLE_MODEL;
    private static String DEPENDENCIES_URL = "https://raw.githubusercontent.com/Boomaa23/Note2JPG/master/src/main/resources/dependencies.conf";
    private static String LIBRARY_FOLDER = "lib/";
    private static List<MavenDependency> dependencyList = new ArrayList<>();

    static {
        MAIN_FRAME = new JFrame("Note2JPG Updater");
        try {
            MAIN_FRAME.setIconImage(ImageIO.read(new URL("https://raw.githubusercontent.com/Boomaa23/Note2JPG/master/note2jpg-icon.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        MAIN_FRAME.setSize(400, 300);
        MAIN_FRAME.setResizable(false);
        MAIN_FRAME.setLocationRelativeTo(null);
        MAIN_FRAME.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        var jt = new JTextArea();
        jt.setEditable(false);
        jt.setAutoscrolls(true);
        var scr = new JScrollPane(jt);
        scr.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scr.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        var taos = new PrintStream(new ConsolePrintOut(jt));
        System.setOut(taos);
        System.setErr(taos);
        MAIN_FRAME.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        MAIN_FRAME.getContentPane().add(scr);
        MAIN_FRAME.setVisible(true);
    }

    private static void setupConfigFrame() {
        CONFIG_FRAME = new JFrame("Note2JPG Config Options");
        TABLE_MODEL = new DefaultTableModel(new String[0][], new String[] { "Option", "Info", "Value" });
        String url = "https://raw.githubusercontent.com/Boomaa23/Note2JPG/master/README.md";
        String existingJson = null;
        try {
            existingJson = Files.readString(Paths.get(".", "config.json"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert existingJson != null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url)
                .openConnection().getInputStream(), StandardCharsets.UTF_8))) {
            int skipCtr = 0;
            String line = "";
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("|")) {
                    if (skipCtr < 2) {
                        skipCtr++;
                    } else {
                        int ioMidBar = line.indexOf('|', 1);
                        int ioLastBar = line.lastIndexOf('|');
                        String name = line.substring(2, ioMidBar).trim();
                        String msg = line.substring(ioLastBar + 1).trim();
                        if (name.contains("NEOUsername")) {
                            TABLE_MODEL.addRow(new Object[]{"NEOUsername", msg.substring(0, 19) + " (username)",
                                    searchJsonManual(existingJson, "NEOUsername")});
                            TABLE_MODEL.addRow(new Object[]{"NEOPassword", msg.substring(0, 19) + " (password)",
                                    searchJsonManual(existingJson, "NEOPassword")});
                        } else {
                            String prevValue = searchJsonManual(existingJson, name);
                            TABLE_MODEL.addRow(new Object[]{name, msg, prevValue});
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        JTable table = new JTable(TABLE_MODEL);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        setWidths(table, 0, 50, -1, 160);
        setWidths(table, 1, 50, -1, 285);
        setWidths(table, 2, 50, -1, 135);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JButton stopBtn = new JButton("Finish");
        stopBtn.addActionListener((e) -> {
            writeConfig();
            CONFIG_FRAME.dispose();
            MAIN_FRAME.dispose();
        });
        CONFIG_FRAME.setSize(new Dimension(600, 500));
        CONFIG_FRAME.setLocationRelativeTo(null);
        CONFIG_FRAME.getContentPane().setLayout(new BorderLayout());
        CONFIG_FRAME.getContentPane().add(scroll, BorderLayout.NORTH);
        CONFIG_FRAME.getContentPane().add(stopBtn, BorderLayout.SOUTH);
        CONFIG_FRAME.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private static void setWidths(JTable table, int index, int min, int max, int preferred) {
        TableColumn col = table.getColumnModel().getColumn(index);
        if (min != -1) {
            col.setMinWidth(min);
        }
        if (max != -1) {
            col.setMaxWidth(max);
        }
        if (preferred != -1) {
            col.setPreferredWidth(preferred);
        }
    }

    private static void writeConfig() {
        try {
            Path configPath = Paths.get(".", "config.json");
            StringBuilder sb = new StringBuilder(Files.readString(configPath));
            for (int r = 0; r < TABLE_MODEL.getRowCount(); r++) {
                String name = String.valueOf(TABLE_MODEL.getValueAt(r, 0));
                String value = String.valueOf(TABLE_MODEL.getValueAt(r, 2));
                int ioStart = sb.indexOf(name) + name.length() + 4;
                sb.replace(ioStart, sb.indexOf("\"", ioStart), value);
            }
            Files.writeString(configPath, sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String searchJsonManual(String json, String search) {
        int ioStart = json.indexOf(search) + search.length() + 4;
        int ioEnd = json.indexOf('"', ioStart);
        return json.substring(ioStart, ioEnd);
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
                && !new File("lib/gsdll64.dll").exists()) {
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
        if (!Arrays.asList(args).contains("--noconfig")
                && !new File("config.json").exists()) {
            System.out.println("Downloading config.json");
            downloadFile("https://raw.githubusercontent.com/Boomaa23/Note2JPG/master/config.json", "config.json", false);
            downloadCounter++;
        }
        if (!Arrays.asList(args).contains("--noappicon")
                && !new File("lib/note2jpg-icon.png").exists()) {
            System.out.println("Downloading note2jpg-icon.png");
            downloadFile("https://raw.githubusercontent.com/Boomaa23/Note2JPG/master/note2jpg-icon.png", "note2jpg-icon.png");
            downloadCounter++;
        }
        if (downloadCounter > 0) {
            System.out.println();
        }

        String folderSize = FileUtil.humanReadable(FileUtil.folderSize(new File(LIBRARY_FOLDER)) - originalFolderSize);
        System.out.println("Downloaded " + downloadCounter + " dependencies to " +
            "/" + LIBRARY_FOLDER + " (" + folderSize + ")");
        MAIN_FRAME.setVisible(false);
        setupConfigFrame();
        CONFIG_FRAME.setIconImage(ImageIO.read(new File("lib/note2jpg-icon.png")));
        CONFIG_FRAME.setVisible(true);
        JOptionPane.showMessageDialog(null,
                "Quickstart users should set NEOUsername and NEOPassword to their NEO credentials. \n" +
                "UseAWS and UseDriveDownload should be set to \"true\"", "Quickstart Config", JOptionPane.INFORMATION_MESSAGE);
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
