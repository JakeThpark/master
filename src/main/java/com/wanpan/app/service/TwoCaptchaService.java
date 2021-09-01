package com.wanpan.app.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

@Slf4j
@Service
@AllArgsConstructor
public class TwoCaptchaService {
    private static final String API_KEY = "b545863b0f8956f2f2cc49ba7fce6249";
    private static final String DELIMITER = "\\|";

    public String getCaptcha(String imageUrl) throws IOException, InterruptedException {
        InputStream imageStream = getImageInputStream(imageUrl);
        String processId = requestProcessId(imageStream);
        if (!isValidResponse(processId)) {
            return null; // todo throw
        }

        processId = getResult(processId);
        for (int i = 0; i < 60; i++) {
            String resultResponse = requestCode(processId);
            if (isValidResponse(resultResponse)) {
                return getResult(resultResponse);
            }
            Thread.sleep(1000);
        }

        return null;
    }

    private InputStream getImageInputStream(String imageUrl) throws IOException {
        BufferedImage originalImage = ImageIO.read(new URL(imageUrl));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(originalImage, "PNG", outputStream);
        outputStream.flush();
        byte[] bytes = outputStream.toByteArray();
        outputStream.close();
        return new ByteArrayInputStream(bytes);
    }

    private String requestProcessId(InputStream imageStream) throws IOException {
        Connection.Response response = Jsoup.connect("https://2captcha.com/in.php")
                .data("key", API_KEY)
                .data("method", "post")
                .data("file", "test.png", imageStream)
                .userAgent("Mozilla")
                .method(Connection.Method.POST)
                .execute();
        // todo error 처리
        return response.body();
    }

    private String requestCode(String id) throws IOException {
        Connection.Response response = Jsoup.connect("https://2captcha.com/res.php")
                .data("key", API_KEY)
                .data("action", "get")
                .data("id", id)
                .userAgent("Mozilla")
                .method(Connection.Method.GET)
                .execute();
        // todo error 처리
        return response.body();
    }

    private boolean isValidResponse(String response) {
        return response.split(DELIMITER).length == 2;
    }

    private String getResult(String response) {
        return response.split(DELIMITER)[1];
    }
}
