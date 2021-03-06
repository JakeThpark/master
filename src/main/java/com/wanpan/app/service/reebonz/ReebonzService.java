package com.wanpan.app.service.reebonz;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.wanpan.app.config.gateway.ReebonzClient;
import com.wanpan.app.dto.BrandDto;
import com.wanpan.app.dto.CategoryDto;
import com.wanpan.app.dto.ShopAccountDto;
import com.wanpan.app.dto.job.*;
import com.wanpan.app.dto.job.order.*;
import com.wanpan.app.dto.job.qna.ShopQnaDto;
import com.wanpan.app.dto.job.qna.ShopQnaJobDto;
import com.wanpan.app.dto.reebonz.*;
import com.wanpan.app.entity.Job;
import com.wanpan.app.entity.ShopAccountToken;
import com.wanpan.app.exception.InvalidRequestException;
import com.wanpan.app.service.ComparisonCheckResult;
import com.wanpan.app.service.ShopService;
import com.wanpan.app.service.reebonz.constant.ReebonzOrderListType;
import com.wanpan.app.service.reebonz.constant.ReebonzSaleStatus;
import com.wanpan.app.service.reebonz.parser.ReebonzOrderConversationParser;
import com.wanpan.app.service.reebonz.parser.ReebonzProductParser;
import com.wanpan.app.service.reebonz.parser.ReebonzQnaParser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.modelmapper.ModelMapper;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class ReebonzService implements ShopService {
    private final int PAGE_SIZE = 20;
    private final String CREATED_FROM_KEY = "api_wanpan";
    private final String TEST_BEARER_TOKEN = "69ece570ca9da14f9b0f28f7c370271580baa9091511c54377cc98a2cbd11141";
    private final String GET_PRODUCT_LIST_URL = "http://dev.reebonz.co.kr:3007/api/marketplace/product/products%s.json";
    private final String DELETE_PRODUCT_STOCK_URL = "http://dev.reebonz.co.kr:3007/api/marketplace/product/products/%s/stock_delete.json";
    private final ObjectMapper objectMapper;
    private final ModelMapper modelMapper;
    private final ReebonzClient reebonzClient;
    private final ReebonzBrandService reebonzBrandService;
    private final ReebonzWebPageService reebonzWebPageService;
    private final ReebonzQnaParser reebonzQnaParser;
    private final ReebonzOrderConversationParser reebonzOrderConversationParser;

    public List<ReebonzCategory> getCategoryListByParentId(boolean newCategory, int parentId) throws IOException {
        ResponseEntity<ReebonzBaseResponse<Categories>> responseEntity
                = reebonzClient.getCategories("bearer " + TEST_BEARER_TOKEN, newCategory, parentId);

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            if("success".equals(responseEntity.getBody().getResult())){
                log.debug("get Data Success!!");
            }else{
                log.debug("get Data Fail!!");
            }
        } else {
            //TODO:???????????? 200??? ?????? ????????? ?????? ??????
            log.debug("Http Error!!");
        }

        return responseEntity.getBody().getData().getCategories();
    }

    public List<ProductMetaInfo> getProductMetaInfoByCode(String skuCode) {
        ResponseEntity<ReebonzBaseResponse<ProductMetaInfoResponse>> responseEntity
                = reebonzClient.getProductMetaInfoByCode("bearer " + TEST_BEARER_TOKEN, skuCode);

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            if("success".equals(responseEntity.getBody().getResult())){
                log.debug("get Data Success!!");
            }else{
                log.debug("get Data Fail!!");
            }
        } else {
            log.debug("Http Error!!");
        }

        return responseEntity.getBody().getData().getProductMetaInfo();
    }


    public ReebonzBaseResponse<DeliveryRequest> createOrUpdateDelivery(DeliveryRequest deliveryRequest) throws IOException {

        ResponseEntity<ReebonzBaseResponse<DeliveryRequest>> responseEntity
                = reebonzClient.createOrUpdateDelivery("bearer " + TEST_BEARER_TOKEN, deliveryRequest);

        log.debug(objectMapper.writeValueAsString(deliveryRequest));

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            if("success".equals(responseEntity.getBody().getResult())){
                log.debug("get Data Success!!");
            }else{
                log.debug("get Data Fail!!");
            }
        } else {
            log.debug("Http Error!!");
        }

        return responseEntity.getBody();
    }

    public List<Product> getProductList(boolean available, boolean soldout) throws IOException {
        ReebonzBaseResponse<ProductListResponse> productListReebonzBaseResponse = new ReebonzBaseResponse<ProductListResponse>();
        Map<String, String> parameter = new HashMap<>();
        parameter.put("available", String.valueOf(available));
        parameter.put("soldout", String.valueOf(soldout));

        //get contents
//        Connection.Response response =
//                jsoupGetByTokenAndUrlAndParameter(TEST_BEARER_TOKEN, String.format(GET_PRODUCT_LIST_URL,""), parameter);
//        log.debug(response.body());
//
//        if (response.statusCode() == 200) {
//            productListReebonzBaseResponse = objectMapper.readValue(response.body(), new TypeReference<>(){});
//
//            if("success".equals(productListReebonzBaseResponse.getResult())){
//                /*
//                 * ????????? ????????? ????????? object??? string?????? ?????? ?????? ??????????????? ?????? ????????? ??? string??? ?????? null?????? ????????? ????????????.
//                 */
//                for(Product product : productListReebonzBaseResponse.getData().getProducts()){
//                    if(product.getImageMainUrl() != null && product.getImageMainUrl().asText().length() > 0){
//                        product.setImageMainUrlObject(objectMapper.readValue(product.getImageMainUrl().get("filename").toString(), Filename.class));
//                    }
//                    if(product.getImageMainOverUrl() != null && product.getImageMainOverUrl().asText().length() > 0){
//                        product.setImageMainOverUrlObject(objectMapper.readValue(product.getImageMainOverUrl().get("filename").toString(), Filename.class));
//                    }
//                }
//                log.debug("get Data Success!!");
//            }else{
//                log.debug("get Data Fail!!");
//            }
//        } else {
//            log.debug("Http Error!!");
//        }
//
//        return productListReebonzBaseResponse.getData().getProducts();
        return new ArrayList<Product>();
    }

    public Product getProductById(int reebonzProductId) throws IOException {
        ReebonzBaseResponse<ProductResponse> productReebonzBaseResponse = new ReebonzBaseResponse<ProductResponse>();
        //get contents
//        Connection.Response response =
//                jsoupGetByTokenAndUrlAndParameter(TEST_BEARER_TOKEN, String.format(GET_PRODUCT_LIST_URL,"/" + reebonzProductId), new HashMap<>());
//        log.debug(response.body());
//
//        if (response.statusCode() == 200) {
//            productReebonzBaseResponse = objectMapper.readValue(response.body(), new TypeReference<>(){});
//
//            if("success".equals(productReebonzBaseResponse.getResult())){
//                log.debug("get Data Success!!");
//            }else{
//                log.debug("get Data Fail!!");
//            }
//        } else {
//            log.debug("Http Error!!");
//        }
//
//        return productReebonzBaseResponse.getData().getProduct();
        return new Product();
    }

    public ReebonzBaseResponse<StockDeleteRequest> deleteProductStock(int reebonzProductId, long stockId) throws IOException {
        ReebonzBaseResponse<StockDeleteRequest> stockDeleteReebonzBaseResponse = new ReebonzBaseResponse<StockDeleteRequest>();

        String requestJson = objectMapper.writeValueAsString(new StockDeleteRequest(stockId));
        log.debug(requestJson);

//        Connection.Response response =
//                jsoupDeleteByTokenAndUrlAndRequestJson(TEST_BEARER_TOKEN, String.format(DELETE_PRODUCT_STOCK_URL, reebonzProductId), requestJson);
//        log.debug(response.body());
//
//        if (response.statusCode() == 200) {
//            stockDeleteReebonzBaseResponse = objectMapper.readValue(response.body(), new TypeReference<>(){});
//
//            if("success".equals(stockDeleteReebonzBaseResponse.getResult())){
//                log.debug("get Data Success!!");
//            }else{
//                log.debug("get Data Fail!!");
//            }
//        } else {
//            log.debug("Http Error!!");
//        }
//
//        return stockDeleteReebonzBaseResponse;
        return new ReebonzBaseResponse<StockDeleteRequest>();
    }

    public void getTest(String url, String bearerToken, Map<String,String> pathVariable, MultiValueMap<String, String> parameter) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.set("Authorization", "bearer " + bearerToken);

        UriComponents builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParams(parameter).build();

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> strResponse = restTemplate.exchange(builder.toUriString(),HttpMethod.GET, requestEntity,String.class,pathVariable);
    }

    @Override
    public ShopAccountDto.Response checkSignIn(String loginId, String password,
                                               ShopAccountDto.Response shopAccountResponseDto)
            throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

        Map<String, String> postData = new HashMap<>();
        postData.put("grant_type", "password");
        postData.put("username", loginId);
        postData.put("password", password);

        Connection.Response mustItLoginResponse = Jsoup.connect("http://dev.reebonz.co.kr:3007/api/token")
                .method(Connection.Method.POST)
                .ignoreContentType(true)
                .data(postData)
                .ignoreHttpErrors(true)
                .execute();
        String response = mustItLoginResponse.body();
        if(mustItLoginResponse.statusCode() == 200){
            log.debug(response);
            LoginResponse reebonzLoginResponse = mapper.readValue(response, LoginResponse.class);
            log.debug("loginInfo: "+ reebonzLoginResponse);
            shopAccountResponseDto.setSuccessFlag(true);
        }else{
            LoginErrorResponse reebonzLoginErrorResponse = mapper.readValue(response, LoginErrorResponse.class);
            log.debug("loginError: "+ reebonzLoginErrorResponse);
            shopAccountResponseDto.setSuccessFlag(false);
            shopAccountResponseDto.setMessage(reebonzLoginErrorResponse.getErrorDescription());

        }
        return shopAccountResponseDto;
    }

    /**
     * ????????? ????????? ????????? ??????????????? ???????????? ???????????? ?????????
     */
    @Override
    public boolean isKeepSignIn(String token, String accountId, ShopAccountToken.Type tokenType) {
        switch(tokenType){
            case BEARER:
                return isKeepSignInByApi(token);
            case SESSION:
                return isKeepSignInByWebPage(token);
            default:
                log.error("Invalid tokenType!");
                return false;
        }
    }

    /**
     * ??? ????????? ????????? ????????? ????????? ???????????? ??? ????????? ?????? ?????????
     */
    private int getPageCount(int dataCount, int pageSize) {
        return (dataCount % pageSize != 0) ? (dataCount / pageSize + 1) : (dataCount / pageSize);
    }

    /**
     * ????????? ?????? ?????? - ?????? ????????? ??????????????? ????????????.
     * @param bearerToken bearer??????
     * @return ???????????????
     */
    private boolean isKeepSignInByApi(String bearerToken){
        try {
            ResponseEntity<ReebonzBaseResponse<Categories>> responseEntity
                    = reebonzClient.getCategories("bearer " + bearerToken, true, 1);

            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                log.error("API login is Fail!");
                return true;
            } else {
                log.error("Reebonz Token Fail - {}", responseEntity);
                return false;
            }
        }catch(Exception e){
            return false;
        }
    }

    /**
     * ????????? ?????? ?????? - ?????? ???????????? ??????????????? ????????????.
     * @param webToken ???????????? ???????????? ???????????? ???????????? ??????
     * @return ????????? ??????
     */
    private boolean isKeepSignInByWebPage(String webToken){
        try {
            return reebonzWebPageService.isKeepSignIn(webToken);
        }catch(Exception e){
            log.error("WebPage login check is Fail!!", e);
            return false;
        }
    }

    @Override
    public String getToken(String accountId, String password, ShopAccountToken.Type tokenType) {
        switch(tokenType){
            case BEARER:
                return getTokenByApi(accountId, password);
            case SESSION:
                return getTokenByWebPage(accountId, password);
            default:
                log.error("Invalid tokenType!");
                return null;
        }
    }

    private String getTokenByApi(String accountId, String password){
        ResponseEntity<LoginResponse> responseEntity
                = reebonzClient.login("password", accountId, password);

        log.info("responseEntity : {}", responseEntity);
        if(responseEntity.getBody() == null){
            log.error("API login is Fail!");
            return null;
        }
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            return "bearer " + responseEntity.getBody().getAccessToken();
        } else {
            log.error("Reebonz login Fail - {}", responseEntity);
            return null;
        }
    }

    private String getTokenByWebPage(String accountId, String password){
        try {
            return reebonzWebPageService.getToken(accountId, password);
        }catch(Exception e){
            log.error("WebPage Login is Fail!!", e);
            return null;
        }
    }

    @Override
    public List<BrandDto> getBrandList(String token) {
        return reebonzBrandService.getBrandListBySearchName(token, "");
    }

    @Override
    public List<CategoryDto> getCategoryList(String token) {
        return null;
    }

    /**
     * (WebPage ??????) ???????????? ???????????? ????????????.
     * @param token login token(bearer)
     * @param job job DB ID
     * @param onlineSaleDto ????????? ?????????
     * @return ????????? ?????????
     */
    @Async
    @Override
    public ListenableFuture<RegisterDto.Response> postSaleToShop(String token, Job job, OnlineSaleDto onlineSaleDto) {
        log.info("Reebonz postSaleToShop JOB START => jobId={}", job.getId());
        boolean successFlag = false;
        String postId = null;
        String message = null;

        try {
            String postResponse = reebonzWebPageService.postProductSale(token, onlineSaleDto);
            ReebonzWebPageProductCreateResponse reebonzWebPageProductCreateResponse = objectMapper.readValue(postResponse, new TypeReference<>(){});
            if ("success".equals(reebonzWebPageProductCreateResponse.getResult())) {
                postId = String.valueOf(reebonzWebPageProductCreateResponse.getDbId());
                message = reebonzWebPageProductCreateResponse.getNotice();
                successFlag = true;
            } else {
                log.error("ReebonzService.postSaleToShop fail => jobId={}\n", job.getId(), postResponse);
            }
        } catch (Exception e) {
            log.error("Reebonz postSaleToShop Fail => jobId={}\n", job.getId(), e);
            successFlag = false;
            message = "????????? ?????? ?????? ??????";
        }

        log.info("Reebonz postSaleToShop JOB END => jobId={}", job.getId());

        return new AsyncResult<>(new RegisterDto.Response(
                onlineSaleDto.getShopSale().getId(),
                postId,
                ShopSaleJobDto.SaleStatus.ON_SALE,
                job.getId(),
                onlineSaleDto.getShopSale().getId(),
                successFlag,
                message)
        );
    }

    /** (?????? ??????)
     * (API ??????) ???????????? ???????????? ????????????.
     */
    public ListenableFuture<RegisterDto.Response> postSaleToShopByApi(String token, long jobId, OnlineSaleDto onlineSaleDto) {
        log.info("Reebonz postSaleToShopByApi Call");
        boolean successFlag = false;
        String message = "";
        String productId = null;
        String jobStatus = "END";
        log.info("bearer token:{}",token);
        try {
            ReebonzApiProductCreate reebonzApiProductCreate = makeReebonzProductPostData(onlineSaleDto);
            log.info("Register Request Json:{}", reebonzApiProductCreate);
            ResponseEntity<ReebonzApiProductCreate.Response> responseEntity
                    = reebonzClient.postProduct(token, reebonzApiProductCreate);

            if (responseEntity.getStatusCode() == HttpStatus.OK) { //???????????? ??????
                if (responseEntity.getBody() == null) { //body ??????
                    log.error("Product Post Fail");
                    message = "Response Body is null";
                } else {
                    if ("success".equals(responseEntity.getBody().getResult())) { //????????? ??????
                        log.error("Product Post Success");
                        successFlag = true;
                        message = responseEntity.getBody().getMessage();
                        productId = String.valueOf(responseEntity.getBody().getProductId());
                    } else {
                        log.error("Product Post Fail - message: {}", responseEntity.getBody().getMessage());
                        message = responseEntity.getBody().getMessage();
                    }
                }
            } else {
                log.error("Product Post Exception Fail");
                message = String.valueOf(responseEntity.getStatusCode());
            }
        }catch(Exception e){
            log.error("Product Post Exception Fail",e);
            successFlag = false;
            message = "Exception";
        }

        return new AsyncResult<>(new RegisterDto.Response(
                onlineSaleDto.getShopSale().getId(),
                productId,
                ShopSaleJobDto.SaleStatus.ON_SALE,
                jobId,
                onlineSaleDto.getShopSale().getId(),
                successFlag,
                message)
        );
    }

    /**
     * (API ??????) ????????? ????????? ?????? POST ????????? ??????
     */
    private ReebonzApiProductCreate makeReebonzProductPostData(OnlineSaleDto onlineSaleDto){
        ReebonzApiProductCreate reebonzApiProductCreate = new ReebonzApiProductCreate();
        reebonzApiProductCreate.setCreatedFrom(CREATED_FROM_KEY); //API ?????? ????????? (CollectQnaFromShopTaskService??????)
        reebonzApiProductCreate.setName(onlineSaleDto.getSubject());//?????????(??????)
        reebonzApiProductCreate.setCode(onlineSaleDto.getProductList().get(0).getOfficialSku());//Sku ??????(??????)
        reebonzApiProductCreate.setDescription(onlineSaleDto.getSaleReebonz().getDetail()); //?????? ?????? ??????
        reebonzApiProductCreate.setBrandId(Long.parseLong(onlineSaleDto.getBrandMap().getSourceCode())); //????????? ID - Brands API ??????(??????)

        reebonzApiProductCreate.setMarketplacePrice(onlineSaleDto.getPrice());//?????? ?????? ??????(??????)
//        reebonzProductCreate.setCommission(); //?????????(0??????~1??????) ex:0.8 ??? 20%
        reebonzApiProductCreate.setMaterial(onlineSaleDto.getMaterial()); //?????? ??????
//        reebonzProductCreate.setLegalInfo(); //?????? ???????????? - Templates API ?????? ??????(????????? ??????) - ?????? ????????? ?????? ??????????????? ???????????? ????????? ?????????
//        reebonzProductCreate.setProductNotification(); //?????? ?????? ?????? ?????? - ?????? ????????? ?????? ??????????????? ???????????? ????????? ?????????
//        reebonzProductCreate.setProductTip(); //?????? ?????? ?????? - Templates API ?????? ??????(????????? ??????) - ?????? ????????? ?????? ??????????????? ???????????? ????????? ?????????
        reebonzApiProductCreate.setSizeInfo(onlineSaleDto.getSize()); //????????? ?????? - Templates API ?????? ??????(????????? ??????)

        // ???????????? ??????
        ShopCategoryDto genderCategory = onlineSaleDto.getShopSale().getShopCategory();
        ShopCategoryDto largeCategory = genderCategory.getChild();
        ShopCategoryDto mediumCategory = largeCategory.getChild();
        ShopCategoryDto smallCategory = mediumCategory.getChild();

        reebonzApiProductCreate.setCategoryGenderId(Integer.parseInt(genderCategory.getShopCategoryCode())); //???????????? - Categories API ??????(?????? = 2, ?????? = 3)
        reebonzApiProductCreate.setCategoryMasterId(Integer.parseInt(largeCategory.getShopCategoryCode())); //???????????? - Categories API ??????
        reebonzApiProductCreate.setCategorySlaveId(Integer.parseInt(mediumCategory.getShopCategoryCode())); //???????????? - Categories API ??????
        List<ReebonzDetailImage> detailImages = new ArrayList<>();
        for(OnlineSaleImageDto onlineSaleImageDto : onlineSaleDto.getSaleImageList()){
            if(onlineSaleImageDto.getSequence() == 0){
                reebonzApiProductCreate.setImageMainUrl(onlineSaleImageDto.getOriginImagePath());
                reebonzApiProductCreate.setImageMainOverUrl(onlineSaleImageDto.getOriginImagePath()); //?????? ?????? ?????????
            }else{
                detailImages.add(new ReebonzDetailImage(onlineSaleImageDto.getOriginImagePath()));
            }

        }
        reebonzApiProductCreate.setDetailImages(detailImages);

        List<ReebonzStock> reebonzStocks = new ArrayList<>();

        // ?????? ??????
        for (ProductDto productDto : onlineSaleDto.getProductList()) {
            for (ProductOptionDto productOptionDto : productDto.getProductOptionList()) {
                reebonzStocks.add(new ReebonzStock(
                        productDto.getClassificationValue(), //group
                        productDto.getClassificationValue() + "|" + productOptionDto.getName(), //name
                        productOptionDto.getQuantity() //stock
                ));
            }
        }
        reebonzApiProductCreate.setStocks(reebonzStocks); //<ReebonzStock> stocks; //?????? ??????(??????)

        return reebonzApiProductCreate;
    }

    /**
     * (WebPage ??????) ???????????? ?????? ???????????? ????????????.
     */
    @Async
    @Override
    public ListenableFuture<RegisterDto.Response> updateSaleToShop(String token, long jobId, OnlineSaleDto onlineSaleDto) throws IOException {
        log.info("Reebonz updateSaleToShop JOB START => jobId={}", jobId);

        ShopSaleJobDto.Request.PostJob shopSaleDto = onlineSaleDto.getShopSale();
        boolean successFlag;
        String postId = onlineSaleDto.getShopSale().getPostId();
        String message;
        ShopSaleJobDto.SaleStatus currentSaleStatus = null;

        try {
            ReebonzSaleStatus collectedReebonzSaleStatus = getReebonzSaleStatus(token, postId);
            if (collectedReebonzSaleStatus == ReebonzSaleStatus.SALE_STOP) {
                // ?????? ??????????????? ???????????? ????????? ??????
                log.info("Reebonz updateSaleToShop JOB END => jobId={}", jobId);
                return new AsyncResult<>(new RegisterDto.Response(
                        shopSaleDto.getId(),
                        postId,
                        ShopSaleJobDto.SaleStatus.SALE_STOP,
                        jobId,
                        shopSaleDto.getId(),
                        false,
                        "?????? ???????????? ?????? ??????????????? ??????????????? ???????????????.")
                );
            } else if (collectedReebonzSaleStatus == ReebonzSaleStatus.NOT_FOUND_SALE) {
                // ????????? ?????????????????? ???????????? ????????? ?????? ???????????? ???????????? ????????? ?????????, ?????????????????? ?????? ????????? ???????????? ???????????? ???????????? ?????? ???????????? ???????????? ????????? ????????????.
                log.info("Reebonz updateSaleToShop JOB END => jobId={}", jobId);
                return new AsyncResult<>(new RegisterDto.Response(
                        shopSaleDto.getId(),
                        postId,
                        ShopSaleJobDto.SaleStatus.NOT_FOUND_SALE,
                        jobId,
                        shopSaleDto.getId(),
                        false,
                        "?????? ???????????? ?????? ??????????????? ??????????????? ???????????????.")
                );
            } else {
                String postUpdateResponse = reebonzWebPageService.postProductSaleUpdate(token, onlineSaleDto);
                ReebonzWebPageProductCreateResponse reebonzWebPageProductCreateResponse = objectMapper.readValue(postUpdateResponse, new TypeReference<>(){});

                if ("success".equals(reebonzWebPageProductCreateResponse.getResult())) {
                    successFlag = true;
                    postId = String.valueOf(reebonzWebPageProductCreateResponse.getDbId());
                    message = reebonzWebPageProductCreateResponse.getNotice();
                } else {
                    successFlag = false;
                    message = reebonzWebPageProductCreateResponse.getNotice();
                }
            }
        } catch (Exception e) {
            log.error("Reebonz updateSaleToShop Fail => jobId={}\n", jobId, e);
            successFlag = false;
            message = e.getMessage();
        }

        // ????????? ???????????? ???????????? ????????? ?????? ????????? ????????? ?????? ??????
        try {
            ReebonzSaleStatus reebonzSaleStatus = getReebonzSaleStatus(token, postId);
            currentSaleStatus = ShopSaleJobDto.SaleStatus.valueOf(reebonzSaleStatus.name());
        } catch (Exception e) {
            e.printStackTrace();
        }

        log.info("Reebonz updateSaleToShop JOB END => jobId={}", jobId);

        return new AsyncResult<>(new RegisterDto.Response(
                shopSaleDto.getId(),
                postId,
                currentSaleStatus,
                jobId,
                shopSaleDto.getId(),
                successFlag,
                message)
        );
    }

    /** (?????? ??????)
     * (API ??????) ???????????? ?????? ???????????? ????????????.
     */
    public ListenableFuture<RegisterDto.Response> updateSaleToShopByApi(String token, long jobId,
                                                                   OnlineSaleDto onlineSaleDto) throws IOException {
        log.info("Reebonz updateSaleToShop Call");
        boolean successFlag = false;
        String message = "";
        String productId = null;
        String jobStatus = "END";
        log.info("bearer token:{}",token);
        try {
            ReebonzProductUpdate reebonzProductUpdate = makeReebonzProductPutData(onlineSaleDto);
            log.info("Update Request Json:{}", reebonzProductUpdate);
            ResponseEntity<ReebonzBaseResponse<ProductResponse>> responseEntity
                    = reebonzClient.updateProduct(token, onlineSaleDto.getShopSale().getPostId(),reebonzProductUpdate);

            if (responseEntity.getStatusCode() == HttpStatus.OK) { //???????????? ??????
                if (responseEntity.getBody() == null) { //body ??????
                    log.error("Product Put Fail");
                    message = "Response Body is null";
                } else {
                    if ("success".equals(responseEntity.getBody().getResult())) { //????????? ??????
                        log.error("Product Put Success");
                        successFlag = true;
                        message = responseEntity.getBody().getMessage();
                        productId = String.valueOf(responseEntity.getBody().getData().getProduct().getId());
                    } else {
                        log.error("Product Put Fail - message: {}", responseEntity.getBody().getMessage());
                        message = responseEntity.getBody().getMessage();
                    }
                }
            } else {
                log.error("Product Post Exception Fail");
                message = String.valueOf(responseEntity.getStatusCode());
            }
        }catch(Exception e){
            log.error("Product Post Exception Fail",e);
            successFlag = false;
            message = "Exception";
        }

        return new AsyncResult<>(new RegisterDto.Response(
                onlineSaleDto.getShopSale().getId(),
                productId,
                ShopSaleJobDto.SaleStatus.ON_SALE,
                jobId,
                onlineSaleDto.getShopSale().getId(),
                successFlag,
                message)
        );
    }

    @Override
    public ListenableFuture<RegisterDto.Response> updateSaleStatusToShop(String token, long jobId, ShopSaleJobDto.Request.UpdateSaleStatusJob updateSaleStatusJob) {
        log.info("Reebonz updateSaleStatusToShop Call => jobId={}", jobId);

        ShopSaleJobDto.SaleStatus requestSaleStatus = updateSaleStatusJob.getRequestSaleStatus();
        String postId = updateSaleStatusJob.getPostId();
        boolean successFlag;
        String message;
        ShopSaleJobDto.SaleStatus currentSaleStatus = null;
        String postUpdateResponse;

        try {
            switch (requestSaleStatus) {
                case SALE_STOP: // "????????????" ??????
                case ON_SALE: // "????????????" ??????
                    postUpdateResponse = reebonzWebPageService.postProductSaleStop(token, postId, ReebonzSaleStatus.valueOf(requestSaleStatus.name()));
                    ReebonzWebPageProductSaleStopResponse reebonzWebPageProductSaleStopResponse = objectMapper.readValue(postUpdateResponse, new TypeReference<>(){});
                    if ("success".equals(reebonzWebPageProductSaleStopResponse.getResult())) {
                        successFlag = true;
                        currentSaleStatus = requestSaleStatus;
                    } else {
                        successFlag = false;
                    }
                    message = reebonzWebPageProductSaleStopResponse.getMessage();
                    break;
                default:
                    throw new InvalidRequestException("???????????? ??????????????????????????????: " + requestSaleStatus.name());
            }
        } catch (Exception e) {
            log.error("Reebonz updateSaleStatusToShop Error =>", e);
            successFlag = false;
            message = "????????? ?????? ??????";
        }

        // ???????????? ???????????? ???????????? ????????? ?????? ????????? ????????? ?????? ??????
        try {
            ReebonzSaleStatus reebonzSaleStatus = getReebonzSaleStatus(token, postId);
            currentSaleStatus = ShopSaleJobDto.SaleStatus.valueOf(reebonzSaleStatus.name());
        } catch (Exception e) {
            e.printStackTrace();
        }

        log.info("Reebonz updateSaleStatusToShop JOB END => jobId={}", jobId);

        return new AsyncResult<>(new RegisterDto.Response(
                updateSaleStatusJob.getId(),
                postId,
                currentSaleStatus,
                jobId,
                updateSaleStatusJob.getId(),
                successFlag,
                message
        ));
    }

    /**
     * ???????????? ????????????
     */
    @Override
    public ListenableFuture<RegisterDto.Response> deleteShopSale(String token, long jobId, ShopSaleJobDto.Request.DeleteSaleJob deleteSaleJob) {
        log.info("Reebonz deleteShopSale Call => jobId={}", jobId);

        String postId = deleteSaleJob.getPostId();
        boolean successFlag;
        String message;
        String postUpdateResponse;

        try {
            // ???????????? ????????? ?????? ????????? ???????????? ?????? ???????????? ???????????? ????????????.
            postUpdateResponse = reebonzWebPageService.postProductSaleStop(token, postId, ReebonzSaleStatus.SALE_STOP);
            ReebonzWebPageProductSaleStopResponse reebonzWebPageProductDeleteResponse = objectMapper.readValue(postUpdateResponse, new TypeReference<>(){});
            if ("success".equals(reebonzWebPageProductDeleteResponse.getResult())) {
                successFlag = true;
                message = "???????????? ??????. ???????????? ????????? ?????? ????????? ???????????? ?????? ??????????????? ???????????????.";
            } else {
                successFlag = false;
                message = "???????????? ??????. ???????????? ????????? ?????? ????????? ???????????? ?????? ??????????????? ???????????????.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            successFlag = false;
            message = "????????? ?????? ??????";
        }

        log.info("Reebonz deleteShopSale JOB END => jobId={}", jobId);

        return new AsyncResult<>(new RegisterDto.Response(
                deleteSaleJob.getId(),
                postId,
                ShopSaleJobDto.SaleStatus.DELETE,
                jobId,
                deleteSaleJob.getId(),
                successFlag,
                message
        ));
    }

    /**
     *
     */
    private ReebonzProductUpdate makeReebonzProductPutData(OnlineSaleDto onlineSaleDto){
        ReebonzProductUpdate reebonzProductUpdate = new ReebonzProductUpdate();
        reebonzProductUpdate.setCreatedFrom(CREATED_FROM_KEY); //API ?????? ?????????
        reebonzProductUpdate.setName(onlineSaleDto.getSubject());//?????????
        reebonzProductUpdate.setDescription(onlineSaleDto.getSaleReebonz().getDetail()); //?????? ?????? ??????
        reebonzProductUpdate.setMarketplacePrice(onlineSaleDto.getPrice());//?????? ?????? ??????(??????)
//        reebonzProductUpdate.setCommission(); //?????????(0??????~1??????) ex:0.8 ??? 20%

        // ???????????? ?????? - ????????? ???????????? ????????????.
        ShopCategoryDto genderCategory = onlineSaleDto.getShopSale().getShopCategory();
        ShopCategoryDto largeCategory = genderCategory.getChild();
        ShopCategoryDto mediumCategory = largeCategory.getChild();
        ShopCategoryDto smallCategory = mediumCategory.getChild();

        List<Long> categoryIds = new ArrayList<>();
        categoryIds.add(Long.parseLong(genderCategory.getShopCategoryCode()));
        categoryIds.add(Long.parseLong(largeCategory.getShopCategoryCode()));
        categoryIds.add(Long.parseLong(mediumCategory.getShopCategoryCode()));
        if(!ObjectUtils.isEmpty(smallCategory)){
            categoryIds.add(Long.parseLong(smallCategory.getShopCategoryCode()));
        }
        reebonzProductUpdate.setCategoryIds(categoryIds);

        List<ReebonzDetailImage> detailImages = new ArrayList<>();
        for(OnlineSaleImageDto onlineSaleImageDto : onlineSaleDto.getSaleImageList()){
            if(onlineSaleImageDto.getSequence() == 0){
                reebonzProductUpdate.setImageMainUrl(onlineSaleImageDto.getOriginImagePath());
                reebonzProductUpdate.setImageMainOverUrl(onlineSaleImageDto.getOriginImagePath()); //?????? ?????? ?????????
            }else{
                detailImages.add(new ReebonzDetailImage(onlineSaleImageDto.getOriginImagePath()));
            }
        }
        reebonzProductUpdate.setDetailImages(detailImages);
//????????? ??????????????? ?????? ???????????? ??????(????????? ????????? ??????)
//        reebonzProductUpdate.setBrandId(Long.parseLong(onlineSaleDto.getOnlineSaleReebonz().getBrandMap().getSourceCode()));

        //SKU ????????? ?????? ????????? ???????????? ????????????.
        reebonzProductUpdate.setMarketplaceCode(onlineSaleDto.getProductList().get(0).getCustomSku());
//        reebonzProductUpdate.setCode(onlineSaleDto.getProductList().get(0).getOfficialSku());

        return reebonzProductUpdate;
    }

    /**
     * ??????????????? ?????? ??????
     */
    @Async
    @Override
    public ListenableFuture<ShopQnaJobDto.Request.CollectCallback> collectQnAFromShop(String webToken, long jobId, ShopQnaJobDto.QuestionStatus questionStatus, ShopAccountDto.Request request) {
        try {
//            String webToken = "01867d7371034a26268fb9dbd9cd9fe8";
//            List<ShopQnaDto.Request.CollectCallback> shopQnAList = new ArrayList<>();
//            switch(questionStatus){
//                case READY: //??? ????????? ?????? - ???????????? ???????????? ????????? ????????? ??????.
//                    shopQnAList.addAll(collectQnABySearchParam(webToken, SearchIsReply.NEW.getParamCode()));
//                    shopQnAList.addAll(collectQnABySearchParam(webToken, SearchIsReply.ADD_NEW.getParamCode()));
//                    //???????????? ?????? ??????
//                    shopQnAList.sort((p1, p2) -> p2.getQuestionId().compareTo(p1.getQuestionId()));
//                    break;
//                case COMPLETE:
//                    shopQnAList.addAll(collectQnABySearchParam(webToken, SearchIsReply.COMPLETE.getParamCode()));
//                    break;
//                default:
//                    break;
//            }

            //?????? ??????????????? ?????? ???????????? ??????
            List<ShopQnaDto.Request.CollectCallback> shopQnAList = collectQnABySearchParam(webToken, SearchIsReply.TOTAL.getParamCode());
            //?????? response ??????
            ShopQnaJobDto.Request.CollectCallback collectShopQnAListCallback = new ShopQnaJobDto.Request.CollectCallback();
            collectShopQnAListCallback.getJobTaskResponseBaseDto().setJobId(jobId);
            collectShopQnAListCallback.getJobTaskResponseBaseDto().setRequestId(request.getRequestId());
            collectShopQnAListCallback.getJobTaskResponseBaseDto().setSuccessFlag(true);
            collectShopQnAListCallback.getJobTaskResponseBaseDto().setMessage("????????????");
            collectShopQnAListCallback.setShopAccount(modelMapper.map(request, ShopAccountDto.Response.class));
            collectShopQnAListCallback.setShopQnAList(shopQnAList);

            return new AsyncResult<>(collectShopQnAListCallback);
        }catch(Exception e){
            log.error("Failed",e);
            return null;
        }
    }

    /**
     * ???????????? ?????? ?????? ??????(????????????,????????????????????? ???????????? ????????? ?????? ??????)
     * @param webToken
     * @param orderConversationStatus
     * @return
     */
    private CollectOrderConversationResult collectOrderConversation(String webToken, OrderBaseConversationJobDto.OrderConversationStatus orderConversationStatus) {
        log.info("Call collectOrderConversation");
        CollectOrderConversationResult collectOrderConversationResult = new CollectOrderConversationResult();
        List<OrderBaseConversationDto> orderBaseConversationList = collectOrderConversationResult.getOrderBaseConversationList();
        String orderConversationResponse;
        int orderConversationCount;
        final int FIRST_PAGE_NUMBER = 1;
        int pageCount;
        try {
            switch(orderConversationStatus) {
                case ALL:
                    // ?????? ??????????????? ????????????
                    orderConversationResponse = reebonzWebPageService.collectOrderConversation(webToken, QnaStatus.ALL.getParamCode(), FIRST_PAGE_NUMBER);
                    orderConversationCount = reebonzOrderConversationParser.parseOrderConversationCountByQnAStatus(orderConversationResponse, QnaStatus.ALL);
                    if (orderConversationCount != 0) {
                        pageCount = getPageCount(orderConversationCount, PAGE_SIZE);

                        for (int i = 1; i <= pageCount; i++) {
                            orderBaseConversationList.addAll(collectOrderConversationBySearchParam(webToken, QnaStatus.ALL.getParamCode(), i));
                        }
                    }
                    collectOrderConversationResult.setSuccessFlag(true);
                    collectOrderConversationResult.setMessage("?????? ???????????? ?????? ??????");
                    break;
                case NEW:
                    // (1) "????????????" ??????????????? ????????????
                    orderConversationResponse = reebonzWebPageService.collectOrderConversation(webToken, QnaStatus.OPEN.getParamCode(), FIRST_PAGE_NUMBER);
                    orderConversationCount = reebonzOrderConversationParser.parseOrderConversationCountByQnAStatus(orderConversationResponse, QnaStatus.OPEN);
                    if (orderConversationCount != 0) {
                        pageCount = getPageCount(orderConversationCount, PAGE_SIZE);

                        for (int i = 1; i <= pageCount; i++) {
                            orderBaseConversationList.addAll(collectOrderConversationBySearchParam(webToken, QnaStatus.OPEN.getParamCode(), i));
                        }
                    }

                    // (2) "????????????" ??????????????? ????????????
                    String orderConversationResponse2 = reebonzWebPageService.collectOrderConversation(webToken, QnaStatus.REOPENED.getParamCode(), FIRST_PAGE_NUMBER);
                    int orderConversationCount2 = reebonzOrderConversationParser.parseOrderConversationCountByQnAStatus(orderConversationResponse2, QnaStatus.REOPENED);
                    int pageCount2;
                    if (orderConversationCount2 != 0) {
                        pageCount2 = getPageCount(orderConversationCount2, PAGE_SIZE);

                        for (int i = 1; i <= pageCount2; i++) {
                            orderBaseConversationList.addAll(collectOrderConversationBySearchParam(webToken, QnaStatus.REOPENED.getParamCode(), i));
                        }
                    }
                    collectOrderConversationResult.setSuccessFlag(true);
                    collectOrderConversationResult.setMessage("?????? ???????????? ?????? ??????");
                    break;
                case COMPLETE:
                    // "????????????" ??????????????? ????????????
                    orderConversationResponse = reebonzWebPageService.collectOrderConversation(webToken, QnaStatus.CLOSED.getParamCode(), FIRST_PAGE_NUMBER);
                    orderConversationCount = reebonzOrderConversationParser.parseOrderConversationCountByQnAStatus(orderConversationResponse, QnaStatus.CLOSED);
                    if (orderConversationCount != 0) {
                        pageCount = getPageCount(orderConversationCount, PAGE_SIZE);

                        for (int i = 1; i <= pageCount; i++) {
                            orderBaseConversationList.addAll(collectOrderConversationBySearchParam(webToken, QnaStatus.CLOSED.getParamCode(), i));
                        }
                    }
                    collectOrderConversationResult.setSuccessFlag(true);
                    collectOrderConversationResult.setMessage("?????? ????????? ???????????? ?????? ??????");
                    break;
                default:
                    collectOrderConversationResult.setMessage("???????????? ?????? ???????????? ?????????");
                    break;
            }

            if (!orderBaseConversationList.isEmpty()) {
                for (OrderBaseConversationDto orderBaseConversation : orderBaseConversationList) {
                    String qnaId = orderBaseConversation.getChannelId();
                    OrderBaseConversationDto orderBaseConversationDetail = getOrderBaseConversation(webToken, qnaId);

                    orderBaseConversation.setOrderId(
                            orderBaseConversationDetail.getOrderId());
                    orderBaseConversation.setOrderUniqueId(
                            orderBaseConversationDetail.getOrderUniqueId());
                    orderBaseConversation.setOrderBaseConversationMessageList(
                            orderBaseConversationDetail.getOrderBaseConversationMessageList());
                }
            }

        } catch(Exception e) {
            collectOrderConversationResult.setSuccessFlag(false);
            collectOrderConversationResult.setMessage("???????????? ?????? ??????");
            log.error("Failed", e);
        }

        return collectOrderConversationResult;
    }

    @Async
    @Override
    public ListenableFuture<OrderBaseConversationJobDto.Request.CollectCallback> collectOrderConversationFromShop(String webToken, long jobId, OrderBaseConversationJobDto.OrderConversationStatus orderConversationStatus, ShopAccountDto.Request request) {
        log.info("Call collectOrderConversationFromShop()");

        //????????? ???????????? ?????? ??????????????? ????????????.
        CollectOrderConversationResult collectOrderConversationResult = collectOrderConversation(webToken, orderConversationStatus);

        //????????? ????????? ????????? ????????????.
        OrderBaseConversationJobDto.Request.CollectCallback collectOrderConversationListCallback = new OrderBaseConversationJobDto.Request.CollectCallback();
        collectOrderConversationListCallback.getJobTaskResponseBaseDto().setJobId(jobId);
        collectOrderConversationListCallback.getJobTaskResponseBaseDto().setRequestId(request.getRequestId());
        collectOrderConversationListCallback.getJobTaskResponseBaseDto().setSuccessFlag(collectOrderConversationResult.isSuccessFlag());
        collectOrderConversationListCallback.getJobTaskResponseBaseDto().setMessage(collectOrderConversationResult.getMessage());
        collectOrderConversationListCallback.setShopAccount(modelMapper.map(request, ShopAccountDto.Response.class));
        if (!collectOrderConversationResult.getOrderBaseConversationList().isEmpty()) {
            collectOrderConversationListCallback.setOrderBaseConversationList(collectOrderConversationResult.getOrderBaseConversationList());
        }

        return new AsyncResult<>(collectOrderConversationListCallback);
    }

    @Override
    public ListenableFuture<OrderJobDto.Request.PostConversationCallback> postConversationMessageForOrderToShop(String webToKen, long jobId, OrderJobDto.Request.PostConversationJob postConversationJob) {
        log.info("Call postConversationMessageForOrderToShop()");

        boolean successFlag = false;
        String resultMessage;
        String orderId = postConversationJob.getShopOrderId();
        String channelId = postConversationJob.getChannelId();

        try {
            String postResponse = reebonzWebPageService.postOrderConversationReply(webToKen, postConversationJob);
            ReebonzBaseResponse<Object> postOrderConversationReplyReebonzBaseResponse = objectMapper.readValue(postResponse, new TypeReference<>(){});
            log.info("postOrderConversationReplyReebonzBaseResponse: {}", postOrderConversationReplyReebonzBaseResponse);
            if("success".equals(postOrderConversationReplyReebonzBaseResponse.getResult())){
                successFlag = true;
            }
            resultMessage = postOrderConversationReplyReebonzBaseResponse.getMessage();
        } catch (Exception e) {
            successFlag = false;
            resultMessage = "???????????? ????????? ?????? ??????";
            log.error("Failed", e);
        }

        OrderJobDto.Request.PostConversationCallback postConversationCallback = new OrderJobDto.Request.PostConversationCallback();
        OrderBaseConversationDto orderBaseConversation = new OrderBaseConversationDto();
        orderBaseConversation.setOrderId(orderId);
        orderBaseConversation.setChannelId(channelId);

        try {
            String response = reebonzWebPageService.collectOrderConversationDetail(webToKen, channelId);
            List<OrderBaseConversationMessageDto> orderBaseConversationMessageList = reebonzOrderConversationParser.getOrderConversationMessages(response, channelId);
            orderBaseConversation.setOrderBaseConversationMessageList(orderBaseConversationMessageList);
            postConversationCallback.getOrderBaseConversationList().add(orderBaseConversation);
        } catch (Exception e) {
            log.error("(???????????? ????????? ?????? ???) ???????????? ?????? ??????", e);
        }

        postConversationCallback.getJobTaskResponseBaseDto().setJobId(jobId);
        postConversationCallback.getJobTaskResponseBaseDto().setRequestId(postConversationJob.getShopAccount().getRequestId());
        postConversationCallback.getJobTaskResponseBaseDto().setSuccessFlag(successFlag);
        postConversationCallback.getJobTaskResponseBaseDto().setMessage(resultMessage);
        postConversationCallback.setShopAccount(modelMapper.map(postConversationJob.getShopAccount(), ShopAccountDto.Response.class));

        return new AsyncResult<>(postConversationCallback);
    }

    @Override
    public ListenableFuture<ShopQnaJobDto.Request.PostCallback> postAnswerForQnaToShop(String webToKen, long jobId, ShopQnaJobDto.Request.PostJob postJobDto) {
        log.info("postAnswerForQnaToShop Call!");
        boolean successFlag = false;
        String resultMessage = null;
        try{
            String postResponse = reebonzWebPageService.postAnswerForQna(webToKen, postJobDto);
            ReebonzBaseResponse<UpdateCommentReply> updateCommentReplyReebonzBaseResponse = objectMapper.readValue(postResponse,new TypeReference<>(){});
            log.info("updateCommentReplyReebonzBaseResponse: {}", updateCommentReplyReebonzBaseResponse);
            if("success".equals(updateCommentReplyReebonzBaseResponse.getResult())){
                successFlag = true;
            }
            resultMessage = updateCommentReplyReebonzBaseResponse.getMessage();

            //?????? ?????? ?????? ?????? ????????? ???????????? ?????? ????????? ?????? ????????? 1page??? ???????????? ??????
            String response = reebonzWebPageService.collectQna(webToKen, SearchIsReply.TOTAL.getParamCode());
            List<ShopQnaDto.Request.PostCallback> shopQnAList = reebonzQnaParser.parseQnaByQuestionId(response, postJobDto.getShopQna().getQuestionId());

            //?????? response ??????
            ShopQnaJobDto.Request.PostCallback postAnswerCallback = new ShopQnaJobDto.Request.PostCallback();
            postAnswerCallback.getJobTaskResponseBaseDto().setJobId(jobId);
            postAnswerCallback.getJobTaskResponseBaseDto().setRequestId(postJobDto.getShopQna().getShopQnaConversation().getRequestId());
            postAnswerCallback.getJobTaskResponseBaseDto().setSuccessFlag(successFlag);
            postAnswerCallback.getJobTaskResponseBaseDto().setMessage(resultMessage);
            postAnswerCallback.setShopAccount(modelMapper.map(postJobDto.getShopAccount(), ShopAccountDto.Response.class));
            postAnswerCallback.setShopQnAList(shopQnAList);

            return new AsyncResult<>(postAnswerCallback);
        }catch(Exception e){
            log.error("Failed",e);
            return null;
        }
    }

    private List<ShopQnaDto.Request.CollectCallback> collectQnABySearchParam(String webToKen, String searchIsReply) throws IOException {
        String response = reebonzWebPageService.collectQna(webToKen, searchIsReply);
        return reebonzQnaParser.parseQna(response);
    }

    private List<OrderBaseConversationDto> collectOrderConversationBySearchParam(String webToKen, String qnAStatus, int pageNumber) throws IOException {
        String response = reebonzWebPageService.collectOrderConversation(webToKen, qnAStatus, pageNumber);
        return reebonzOrderConversationParser.parseOrderConversation(response);
    }

    private OrderBaseConversationDto getOrderBaseConversation(String webToKen, String qnaId) throws IOException {
        String response = reebonzWebPageService.collectOrderConversationDetail(webToKen, qnaId);
        return reebonzOrderConversationParser.parseOrderBaseConversation(response, qnaId);
    }

    @Override
    public ListenableFuture<OrderJobDto.Request.CollectCallback> collectOrderFromShop(String token, long jobId, OrderJobDto.OrderProcessStatus orderProcessStatus, ShopAccountDto.Request request) {
        log.info("Reebonz collectOrderFromShop Call");
        log.info("token: {}",token);
//        return new AsyncResult<>(collectOrderFromShopByApi(token, jobId, orderProcessStatus, request));
        return new AsyncResult<>(collectOrderFromShopByWebPage(token, jobId, orderProcessStatus, request));
    }

    public OrderJobDto.Request.CollectCallback collectOrderFromShopByApi(String token, long jobId, OrderJobDto.OrderProcessStatus orderProcessStatus, ShopAccountDto.Request request){
        log.info("Reebonz collectOrderFromShopByApi Call");
        boolean successFlag = false;
        String resultMessage = "";
        List<OrderDto.Request.CollectCallback> collectOrderList = new ArrayList<>();
        try {
            ResponseEntity<ReebonzBaseResponse<ReebonzOrderDto.Response.Collect>> responseEntity = reebonzClient.getOrderList(
                    token,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "2020-09-01",//null,
                    "2020-09-18",//null,
                    null
            );

            if (responseEntity.getStatusCode() == HttpStatus.OK) { //???????????? ??????
                if (responseEntity.getBody() == null) { //body ??????
                    log.error("collectOrder Fail");
                    resultMessage = "Response Body is null";
                } else {
                    if ("success".equals(responseEntity.getBody().getResult())) { //????????? ??????
                        log.error("collectOrder Success");
                        successFlag = true;
                        resultMessage = responseEntity.getBody().getMessage();

                        log.info("{}",responseEntity.getBody().getData().getOrders());
                        for( ReebonzOrder reebonzOrder : responseEntity.getBody().getData().getOrders()){
                            collectOrderList.add(modelMapper.map(reebonzOrder, OrderDto.Request.CollectCallback.class));
                        }
                    } else {
                        log.error("collectOrder Fail - message: {}", responseEntity.getBody().getMessage());
                        resultMessage = responseEntity.getBody().getMessage();
                    }
                }
            } else {
                log.error("collectOrder Exception Fail");
                resultMessage = String.valueOf(responseEntity.getStatusCode());
            }
        }catch(Exception e){
            log.error("collectOrder Exception Fail",e);
            successFlag = false;
            resultMessage = "Exception";
        }

        OrderJobDto.Request.CollectCallback collectOrderCallback = new OrderJobDto.Request.CollectCallback();
        collectOrderCallback.getJobTaskResponseBaseDto().setJobId(jobId);
        collectOrderCallback.getJobTaskResponseBaseDto().setRequestId(request.getRequestId());
        collectOrderCallback.getJobTaskResponseBaseDto().setSuccessFlag(successFlag);
        collectOrderCallback.getJobTaskResponseBaseDto().setMessage(resultMessage);
        collectOrderCallback.setShopAccount(modelMapper.map(request, ShopAccountDto.Response.class));
        collectOrderCallback.setOrderList(collectOrderList);

        return collectOrderCallback;
    }

    /**
     * ?????? ????????? ?????? ???????????? ??????,?????? ????????? ???????????? ????????????.
     * @param webToken
     * @param reebonzOrderListType
     */
    private List<OrderDto.Request.CollectCallback> collectOrderFromShopByOrderListType(String webToken, ReebonzOrderListType reebonzOrderListType) throws IOException {
        List<OrderDto.Request.CollectCallback> collectCallbackListByExcel = new ArrayList<>();
        List<OrderDto.Request.CollectCallback> collectCallbackListByWeb = new ArrayList<>();

        //??? ????????? ????????? ?????? ????????? ???????????? ????????? case?????? ????????????.(????????????, ??????????????????, ????????????)
        switch(reebonzOrderListType){
            //??? ???????????? ????????? ???????????? ???????????? ?????? ???????????? ????????? ???????????? ???????????? ????????????.
            case PROCESSING_TOTAL_ORDER: //????????????-??????
                collectCallbackListByExcel.addAll(reebonzWebPageService.collectOrderProcessingFromExcel(webToken, reebonzOrderListType));
                collectCallbackListByWeb.addAll(reebonzWebPageService.collectOrderProcessingFromWeb(webToken, reebonzOrderListType));
                break;
            case CLAIM_TOTAL_ORDER: //??????????????????-??????
                collectCallbackListByExcel.addAll(reebonzWebPageService.collectOrderClaimFromExcel(webToken, reebonzOrderListType));
                collectCallbackListByWeb.addAll(reebonzWebPageService.collectOrderClaimFromWeb(webToken, reebonzOrderListType));
                break;
            case CALCULATION_SCHEDULE: //????????????-????????????
            case CALCULATION_COMPLETE: //????????????-????????????
            case CANCEL_COMPLETE: //????????????-????????????
            case RETURN_COMPLETE: //????????????-????????????
                collectCallbackListByExcel.addAll(reebonzWebPageService.collectOrderCompleteFromExcel(webToken, reebonzOrderListType));
                collectCallbackListByWeb.addAll(reebonzWebPageService.collectOrderCompleteFromWeb(webToken, reebonzOrderListType));
                break;
            default:
                log.error("Not Matching Request OrderListType:{}", reebonzOrderListType);
        }
        log.info("collectCallbackListByExcel:{}",collectCallbackListByExcel);

        //????????? ?????? ???????????? ????????????.(Excel??? ???????????? Web??? ?????? ????????? ?????????)
        return mergeWebOrderToExcelOrder(webToken, collectCallbackListByExcel, collectCallbackListByWeb);
    }

    private List<OrderDto.Request.CollectCallback> mergeWebOrderToExcelOrder(String webToken, List<OrderDto.Request.CollectCallback> excelOrderList, List<OrderDto.Request.CollectCallback> webOrderList){
        for(OrderDto.Request.CollectCallback collectCallbackExcel : excelOrderList){
            for(OrderDto.Request.CollectCallback collectCallbackWeb : webOrderList){
                //????????? ???????????? ?????????.
                if(collectCallbackExcel.getOrderUniqueId().equals(collectCallbackWeb.getOrderUniqueId())){
                    //????????? - ?????? ????????? ????????? ?????? ????????? ???????????? ????????? ??????????????? ????????????.,
                    collectCallbackExcel.setStatus(collectCallbackWeb.getStatus());
                    log.info("collectCallbackWeb.getStatus():{}",collectCallbackWeb.getStatus());
                    log.info("collectCallbackExcel.getStatus():{}",collectCallbackExcel.getStatus());
                    log.info("collectCallbackExcel.getOrderUniqueId():{}",collectCallbackExcel.getOrderUniqueId());
                    //???????????? ?????? ?????? ??????. ??? ????????? ??????
                    collectCallbackExcel.setBrandName(collectCallbackWeb.getBrandName());
                    //???????????? ????????? ???????????? ????????? ????????? ??? ????????? ??????
                    collectCallbackExcel.setOptionName(collectCallbackWeb.getOptionName());
                    //?????????????????? ????????? ???????????? ???????????? ????????????. ???????????? ??????
                    collectCallbackExcel.setTrackingNumber(collectCallbackWeb.getTrackingNumber());
                    collectCallbackExcel.setExchangeTrackingNumber(collectCallbackWeb.getExchangeTrackingNumber());
                    collectCallbackExcel.setReturnTrackingNumber(collectCallbackWeb.getReturnTrackingNumber());
                    //????????? ????????? ????????? ????????????.???????????? ??????
                    collectCallbackExcel.setCourierCode(collectCallbackWeb.getCourierCode());
                    collectCallbackExcel.setCourierName(collectCallbackWeb.getCourierName());
                    collectCallbackExcel.setExchangeCourierCode(collectCallbackWeb.getExchangeCourierCode());
                    collectCallbackExcel.setExchangeCourierName(collectCallbackWeb.getExchangeCourierName());
                    collectCallbackExcel.setReturnCourierCode(collectCallbackWeb.getReturnCourierCode());
                    collectCallbackExcel.setReturnCourierName(collectCallbackWeb.getReturnCourierName());
                    break;
                }
            }
        }

        //????????? ???????????? ?????? ??????????????? ????????????.
        CollectOrderConversationResult collectOrderConversationResult = collectOrderConversation(webToken, OrderBaseConversationJobDto.OrderConversationStatus.ALL);
        if(collectOrderConversationResult.isSuccessFlag()) { //???????????? ????????? ????????????.
            for (OrderDto.Request.CollectCallback collectCallback : excelOrderList) {
                for (OrderBaseConversationDto orderBaseConversationDto : collectOrderConversationResult.getOrderBaseConversationList()) {
                    //???????????? ???????????? ?????? ????????? ????????? ??????(????????? ????????? 2????????? ????????? ??????)
                    if(collectCallback.getOrderId().equals(orderBaseConversationDto.getOrderId())){
                        collectCallback.getOrderBaseConversationList().add(orderBaseConversationDto);
                    }
                }
            }
        }
        log.info("Add Conversation Result OrderList Size: {}",excelOrderList.size());

        return excelOrderList;
    }

    public OrderJobDto.Request.CollectCallback collectOrderFromShopByWebPage(String webToken, long jobId, OrderJobDto.OrderProcessStatus orderProcessStatus, ShopAccountDto.Request request){
        log.info("Reebonz collectOrderFromShopByWebPage Call");
        boolean successFlag = false;
        String resultMessage = "";
        List<OrderDto.Request.CollectCallback> collectOrderList = new ArrayList<>();
        try {
            //???????????? ?????? ????????? ???????????? ?????? ????????????.
            //???????????? ??????
            collectOrderList.addAll(collectOrderFromShopByOrderListType(webToken, ReebonzOrderListType.PROCESSING_TOTAL_ORDER));
            //??????,???????????? ??????
            collectOrderList.addAll(collectOrderFromShopByOrderListType(webToken, ReebonzOrderListType.CLAIM_TOTAL_ORDER));
            //????????????-????????????
            collectOrderList.addAll(collectOrderFromShopByOrderListType(webToken, ReebonzOrderListType.CALCULATION_SCHEDULE));
            //????????????-????????????
            collectOrderList.addAll(collectOrderFromShopByOrderListType(webToken, ReebonzOrderListType.CALCULATION_COMPLETE));
            //????????????-????????????
            collectOrderList.addAll(collectOrderFromShopByOrderListType(webToken, ReebonzOrderListType.CANCEL_COMPLETE));
            //????????????-????????????
            collectOrderList.addAll(collectOrderFromShopByOrderListType(webToken, ReebonzOrderListType.RETURN_COMPLETE));
            log.info("Collect Result OrderList Size: {}",collectOrderList.size());
            log.info("collectOrderList: {}",collectOrderList);

        }catch(Exception e){
            log.error("collectOrder Exception Fail",e);
            successFlag = false;
            resultMessage = "Exception";
        }

        OrderJobDto.Request.CollectCallback collectOrderCallback = new OrderJobDto.Request.CollectCallback();
        collectOrderCallback.getJobTaskResponseBaseDto().setJobId(jobId);
        collectOrderCallback.getJobTaskResponseBaseDto().setRequestId(request.getRequestId());
        collectOrderCallback.getJobTaskResponseBaseDto().setSuccessFlag(successFlag);
        collectOrderCallback.getJobTaskResponseBaseDto().setMessage(resultMessage);
        collectOrderCallback.setShopAccount(modelMapper.map(request, ShopAccountDto.Response.class));
        collectOrderCallback.setOrderList(collectOrderList);

        return collectOrderCallback;
    }


    public List<OrderDto.Request.CollectCallback> getOrderFromWebAndExcelByOrderId(String webToken, String orderId) throws IOException {
        log.info("Call getOrderFromWebAndExcelByOrderId");

        //1.???????????? ?????? ????????? ????????????.
        List<OrderDto.Request.CollectCallback> excelOrderList = reebonzWebPageService.collectOrderProcessingFromExcel(webToken, ReebonzOrderListType.PROCESSING_TOTAL_ORDER, orderId);
        if(excelOrderList.size() == 1){ //????????? ?????? ????????? Excel??? ????????? ????????????.
            return mergeWebOrderToExcelOrder(webToken, excelOrderList, reebonzWebPageService.collectOrderProcessingFromWeb(webToken, ReebonzOrderListType.PROCESSING_TOTAL_ORDER,orderId));
        }

        //2.??????,???????????? ?????? ????????? ????????????.
        excelOrderList = reebonzWebPageService.collectOrderClaimFromExcel(webToken, ReebonzOrderListType.CLAIM_TOTAL_ORDER, orderId);
        if(excelOrderList.size() == 1){ //????????? ?????? ????????? Excel??? ????????? ????????????.
            return mergeWebOrderToExcelOrder(webToken, excelOrderList, reebonzWebPageService.collectOrderClaimFromWeb(webToken, ReebonzOrderListType.CLAIM_TOTAL_ORDER,orderId));
        }

        //3.???????????? ?????? ????????? ????????????.
        excelOrderList = reebonzWebPageService.collectOrderCompleteFromExcel(webToken, ReebonzOrderListType.CALCULATION_SCHEDULE, orderId);
        if(excelOrderList.size() == 1){ //????????? ?????? ????????? Excel??? ????????? ????????????.
            return mergeWebOrderToExcelOrder(webToken, excelOrderList, reebonzWebPageService.collectOrderCompleteFromWeb(webToken, ReebonzOrderListType.CALCULATION_SCHEDULE,orderId));
        }

        //4.???????????? ?????? ????????? ????????????.
        excelOrderList = reebonzWebPageService.collectOrderCompleteFromExcel(webToken, ReebonzOrderListType.CALCULATION_COMPLETE, orderId);
        if(excelOrderList.size() == 1){ //????????? ?????? ????????? Excel??? ????????? ????????????.
            return mergeWebOrderToExcelOrder(webToken, excelOrderList, reebonzWebPageService.collectOrderCompleteFromWeb(webToken, ReebonzOrderListType.CALCULATION_COMPLETE,orderId));
        }

        //5.???????????? ?????? ????????? ????????????.
        excelOrderList = reebonzWebPageService.collectOrderCompleteFromExcel(webToken, ReebonzOrderListType.CANCEL_COMPLETE, orderId);
        if(excelOrderList.size() == 1){ //????????? ?????? ????????? Excel??? ????????? ????????????.
            return mergeWebOrderToExcelOrder(webToken, excelOrderList, reebonzWebPageService.collectOrderCompleteFromWeb(webToken, ReebonzOrderListType.CANCEL_COMPLETE,orderId));
        }

        //6.???????????? ?????? ????????? ????????????.
        excelOrderList = reebonzWebPageService.collectOrderCompleteFromExcel(webToken, ReebonzOrderListType.RETURN_COMPLETE, orderId);
        if(excelOrderList.size() == 1){ //????????? ?????? ????????? Excel??? ????????? ????????????.
            return mergeWebOrderToExcelOrder(webToken, excelOrderList, reebonzWebPageService.collectOrderCompleteFromWeb(webToken, ReebonzOrderListType.RETURN_COMPLETE,orderId));
        }

        //????????? ???????????? ?????? ??????????????? ????????????.
        CollectOrderConversationResult collectOrderConversationResult = collectOrderConversation(webToken, OrderBaseConversationJobDto.OrderConversationStatus.ALL);
        if(collectOrderConversationResult.isSuccessFlag()) { //???????????? ????????? ????????????.
            for (OrderDto.Request.CollectCallback collectCallback : excelOrderList) {
                for (OrderBaseConversationDto orderBaseConversationDto : collectOrderConversationResult.getOrderBaseConversationList()) {
                    //???????????? ???????????? ?????? ????????? ????????? ??????(????????? ????????? 2????????? ????????? ??????)
                    if(collectCallback.getOrderId().equals(orderBaseConversationDto.getOrderId())){
                        collectCallback.getOrderBaseConversationList().add(orderBaseConversationDto);
                    }
                }
            }
        }
        log.info("Add Conversation Result OrderList Size: {}",excelOrderList.size());

        return excelOrderList;
    }

    /**
     * ?????? ????????? ?????? ???????????? ?????? ??????,?????????,????????? ????????? ????????????.
     * @param requestStatus
     * @param shopCurrentStatus
     * @return
     */
    private ComparisonCheckResult comparisonCheckRequestStatusWithShopStatus(OrderJobDto.Request.OrderUpdateActionStatus requestStatus,
                                                                             OrderDto.OrderStatus shopCurrentStatus){
        switch(requestStatus){
            case EXCHANGE_CONFIRM: //?????????,??????????????? ???????????? ?????? ?????????
            case EXCHANGE_REJECT: //?????????,??????????????? ???????????? ?????? ?????????
            case CALCULATION_DELAY: //?????????,??????????????? ???????????? ?????? ?????????
            case BUY_CANCEL_REJECT: //??????????????? ???????????? ?????? ????????? - ????????? ???????????? ?????? ?????????
            case CALCULATION_SCHEDULE: //??????????????? ????????? ???????????? ????????? ?????? ????????? ????????? ????????? ??????
                return ComparisonCheckResult.IMPOSIBLE;
            case DELIVERY_READY: //?????? ????????? ?????? ??????(???????????? ??????
                switch(shopCurrentStatus){
                    case PAYMENT_COMPLETE: //????????????(PAYMENT_COMPLETE)
                        return ComparisonCheckResult.POSIBLE;
                    case DELIVERY_READY: //?????? ??????????????? ????????? ?????? ??????
                        return ComparisonCheckResult.NEEDLESS;
                    default:
                        return ComparisonCheckResult.IMPOSIBLE;
                }
            case DELIVERY:
                switch(shopCurrentStatus){
                    case PAYMENT_COMPLETE: //????????????(PAYMENT_COMPLETE)-????????? ????????? ????????????(????????????->???????????????->?????????) ->???????????????????????????
                    case DELIVERY_READY: //???????????????
                    case DELIVERY: //?????????(DELIVERY) - ?????????????????? ??????
                        return ComparisonCheckResult.POSIBLE;
                    default: //BUY_CANCEL_REQUEST: //????????? ???????????? ?????? - ???????????? ?????? ????????? ???????????? ??????
                        return ComparisonCheckResult.IMPOSIBLE;
                }
            case BUY_CANCEL_CONFIRM:
                switch(shopCurrentStatus){
                    case SELL_CANCEL: //???????????? ????????? ????????? ?????? ????????? ????????? ??????
                        return ComparisonCheckResult.NEEDLESS;
                    case BUY_CANCEL_REQUEST: //????????? ???????????? ?????? - ?????? ?????? ?????? ??????
                        return ComparisonCheckResult.POSIBLE;
                    default:
                        return ComparisonCheckResult.IMPOSIBLE;
                }
            case SELL_CANCEL: //???????????? ?????? ?????? ??????
                switch(shopCurrentStatus){
                    case SELL_CANCEL: //???????????? ????????? ?????? ???????????? ?????? ??????
                        return ComparisonCheckResult.NEEDLESS;
                    case PAYMENT_COMPLETE: //???????????? - ?????? ?????? ??????
                    case BUY_CANCEL_REQUEST: //?????? ?????? ?????? - ?????? ?????? ?????? ??????
                        return ComparisonCheckResult.POSIBLE;
                    default: //RETURN_REQUEST: ??????????????? ?????? ???????????? ????????????
                        return ComparisonCheckResult.IMPOSIBLE;
                }
            case RETURN_CONFIRM:
                switch(shopCurrentStatus){
                    case RETURN_COMPLETE: //???????????? ????????? ?????? ???????????? ?????? ??????
                        return ComparisonCheckResult.NEEDLESS;
                    case RETURN_REQUEST: //????????????(RETURN_REQUEST) - ??????????????? ??????????????? ?????? ??????
                    case RETURN_CONFIRM: //????????? ?????? ?????? ?????? ????????? ?????? ??????????????? ????????????.
                        return ComparisonCheckResult.POSIBLE;
                    default:
                        return ComparisonCheckResult.IMPOSIBLE;
                }
            case RETURN_REJECT:
                switch(shopCurrentStatus){
                    case DELIVERY: //????????? ????????? ?????? ???????????? ?????? ??????
                        return ComparisonCheckResult.NEEDLESS;
                    case RETURN_REQUEST: //????????????(RETURN_REQUEST) - ??????????????? ??????????????? ?????? ??????
                    case RETURN_CONFIRM: //???????????? ??? ??? ????????????(RETURN_CONFIRM) - ?????????????????? ??????
                        return ComparisonCheckResult.POSIBLE;
                    default:
                        return ComparisonCheckResult.IMPOSIBLE;
                }
            default:
                log.error("Not Matched Shop Status - ShopStatus:{}, RequestStatus:{}",shopCurrentStatus.name(), requestStatus.name());
                return ComparisonCheckResult.IMPOSIBLE;
        }
    }

    @Override
    public ListenableFuture<OrderJobDto.Request.UpdateCallback> updateOrderToShop(String webToken, long jobId, OrderJobDto.Request.UpdateJob updateJob) {
        log.info("Reebonz updateOrderToShop JOB START => jobId={}", jobId);
        boolean successFlag = false;
        String resultMessage = null;
        List<OrderDto.Request.CollectCallback> collectOrderList = new ArrayList<>();
        try {
            //1.?????? ?????? ??????
            collectOrderList = getOrderFromWebAndExcelByOrderId(webToken, updateJob.getShopOrderId());
            if (collectOrderList.size() != 1) {
                //?????? ????????? ??????
                log.error("updateOrderToShop - (Before update) Collect Order Fail!! - {}", collectOrderList);
                successFlag = false;
                resultMessage = "(???????????? ???) ?????? ?????? ??????";
            }else {
                //?????? ????????? ?????? ??????
                OrderDto.Request.CollectCallback collectOrder = collectOrderList.get(0);

                //2.???????????? ???????????? ????????????,???????????????,???????????? ????????? ????????????.
                ComparisonCheckResult comparisonCheckResult = comparisonCheckRequestStatusWithShopStatus(updateJob.getStatus(), collectOrder.getStatus());
                switch(comparisonCheckResult){
                    case POSIBLE:
                        //3-1.??????????????????
                        //?????? ??? ????????? ????????????.
                        String response = "";
                        switch(updateJob.getStatus()){
                            case DELIVERY_READY: //?????? ?????????(?????? ?????? ??????) ??????
                                response = reebonzWebPageService.updateOrderConfirm(webToken, updateJob);
                                break;
                            case DELIVERY: //????????? ???????????? ??????(???????????? ??????????????? ????????? ???????????? ?????? ??????)
                                response = reebonzWebPageService.updateOrderDelivery(webToken, updateJob);
                                break;
                            case BUY_CANCEL_CONFIRM: //?????? ?????? ?????? ??????
                                response = reebonzWebPageService.updateOrderBuyCancelConfirm(webToken, updateJob);
                                break;
                            case SELL_CANCEL: //????????????, ??????????????????
                                if(collectOrder.getStatus() == OrderDto.OrderStatus.PAYMENT_COMPLETE){
                                    //???????????? - ?????? ?????? ??????
                                    response = reebonzWebPageService.updateOrderSellCancel(webToken, updateJob);
                                }else if(collectOrder.getStatus() == OrderDto.OrderStatus.BUY_CANCEL_REQUEST){
                                    //?????? ?????? ?????? - ?????? ?????? ?????? ??????
                                    response = reebonzWebPageService.updateOrderBuyCancelConfirm(webToken, updateJob);
                                }else{
                                    log.error("Failed SELL_CANCEL process");
                                }
                                break;
                            case RETURN_CONFIRM:
                                if(collectOrder.getStatus() == OrderDto.OrderStatus.RETURN_CONFIRM){
                                    //?????? ?????? ????????? ?????? ??????????????? ????????????.
                                    response = reebonzWebPageService.updateOrderReturnComplete(webToken, updateJob);
                                }else if(collectOrder.getStatus() == OrderDto.OrderStatus.RETURN_REQUEST){
                                    //????????????(RETURN_REQUEST) - ??????????????? ??????????????? ?????? ??????(?????? ?????? ?????????)
                                    response = reebonzWebPageService.updateOrderReturnConfirm(webToken, updateJob);
                                    response = reebonzWebPageService.updateOrderReturnComplete(webToken, updateJob);
                                }else{
                                    log.error("Failed RETURN_CONFIRM process");
                                }
                                break;
                            case RETURN_REJECT:
                                //???????????? ??? ??? ????????????(RETURN_CONFIRM) - ?????????????????? ??????
                                //????????????(RETURN_REQUEST) - ??????????????? ??????????????? ?????? ??????(???????????? ???????????? ?????????)
                                response = reebonzWebPageService.updateOrderReturnReject(webToken, updateJob);
                                break;
                        }

                        log.info("result html : {}", response);
                        //?????? json??? ?????? ??????
                        //"{"result":"success","message":null}"
                        //"{"result":"failed","message":"Couldn't find OrderedItem with id=407272341"}"
                        ReebonzBaseResponse<Void> baseResponse = objectMapper.readValue(response, new TypeReference<>() {});
                        if("success".equals(baseResponse.getResult())){
                            successFlag = true;
                        }
                        resultMessage = baseResponse.getMessage();
                        log.info("resultMessage : {}", resultMessage);

                        //POST ?????? ?????? ????????? ?????? return??? ????????????.
                        collectOrderList = getOrderFromWebAndExcelByOrderId(webToken, updateJob.getShopOrderId());
                        if (collectOrderList.size() != 1) {
                            log.error("updateOrderToShop - (After update) Collect Order Fail!! - {}", collectOrderList);
                            successFlag = false;
                            resultMessage = "(???????????? ???) ?????? ?????? ??????";
                        }
                        break;
                    case NEEDLESS:
                        //3-2.?????????????????????(?????? ????????? ??????) - ?????? ???????????? ???????????? ????????? ????????? ????????????.
                        successFlag = true;
                        resultMessage = "???????????? ?????????";
                        break;
                    case IMPOSIBLE:
                        //3-3.??????????????????(?????? ?????? ???????????? ???????????? ??????) - ?????? ????????? ???????????? ????????? ????????? ????????????.
                        successFlag = false;
                        resultMessage = "???????????? ?????????";
                        break;
                }
            }
        }catch(Exception e){
            log.error("Reebonz updateOrderToShop Fail => jobId={}\n", jobId, e);
            resultMessage = e.getMessage();
        }

        //?????? response ??????
        OrderJobDto.Request.UpdateCallback updateCallback = new OrderJobDto.Request.UpdateCallback();
        updateCallback.getJobTaskResponseBaseDto().setJobId(jobId);
        updateCallback.getJobTaskResponseBaseDto().setRequestId(updateJob.getShopAccount().getRequestId());
        updateCallback.getJobTaskResponseBaseDto().setSuccessFlag(successFlag);
        updateCallback.getJobTaskResponseBaseDto().setMessage(resultMessage);
        updateCallback.setShopAccount(modelMapper.map(updateJob.getShopAccount(), ShopAccountDto.Response.class));
        updateCallback.setOrderList(collectOrderList);

        log.info("Reebonz updateOrderToShop JOB END => jobId={}", jobId);

        return new AsyncResult<>(updateCallback);
    }



    @Data
    private static class CollectOrderConversationResult{
        private boolean successFlag;
        private String message;
        List<OrderBaseConversationDto> orderBaseConversationList = new ArrayList<>();
    }

    /**
     * ?????? ????????? ???????????? ???????????? ???????????? ????????? ???????????? ????????????.
     * @return
     * @throws IOException
     */
    public boolean isSaleUpdatable(String token, String productNumber)
            throws IOException {
        log.info("Reebonz isProductSellingByProductNumber CALL");
        ReebonzSaleStatus reebonzSaleStatus = getReebonzSaleStatus(token, productNumber);
        return !Arrays.asList(ReebonzSaleStatus.SALE_STOP, ReebonzSaleStatus.NOT_FOUND_SALE).contains(reebonzSaleStatus);
    }

    /**
     * ????????? ????????? ????????? ?????????
     */
    public ReebonzSaleStatus getReebonzSaleStatus(String token, String productNumber) throws IOException {
        log.info("Reebonz getReebonzSaleStatus CALL");
        String sellingProductHtml = reebonzWebPageService.getSellingProductByProductNumber(token, productNumber);
        ReebonzProductSaleStatus reebonzProductSaleStatus = ReebonzProductParser.parseSaleStatus(sellingProductHtml);
        return ReebonzSaleStatus.getBySaleStatusAndQuantity(reebonzProductSaleStatus.getSaleStatus(), reebonzProductSaleStatus.getQuantity());
    }

}
