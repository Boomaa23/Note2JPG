package com.boomaa.note2jpg.uxutil;

import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.io.StreamTokenizer;

public class SwingInputField extends InputStream implements ActionListener {
    private final JTextField tf;
    private String str = null;
    private int pos = 0;

    public SwingInputField(JTextField textField) {
        this.tf = textField;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        str = tf.getText() + "\n";
        System.out.print(str);
        pos = 0;
        tf.setText("");
        synchronized (this) {
            this.notifyAll();
        }
    }

    @Override
    public int read() {
        if (str != null && pos == str.length()) {
            str = null;
            return StreamTokenizer.TT_EOF;
        }
        while (str == null || pos >= str.length()) {
            try {
                synchronized (this) {
                    this.wait();
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        return str.charAt(pos++);
    }
}