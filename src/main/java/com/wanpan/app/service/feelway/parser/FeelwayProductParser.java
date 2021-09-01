package com.wanpan.app.service.feelway.parser;

import com.wanpan.app.config.PatternExtractor;
import com.wanpan.app.dto.CategoryDto;
import com.wanpan.app.dto.feelway.FeelwayProductForCreate;
import com.wanpan.app.dto.feelway.FeelwayProductForUpdate;
import com.wanpan.app.dto.feelway.FeelwaySellingProduct;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class FeelwayProductParser {
    private static final String IMAGE_UPLOAD_ID_GROUP = "uploadId";
    private static final Pattern IMAGE_UPLOAD_ID_PATTERN = Pattern.compile(",'(?<uploadId>[0-9]+)',");
    private static final Pattern SUB_CATEGORY_NUMBER_PATTERN =
            Pattern.compile("sub_category_no\\[(?<parentId>[0-9]+)\\] = new Array\\((?<childId>.+)\\);");
    private static final Pattern SUB_CATEGORY_NAME_PATTERN = Pattern.compile(
            "sub_category_name\\[(?<parentId>[0-9]+)\\] = new Array\\((?<childName>.+)\\);");

    public static String getCaptchaImage(String html) {
        // todo 페이지 에러로 html 을 못가져 올 경우에 대비한 로직을 만들어두어야 한다.
        String imagePath = Jsoup.parse(html)
                .select("[name=up_form] > table")
                .get(10)
                .select("img")
                .attr("src");
        return "https://www.feelway.com/" + imagePath;
    }

    public static String getImageUploadId(String html) {
        String onclickScript = Jsoup.parse(html)
                .select("[name=photo_button]")
                .attr("onclick");

        Matcher matcher = IMAGE_UPLOAD_ID_PATTERN.matcher(onclickScript);
        if (matcher.find()) {
            return matcher.group(IMAGE_UPLOAD_ID_GROUP);
        }

        return null;
    }

    public static List<CategoryDto> getCategories(String html) {
        List<CategoryDto> categoryDtoList = getRootCategoryList(html);
        Map<String, List<String>> subCategoryIdMap = getSubCategoryIdMap(html);
        Map<String, List<String>> subCategoryNameMap = getSubCategoryNameMap(html);

        for (CategoryDto categoryDto : categoryDtoList) {
            String id = categoryDto.getId();
            List<String> childIdList = subCategoryIdMap.get(id);
            List<String> childNameList = subCategoryNameMap.get(id);
            List<CategoryDto> subCategoryDtoList = new ArrayList<>();
            for (int i = 0, size = childIdList.size(); i < size; i++) {
                CategoryDto subCategoryDto = new CategoryDto();
                subCategoryDto.setId(childIdList.get(i));
                subCategoryDto.setName(childNameList.get(i));
                subCategoryDto.setParentId(id);
                subCategoryDtoList.add(subCategoryDto);
            }
            categoryDto.setChild(subCategoryDtoList);
        }

        return categoryDtoList;
    }

    private static Map<String, List<String>> getSubCategoryIdMap(String html) {
        Matcher matcher = SUB_CATEGORY_NUMBER_PATTERN.matcher(html);

        Map<String, List<String>> subCategoryIdMap = new HashMap<>();
        while (matcher.find()) {
            String id = matcher.group("parentId");
            List<String> child = Arrays.asList(
                    matcher.group("childId").replace("'", "").split(","));
            subCategoryIdMap.put(id, child);

        }
        return subCategoryIdMap;
    }

    private static Map<String, List<String>> getSubCategoryNameMap(String html) {
        Matcher matcher = SUB_CATEGORY_NAME_PATTERN.matcher(html);

        Map<String, List<String>> subCategoryNameMap = new HashMap<>();
        while (matcher.find()) {
            String id = matcher.group("parentId");
            List<String> child = Arrays.asList(
                    matcher.group("childName").replace("'", "").split(","));
            subCategoryNameMap.put(id, child);
        }
        return subCategoryNameMap;
    }

    private static List<CategoryDto> getRootCategoryList(String html) {
        Elements options = Jsoup.parse(html)
                .select("[name=cate_no] > option");
        List<CategoryDto> categoryDtoList = new ArrayList<>();
        for (Element element : options) {
            String id = element.val();
            if (Objects.isNull(id) || id.isEmpty()) {
                continue;
            }

            CategoryDto parent = new CategoryDto();
            parent.setId(id);
            parent.setName(element.text());
            categoryDtoList.add(parent);
        }

        return categoryDtoList;
    }

    public static FeelwayProductForCreate getFeelwayProductForCreate(String html) {
        FeelwayProductForCreate feelwayProduct = new FeelwayProductForCreate();
        Document document = Jsoup.parse(html);
        feelwayProduct.setRandomId(document.select("[name=rd_no]").val());
        feelwayProduct.setUserId(document.select("[name=u_id]").val());
        feelwayProduct.setMoreUpPrice(document.select("[name=more_up_price]").val());
        feelwayProduct.setMaxMinusEMoney(document.select("[name=max_minus_emoney]").val());
        feelwayProduct.setCustomerServiceManager(document.select("[name=as_manager]").val());
        feelwayProduct.setAHd(document.select("[name=a_hd]").val());
        feelwayProduct.setBHd(document.select("[name=b_hd]").val());
        feelwayProduct.setEHd(document.select("[name=e_hd]").val());
        feelwayProduct.setCardMax(document.select("[name=card_max]").val());
        feelwayProduct.setCommissionPayer(document.select("[name=commission_payer]").val());
        feelwayProduct.setMoreUpPrice(document.select("[name=more_up_price]").val());
        feelwayProduct.setAutoRollIn(document.select("[name=auto_roll_in]").val());
        feelwayProduct.setUMoney(document.select("[name=u_money]").val());
        feelwayProduct.setNeedPrice(document.select("[name=need_price]").val());

        return feelwayProduct;
    }

    public static FeelwayProductForUpdate getFeelwayProductForUpdate(String html) {
        FeelwayProductForUpdate feelwayProduct = new FeelwayProductForUpdate();
        Document document = Jsoup.parse(html);
        feelwayProduct.setRandomId(document.select("[name=rd_no]").val());
        feelwayProduct.setUserId(document.select("[name=u_id]").val());
        feelwayProduct.setGoodsId(document.select("[name=g_no]").val());
        feelwayProduct.setMaxMinusEMoney(document.select("[name=max_minus_emoney]").val());
        feelwayProduct.setMoreUpPrice(document.select("[name=more_up_price]").val());
        feelwayProduct.setGoodsPriceBack(document.select("[name=g_price_back]").val());

        feelwayProduct.setCustomerServiceManager(document.select("[name=as_manager]").val());
        feelwayProduct.setAHd(document.select("[name=a_hd]").val());
        feelwayProduct.setBHd(document.select("[name=b_hd]").val());
        feelwayProduct.setEHd(document.select("[name=e_hd]").val());
        feelwayProduct.setCardMax(document.select("[name=card_max]").val());
        feelwayProduct.setCommissionPayer(document.select("[name=commission_payer]").val());
        feelwayProduct.setMoreUpPrice(document.select("[name=more_up_price]").val());
        feelwayProduct.setAutoRollIn(document.select("[name=auto_roll_in]").val());

        feelwayProduct.setBosangText(document.select("[name=bosang_text]").val());
        feelwayProduct.setBosangTextNow(document.select("[name=bosang_text_now]").val());
        feelwayProduct.setBrandNameCheck(document.select("[name=brand_name_check]").val());
        feelwayProduct.setBrandNoCheck(document.select("[name=brand_no_check]").val());

        int lastImageIndex = 10;
        while (lastImageIndex > 0) {
            Elements select = document.select("#g_photo" + lastImageIndex + "_div img");
            if (!select.isEmpty()) {
                break;
            }
            lastImageIndex--;
        }
        feelwayProduct.setUploadedImageCount(lastImageIndex);

        return feelwayProduct;
    }

    /**
     * 판매중 탭의 html가져와서 판매중 상품의 개수를 뽑아온다.
     * @param html
     * @return
     */
    public static int getSellingProductCount(String html) {
        String sellingProduct = Jsoup.parse(html)
                .select("label[for=check_radio_selling]")
                .get(0)
                .html();
        return Integer.parseInt(PatternExtractor.FEELWAY_SELLING_PRODUCT_COUNT.extract(sellingProduct,1));
    }

    /**
     * 해당 탭의 제일 마지막 상품 정보를 파싱한다.
     * @param html
     * @return
     */
    public static FeelwaySellingProduct getlatestSellingProduct(String html) {
        log.info("CALL getlatestSellingProduct");
        Elements elements = Jsoup.parse(html)
                .getElementsByClass("link2").first().getElementsByTag("tr");
        if(elements.size() > 4){
            //등록된 상품이 있다.
            Element brandSubjectElement = elements.get(3).getElementsByTag("td").get(1).getElementsByTag("a").get(0);

            FeelwaySellingProduct feelwaySellingProduct = new FeelwaySellingProduct();
            //상품번호(화면에는 NV4408182616라고 보이나 링크에 있는거는 4408182616 숫자값만 뽑아냄)
            String productNumber = PatternExtractor.FEELWAY_SELLING_PRODUCT_NUMBER.extract(brandSubjectElement.attr("href"),1);
            feelwaySellingProduct.setProductNumber(productNumber);

            //브랜드, 상품 제목
            Map<Integer,String> productInfo = PatternExtractor.FEELWAY_SELLING_PRODUCT_BRAND_SUBJECT.extractGroups(brandSubjectElement.html());
            feelwaySellingProduct.setBrand(productInfo.get(1));
            feelwaySellingProduct.setSubject(productInfo.get(2).trim());

            //현재상태
            String status = PatternExtractor.removeTag(elements.get(3).getElementsByTag("td").get(5).html()).trim();
            FeelwaySellingProduct.FeelwaySaleStatus feelwaySaleStatus = FeelwaySellingProduct.FeelwaySaleStatus.getByCode(status);

            if(feelwaySaleStatus == null){
                log.error("Status Parse Failed!! - productNumber:{}",productNumber);
                return null;
            }

            feelwaySellingProduct.setSaleStatus(FeelwaySellingProduct.FeelwaySaleStatus.convertToShopSaleStatus(feelwaySaleStatus));

            return feelwaySellingProduct;
        }else{
            //등록된 상품이 없다.
            log.info("Not Exist Product");
            return null;
        }
    }


}
