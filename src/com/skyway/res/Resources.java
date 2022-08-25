package com.skyway.res;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
/**
 * Класс для обращения к ресурсам внуртри Jar
 * */
public class Resources {

    public static InputStream getStream(String path) {
        return Resources.class.getResourceAsStream(path);
    }

    public static byte[] getBytes(String path) {
        try {
            InputStream inputStream = getStream(path);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            return bytes;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getString(String path) {
        byte[] bytes = getBytes(path);
        if (bytes != null)
            return new String(bytes);
        return null;
    }
}
