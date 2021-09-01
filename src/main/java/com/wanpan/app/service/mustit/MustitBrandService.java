package com.wanpan.app.service.mustit;

import com.wanpan.app.config.PatternExtractor;
import com.wanpan.app.config.gateway.MustitClient;
import com.wanpan.app.dto.BrandDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class MustitBrandService {
    private final MustitClient mustitClient;

    /*
     * 기본 Html 파싱 방식을 이용한 브랜드 리스트 페이지를 호출한다.
     */
    public List<BrandDto> getBrandListBySearchName(String shopToken, String searchName) {
        log.info("shopToken: {}", shopToken);
        //header
        HashMap<String, String> headers = new HashMap<>();
        headers.put("cookie", shopToken);
        headers.put("Referer", "https://mustit.co.kr/product/add01");
        //parameter
        HashMap<String, String> data = new HashMap<>();
        data.put("ajax_type", "add02");
        data.put("str", searchName);

        try {
            Connection.Response response = mustitClient.getBrands(headers, data);
            if (response.statusCode() == 200 || response.statusCode() == 302) {
                final int brandKeyGroup = 1;
                final int brandNameGroup = 2;
                List<BrandDto> brandList = new ArrayList<>();
                log.info("pattern:{}", PatternExtractor.MUSTIT_BRAND_CODE_NAME.getPattern().pattern());
                List<Map<Integer, String>> parsedBrandMapList = PatternExtractor.MUSTIT_BRAND_CODE_NAME
                        .extractAll(response.body(), Arrays.asList(brandKeyGroup, brandNameGroup));
                for (Map<Integer, String> brandGroupMap : parsedBrandMapList) {
                    brandList.add(new BrandDto(Long.parseLong(brandGroupMap.get(brandKeyGroup)),
                            brandGroupMap.get(brandNameGroup)));
                }
                log.info("brandList: {}", brandList);
                log.info("brandList Size: {}", brandList.size());
                //브랜드결과
                return brandList;
            }
        } catch (IOException e) {
            log.error("MustIt getToken Fail, IOException", e);
        } catch (NullPointerException e) {
            log.error("unexpected null data is arrived from MustIt", e);
        } catch (Exception e) {
            log.error("unexpected exception occurred during crawl and save MustIt data.", e);
        }

        return null;

    }
}
