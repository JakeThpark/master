package com.wanpan.app.service.feelway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.wanpan.app.config.gateway.FeelwayClient;
import com.wanpan.app.dto.feelway.FeelwayProductForCreate;
import com.wanpan.app.dto.feelway.FeelwayProductForUpdate;
import com.wanpan.app.dto.feelway.FeelwaySignIn;
import com.wanpan.app.dto.job.OnlineSaleDto;
import com.wanpan.app.dto.job.order.OrderDto;
import com.wanpan.app.dto.job.order.OrderJobDto;
import com.wanpan.app.dto.job.qna.ShopQnaJobDto;
import com.wanpan.app.service.feelway.parser.FeelwaySignInParser;
import com.wanpan.app.service.feelway.util.FeelwayResponseConverter;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@AllArgsConstructor
public class FeelwayRequestPageService {
    @NonNull
    private final FeelwayClient feelwayClient;

    protected String requestSignInCheckPage(String session) throws IOException {
        return FeelwayResponseConverter
                .convert(feelwayClient.getForCheckingSignIn(session));
    }

    protected String requestSession() throws IOException {
        Connection.Response rs = feelwayClient.getCookie();
        return FeelwaySignInParser.getSession(rs.header("set-cookie"));
    }

    protected String requestSignIn(FeelwaySignIn feelwaySignIn, String session) throws IOException {
        Map<String, String> signInData = createSignInData(feelwaySignIn);
        Connection.Response rs = feelwayClient.login(session, signInData);
        return FeelwayResponseConverter.convert(rs);
    }

    protected String requestRegisterProductPage(String session) throws IOException {
        Map<String, String> data = new HashMap<>();
        String checkboxValue = "checkbox";
        data.put("check_ok", "ok");
        data.put("checkbox1", checkboxValue);
        data.put("checkbox2", checkboxValue);
        data.put("checkbox3", checkboxValue);
        Connection.Response rs = feelwayClient.getRegisterProduct(session, data);
        return FeelwayResponseConverter.convert(rs);
    }

    protected String requestUpdateProductPage(String session, OnlineSaleDto onlineSaleDto) throws IOException {
        Map<String, String> data = new HashMap<>();

        data.put("g_no", onlineSaleDto.getShopSale().getPostId());
        data.put("brand_no", onlineSaleDto.getBrandMap().getSourceCode());
        data.put("brand_name", onlineSaleDto.getBrandMap().getSourceName());
        data.put("g_name", URLEncoder.encode(onlineSaleDto.getSubject(), StandardCharsets.UTF_8));
        data.put("u_id", onlineSaleDto.getShopSale().getShopAccount().getLoginId());
        data.put("back_query_string", "");
        data.put("mode", "modify");

        Connection.Response rs = feelwayClient.getUpdateProduct(session, data);
        return FeelwayResponseConverter.convert(rs);
    }

    protected String requestFileUpload(String session, String uploadId, List<String> imageUrlList) throws IOException {
        // todo 한번에 이미지를 메모리로 읽어와 업로드하는 방식은 서버 메모리를 많이 차지할 수 있다.
        // todo 그러므로 이미지를 하나 읽어와 하나 올리는 방식으로 변경해야할 수도 있다.
        Map<String, InputStream> files = new HashMap<>();
        for (int i = 0, size = imageUrlList.size(), photoNumber = 1; i < size; i++) {
            if (photoNumber == 7) {
                photoNumber++;
            }
            files.put("g_photo" + photoNumber, getImageInputStream(imageUrlList.get(i)));
            photoNumber++;
        }
        if (imageUrlList.size() == 10) {
            files.put("g_photo" + 7, getImageInputStream(imageUrlList.get(9)));
        }

        Map<String, String> data = new HashMap<>();

        data.put("mode", "new");
        data.put("input_mode", "input");
        data.put("file_up_no", uploadId);
        Connection.Response rs = feelwayClient.postImages(session, data, files);
        return FeelwayResponseConverter.convert(rs);
    }

