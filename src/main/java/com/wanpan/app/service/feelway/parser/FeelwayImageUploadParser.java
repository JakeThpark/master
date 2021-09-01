package com.wanpan.app.service.feelway.parser;

import com.wanpan.app.dto.feelway.FeelwayProduct;
import lombok.extern.slf4j.Slf4j;

import java.beans.Statement;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class FeelwayImageUploadParser {
    private static final String IMAGE_NAME_GROUP_NAME = "imageName";
    private static final String IMAGE_NUMBER_GROUP_NAME = "imageNumber";
    private static final Pattern IMAGE_PATH_PATTERN =
            Pattern.compile("g_photo(?<" + IMAGE_NUMBER_GROUP_NAME + ">[0-9]+)_imsi.value='(?<" +
                    IMAGE_NAME_GROUP_NAME + ">[a-zA-Z0-9\\/\\._]+)");


    public static FeelwayProduct getImages(String html) {
        String warrantyNumber = "7";
        Matcher matcher = IMAGE_PATH_PATTERN.matcher(html);
        FeelwayProduct feelwayProduct = new FeelwayProduct();


        while (matcher.find()) {
            String photoNumber = matcher.group(IMAGE_NUMBER_GROUP_NAME);
            String path = matcher.group(IMAGE_NAME_GROUP_NAME);
            if (photoNumber.equals(warrantyNumber)) {
                feelwayProduct.setWarrantyPhoto(path);
                continue;
            }

            try {
                Statement stmt =
                        new Statement(feelwayProduct, "setPhoto" + photoNumber, new Object[]{path});
                stmt.execute();

            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                log.error(e.toString() + sw.toString());
            }
        }
        return feelwayProduct;
    }
}
