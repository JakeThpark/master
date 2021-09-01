package com.wanpan.app.config;

import com.wanpan.app.dto.reebonz.ReebonzImageFileData;
import lombok.Data;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class JsoupHttpClient {
    public static Connection.Response get(String url,
                                          Map<String, String> headers,
                                          Map<String, String> data,
                                          Map<String, String> cookies,
                                          boolean ignoreContentType,
                                          boolean ignoreHttpError,
                                          boolean followRedirect) throws IOException {
        Connection connection = Jsoup.connect(url)
                .timeout(60*1000)
                .method(Connection.Method.GET)
                .ignoreContentType(ignoreContentType)
                .ignoreHttpErrors(ignoreHttpError)
                .followRedirects(followRedirect);

        if(!ObjectUtils.isEmpty(headers)){
            connection.headers(headers);
        }

        if(!ObjectUtils.isEmpty(data)){
            connection.data(data);
        }

        if(!ObjectUtils.isEmpty(cookies)){
            connection.cookies(cookies);
        }

        return connection.execute();
    }

    public static Connection.Response post(String url,
                                          Map<String, String> headers,
                                          Map<String, String> data,
                                          String requestBody,
                                           Map<String, String> cookies,
                                          boolean ignoreContentType,
                                          boolean ignoreHttpError,
                                          boolean followRedirect) throws IOException {

        Connection connection = Jsoup.connect(url)
                .timeout(60*1000)
                .method(Connection.Method.POST)
                .ignoreContentType(ignoreContentType)
                .ignoreHttpErrors(ignoreHttpError)
                .followRedirects(followRedirect);

        if(!ObjectUtils.isEmpty(headers)){
            connection.headers(headers);
        }

        if(!ObjectUtils.isEmpty(data)){
            connection.data(data);
        }

        if(!StringUtils.isEmpty(requestBody)){
            connection.requestBody(requestBody);
        }

        if(!ObjectUtils.isEmpty(cookies)){
            connection.cookies(cookies);
        }

        return connection.execute();
    }

    public static Connection.Response post(String url,
                                           Map<String, String> headers,
                                           Map<String, String> data,
                                           List<FormDataForInputStream> formDataForInputStreamList,
                                           String requestBody,
                                           Map<String, String> cookies,
                                           boolean ignoreContentType,
                                           boolean ignoreHttpError,
                                           boolean followRedirect) throws IOException {

        Connection connection = Jsoup.connect(url)
                .timeout(60*1000)
                .method(Connection.Method.POST)
                .ignoreContentType(ignoreContentType)
                .ignoreHttpErrors(ignoreHttpError)
                .followRedirects(followRedirect);

        if(!ObjectUtils.isEmpty(headers)){
            connection.headers(headers);
        }

        if(!ObjectUtils.isEmpty(data)){
            connection.data(data);
        }

        if(!ObjectUtils.isEmpty(formDataForInputStreamList)){
            for (FormDataForInputStream formDataForInputStream : formDataForInputStreamList) {
                connection.data(formDataForInputStream.getKeyName(), formDataForInputStream.getFileNameWithExtension(), formDataForInputStream.getInputStream());
            }
        }

        if(!StringUtils.isEmpty(requestBody)){
            connection.requestBody(requestBody);
        }

        if(!ObjectUtils.isEmpty(cookies)){
            connection.cookies(cookies);
        }

        return connection.execute();
    }

    @Data
    public static class FormDataForInputStream{
        private String keyName;
        private String fileNameWithExtension;
        private InputStream inputStream;
    }
}
