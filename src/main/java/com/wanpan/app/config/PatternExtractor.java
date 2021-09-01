package com.wanpan.app.config;

import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class PatternExtractor {
    //FeelWay
    public static final PatternExtractor PHP_SESSION = new PatternExtractor("PHPSESSID=([a-zA-Z0-9]+)");
    public static final PatternExtractor FEELWAY_QUESTION_BRAND_PRODUCT = new PatternExtractor("<font color=\"#003366\">\\s*(.*)\\s*<\\/font><");
    public static final PatternExtractor FEELWAY_QUESTION_FIELDS = new PatternExtractor(
            "\\(([0-9-:\\s]+),\\s*([\\w]+)(\\)|<.*?>)[\\w\\W]+'([0-9]+)',\\s*'([0-9]+)'[\\w\\W]+>\\s*([\\w\\W]+)"
    );
    public static final PatternExtractor FEELWAY_ANSWER_FIELDS = new PatternExtractor(
            "\\(([0-9-:\\s]+),\\s*([\\w]+)\\)[\\w\\W]+[\\w\\W]+>\\s*([\\w\\W]+)\\s*"
    );
    public static final PatternExtractor FEELWAY_POST_ANSWER_RESULT = new PatternExtractor("alert\\('(.+)'\\)");
    public static final PatternExtractor FEELWAY_POST_DELIVERY_UPDATE_ANSWER_RESULT = new PatternExtractor("alert\\(['\"](.+)['\"]\\)");
    //FeelWay-Order
    public static final PatternExtractor FEELWAY_ORDER_PRODUCT_NUMBER = new PatternExtractor("popup_adress\\('(?<orderNumber>[0-9]+)'\\s*,\\s*'(?<productNumber>[0-9]+)'\\)");
    public static final PatternExtractor FEELWAY_ORDER_STATUS_DATE = new PatternExtractor("<strong>(.*)<\\/strong>\\s*\\((.*)\\)");
    public static final PatternExtractor FEELWAY_ORDER_BRAND_PRODUCT_NAME = new PatternExtractor("(.*)<a.*>(.*)<\\/a>");
    public static final PatternExtractor FEELWAY_ORDER_PRICE = new PatternExtractor("<strong>\\s*([0-9,]+)\\s*<\\/strong>");
    public static final PatternExtractor FEELWAY_ORDER_BUYER = new PatternExtractor("(.*?)\\s([a-zA-Z0-9_-]+)\\s<.*");
    public static final PatternExtractor FEELWAY_ORDER_BUYER_PHONE = new PatternExtractor("\\(([0-9\\s]+)*[\\s\\/]+([0-9\\s]+)*\\)");
    public static final PatternExtractor FEELWAY_ORDER_DELIVERY_ADDRESS = new PatternExtractor("\\s*우편번호\\s*\\(([0-9-]+)\\)\\s*,\\s*(.*)<input");
    public static final PatternExtractor FEELWAY_ORDER_DELIVERY_INFO_REAL = new PatternExtractor("(.*)\\s*<a.*>\\s*([0-9a-zA-Z]+)\\s*<\\/a>(.*)");
    public static final PatternExtractor FEELWAY_ORDER_DELIVERY_INFO_DIRECT_INPUT = new PatternExtractor("(.*)\\s(.*)\\s*<br>(.*)");
    public static final PatternExtractor FEELWAY_ORDER_RETURN_DELIVERY = new PatternExtractor("(.*)\\s*<a.*>\\s*([0-9a-zA-Z]+)\\s*<\\/a>");
    public static final PatternExtractor FEELWAY_ORDER_OPTION = new PatternExtractor("사이즈\\/컬러\\s:\\s(.*)");
    public static final PatternExtractor FEELWAY_ORDER_REQUIREMENT = new PatternExtractor("요구사항\\s:\\s(.*)");
    public static final PatternExtractor FEELWAY_ORDER_BUYER_VOTE = new PatternExtractor("<font.*>(.*)<\\/font>\\s*(.*)<br>\\s*<font.*>(.*)<\\/font>(.*)&nbsp[;]*&nbsp[;]*<input");
    public static final PatternExtractor FEELWAY_ORDER_CALCULATE_DATE = new PatternExtractor("<font.*>(.*)<\\/font>\\s*(.*)");
    public static final PatternExtractor FEELWAY_ORDER_CALCULATE_AMOUNT = new PatternExtractor("([0-9,]+)\\s*<");
//    public static final PatternExtractor FEELWAY_ORDER_CONVERSATION_NUMBER = new PatternExtractor("'([0-9]+)'");
    public static final PatternExtractor FEELWAY_ORDER_CONVERSATION_DATE_ID = new PatternExtractor("\\(([0-9-]+?\\s[0-9:]+?)\\s([0-9a-zA-Z]+)\\)");
    public static final PatternExtractor FEELWAY_ORDER_CONVERSATION_CONTENT = new PatternExtractor("\\[(판매자|구매자)\\]\\s*(.*)\\s(&nbsp;)*\\(.*\\)");
    //Date와 메세지를 한번에
    public static final PatternExtractor FEELWAY_ORDER_CONVERSATION_INFO_ID = new PatternExtractor("\\[(판매자|구매자)\\]<\\/font>\\s*(.*)\\s(&nbsp;)*<font\\s[a-z=\"#0-9]+>\\(([0-9-]+?\\s[0-9:]+?)\\s([0-9a-zA-Z]+)\\)");

    public static final PatternExtractor FEELWAY_ORDER_DATE = new PatternExtractor("([0-9-:\\/\\s]+)-\\s*(주문|입금)");
    public static final PatternExtractor FEELWAY_NOTICE_ID = new PatternExtractor("main_no=([0-9]+)");

    public static final PatternExtractor FEELWAY_SELLING_PRODUCT_COUNT = new PatternExtractor("\\(([0-9]+)\\)");
    public static final PatternExtractor FEELWAY_SELLING_PRODUCT_NUMBER = new PatternExtractor("([0-9]+)");
    public static final PatternExtractor FEELWAY_SELLING_PRODUCT_BRAND_SUBJECT = new PatternExtractor("\\[(.*)\\]<br>\\s*(.*)");

    public static final PatternExtractor FEELWAY_ORDER_TITLE_COURIER = new PatternExtractor("\\s*배송정보\\(실제\\)|\\s*배송정보");
    public static final PatternExtractor FEELWAY_ORDER_TITLE_ADDRESS = new PatternExtractor("\\s*배송지\\s*주소");

    //MustIt
    public static final PatternExtractor MUSTIT_SESSIONID_FULL_STR = new PatternExtractor("__f_i_ss_d=([a-zA-Z0-9]+);");
//    public static final PatternExtractor MUSTIT_PHPSESSIONID_FULL_STR = new PatternExtractor("__f_i_ss_d=([a-zA-Z0-9]+);");
//    public static final PatternExtractor MUSTIT_AWSALB_FULL_STR = new PatternExtractor("AWSALB=([a-zA-Z0-9+\\/]+);");
//    public static final PatternExtractor MUSTIT_AWSALBCORS_FULL_STR = new PatternExtractor("AWSALBCORS=([a-zA-Z0-9+\\/]+);");
    public static final PatternExtractor MUSTIT_BRAND_CODE_NAME = new PatternExtractor(
            "change_choice\\([a-zA-Z0-9']+\\s*,\\s*[a-zA-Z0-9']+\\s*,\\s*'([0-9]+)'\\s*,\\s*[a-zA-Z0-9\\\\=\"_,()\\s';\\/]+>([a-zA-Z0-9\\s\\\\\\/]+)<\\\\\\/span>"
            );
//    public static final PatternExtractor MUSTIT_SUB_CATEGORY_HTML = new PatternExtractor(
//            "getSubCateHtml\\(\\s*[a-zA-Z0-9]+\\s*,\\s*'([a-zA-Z0-9]+)',\\s*'([a-zA-Z0-9,]+)'\\s*\\)[;\\\\\"]+>\\s*([ㄱ-ㅎㅏ-ㅣ가-힣\\/]+)\\s*<"
//    );
    public static final PatternExtractor MUSTIT_SUB_CATEGORY_HTML = new PatternExtractor(
            "getSubCateHtml\\(\\s*[a-zA-Z0-9]+\\s*,\\s*'([a-zA-Z0-9]+)',\\s*'([a-zA-Z0-9,]+)'\\s*\\)[;\\\\\"]+>\\s*([a-zA-Z0-9ㄱ-ㅎㅏ-ㅣ가-힣\\/\\s]+)\\s*<"
    );
    public static final PatternExtractor MUSTIT_CATEGORY_FILTER_HTML = new PatternExtractor("fnFilterSelect\\(([0-9]+)");
    public static final PatternExtractor MUSTIT_QNA_NUMBER = new PatternExtractor("([0-9]+)'");
    public static final PatternExtractor MUSTIT_POST_ANSWER_RESULT = new PatternExtractor("alert\\('(.+)'\\)");

    public static final PatternExtractor MUSTIT_NOTICE_ID = new PatternExtractor("number=([0-9]+)");

    //Reebonz
    public static final PatternExtractor REEBONZ_PRODUCT_NUMBER = new PatternExtractor("[0-9]+'");
    public static final PatternExtractor REEBONZ_PRODUCT_NAME = new PatternExtractor("(.*)<span>.*<\\/span>");
    public static final PatternExtractor REEBONZ_BUYER_NAME_PHONE = new PatternExtractor("(.*)\\((.*)\\)");
    public static final PatternExtractor REEBONZ_OPTION_NAME_AMOUNT = new PatternExtractor("(.*)\\sX\\s([0-9]+)개");
    public static final PatternExtractor REEBONZ_OPTION_AMOUNT_INFO = new PatternExtractor("((.*)\\s*\\|\\s*(.*))\\sX\\s([0-9]+)개");
    public static final PatternExtractor REEBONZ_TRACKING_INFO = new PatternExtractor("(.*)\\s*<br>\\s*\\(\\/(.*)\\)");

    public static final PatternExtractor REEBONZ_EXCEL_ORDER_NUMBER = new PatternExtractor("([A-Za-z0-9]+)\\s*\\(([0-9]+)\\)");
    public static final PatternExtractor REEBONZ_EXCEL_BUYER_INFO = new PatternExtractor("([ㄱ-ㅎㅏ-ㅣ가-힣A-Za-z]+).*\\(([0-9-]+)\\)");
    public static final PatternExtractor REEBONZ_EXCEL_ADDRESS_INFO = new PatternExtractor("\\[([0-9]+)\\]\\s(.*)");
    public static final PatternExtractor REEBONZ_EXCEL_PRODUCT_NAME = new PatternExtractor("(.*)\\[옵션.*\\]");

    public static final PatternExtractor REEBONZ_NOTICE_ID = new PatternExtractor("title_([0-9]+)");



    private final Pattern pattern;

    public PatternExtractor(final String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    public List<Map<Integer,String>> extractAll(final String from, final List<Integer> groups) {
        List<Map<Integer,String>> matcherGroupList = new ArrayList<>();
        Matcher matcher = this.pattern.matcher(from);

        while(matcher.find()){
            Map<Integer,String> dataOfEachGroup = new HashMap<>();
            for(Integer group: groups){
                dataOfEachGroup.put(group, matcher.group(group));
            }
            matcherGroupList.add(dataOfEachGroup);
        }
        return matcherGroupList;
    }

    public Map<Integer,String> extractGroups(final String from) {
        Map<Integer,String> matcherDataMap = new HashMap<>();
        Matcher matcher = this.pattern.matcher(from);

        if(matcher.find()){
            for(int i=0; i <= matcher.groupCount() ;i++){
                matcherDataMap.put(i, matcher.group(i));
            }
        }
        return matcherDataMap;
    }

    public String extract(final String from) {
        Matcher matcher = this.pattern.matcher(from);
        return matcher.find() ? matcher.group() : null;
    }

    public String extract(final String from, final int group) {
        Matcher matcher = this.pattern.matcher(from);
        return matcher.find() ? matcher.group(group) : null;
    }

    public String extractDefaultValue(final String from, final int group, final String defaultValue) {
        String s = this.extract(from, group);
        return StringUtils.isEmpty(s) ? defaultValue : s;
    }

    public Integer extractInteger(final String from, final int group) {
        return Integer.parseInt(this.extract(from, group));
    }

    public Integer extractIntegerDefaultValue(final String from, final int group, final Integer defaultValue) {
        String s = this.extract(from, group);
        return StringUtils.isEmpty(s) ? defaultValue : Integer.parseInt(s);
    }

    public Float extractFloat(final String from, final int group) {
        return Float.parseFloat(this.extract(from, group));
    }

    public Float extractFloatDefaultValue(final String from, final int group, final Float defaultValue) {
        String s = this.extract(from, group);
        return StringUtils.isEmpty(s) ? defaultValue : Float.parseFloat(s);
    }

    public Double extractDouble(final String from, final int group) {
        return Double.parseDouble(this.extract(from, group));
    }

    public Double extractDoubleDefaultValue(final String from, final int group, final Double defaultValue) {
        String s = this.extract(from, group);
        return StringUtils.isEmpty(s) ? defaultValue : Double.parseDouble(s);
    }

    /**
     * 모든 HTML 태그를 제거하고 반환한다.
     *
     * @param html
     * @throws Exception
     */
    public static String removeTag(String html) {
        return html.replaceAll("<[^>]*>", "");
    }
}
