package com.boomaa.note2jpg.uxutil;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;

public class NEOLoginUI {
    private static final int WIDTH = 225;
    private static final int HEIGHT = 125;
    private static final int IN_COLS = 10;
    private final JFrame frame;
    private final JTextField inUser;
    private final JPasswordField inPass;
    private boolean inputDone;

    public NEOLoginUI() {
        this.frame = new JFrame("NEO Login");
        this.inUser = new JTextField(IN_COLS);
        this.inPass = new JPasswordField(IN_COLS);
        this.inputDone = false;

        try {
            frame.setIconImage(ImageIO.read(new File("lib/note2jpg-icon.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        JButton btn = new JButton("Submit");
        btn.addActionListener((e) -> {
            if (!getUsername().isBlank() && !getPassword().isBlank()) {
                inputDone = true;
            }
        });
        Container content = frame.getContentPane();
        content.setLayout(new GridBagLayout());
        GBCPanelBuilder gbc = new GBCPanelBuilder(content)
                .setFill(GridBagConstraints.NONE)
                .setAnchor(GridBagConstraints.CENTER)
                .setInsets(new Insets(0, 5, 5, 5));
        gbc.clone().setY(0).build(new JLabel("Student ID"));
        gbc.clone().setY(1).build(new JLabel("Password"));
        gbc.clone().setX(1).setY(0).build(inUser);
        gbc.clone().setX(1).setY(1).build(inPass);
        gbc.clone().setX(0).setY(2).setWidth(2).build(btn);

        frame.getRootPane().setDefaultButton(btn);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        content.setSize(new Dimension(WIDTH, HEIGHT));
        frame.setSize(new Dimension(WIDTH, HEIGHT));
    }

    public void setVisible(boolean isVisible) {
        frame.setVisible(isVisible);
    }

    public void show() {
        frame.setVisible(true);
    }

    public void hide() {
        frame.setVisible(false);
    }

    public void destroy() {
        hide();
        frame.dispose();
    }

    public String getUsername() {
        return inUser.getText();
    }

    public String getPassword() {
        return new String(inPass.getPassword());
    }

    public boolean isInputDone() {
        return inputDone;
    }

    public synchronized void waitForInput() {
        while (!isInputDone()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
