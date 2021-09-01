package com.wanpan.app.service.mustit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanpan.app.config.PatternExtractor;
import com.wanpan.app.config.gateway.MustitClient;
import com.wanpan.app.dto.ShopCategoryDto;
import com.wanpan.app.dto.mustit.MustitCategory;
import com.wanpan.app.dto.mustit.MustitHeadCategoryResponse;
import com.wanpan.app.dto.mustit.MustitSubCategoryResponse;
import com.wanpan.app.entity.NotificationType;
import com.wanpan.app.entity.Shop;
import com.wanpan.app.entity.ShopAccount;
import com.wanpan.app.entity.ShopCategory;
import com.wanpan.app.repository.NotificationTypeRepository;
import com.wanpan.app.repository.ShopAccountRepository;
import com.wanpan.app.repository.ShopCategoryRepository;
import com.wanpan.app.service.ShopAccountService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class MustitCategoryService {
    private final ObjectMapper objectMapper;
    private final ShopAccountService shopAccountService;
    private final MustitClient mustitClient;
    private final ShopCategoryRepository shopCategoryRepository;
    private final ShopAccountRepository shopAccountRepository;
    private final NotificationTypeRepository notificationTypeRepository;

    /**
     * Json 파일로부터 쇼핑몰 카테고리 데이터를 업데이트한다.
     */
    public Map<String,Long>  getNotificationTypeFromJsonFile() throws IOException {
        List<NotificationType> notificationTypeList = notificationTypeRepository.findAll();
        Map<String,Long> noticationTypeMap = new HashMap<>();
        String pathName = "./category-mustit-notification-type.json";
        JsonNode shopCategoryList = objectMapper.readTree(new File(pathName));
        for(JsonNode jsonNode :shopCategoryList){
            Long notificationId = null;
            for(NotificationType notificationType : notificationTypeList){
                if(notificationType.getName().equals(jsonNode.get("notification_type").textValue())){
                    notificationId = notificationType.getId();
                    break;
                }
            }
            noticationTypeMap.put(jsonNode.get("category_name").textValue(),notificationId);
        }
        return noticationTypeMap;
    }
    /*
     * 머스트 잇 카테고리 전부를 가져와서 입력한다.
     */
    public Boolean createCategories(long shopAccountId) throws IOException {
        Map<String,Long> notificationTypeMap = getNotificationTypeFromJsonFile();

        Optional<ShopAccount> shopAccount = shopAccountRepository.findById(shopAccountId);
        if(shopAccount.isEmpty()){
            return false;
        }
        String shopId = shopAccount.get().getShop().getId();
        for(MustitClient.HeadCategoryType headCategoryType : MustitClient.HeadCategoryType.values()) {
//            if(headCategoryType != MustitClient.HeadCategoryType.KIDS){
//                continue;
//            }
            //main 4개 + Share 1개의 카테고리를 저장한다.(총 5개)
            ShopCategory findShopCategory = shopCategoryRepository.findByShopIdAndNameAndParentId(shopId ,headCategoryType.name(), null);
            if (ObjectUtils.isEmpty(findShopCategory)) {
                ShopCategory shopCategory = new ShopCategory();
                shopCategory.setShopId(shopId);
                shopCategory.setName(headCategoryType.name());
                shopCategory.setShopCategoryCode(headCategoryType.getCode());
                shopCategory.setParentId(null);
                shopCategory.setDescription(headCategoryType.name());
                shopCategory.setNotificationTypeId(notificationTypeMap.get(headCategoryType.name()));


                findShopCategory = shopCategoryRepository.save(shopCategory);
            }
            //모든 category Json 가져오기
            List<MustitCategory> allMustitCategory = getAllCategoryListByHeadType(shopAccountId, headCategoryType);
            //가져온 Json으로 모든 카테고리 DB저장하기
            subRecursiveProcess(shopId, allMustitCategory, findShopCategory, notificationTypeMap);
        }

        return true;
    }

    private void subRecursiveProcess(String shopId, List<MustitCategory> mustitCategoryList, ShopCategory parentShopCategory, Map<String,Long> notificationTypeMap){
        for (MustitCategory mustitSubCategory : mustitCategoryList) {
            //중복체크
            ShopCategory findShopCategory = shopCategoryRepository.findByShopIdAndNameAndParentId(shopId, mustitSubCategory.getName(), parentShopCategory.getId());
            if (ObjectUtils.isEmpty(findShopCategory)) {
                ShopCategory shopCategory = new ShopCategory();
                shopCategory.setShopId(parentShopCategory.getShopId());
                shopCategory.setName(mustitSubCategory.getName());
                shopCategory.setShopCategoryCode(mustitSubCategory.getCode());
                shopCategory.setParentId(parentShopCategory.getId());
                shopCategory.setDescription(mustitSubCategory.getDescription());
                if(notificationTypeMap.get(mustitSubCategory.getName()) == null){
                    shopCategory.setNotificationTypeId(parentShopCategory.getNotificationTypeId());
                }else{
                    shopCategory.setNotificationTypeId(notificationTypeMap.get(mustitSubCategory.getName()));
                }

                findShopCategory = shopCategoryRepository.save(shopCategory);
            }

            //하위 작업이 필요한지 확인
            if(!ObjectUtils.isEmpty(mustitSubCategory.getSubCategories())){
                subRecursiveProcess(shopId, mustitSubCategory.getSubCategories(), findShopCategory, notificationTypeMap);
            }
        }
    }

    /*
     * Category Head Json을 읽어온다.
     */
    public List<MustitCategory> getAllCategoryListByHeadType(long shopAccountId, MustitClient.HeadCategoryType headCategoryType ) {
        try {
            //shopAccountId를 가지고 로그인을 수행하여 Token을 받아온다
            String shopToken = shopAccountService.getTokenByShopAccountId(shopAccountId);
            log.info("shopToken: {}", shopToken);
            //header
            HashMap<String, String> headers = new HashMap<>();
            headers.put("cookie", shopToken);
            headers.put("Referer", "https://mustit.co.kr/product/add02");
            //form data (header:W,M,K,L)
            HashMap<String, String> formData = new HashMap<>();
            formData.put("header", headCategoryType.getCode());

            try{
                Connection.Response response = mustitClient.getHeadCategory(headers, formData, null);
                if(response.statusCode() == 200 || response.statusCode() == 302) {
                    List<MustitCategory> mustitCategoryList = new ArrayList<>();
                    List<MustitHeadCategoryResponse> mustitHeadCategoryResponseList = objectMapper.readValue(response.body(), new TypeReference<>(){});
                    log.info("mustitHeadCategoryList: {}", mustitHeadCategoryResponseList);
                    for(MustitHeadCategoryResponse mustitHeadCategoryResponse : mustitHeadCategoryResponseList){
                        MustitCategory mustitCategory = new MustitCategory(
                                mustitHeadCategoryResponse.getThread(),
                                mustitHeadCategoryResponse.getTitle(),
                                null,
                                mustitHeadCategoryResponse.getHeaderCategory());
                        mustitCategory.setDescription(mustitHeadCategoryResponse.getTitleEn());
                        //해당 카테고리의 Sub를 구성한다.
                        List<MustitCategory> mustitSubCategoryList = getAllSubCategoryListByHeadAndFlag(shopToken, mustitCategory.getCode(), headCategoryType);
                        mustitCategory.setSubCategories(mustitSubCategoryList);
                        mustitCategoryList.add(mustitCategory);
                    }
                    //카테고리 결과
                    return mustitCategoryList;
                }
            } catch (IOException e) {
                log.error("MustIt getToken Fail, IOException", e);
            } catch (NullPointerException e) {
                log.error("unexpected null data is arrived from MustIt", e);
            } catch (Exception e) {
                log.error("unexpected exception occurred during crawl and save MustIt data.", e);
            }

            return null;
        }catch(Exception e){
            log.error("Fail~",e);
            return null;
        }

    }

    /*
     * Category Sub Json을 읽어온다.
     */
    public List<MustitCategory> getAllSubCategoryListByHeadAndFlag(String shopToken, String parentCategory, MustitClient.HeadCategoryType headCategoryType ) {
        log.info("Call getAllSubCategoryListByHeadAndFlag parentCategory:{}", parentCategory);
        try {
            log.info("shopToken: {}", shopToken);
            //하나의 ID에서 콜수의 제한으로 인해 delay를 설정한다.
            Thread.sleep(2000);
            //header
            HashMap<String, String> headers = new HashMap<>();
            headers.put("cookie", shopToken);
            headers.put("Referer", "https://mustit.co.kr/product/add02");
            //form data (header:W,M,K,L)
            HashMap<String, String> formData = new HashMap<>();
            formData.put("category", parentCategory);
            formData.put("flag", headCategoryType.getCode());

            try{
                Connection.Response response = mustitClient.getSubCategory(headers, formData, null);
                if(response.statusCode() == 200 || response.statusCode() == 302) {
                    log.info("response.body(): {}",response.body());
                    MustitSubCategoryResponse mustitSubCategoryResponse = objectMapper.readValue(response.body(), MustitSubCategoryResponse.class);
                    log.info("parentCategory:{}, mustitSubCategory: {}", parentCategory, mustitSubCategoryResponse);
                    //sub category 결과가 비어있으면 null을 리턴한다.
                    if(StringUtils.isEmpty(mustitSubCategoryResponse.getCategoryHtml())){
                        return null;
                    }else {
                        log.info("Process.parentCategory: {}",parentCategory);
                        //카테고리결과
                        final int categoryKeyGroup = 1;
                        final int categoryHeaderCategoryGroup = 2;
                        final int categoryNameGroup = 3;
                        List<MustitCategory> mustitCategoryList = new ArrayList<>();
                        List<Map<Integer, String>> parsedSubCategoryMapList = PatternExtractor.MUSTIT_SUB_CATEGORY_HTML.extractAll(mustitSubCategoryResponse.getCategoryHtml(), Arrays.asList(categoryKeyGroup, categoryHeaderCategoryGroup, categoryNameGroup));
                        log.info("{}", parsedSubCategoryMapList);
                        for (Map<Integer, String> categoryGroupMap : parsedSubCategoryMapList) {
                            MustitCategory mustitCategory = new MustitCategory(categoryGroupMap.get(categoryKeyGroup), categoryGroupMap.get(categoryNameGroup), parentCategory, categoryGroupMap.get(categoryHeaderCategoryGroup));
                            mustitCategory.setSubCategories(getAllSubCategoryListByHeadAndFlag(shopToken, mustitCategory.getCode(), headCategoryType));
                            mustitCategoryList.add(mustitCategory);
                        }
                        log.info("parentCategory:{}, mustitCategoryList:{}", parentCategory,mustitCategoryList);

                        return mustitCategoryList;
                    }

                }
            } catch (IOException e) {
                log.error("MustIt getCategory Fail, IOException", e);
            } catch (NullPointerException e) {
                log.error("unexpected null data is arrived from MustIt", e);
            } catch (Exception e) {
                log.error("unexpected exception occurred during crawl and save MustIt data.", e);
            }

            return null;
        }catch(Exception e){
            log.error("Fail~",e);
            return null;
        }

    }


    /*
     * Category Head Json을 읽어온다.
     */
    public List<MustitCategory> getHeadCategoryListByHeadType(long shopAccountId, MustitClient.HeadCategoryType headCategoryType ) {
        try {
            //TODO: shopAccountId를 가지고 Token을 받아온다
            String shopToken = shopAccountService.getTokenByShopAccountId(shopAccountId);
            log.info("shopToken: {}", shopToken);
            //header
            HashMap<String, String> headers = new HashMap<>();
            headers.put("cookie", shopToken);
            headers.put("Referer", "https://mustit.co.kr/product/add02");
            //form data (header:W,M,K,L)
            HashMap<String, String> formData = new HashMap<>();
            formData.put("header", headCategoryType.getCode());

            try{
                Connection.Response response = mustitClient.getHeadCategory(headers, formData, null);
                if(response.statusCode() == 200 || response.statusCode() == 302) {
                    List<MustitCategory> mustitCategoryList = new ArrayList<>();
                    List<MustitHeadCategoryResponse> mustitHeadCategoryResponseList = objectMapper.readValue(response.body(), new TypeReference<>(){});
                    log.info("mustitHeadCategoryList: {}", mustitHeadCategoryResponseList);
                    for(MustitHeadCategoryResponse mustitHeadCategoryResponse : mustitHeadCategoryResponseList){
                        MustitCategory mustitCategory = new MustitCategory(
                                mustitHeadCategoryResponse.getThread(),
                                mustitHeadCategoryResponse.getTitle(),
                                null,
                                mustitHeadCategoryResponse.getHeaderCategory());
                        mustitCategory.setDescription(mustitHeadCategoryResponse.getTitleEn());
                        mustitCategoryList.add(mustitCategory);
                    }
                    //브랜드결과
                    return mustitCategoryList;
                }
            } catch (IOException e) {
                log.error("MustIt getToken Fail, IOException", e);
            } catch (NullPointerException e) {
                log.error("unexpected null data is arrived from MustIt", e);
            } catch (Exception e) {
                log.error("unexpected exception occurred during crawl and save MustIt data.", e);
            }

            return null;
        }catch(Exception e){
            log.error("Fail~",e);
            return null;
        }

    }


    /*
     * Category Sub Json을 읽어온다.
     */
    public List<MustitCategory> getSubCategoryListByHeadAndFlag(long shopAccountId, String parentCategory, MustitClient.HeadCategoryType headCategoryType ) {
        try {
            //TODO: shopAccountId를 가지고 Token을 받아온다
            String shopToken = shopAccountService.getTokenByShopAccountId(shopAccountId);
            log.info("shopToken: {}", shopToken);
            //header
            HashMap<String, String> headers = new HashMap<>();
            headers.put("cookie", shopToken);
            headers.put("Referer", "https://mustit.co.kr/product/add02");
            //form data (header:W,M,K,L)
            HashMap<String, String> formData = new HashMap<>();
            formData.put("category", parentCategory);
            formData.put("flag", headCategoryType.getCode());


            try{
                Connection.Response response = mustitClient.getSubCategory(headers, formData, null);
                if(response.statusCode() == 200 || response.statusCode() == 302) {
                    log.info("response.body(): {}",response.body());
                    MustitSubCategoryResponse mustitSubCategoryResponse = objectMapper.readValue(response.body(), MustitSubCategoryResponse.class);
                    log.info("mustitSubCategory: {}", mustitSubCategoryResponse);
                    //카테고리결과
                    final int categoryKeyGroup = 1;
                    final int categoryHeaderCategoryGroup = 2;
                    final int categoryNameGroup = 3;
                    List<MustitCategory> mustitCategoryList = new ArrayList<>();
                    List<Map<Integer,String>> parsedSubCategoryMapList = PatternExtractor.MUSTIT_SUB_CATEGORY_HTML.extractAll(mustitSubCategoryResponse.getCategoryHtml(), Arrays.asList(categoryKeyGroup,categoryHeaderCategoryGroup,categoryNameGroup));
                    log.info("{}",parsedSubCategoryMapList);
                    for(Map<Integer,String> categoryGroupMap : parsedSubCategoryMapList){
                        mustitCategoryList.add(new MustitCategory(categoryGroupMap.get(categoryKeyGroup), categoryGroupMap.get(categoryNameGroup),parentCategory, categoryGroupMap.get(categoryHeaderCategoryGroup)));
                    }

                    log.info("mustitCategoryList:{}",mustitCategoryList);
                    return mustitCategoryList;

                }
            } catch (IOException e) {
                log.error("MustIt getCategory Fail, IOException", e);
            } catch (NullPointerException e) {
                log.error("unexpected null data is arrived from MustIt", e);
            } catch (Exception e) {
                log.error("unexpected exception occurred during crawl and save MustIt data.", e);
            }

            return null;
        }catch(Exception e){
            log.error("Fail~",e);
            return null;
        }
    }


    public Boolean getCategoryFilter(long shopAccountId){
        Optional<ShopAccount> shopAccount = shopAccountRepository.findById(shopAccountId);
        if(shopAccount.isEmpty()){
            return false;
        }
        String shopId = shopAccount.get().getShop().getId();
        List<ShopCategory> topCategoryList = shopCategoryRepository.findByShopIdAndParentId(shopId , null);
        for(ShopCategory shopCategory : topCategoryList) {

            List<ShopCategory> twoCategoryList = shopCategoryRepository.findByShopIdAndParentId(shopId , shopCategory.getId());
            for(ShopCategory shopTwoCategory : twoCategoryList) {

                List<ShopCategory> threeCategoryList = shopCategoryRepository.findByShopIdAndParentId(shopId , shopTwoCategory.getId());
                for(ShopCategory shopThreeCategory : threeCategoryList) {
                    if(ObjectUtils.isEmpty(shopThreeCategory.getParentId())){
                        //세번째가 마지막 카테고리인 경우로
                        String categoryCode = shopTwoCategory+"r"+shopThreeCategory;
                        log.debug("Three CategoryCode:{}",shopThreeCategory.getShopCategoryCode());
                        //filter가 있는지에 대해서 http요청을 한다.
                        List<String> filterList = getFilterFromMustit(shopThreeCategory.getShopCategoryCode(),MustitClient.HeadCategoryType.valueOf(shopCategory.getName()));
                        shopThreeCategory.setFilter(String.join(",", filterList));
                        shopCategoryRepository.save(shopThreeCategory);
                    }else{

                        List<ShopCategory> lastCategoryList = shopCategoryRepository.findByShopIdAndParentId(shopId , shopThreeCategory.getId());
                        for(ShopCategory shopLastCategory : lastCategoryList) {
                            log.debug("Three CategoryCode:{}",shopLastCategory.getShopCategoryCode());
                            //filter가 있는지에 대해서 http요청을 한다.
                            List<String> filterList = getFilterFromMustit(shopLastCategory.getShopCategoryCode(),MustitClient.HeadCategoryType.valueOf(shopCategory.getName()));
                            shopLastCategory.setFilter(String.join(",", filterList));
                            shopCategoryRepository.save(shopLastCategory);
                        }
                    }
                }

            }
        }

        return true;
    }


    /*
     * Category Filter Html을 읽어온다.(로그인 불필요)
     */
    public List<String> getFilterFromMustit(String categoryCode, MustitClient.HeadCategoryType headCategoryType ) {
        try {
            //header
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Referer", "https://mustit.co.kr/product/add02");

            //form data (header:W,M,K,L)
            HashMap<String, String> formData = new HashMap<>();
            formData.put("category", categoryCode);
            formData.put("flag", headCategoryType.getCode());

            try{
                log.debug("formData:{}",formData);
                Connection.Response response = mustitClient.getFilterHtml(headers, formData, null);
                if(response.statusCode() == 200 || response.statusCode() == 302) {
                    log.info("response.body(): {}",response.body());
                    MustitSubCategoryResponse mustitSubCategoryResponse = objectMapper.readValue(response.body(), MustitSubCategoryResponse.class);
                    log.info("mustitSubCategory: {}", mustitSubCategoryResponse);
                    //카테고리결과
                    final int filterGroup = 1;
                    final int categoryHeaderCategoryGroup = 2;
                    final int categoryNameGroup = 3;
                    List<String> filterList = new ArrayList<>();
                    List<Map<Integer,String>> parsedSubCategoryMapList = PatternExtractor.MUSTIT_CATEGORY_FILTER_HTML.extractAll(mustitSubCategoryResponse.getFilterHtml(), Arrays.asList(filterGroup));
                    log.info("{}",parsedSubCategoryMapList);
                    for(Map<Integer,String> categoryGroupMap : parsedSubCategoryMapList){
                        filterList.add(categoryGroupMap.get(filterGroup));
                    }

                    log.debug("filterStr:{}", String.join(",", filterList));
                    return filterList;

                }
            } catch (IOException e) {
                log.error("MustIt getCategory Fail, IOException", e);
            } catch (NullPointerException e) {
                log.error("unexpected null data is arrived from MustIt", e);
            } catch (Exception e) {
                log.error("unexpected exception occurred during crawl and save MustIt data.", e);
            }

            return null;
        }catch(Exception e){
            log.error("Fail~",e);
            return null;
        }
    }


}
