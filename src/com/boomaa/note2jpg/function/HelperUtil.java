package com.boomaa.note2jpg.function;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class HelperUtil extends Main {
    public static String fileToString(String filePath) {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder.append(sCurrentLine);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

    public static float[] makePositive(float[] input) {
        for (int i = 0;i < input.length;i++) {
            if (input[i] < 0) {
                input[i] *= -1;
            }
        }
        return input;
    }
}
