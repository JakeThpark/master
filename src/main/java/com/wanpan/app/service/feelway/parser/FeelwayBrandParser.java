package com.wanpan.app.service.feelway.parser;

import com.wanpan.app.dto.BrandDto;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

@Slf4j
public class FeelwayBrandParser {
    public static List<BrandDto> getBrands(String html) {
        Elements elements = Jsoup.parse(html)
                .selectFirst("[name=up_form] [name=brand_no]")
                .select("option");

        List<BrandDto> brands = new ArrayList<>();
        for (Element element : elements) {

            String brandIdValue = element.val();
            if (Objects.isNull(brandIdValue) || brandIdValue.isEmpty()) {
                continue;
            }
            int brandId = Integer.parseInt(brandIdValue);
            brands.add(new BrandDto(brandId, element.text()));
        }

        return brands;
    }

    private FeelwayBrandParser() {
    }
}
