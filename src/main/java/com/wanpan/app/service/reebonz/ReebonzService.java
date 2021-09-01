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
            //TODO:상태값이 200이 아닐 경우에 대한 처리
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
//                 * 리본즈 데이타 자체가 object와 string으로 때에 따라 복합적으로 주기 때문에 빈 string일 경우 null변환 처리를 추가한다.
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
     * 토큰이 사인인 상태로 유효한지를 체크해서 알려주는 메소드
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
     * 총 데이터 건수와 페이지 크기를 기준으로 총 페이지 수를 구한다
     */
    private int getPageCount(int dataCount, int pageSize) {
        return (dataCount % pageSize != 0) ? (dataCount / pageSize + 1) : (dataCount / pageSize);
    }

    /**
     * 로그인 여부 체크 - 해당 토큰이 유효한지를 체크한다.
     * @param bearerToken bearer토큰
     * @return 로그인여부
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
     * 로그인 여부 체크 - 해당 웹토큰이 유효한지를 체크한다.
     * @param webToken 웹페이지 로그인에 사용되는 토큰이나 세션
     * @return 로그인 여부
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
     * (WebPage 버전) 쇼핑몰에 판매글을 등록한다.
     * @param token login token(bearer)
     * @param job job DB ID
     * @param onlineSaleDto 판매글 데이타
     * @return 등록된 결과값
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
            message = "리본즈 판매 등록 실패";
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

    /** (사용 보류)
     * (API 버전) 쇼핑몰에 판매글을 등록한다.
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

            if (responseEntity.getStatusCode() == HttpStatus.OK) { //상태코드 체크
                if (responseEntity.getBody() == null) { //body 체크
                    log.error("Product Post Fail");
                    message = "Response Body is null";
                } else {
                    if ("success".equals(responseEntity.getBody().getResult())) { //결과값 체크
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
     * (API 버전) 판매글 등록을 위한 POST 데이터 생성
     */
    private ReebonzApiProductCreate makeReebonzProductPostData(OnlineSaleDto onlineSaleDto){
        ReebonzApiProductCreate reebonzApiProductCreate = new ReebonzApiProductCreate();
        reebonzApiProductCreate.setCreatedFrom(CREATED_FROM_KEY); //API 호출 아이디 (CollectQnaFromShopTaskService필수)
        reebonzApiProductCreate.setName(onlineSaleDto.getSubject());//상품명(필수)
        reebonzApiProductCreate.setCode(onlineSaleDto.getProductList().get(0).getOfficialSku());//Sku 정보(필수)
        reebonzApiProductCreate.setDescription(onlineSaleDto.getSaleReebonz().getDetail()); //상품 상세 설명
        reebonzApiProductCreate.setBrandId(Long.parseLong(onlineSaleDto.getBrandMap().getSourceCode())); //브랜드 ID - Brands API 참고(필수)

        reebonzApiProductCreate.setMarketplacePrice(onlineSaleDto.getPrice());//상품 마켓 가격(필수)
//        reebonzProductCreate.setCommission(); //수수료(0초과~1미만) ex:0.8 은 20%
        reebonzApiProductCreate.setMaterial(onlineSaleDto.getMaterial()); //상품 재질
//        reebonzProductCreate.setLegalInfo(); //법정 카테고리 - Templates API 선택 가능(템플릿 이름) - 값을 넘겨줄 경우 체크박스가 체크되서 내용이 들어감
//        reebonzProductCreate.setProductNotification(); //상품 품목 고시 정보 - 값을 넘겨줄 경우 체크박스가 체크되서 내용이 들어감
//        reebonzProductCreate.setProductTip(); //취급 유의 사항 - Templates API 선택 가능(템플릿 이름) - 값을 넘겨줄 경우 체크박스가 체크되서 내용이 들어감
        reebonzApiProductCreate.setSizeInfo(onlineSaleDto.getSize()); //사이즈 정보 - Templates API 선택 가능(템플릿 이름)

        // 카테고리 항목
        ShopCategoryDto genderCategory = onlineSaleDto.getShopSale().getShopCategory();
        ShopCategoryDto largeCategory = genderCategory.getChild();
        ShopCategoryDto mediumCategory = largeCategory.getChild();
        ShopCategoryDto smallCategory = mediumCategory.getChild();

        reebonzApiProductCreate.setCategoryGenderId(Integer.parseInt(genderCategory.getShopCategoryCode())); //카테고리 - Categories API 참고(남성 = 2, 여성 = 3)
        reebonzApiProductCreate.setCategoryMasterId(Integer.parseInt(largeCategory.getShopCategoryCode())); //카테고리 - Categories API 참고
        reebonzApiProductCreate.setCategorySlaveId(Integer.parseInt(mediumCategory.getShopCategoryCode())); //카테고리 - Categories API 참고
        List<ReebonzDetailImage> detailImages = new ArrayList<>();
        for(OnlineSaleImageDto onlineSaleImageDto : onlineSaleDto.getSaleImageList()){
            if(onlineSaleImageDto.getSequence() == 0){
                reebonzApiProductCreate.setImageMainUrl(onlineSaleImageDto.getOriginImagePath());
                reebonzApiProductCreate.setImageMainOverUrl(onlineSaleImageDto.getOriginImagePath()); //오버 메인 이미지
            }else{
                detailImages.add(new ReebonzDetailImage(onlineSaleImageDto.getOriginImagePath()));
            }

        }
        reebonzApiProductCreate.setDetailImages(detailImages);

        List<ReebonzStock> reebonzStocks = new ArrayList<>();

        // 상품 항목
        for (ProductDto productDto : onlineSaleDto.getProductList()) {
            for (ProductOptionDto productOptionDto : productDto.getProductOptionList()) {
                reebonzStocks.add(new ReebonzStock(
                        productDto.getClassificationValue(), //group
                        productDto.getClassificationValue() + "|" + productOptionDto.getName(), //name
                        productOptionDto.getQuantity() //stock
                ));
            }
        }
        reebonzApiProductCreate.setStocks(reebonzStocks); //<ReebonzStock> stocks; //상품 재고(필수)

        return reebonzApiProductCreate;
    }

    /**
     * (WebPage 버전) 쇼핑몰에 있는 판매글을 수정한다.
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
                // 현재 쇼핑몰에서 판매중지 상태인 경우
                log.info("Reebonz updateSaleToShop JOB END => jobId={}", jobId);
                return new AsyncResult<>(new RegisterDto.Response(
                        shopSaleDto.getId(),
                        postId,
                        ShopSaleJobDto.SaleStatus.SALE_STOP,
                        jobId,
                        shopSaleDto.getId(),
                        false,
                        "해당 판매글은 현재 리본즈에서 판매중지된 상태입니다.")
                );
            } else if (collectedReebonzSaleStatus == ReebonzSaleStatus.NOT_FOUND_SALE) {
                // 리본즈 상용기에서는 판매삭제 기능이 없어 판매글이 사라지는 경우는 없지만, 개발기에서는 매일 상용기 데이터를 덮어쓰는 상황이라 기존 판매글이 사라지는 경우가 발생한다.
                log.info("Reebonz updateSaleToShop JOB END => jobId={}", jobId);
                return new AsyncResult<>(new RegisterDto.Response(
                        shopSaleDto.getId(),
                        postId,
                        ShopSaleJobDto.SaleStatus.NOT_FOUND_SALE,
                        jobId,
                        shopSaleDto.getId(),
                        false,
                        "해당 판매글은 현재 리본즈에서 판매삭제된 상태입니다.")
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

        // 판매글 업데이트 성공이든 실패든 최종 쇼핑몰 판매글 상태 수집
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

    /** (사용 보류)
     * (API 버전) 쇼핑몰에 있는 판매글을 수정한다.
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

            if (responseEntity.getStatusCode() == HttpStatus.OK) { //상태코드 체크
                if (responseEntity.getBody() == null) { //body 체크
                    log.error("Product Put Fail");
                    message = "Response Body is null";
                } else {
                    if ("success".equals(responseEntity.getBody().getResult())) { //결과값 체크
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
                case SALE_STOP: // "판매중지" 설정
                case ON_SALE: // "판매중지" 해제
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
                    throw new InvalidRequestException("불가능한 판매글상태변경요청값: " + requestSaleStatus.name());
            }
        } catch (Exception e) {
            log.error("Reebonz updateSaleStatusToShop Error =>", e);
            successFlag = false;
            message = "리본즈 연결 오류";
        }

        // 판매상태 업데이트 성공이든 실패든 최종 쇼핑몰 판매글 상태 수집
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
     * 판매글을 삭제한다
     */
    @Override
    public ListenableFuture<RegisterDto.Response> deleteShopSale(String token, long jobId, ShopSaleJobDto.Request.DeleteSaleJob deleteSaleJob) {
        log.info("Reebonz deleteShopSale Call => jobId={}", jobId);

        String postId = deleteSaleJob.getPostId();
        boolean successFlag;
        String message;
        String postUpdateResponse;

        try {
            // 리본즈는 판매글 삭제 기능을 제공하지 않아 판매중지 기능으로 대체한다.
            postUpdateResponse = reebonzWebPageService.postProductSaleStop(token, postId, ReebonzSaleStatus.SALE_STOP);
            ReebonzWebPageProductSaleStopResponse reebonzWebPageProductDeleteResponse = objectMapper.readValue(postUpdateResponse, new TypeReference<>(){});
            if ("success".equals(reebonzWebPageProductDeleteResponse.getResult())) {
                successFlag = true;
                message = "판매중지 성공. 리본즈는 판매글 삭제 기능을 지원하지 않아 판매중지로 대체합니다.";
            } else {
                successFlag = false;
                message = "판매중지 실패. 리본즈는 판매글 삭제 기능을 지원하지 않아 판매중지로 대체합니다.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            successFlag = false;
            message = "리본즈 연결 오류";
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
        reebonzProductUpdate.setCreatedFrom(CREATED_FROM_KEY); //API 호출 아이디
        reebonzProductUpdate.setName(onlineSaleDto.getSubject());//상품명
        reebonzProductUpdate.setDescription(onlineSaleDto.getSaleReebonz().getDetail()); //상품 상세 설명
        reebonzProductUpdate.setMarketplacePrice(onlineSaleDto.getPrice());//상품 마켓 가격(필수)
//        reebonzProductUpdate.setCommission(); //수수료(0초과~1미만) ex:0.8 은 20%

        // 카테고리 항목 - 배열로 구성해서 요청한다.
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
                reebonzProductUpdate.setImageMainOverUrl(onlineSaleImageDto.getOriginImagePath()); //오버 메인 이미지
            }else{
                detailImages.add(new ReebonzDetailImage(onlineSaleImageDto.getOriginImagePath()));
            }
        }
        reebonzProductUpdate.setDetailImages(detailImages);
//브랜드 수정기능은 현재 제공하지 않음(제공시 입력값 대체)
//        reebonzProductUpdate.setBrandId(Long.parseLong(onlineSaleDto.getOnlineSaleReebonz().getBrandMap().getSourceCode()));

        //SKU 변경을 위해 들어온 데이타로 변경한다.
        reebonzProductUpdate.setMarketplaceCode(onlineSaleDto.getProductList().get(0).getCustomSku());
//        reebonzProductUpdate.setCode(onlineSaleDto.getProductList().get(0).getOfficialSku());

        return reebonzProductUpdate;
    }

    /**
     * 상품문의에 대한 수집
     */
    @Async
    @Override
    public ListenableFuture<ShopQnaJobDto.Request.CollectCallback> collectQnAFromShop(String webToken, long jobId, ShopQnaJobDto.QuestionStatus questionStatus, ShopAccountDto.Request request) {
        try {
//            String webToken = "01867d7371034a26268fb9dbd9cd9fe8";
//            List<ShopQnaDto.Request.CollectCallback> shopQnAList = new ArrayList<>();
//            switch(questionStatus){
//                case READY: //새 문의의 경우 - 새문의와 추가문의 모두를 담아야 한다.
//                    shopQnAList.addAll(collectQnABySearchParam(webToken, SearchIsReply.NEW.getParamCode()));
//                    shopQnAList.addAll(collectQnABySearchParam(webToken, SearchIsReply.ADD_NEW.getParamCode()));
//                    //문의번호 기준 정렬
//                    shopQnAList.sort((p1, p2) -> p2.getQuestionId().compareTo(p1.getQuestionId()));
//                    break;
//                case COMPLETE:
//                    shopQnAList.addAll(collectQnABySearchParam(webToken, SearchIsReply.COMPLETE.getParamCode()));
//                    break;
//                default:
//                    break;
//            }

            //현재 전체문의를 담는 구성으로 진행
            List<ShopQnaDto.Request.CollectCallback> shopQnAList = collectQnABySearchParam(webToken, SearchIsReply.TOTAL.getParamCode());
            //최종 response 구성
            ShopQnaJobDto.Request.CollectCallback collectShopQnAListCallback = new ShopQnaJobDto.Request.CollectCallback();
            collectShopQnAListCallback.getJobTaskResponseBaseDto().setJobId(jobId);
            collectShopQnAListCallback.getJobTaskResponseBaseDto().setRequestId(request.getRequestId());
            collectShopQnAListCallback.getJobTaskResponseBaseDto().setSuccessFlag(true);
            collectShopQnAListCallback.getJobTaskResponseBaseDto().setMessage("수집성공");
            collectShopQnAListCallback.setShopAccount(modelMapper.map(request, ShopAccountDto.Response.class));
            collectShopQnAListCallback.setShopQnAList(shopQnAList);

            return new AsyncResult<>(collectShopQnAListCallback);
        }catch(Exception e){
            log.error("Failed",e);
            return null;
        }
    }

    /**
     * 주문대화 수집 실제 모듈(주문수집,주문대화수집등 여러군데 사용을 위해 분리)
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
                    // 모든 주문대화를 수집한다
                    orderConversationResponse = reebonzWebPageService.collectOrderConversation(webToken, QnaStatus.ALL.getParamCode(), FIRST_PAGE_NUMBER);
                    orderConversationCount = reebonzOrderConversationParser.parseOrderConversationCountByQnAStatus(orderConversationResponse, QnaStatus.ALL);
                    if (orderConversationCount != 0) {
                        pageCount = getPageCount(orderConversationCount, PAGE_SIZE);

                        for (int i = 1; i <= pageCount; i++) {
                            orderBaseConversationList.addAll(collectOrderConversationBySearchParam(webToken, QnaStatus.ALL.getParamCode(), i));
                        }
                    }
                    collectOrderConversationResult.setSuccessFlag(true);
                    collectOrderConversationResult.setMessage("모든 주문대화 수집 성공");
                    break;
                case NEW:
                    // (1) "신규문의" 주문대화를 수집한다
                    orderConversationResponse = reebonzWebPageService.collectOrderConversation(webToken, QnaStatus.OPEN.getParamCode(), FIRST_PAGE_NUMBER);
                    orderConversationCount = reebonzOrderConversationParser.parseOrderConversationCountByQnAStatus(orderConversationResponse, QnaStatus.OPEN);
                    if (orderConversationCount != 0) {
                        pageCount = getPageCount(orderConversationCount, PAGE_SIZE);

                        for (int i = 1; i <= pageCount; i++) {
                            orderBaseConversationList.addAll(collectOrderConversationBySearchParam(webToken, QnaStatus.OPEN.getParamCode(), i));
                        }
                    }

                    // (2) "추가문의" 주문대화를 수집한다
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
                    collectOrderConversationResult.setMessage("신규 주문대화 수집 성공");
                    break;
                case COMPLETE:
                    // "처리완료" 주문대화를 수집한다
                    orderConversationResponse = reebonzWebPageService.collectOrderConversation(webToken, QnaStatus.CLOSED.getParamCode(), FIRST_PAGE_NUMBER);
                    orderConversationCount = reebonzOrderConversationParser.parseOrderConversationCountByQnAStatus(orderConversationResponse, QnaStatus.CLOSED);
                    if (orderConversationCount != 0) {
                        pageCount = getPageCount(orderConversationCount, PAGE_SIZE);

                        for (int i = 1; i <= pageCount; i++) {
                            orderBaseConversationList.addAll(collectOrderConversationBySearchParam(webToken, QnaStatus.CLOSED.getParamCode(), i));
                        }
                    }
                    collectOrderConversationResult.setSuccessFlag(true);
                    collectOrderConversationResult.setMessage("처리 완료된 주문대화 수집 성공");
                    break;
                default:
                    collectOrderConversationResult.setMessage("유효하지 않은 주문대화 상태값");
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
            collectOrderConversationResult.setMessage("주문대화 수집 실패");
            log.error("Failed", e);
        }

        return collectOrderConversationResult;
    }

    @Async
    @Override
    public ListenableFuture<OrderBaseConversationJobDto.Request.CollectCallback> collectOrderConversationFromShop(String webToken, long jobId, OrderBaseConversationJobDto.OrderConversationStatus orderConversationStatus, ShopAccountDto.Request request) {
        log.info("Call collectOrderConversationFromShop()");

        //웹에서 상태값에 따른 주문대화를 수집한다.
        CollectOrderConversationResult collectOrderConversationResult = collectOrderConversation(webToken, orderConversationStatus);

        //콜백에 전달할 객체를 구성한다.
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
            resultMessage = "주문대화 메세지 전송 실패";
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
            log.error("(주문대화 메세지 전송 후) 주문대화 수집 실패", e);
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

            //글이 달린 경우 전체 목록의 최상단에 표기 되므로 전체 목록의 1page만 끌어오면 된다
            String response = reebonzWebPageService.collectQna(webToKen, SearchIsReply.TOTAL.getParamCode());
            List<ShopQnaDto.Request.PostCallback> shopQnAList = reebonzQnaParser.parseQnaByQuestionId(response, postJobDto.getShopQna().getQuestionId());

            //최종 response 구성
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

            if (responseEntity.getStatusCode() == HttpStatus.OK) { //상태코드 체크
                if (responseEntity.getBody() == null) { //body 체크
                    log.error("collectOrder Fail");
                    resultMessage = "Response Body is null";
                } else {
                    if ("success".equals(responseEntity.getBody().getResult())) { //결과값 체크
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
     * 해당 타입의 주문 리스트를 엑셀,목록 등에서 취합해서 뽑아낸다.
     * @param webToken
     * @param reebonzOrderListType
     */
    private List<OrderDto.Request.CollectCallback> collectOrderFromShopByOrderListType(String webToken, ReebonzOrderListType reebonzOrderListType) throws IOException {
        List<OrderDto.Request.CollectCallback> collectCallbackListByExcel = new ArrayList<>();
        List<OrderDto.Request.CollectCallback> collectCallbackListByWeb = new ArrayList<>();

        //각 타입에 대해서 실제 주소가 달라지기 때문에 case별로 처리한다.(진행주문, 취소반품요청, 완료주문)
        switch(reebonzOrderListType){
            //각 타입별로 엑셀과 웹페이지 데이타를 각각 가져와서 두개의 데이타를 취합해서 사용한다.
            case PROCESSING_TOTAL_ORDER: //진행주문-전체
                collectCallbackListByExcel.addAll(reebonzWebPageService.collectOrderProcessingFromExcel(webToken, reebonzOrderListType));
                collectCallbackListByWeb.addAll(reebonzWebPageService.collectOrderProcessingFromWeb(webToken, reebonzOrderListType));
                break;
            case CLAIM_TOTAL_ORDER: //취소반품요청-전체
                collectCallbackListByExcel.addAll(reebonzWebPageService.collectOrderClaimFromExcel(webToken, reebonzOrderListType));
                collectCallbackListByWeb.addAll(reebonzWebPageService.collectOrderClaimFromWeb(webToken, reebonzOrderListType));
                break;
            case CALCULATION_SCHEDULE: //완료주문-정산예정
            case CALCULATION_COMPLETE: //완료주문-정산완료
            case CANCEL_COMPLETE: //완료주문-취소완료
            case RETURN_COMPLETE: //완료주문-반품완료
                collectCallbackListByExcel.addAll(reebonzWebPageService.collectOrderCompleteFromExcel(webToken, reebonzOrderListType));
                collectCallbackListByWeb.addAll(reebonzWebPageService.collectOrderCompleteFromWeb(webToken, reebonzOrderListType));
                break;
            default:
                log.error("Not Matching Request OrderListType:{}", reebonzOrderListType);
        }
        log.info("collectCallbackListByExcel:{}",collectCallbackListByExcel);

        //두개의 파싱 데이타를 취합한다.(Excel을 기준으로 Web에 추가 정보를 넣는다)
        return mergeWebOrderToExcelOrder(webToken, collectCallbackListByExcel, collectCallbackListByWeb);
    }

    private List<OrderDto.Request.CollectCallback> mergeWebOrderToExcelOrder(String webToken, List<OrderDto.Request.CollectCallback> excelOrderList, List<OrderDto.Request.CollectCallback> webOrderList){
        for(OrderDto.Request.CollectCallback collectCallbackExcel : excelOrderList){
            for(OrderDto.Request.CollectCallback collectCallbackWeb : webOrderList){
                //일치된 데이타를 찾는다.
                if(collectCallbackExcel.getOrderUniqueId().equals(collectCallbackWeb.getOrderUniqueId())){
                    //상태값 - 버튼 여부에 따라서 상세 상태를 결정하기 때문에 웹데이타를 사용한다.,
                    collectCallbackExcel.setStatus(collectCallbackWeb.getStatus());
                    log.info("collectCallbackWeb.getStatus():{}",collectCallbackWeb.getStatus());
                    log.info("collectCallbackExcel.getStatus():{}",collectCallbackExcel.getStatus());
                    log.info("collectCallbackExcel.getOrderUniqueId():{}",collectCallbackExcel.getOrderUniqueId());
                    //엑셀에서 별도 제공 안함. 웹 데이타 사용
                    collectCallbackExcel.setBrandName(collectCallbackWeb.getBrandName());
                    //엑셀에서 제목에 붙어있기 때문에 확실한 웹 데이타 사용
                    collectCallbackExcel.setOptionName(collectCallbackWeb.getOptionName());
                    //배송정보들은 주소를 제외하고 엑셀에서 제공안함. 웹데이타 사용
                    collectCallbackExcel.setTrackingNumber(collectCallbackWeb.getTrackingNumber());
                    collectCallbackExcel.setExchangeTrackingNumber(collectCallbackWeb.getExchangeTrackingNumber());
                    collectCallbackExcel.setReturnTrackingNumber(collectCallbackWeb.getReturnTrackingNumber());
                    //택배사 정보도 엑셀에 제공안함.웹데이타 사용
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

        //웹에서 상태값에 따른 주문대화를 수집한다.
        CollectOrderConversationResult collectOrderConversationResult = collectOrderConversation(webToken, OrderBaseConversationJobDto.OrderConversationStatus.ALL);
        if(collectOrderConversationResult.isSuccessFlag()) { //실패시에 처리를 스킵한다.
            for (OrderDto.Request.CollectCallback collectCallback : excelOrderList) {
                for (OrderBaseConversationDto orderBaseConversationDto : collectOrderConversationResult.getOrderBaseConversationList()) {
                    //주문번호 일치시에 해당 대화를 목록에 추가(하나의 주문당 2개이상 나올수 있다)
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
            //상태별에 따른 각각의 데이타를 모두 파싱한다.
            //진행주문 전체
            collectOrderList.addAll(collectOrderFromShopByOrderListType(webToken, ReebonzOrderListType.PROCESSING_TOTAL_ORDER));
            //취소,반품주문 전체
            collectOrderList.addAll(collectOrderFromShopByOrderListType(webToken, ReebonzOrderListType.CLAIM_TOTAL_ORDER));
            //완료주문-정산예정
            collectOrderList.addAll(collectOrderFromShopByOrderListType(webToken, ReebonzOrderListType.CALCULATION_SCHEDULE));
            //완료주문-정산완료
            collectOrderList.addAll(collectOrderFromShopByOrderListType(webToken, ReebonzOrderListType.CALCULATION_COMPLETE));
            //완료주문-취소완료
            collectOrderList.addAll(collectOrderFromShopByOrderListType(webToken, ReebonzOrderListType.CANCEL_COMPLETE));
            //완료주문-반품완료
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

        //1.진행주문 엑셀 주문을 수집한다.
        List<OrderDto.Request.CollectCallback> excelOrderList = reebonzWebPageService.collectOrderProcessingFromExcel(webToken, ReebonzOrderListType.PROCESSING_TOTAL_ORDER, orderId);
        if(excelOrderList.size() == 1){ //주문을 찾은 경우에 Excel을 합쳐서 리턴한다.
            return mergeWebOrderToExcelOrder(webToken, excelOrderList, reebonzWebPageService.collectOrderProcessingFromWeb(webToken, ReebonzOrderListType.PROCESSING_TOTAL_ORDER,orderId));
        }

        //2.취소,반품주문 엑셀 주문을 수집한다.
        excelOrderList = reebonzWebPageService.collectOrderClaimFromExcel(webToken, ReebonzOrderListType.CLAIM_TOTAL_ORDER, orderId);
        if(excelOrderList.size() == 1){ //주문을 찾은 경우에 Excel을 합쳐서 리턴한다.
            return mergeWebOrderToExcelOrder(webToken, excelOrderList, reebonzWebPageService.collectOrderClaimFromWeb(webToken, ReebonzOrderListType.CLAIM_TOTAL_ORDER,orderId));
        }

        //3.정산예정 엑셀 주문을 수집한다.
        excelOrderList = reebonzWebPageService.collectOrderCompleteFromExcel(webToken, ReebonzOrderListType.CALCULATION_SCHEDULE, orderId);
        if(excelOrderList.size() == 1){ //주문을 찾은 경우에 Excel을 합쳐서 리턴한다.
            return mergeWebOrderToExcelOrder(webToken, excelOrderList, reebonzWebPageService.collectOrderCompleteFromWeb(webToken, ReebonzOrderListType.CALCULATION_SCHEDULE,orderId));
        }

        //4.정산완료 엑셀 주문을 수집한다.
        excelOrderList = reebonzWebPageService.collectOrderCompleteFromExcel(webToken, ReebonzOrderListType.CALCULATION_COMPLETE, orderId);
        if(excelOrderList.size() == 1){ //주문을 찾은 경우에 Excel을 합쳐서 리턴한다.
            return mergeWebOrderToExcelOrder(webToken, excelOrderList, reebonzWebPageService.collectOrderCompleteFromWeb(webToken, ReebonzOrderListType.CALCULATION_COMPLETE,orderId));
        }

        //5.취소완료 엑셀 주문을 수집한다.
        excelOrderList = reebonzWebPageService.collectOrderCompleteFromExcel(webToken, ReebonzOrderListType.CANCEL_COMPLETE, orderId);
        if(excelOrderList.size() == 1){ //주문을 찾은 경우에 Excel을 합쳐서 리턴한다.
            return mergeWebOrderToExcelOrder(webToken, excelOrderList, reebonzWebPageService.collectOrderCompleteFromWeb(webToken, ReebonzOrderListType.CANCEL_COMPLETE,orderId));
        }

        //6.반품완료 엑셀 주문을 수집한다.
        excelOrderList = reebonzWebPageService.collectOrderCompleteFromExcel(webToken, ReebonzOrderListType.RETURN_COMPLETE, orderId);
        if(excelOrderList.size() == 1){ //주문을 찾은 경우에 Excel을 합쳐서 리턴한다.
            return mergeWebOrderToExcelOrder(webToken, excelOrderList, reebonzWebPageService.collectOrderCompleteFromWeb(webToken, ReebonzOrderListType.RETURN_COMPLETE,orderId));
        }

        //웹에서 상태값에 따른 주문대화를 수집한다.
        CollectOrderConversationResult collectOrderConversationResult = collectOrderConversation(webToken, OrderBaseConversationJobDto.OrderConversationStatus.ALL);
        if(collectOrderConversationResult.isSuccessFlag()) { //실패시에 처리를 스킵한다.
            for (OrderDto.Request.CollectCallback collectCallback : excelOrderList) {
                for (OrderBaseConversationDto orderBaseConversationDto : collectOrderConversationResult.getOrderBaseConversationList()) {
                    //주문번호 일치시에 해당 대화를 목록에 추가(하나의 주문당 2개이상 나올수 있다)
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
     * 요청 들어온 상태 기준으로 작업 필요,불필요,불가능 상태를 판단한다.
     * @param requestStatus
     * @param shopCurrentStatus
     * @return
     */
    private ComparisonCheckResult comparisonCheckRequestStatusWithShopStatus(OrderJobDto.Request.OrderUpdateActionStatus requestStatus,
                                                                             OrderDto.OrderStatus shopCurrentStatus){
        switch(requestStatus){
            case EXCHANGE_CONFIRM: //필웨이,리본즈에는 존재하지 않는 기능임
            case EXCHANGE_REJECT: //필웨이,리본즈에는 존재하지 않는 기능임
            case CALCULATION_DELAY: //필웨이,리본즈에는 존재하지 않는 기능임
            case BUY_CANCEL_REJECT: //리본즈에는 존재하지 않는 기능임 - 거절시 고객센타 이용 메세지
            case CALCULATION_SCHEDULE: //리본즈에서 정산은 자동으로 이루어 지기 때문에 처리할 사항이 없음
                return ComparisonCheckResult.IMPOSIBLE;
            case DELIVERY_READY: //배송 준비중 변경 요청(주문확인 버튼
                switch(shopCurrentStatus){
                    case PAYMENT_COMPLETE: //주문완료(PAYMENT_COMPLETE)
                        return ComparisonCheckResult.POSIBLE;
                    case DELIVERY_READY: //같은 상태일때는 작업이 필요 없음
                        return ComparisonCheckResult.NEEDLESS;
                    default:
                        return ComparisonCheckResult.IMPOSIBLE;
                }
            case DELIVERY:
                switch(shopCurrentStatus){
                    case PAYMENT_COMPLETE: //주문완료(PAYMENT_COMPLETE)-두단계 처리를 해줘야함(주문완료->배송준비중->배송중) ->배송중바로호출가능
                    case DELIVERY_READY: //배송준비중
                    case DELIVERY: //배송중(DELIVERY) - 송장업데이트 기능
                        return ComparisonCheckResult.POSIBLE;
                    default: //BUY_CANCEL_REQUEST: //배송전 구매취소 요청 - 리본즈는 취소 거절이 존재하지 않음
                        return ComparisonCheckResult.IMPOSIBLE;
                }
            case BUY_CANCEL_CONFIRM:
                switch(shopCurrentStatus){
                    case SELL_CANCEL: //판매취소 상태일 경우는 이미 처리된 상태로 간주
                        return ComparisonCheckResult.NEEDLESS;
                    case BUY_CANCEL_REQUEST: //배송전 구매취소 요청 - 취소 확인 버튼 기능
                        return ComparisonCheckResult.POSIBLE;
                    default:
                        return ComparisonCheckResult.IMPOSIBLE;
                }
            case SELL_CANCEL: //리본즈는 품절 버튼 처리
                switch(shopCurrentStatus){
                    case SELL_CANCEL: //판매취소 상태는 이미 처리되어 있는 상태
                        return ComparisonCheckResult.NEEDLESS;
                    case PAYMENT_COMPLETE: //주문완료 - 품절 버튼 기능
                    case BUY_CANCEL_REQUEST: //취소 요청 상태 - 취소 확인 버튼 기능
                        return ComparisonCheckResult.POSIBLE;
                    default: //RETURN_REQUEST: 반품요청일 경우 리본즈는 처리불가
                        return ComparisonCheckResult.IMPOSIBLE;
                }
            case RETURN_CONFIRM:
                switch(shopCurrentStatus){
                    case RETURN_COMPLETE: //반품완료 상태는 이미 처리되어 있는 상태
                        return ComparisonCheckResult.NEEDLESS;
                    case RETURN_REQUEST: //반품요청(RETURN_REQUEST) - 반품확인과 반품완료를 동시 처리
                    case RETURN_CONFIRM: //리본즈 경우 반품 확인 상태일 경우 완료처리를 수행한다.
                        return ComparisonCheckResult.POSIBLE;
                    default:
                        return ComparisonCheckResult.IMPOSIBLE;
                }
            case RETURN_REJECT:
                switch(shopCurrentStatus){
                    case DELIVERY: //배송중 상태는 이미 처리되어 있는 상태
                        return ComparisonCheckResult.NEEDLESS;
                    case RETURN_REQUEST: //반품요청(RETURN_REQUEST) - 반품확인과 반품거절을 동시 처리
                    case RETURN_CONFIRM: //반품확인 일 때 버튼존재(RETURN_CONFIRM) - 반품거절버튼 처리
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
            //1.현재 상태 수집
            collectOrderList = getOrderFromWebAndExcelByOrderId(webToken, updateJob.getShopOrderId());
            if (collectOrderList.size() != 1) {
                //수집 데이타 오류
                log.error("updateOrderToShop - (Before update) Collect Order Fail!! - {}", collectOrderList);
                successFlag = false;
                resultMessage = "(업데이트 전) 주문 수집 실패";
            }else {
                //현재 테이타 수집 성공
                OrderDto.Request.CollectCallback collectOrder = collectOrderList.get(0);

                //2.상태값을 비교해서 처리가능,처리불필요,처리불가 상태를 판별한다.
                ComparisonCheckResult comparisonCheckResult = comparisonCheckRequestStatusWithShopStatus(updateJob.getStatus(), collectOrder.getStatus());
                switch(comparisonCheckResult){
                    case POSIBLE:
                        //3-1.작업가능상태
                        //해야 할 작업을 선택한다.
                        String response = "";
                        switch(updateJob.getStatus()){
                            case DELIVERY_READY: //배송 준비중(주문 확인 버튼) 처리
                                response = reebonzWebPageService.updateOrderConfirm(webToken, updateJob);
                                break;
                            case DELIVERY: //배송중 송장저장 기능(주문완료 상태일때도 배송중 한번으로 처리 가능)
                                response = reebonzWebPageService.updateOrderDelivery(webToken, updateJob);
                                break;
                            case BUY_CANCEL_CONFIRM: //취소 확인 버튼 기능
                                response = reebonzWebPageService.updateOrderBuyCancelConfirm(webToken, updateJob);
                                break;
                            case SELL_CANCEL: //품절버튼, 취소확인버튼
                                if(collectOrder.getStatus() == OrderDto.OrderStatus.PAYMENT_COMPLETE){
                                    //주문완료 - 품절 버튼 기능
                                    response = reebonzWebPageService.updateOrderSellCancel(webToken, updateJob);
                                }else if(collectOrder.getStatus() == OrderDto.OrderStatus.BUY_CANCEL_REQUEST){
                                    //취소 요청 상태 - 취소 확인 버튼 기능
                                    response = reebonzWebPageService.updateOrderBuyCancelConfirm(webToken, updateJob);
                                }else{
                                    log.error("Failed SELL_CANCEL process");
                                }
                                break;
                            case RETURN_CONFIRM:
                                if(collectOrder.getStatus() == OrderDto.OrderStatus.RETURN_CONFIRM){
                                    //반품 확인 상태일 경우 완료처리를 수행한다.
                                    response = reebonzWebPageService.updateOrderReturnComplete(webToken, updateJob);
                                }else if(collectOrder.getStatus() == OrderDto.OrderStatus.RETURN_REQUEST){
                                    //반품요청(RETURN_REQUEST) - 반품확인과 반품완료를 동시 처리(두번 처리 해야함)
                                    response = reebonzWebPageService.updateOrderReturnConfirm(webToken, updateJob);
                                    response = reebonzWebPageService.updateOrderReturnComplete(webToken, updateJob);
                                }else{
                                    log.error("Failed RETURN_CONFIRM process");
                                }
                                break;
                            case RETURN_REJECT:
                                //반품확인 일 때 버튼존재(RETURN_CONFIRM) - 반품거절버튼 처리
                                //반품요청(RETURN_REQUEST) - 반품확인과 반품거절을 동시 처리(거절처리 한번으로 처리됨)
                                response = reebonzWebPageService.updateOrderReturnReject(webToken, updateJob);
                                break;
                        }

                        log.info("result html : {}", response);
                        //결과 json에 대한 처리
                        //"{"result":"success","message":null}"
                        //"{"result":"failed","message":"Couldn't find OrderedItem with id=407272341"}"
                        ReebonzBaseResponse<Void> baseResponse = objectMapper.readValue(response, new TypeReference<>() {});
                        if("success".equals(baseResponse.getResult())){
                            successFlag = true;
                        }
                        resultMessage = baseResponse.getMessage();
                        log.info("resultMessage : {}", resultMessage);

                        //POST 이후 다시 수집을 해서 return을 구성한다.
                        collectOrderList = getOrderFromWebAndExcelByOrderId(webToken, updateJob.getShopOrderId());
                        if (collectOrderList.size() != 1) {
                            log.error("updateOrderToShop - (After update) Collect Order Fail!! - {}", collectOrderList);
                            successFlag = false;
                            resultMessage = "(업데이트 후) 주문 수집 실패";
                        }
                        break;
                    case NEEDLESS:
                        //3-2.작업불필요상태(이미 적용된 상태) - 작업 성공으로 기록하고 조회된 주문을 내려준다.
                        successFlag = true;
                        resultMessage = "상태변경 불필요";
                        break;
                    case IMPOSIBLE:
                        //3-3.작업불가상태(전혀 다른 상태값을 가져있는 경우) - 작업 실패로 기록하고 조회된 주문을 내려준다.
                        successFlag = false;
                        resultMessage = "상태변경 불가능";
                        break;
                }
            }
        }catch(Exception e){
            log.error("Reebonz updateOrderToShop Fail => jobId={}\n", jobId, e);
            resultMessage = e.getMessage();
        }

        //최종 response 구성
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
     * 상품 번호로 페이지를 요청해서 판매수정 가능한 상태인지 검사한다.
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
     * 리본즈 판매글 상태를 구한다
     */
    public ReebonzSaleStatus getReebonzSaleStatus(String token, String productNumber) throws IOException {
        log.info("Reebonz getReebonzSaleStatus CALL");
        String sellingProductHtml = reebonzWebPageService.getSellingProductByProductNumber(token, productNumber);
        ReebonzProductSaleStatus reebonzProductSaleStatus = ReebonzProductParser.parseSaleStatus(sellingProductHtml);
        return ReebonzSaleStatus.getBySaleStatusAndQuantity(reebonzProductSaleStatus.getSaleStatus(), reebonzProductSaleStatus.getQuantity());
    }

}