    protected String registerProduct(String session, FeelwayProductForCreate feelwayProduct)
            throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> data = objectMapper.convertValue(feelwayProduct, Map.class);
        Connection.Response rs = feelwayClient.registerProduct(session, data);
        return FeelwayResponseConverter.convert(rs);
    }

    protected String deleteProduct(String session, String productId) throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "del");
        data.put("g_no", productId);
        Connection.Response rs = feelwayClient.deleteProduct(session, data);
        return FeelwayResponseConverter.convert(rs);
    }

    protected String updateProduct(String session, FeelwayProductForUpdate feelwayProduct) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> data = objectMapper.convertValue(feelwayProduct, Map.class);
        log.info("==========Before Feelway updateProduct Form =>\n{}", data);
        Connection.Response rs = feelwayClient.registerProduct(session, data);
        String responseBody = FeelwayResponseConverter.convert(rs);
        log.info("==========After Feelway updateProduct Response Body =>\n{}", responseBody);
        return responseBody;
    }

    private InputStream getImageInputStream(String imageUrl) throws IOException {
        log.info("FeelwayRequestPageService.getImageInputStream: " + imageUrl);
        BufferedImage originalImage = ImageIO.read(new URL(imageUrl));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(originalImage, "PNG", outputStream);
        outputStream.flush();
        byte[] bytes = outputStream.toByteArray();
        outputStream.close();
        return new ByteArrayInputStream(bytes);
    }

    private Map<String, String> createSignInData(FeelwaySignIn feelwaySignIn) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        return objectMapper.convertValue(feelwaySignIn, Map.class);
    }

    protected String collectQna(String token, ShopQnaJobDto.QuestionStatus questionStatus, String askId) throws IOException {
        Map<String, String> data = new HashMap<>();
        if(questionStatus == ShopQnaJobDto.QuestionStatus.READY){
            data.put("mode", "noanswer");
        }
        if (askId != null && askId.length() > 0) {
            log.info("askId not empty:{}", askId);
            data.put("ask_id", askId);
        }

        Connection.Response rs = feelwayClient.getQnA(token, data);
        return FeelwayResponseConverter.convert(rs);
    }

    public String requestOrderFromExcel(String session, LocalDate startDate, LocalDate endDate) throws IOException {
        // end_date=2020.07.13&start_no=1
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        Map<String, String> data = new HashMap<>();
        data.put("buyer_id", "");
        data.put("buyer_name", "");
        data.put("find_order_no", "");
        data.put("find_g_no", "");
        data.put("find_g_name", "");
        data.put("check_o_result", "");
        data.put("start_date", startDate.format(formatter));
        data.put("end_date", endDate.format(formatter));
        data.put("start_no", "1");

        Connection.Response rs = feelwayClient.getOrderFromExcel(session, data);
        return FeelwayResponseConverter.convert(rs);
    }

    /**
     * 웹 주문 페이지로 부터 HTML형식을 받아온다.
     * @param session
     * @param startDate
     * @param endDate
     * @return
     * @throws IOException
     */
    public String requestOrderFromPage(String session, LocalDate startDate, LocalDate endDate, String pageNumber) throws IOException {
        // end_date=2020.07.13&start_no=1
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        return requestOrderFromPageByShopOrderId(session, startDate.format(formatter), endDate.format(formatter), "", pageNumber);
    }

    public String requestOrderFromPageByShopOrderId(String session, String startDate, String endDate, String shopOrderId, String pageNumber) throws IOException {
        // end_date=2020.07.13&start_no=1
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        Map<String, String> data = new HashMap<>();
        data.put("buyer_id", "");
        data.put("buyer_name", "");
        data.put("find_order_no", shopOrderId);
        data.put("find_g_no", "");
        data.put("find_g_name", "");
        data.put("start_date", startDate);
        data.put("end_date", endDate);
        //페이징이 들어왔을때는 해당 페이지를 읽어온다.
        if(!StringUtils.isEmpty(pageNumber)){
            data.put("page", pageNumber);
        }

        Connection.Response rs = feelwayClient.getOrderFromPage(session, data);
        return FeelwayResponseConverter.convert(rs);
    }

    protected String postAnswerForQna(String token, ShopQnaJobDto.Request.PostJob postJobRequest)
            throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "answer");
        data.put("re_main_no", postJobRequest.getShopQna().getQuestionId());//질문번호
        data.put("re_g_no", postJobRequest.getShopQna().getShopProductId());//상품번호
        data.put("re_u_id", postJobRequest.getShopQna().getShopQnaConversation().getWriterId());//답변자 아이디
        data.put("ask_id", postJobRequest.getShopQna().getQuestionWriter()); //질문자 아이디
        data.put("re_memo", postJobRequest.getShopQna().getShopQnaConversation().getSubject()); //post 답변 제목
        data.put("re_memo_text", postJobRequest.getShopQna().getShopQnaConversation().getContent()); //post 답변 내용

        Connection.Response rs = feelwayClient.postAnswerForQna(token, data);
        return FeelwayResponseConverter.convert(rs);
    }

    protected String postConversationMessageForOrder(String token, OrderJobDto.Request.PostConversationJob postConversationJob)
            throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("mode", "new");
        data.put("main_no", "");
        data.put("order_no", postConversationJob.getShopUniqueOrderId());//주문번호
        data.put("memo", postConversationJob.getOrderConversationMessage()); //post 내용

        Connection.Response rs = feelwayClient.postConversationMessageForOrder(token, data);
        return FeelwayResponseConverter.convert(rs);
    }

    /**
     * 결제완료된 주문에 대해서 판매 취소를 진행한다.
     * @param token
     * @return
     * @throws IOException
     */
    protected String updateCancelOrder(String token, String shopOrderId, String shopPostId, String sellerId, String buyerId, String cancelReason)
            throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("order_no", shopOrderId);
        data.put("g_no", shopPostId);
        data.put("go_no", "2"); //값에 따라 액션이 달라진다
        data.put("mode", "");
        data.put("u_id", sellerId); //판매자
        data.put("s_id", buyerId); //구매자
        data.put("page", "");
        data.put("check_o_result", "");
        data.put("cancel_reason", cancelReason);//취소사유

        log.info("data: {}",data);
        Connection.Response rs = feelwayClient.updateShopOrderByStatus(token, data);
        return FeelwayResponseConverter.convert(rs);
    }

    /**
     * 배송확인, 송장입력 진행한다.
     * @param token
     * @param mode 빈문자열"":생성, "change_send_info":수정
     * @return
     * @throws IOException
     */
    protected String createOrUpdateDeliveryOrder(String token,
                                         String shopOrderId,
                                         String shopPostId,
                                         String sellerId,
                                         String buyerId,
                                         String sellerMessage,
                                         String selectedCourierCode,
                                         String courierCustomName,
                                         String trackingNumber,
                                         String mode
    ) throws IOException {
        log.info("CALL createOrUpdateDeliveryOrder");
        Map<String, String> data = new HashMap<>();
        data.put("order_no", shopOrderId);
        data.put("g_no", shopPostId);
        data.put("go_no", "3"); //값에 따라 액션이 달라진다
        data.put("mode", mode); //생성시는 빈값, 배송정보변경시는 change_send_info
        data.put("u_id", sellerId); //판매자
        data.put("s_id", buyerId); //구매자
        data.put("page", "");
        data.put("check_o_result", "1");
        data.put("send_info1", selectedCourierCode); //택배사이름 선택 박스
        //직접 입력시 택배사이름(위의 값이 ETC일 경우 사용)
        data.put("send_info1_1", Objects.requireNonNullElse(courierCustomName, "")); //직접 입력시 택배사이름(위의 값이 ETC일 경우 사용)
        data.put("send_info2", trackingNumber); //송장번호
        //메모(구매자에게 하실말씀 간단히...)
        data.put("send_info3", Objects.requireNonNullElse(sellerMessage, ""));//메모(구매자에게 하실말씀 간단히...)
        data.put("eta_phone_check", "ok");//문자로 구매자에게 내용을 보낼껀지(최초 상태 변경시만 사용. 업데이트 시에는 빠지게 됨)

        log.info("data: {}",data);
        Connection.Response rs = feelwayClient.updateShopOrderByStatus(token, data);
        log.info("============");
        return FeelwayResponseConverter.convert(rs);
    }

    /**
     * 반품요청에 대해서 반품동의를 한다.
     * @param token
     * @return
     * @throws IOException
     */
    protected String updateReturnConfirm(String token, String shopOrderId, String shopPostId, String sellerId, String buyerId, String cancelReason)
            throws IOException {
        log.info("CALL updateReturnConfirm");
        Map<String, String> data = new HashMap<>();
        data.put("order_no", shopOrderId);
        data.put("g_no", shopPostId);
        data.put("go_no", "8"); //값에 따라 액션이 달라진다
        data.put("mode", "");
        data.put("u_id", sellerId); //판매자
        data.put("s_id", buyerId); //구매자
        data.put("page", "");
        data.put("check_o_result", "");

        log.info("data: {}",data);
        Connection.Response rs = feelwayClient.updateShopOrderByStatus(token, data);
        return FeelwayResponseConverter.convert(rs);
    }

    /**
     * 반품요청에 대해서 반품거부를 한다.
     * @param token
     * @return
     * @throws IOException
     */
    protected String updateReturnReject(String token, String shopOrderId, String shopPostId, String sellerId, String buyerId, String cancelReason)
            throws IOException {
        log.info("CALL updateReturnReject");
        Map<String, String> data = new HashMap<>();
        data.put("order_no", shopOrderId);
        data.put("g_no", shopPostId);
        data.put("go_no", "6"); //값에 따라 액션이 달라진다
        data.put("mode", "");
        data.put("u_id", sellerId); //판매자
        data.put("s_id", buyerId); //구매자
        data.put("page", "");
        data.put("check_o_result", "");

        log.info("data: {}",data);
        Connection.Response rs = feelwayClient.updateShopOrderByStatus(token, data);
        return FeelwayResponseConverter.convert(rs);
    }

    /**
     * 물건을 반송 받았을때 반품완료 처리를 한다.
     * @param token
     * @return
     * @throws IOException
     */
    protected String updateReturnComplete(String token, String shopOrderId, String shopPostId, String sellerId, String buyerId)
            throws IOException {
        log.info("CALL updateReturnComplete");
        Map<String, String> data = new HashMap<>();
        data.put("order_no", shopOrderId);
        data.put("g_no", shopPostId);
        data.put("go_no", "9"); //값에 따라 액션이 달라진다
        data.put("mode", "");
        data.put("u_id", sellerId); //판매자
        data.put("s_id", buyerId); //구매자
        data.put("page", "");
        data.put("check_o_result", "");

        log.info("data: {}",data);
        Connection.Response rs = feelwayClient.updateShopOrderByStatus(token, data);
        return FeelwayResponseConverter.convert(rs);
    }


    /**
     * 주문에 대한 상태변경을 한다. 여러 상태 변화에 대한 통합 메소드
     * @param token
     * @return
     * @throws IOException
     */
    protected String updateShopOrderByStatus(
            String token,
            String shopOrderId,
            String shopPostId,
            String buyerId,
            OrderDto.OrderStatus currentShopOrderStatus,
            OrderJobDto.Request.UpdateJob updateJobDto
    ) throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("order_no", shopOrderId);
        data.put("g_no", shopPostId);
        //상태값에 따라서 액션 넘버가 달라진다
        data.put("go_no", getGoNoFromOrderStatus(updateJobDto.getStatus()));
        data.put("u_id", updateJobDto.getShopAccount().getLoginId()); //판매자
        data.put("s_id", buyerId); //구매자
        data.put("page", "");
        data.put("check_o_result", "");

        //배송중-배송중 일때 배송정보 업데이트 모드, 배송중-배송중이 아닐때 배송중으로 돌리는 모드로 form값들 추가
        if(updateJobDto.getStatus() == OrderJobDto.Request.OrderUpdateActionStatus.DELIVERY){
            data.put("send_info1", updateJobDto.getCourier().getCode()); //택배사이름 선택 박스
            data.put("send_info1_1", Objects.requireNonNullElse(updateJobDto.getCourier().getCustomName(), "")); //직접 입력시 택배사이름(위의 값이 ETC일 경우 사용)
            data.put("send_info2", updateJobDto.getTrackingNumber()); //송장번호
            if(updateJobDto.getSellerMessage() == null){
                data.put("send_info3", "");//메모(구매자에게 하실말씀 간단히...)
            }else{
                data.put("send_info3", updateJobDto.getSellerMessage());//메모(구매자에게 하실말씀 간단히...)
            }
            if(currentShopOrderStatus == OrderDto.OrderStatus.DELIVERY){ //현재 샵의 상태가 배송중일때 변경 mode
                data.put("mode", "change_send_info");
            }else{
                data.put("mode", "");
                data.put("eta_phone_check", "ok");//문자로 구매자에게 내용을 보낼껀지(최초 상태 변경시만 사용. 업데이트 시에는 빠지게 됨)
            }
        }else{
            data.put("mode", "");
        }

        //취소일때 취소사유 form data
        if(updateJobDto.getStatus() == OrderJobDto.Request.OrderUpdateActionStatus.SELL_CANCEL){
            data.put("cancel_reason", updateJobDto.getSellerMessage());//취소사유
        }

        log.info("data: {}",data);
        Connection.Response rs = feelwayClient.updateShopOrderByStatus(token, data);
        return FeelwayResponseConverter.convert(rs);
    }

    private String getGoNoFromOrderStatus(OrderJobDto.Request.OrderUpdateActionStatus orderStatus){
        switch(orderStatus){
            case SELL_CANCEL:
            case BUY_CANCEL_CONFIRM: //구매취소 승인 -> 판매취소 액션과 동일
                return "2";
            case DELIVERY:
            case BUY_CANCEL_REJECT: //구매취소 거절 -> 배송확인(배송중) 액션과 동일
                return "3";
            case RETURN_REJECT:
                return "6";
            case RETURN_CONFIRM:
                return "8";
//            case RETURN_COMPLETE:
//                return "9";
            case CALCULATION_SCHEDULE:
                //TODO:정산요청 처리해야함
                return "???";
            default:
                //TODO:데이타 오류일때 처리방안
                return null;
        }
    }

    /**
     *
     * @return
     * @throws IOException
     */
    public String collectShopNotice()
            throws IOException {
        log.info("CALL collectShopNotice");
        Connection.Response rs = feelwayClient.getShopNotice();
        return FeelwayResponseConverter.convert(rs);
    }

    /**
     *
     * @return
     * @throws IOException
     */
    public String collectShopNoticeDetail(String noticeId)
            throws IOException {
        log.info("CALL collectShopNoticeDetail");
        Map<String, String> data = new HashMap<>();
        data.put("main_no", noticeId);
        Connection.Response rs = feelwayClient.getShopNoticeDetail(data);
        return FeelwayResponseConverter.convert(rs);
    }


    /**
     * 상품 번호로 페이지를 검색해서 상품 여부를 검사한다.
     * @return
     * @throws IOException
     */
    public String getProducyByProductNumberAndStatus(String token, String productNumber, boolean isSold)
            throws IOException {
        log.info("CALL existSalingProducyByProductNumber");

        Map<String, String> data = new HashMap<>();
        data.put("key_g_no", productNumber);
        data.put("plus_check", "2");
        if(isSold){
            data.put("check_sold", "sold");
        }
        Connection.Response rs = feelwayClient.getSellingProduct(token, data);
        return FeelwayResponseConverter.convert(rs);
    }

    /**
     * 판매중인 상품 목록을 조회한다.
     * @param token
     * @return
     * @throws IOException
     */
    public String getSellingProduct(String token) throws IOException {
        log.info("CALL checkSellingProduct");
        Connection.Response rs = feelwayClient.getSellingProduct(token, null);
        return FeelwayResponseConverter.convert(rs);
    }

    public String changeAbsenceProduct(String token, String productNumber, boolean isAbsence) throws IOException {
        log.info("CALL changeAbsenceProduct");

        Map<String, String> data = new HashMap<>();
        data.put("g_no", productNumber);
        if(isAbsence){
            data.put("mode", "absence");
        }else{
            data.put("mode", "not_absence");
        }

        Connection.Response rs = feelwayClient.changeAbsenceProduct(token, data);
        return FeelwayResponseConverter.convert(rs);
    }


}