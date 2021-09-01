package com.wanpan.app.service.feelway.util;

import org.jsoup.Connection;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FeelwayResponseConverter {
    private static final String CHARSET = "euc-kr";

    public static String convert(Connection.Response response) {
        return response.charset(CHARSET).body();
    }

    private FeelwayResponseConverter() {
    }

    public static String timeConvert(String time){
        return String.valueOf(LocalDate.now().getYear()) + "-" + time + ":00";
    }

    public static LocalDateTime timeConvertToLocalDateTime(String time){
        String datetime = String.valueOf(LocalDate.now().getYear()) + "-" + time + ":00";
        return convertStrToLocalDateTime(datetime);
    }

    public static LocalDateTime convertStrToLocalDateTime(String dateTimeStr){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(dateTimeStr, formatter);
    }

    public static LocalDateTime noticeTimeConvertToLocalDateTime(String dateTimeStr){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse("20"+dateTimeStr, formatter);
    }
}
