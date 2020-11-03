package com.boomaa.note2jpg.integration.s3upload;

import java.util.Arrays;
import java.util.Scanner;

public class UserUpload {
    public static void main(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Must have credentials");
        } else {
            Connections.create(args[0], args[1]);
        }

        if (args.length == 2) {
            Scanner sc = new Scanner(System.in);
            System.out.println("Enter a filename (with extension) to upload");
            System.out.print(">> ");
            System.out.println(Arrays.toString(Connections.getAwsExecutor().uploadFile(sc.nextLine(), "", false)));
            sc.close();
        } else {
            for (int i = 2; i < args.length; i++) {
                System.out.println(Arrays.toString(Connections.getAwsExecutor().uploadFile(args[i], "", false)));
            }
        }
    }
}
