package com.ontrac.warehouse.Utilities;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileSystem {

    public static void writeFile(String path, String content) throws IOException {
        File file = new File(path);
        writeFile(file, content, false);
    }

    public static void writeFile(String path, String content, boolean append) throws IOException {
        File file = new File(path);
        writeFile(file, content, false);
    }

    public static void writeFile(File file, String content) throws IOException {
        writeFile(file, content, false);
    }

    public static void writeFile(File file, String content, boolean append) throws IOException {
        try {
            FileWriter writer = new FileWriter(file, append);
            writer.write(content);
            writer.flush();
            writer.close();

        } catch (Exception e) {
            throw e;
        }
    }

    public static String readFile(File file) {
        String result = null;

        try(BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }

            result = sb.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

}
