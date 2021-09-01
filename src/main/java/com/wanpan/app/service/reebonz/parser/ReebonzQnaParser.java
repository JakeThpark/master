package com.wanpan.app.service.reebonz.parser;

import com.wanpan.app.config.PatternExtractor;
import com.wanpan.app.dto.job.qna.ShopQnaConversationDto;
import com.wanpan.app.dto.job.qna.ShopQnaDto;
import com.wanpan.app.dto.job.qna.ShopQnaJobDto;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ReebonzQnaParser {
    /*
     * QNA Page html을 파싱한다.
     */
    public List<ShopQnaDto.Request.CollectCallback> parseQna(String htmlContents){
        List<ShopQnaDto.Request.CollectCallback> shopQnAList = new ArrayList<>();
        Document document = Jsoup.parse(htmlContents);
        Elements elements = document.getElementsByClass("comments-list");
        if(elements.size() == 1){
            //각 문의 블럭단위
            Elements commentBlockElements = elements.first().getElementsByClass("block-comment-list");
            for(Element commentBlockElement : commentBlockElements){
                log.info("=============comment block start==================");
                //문의글 번호
                String questionNumber = commentBlockElement.attr("data-id");
                //class이름에 status에 따라 달라지기 때문에 div 마지막 태그로 뽑아낸다.
                Element headerElement = commentBlockElement.getElementsByClass("comment-header").first();
                String userEmail = headerElement.getElementsByClass("user-email").first().html();
                String productName = headerElement.getElementsByClass("product-name").first().html();
                String status = headerElement.getElementsByTag("div").last().html();
                log.info("status:{}",status);

                //body 블럭
                Element bodyElement = commentBlockElement.getElementsByClass("comment-body").first();

                //상품영역
                Element productElement = bodyElement.getElementsByClass("area-product").first();
                String thumbnailLink = productElement.getElementsByClass("product-img")
                        .first().getElementsByTag("img").first().attr("src");
                log.info("thumbnailLink:{}",thumbnailLink);

                Element productInfoElement = productElement.getElementsByClass("product-info").first();
                String productLink = productInfoElement.getElementsByTag("a").first().attr("href");
                String productId = PatternExtractor.REEBONZ_PRODUCT_NUMBER.extract(productLink);
                String productBrand = productInfoElement.getElementsByClass("brand").first().html();
                log.info("productLink:{}",productLink);
                log.info("productBrand:{}",productBrand);

                //SKU의 경우 존재하는 경우와 없는 경우가 존재한다.
//                String sellerSku = productInfoElement.getElementsByClass("seller-sku").first().html();
//                log.info("sellerSku:{}",sellerSku);

                ShopQnaDto.Request.CollectCallback shopQnA = new ShopQnaDto.Request.CollectCallback();
                shopQnA.setQuestionId(questionNumber);
                shopQnA.setShopProductSubject(productName);
                shopQnA.setShopProductId(productId);
                shopQnA.setShopProductLink(productLink);
                //제일 첫 질문글에서 입력해야 한다.
                shopQnA.setQuestionWriter(userEmail);
                shopQnA.setPostAt("");
                shopQnA.setQuestionTitle("");

                //질문답변 글 영역
                Element replyElement = bodyElement.getElementsByClass("area-reply").first();
                //답변 작성영역 제거
                replyElement.getElementsByClass("reply-box-input").first().remove();
                //코멘트 영역 추출
                Elements qnaElements = replyElement.getElementsByClass("reply-box");
                //질문 답변 루프
                int replyIndex = 1;
                String cmtId = questionNumber;
                for(Element qnaElement : qnaElements){
                    String type = "QUESTION";
                    if(qnaElement.attr("class").indexOf("answer") > 0){
                        type = "ANSWER";
                    }
                    String userId = qnaElement.attr("data-user-id");
                    String content = qnaElement.getElementsByClass("content").first().html();
                    String questionTime = qnaElement.getElementsByClass("time").first().html();

                    //첫번째 질문의 경우 문의 번호가 코멘트 ID가 된다. 그리고 QNA메인의 시간과 작성자를 세팅한다.
                    if(replyIndex > 1){
                        cmtId = qnaElement.getElementsByClass("cmt-id").first().html().replace("(","").replace(")","");
                    }else{ //QNA메인의 시간과 작성자를 세팅. 작성자의 경우 USERID를 추가로 기입한다.
                        shopQnA.setPostAt(convertQnaPostAt(questionTime));
                        shopQnA.setQuestionWriter(shopQnA.getQuestionWriter() +"(" + userId +")");
                        //리본즈 경우 문의 제목이 없으므로 목록에 보이는 제목을 내용으로 변경
                        shopQnA.setQuestionTitle(content);
                    }

                    ShopQnaConversationDto.Request.CollectCallback shopQnaConversationDto = new ShopQnaConversationDto.Request.CollectCallback();
                    shopQnaConversationDto.setType(type);
                    shopQnaConversationDto.setWriterId(userId);
                    shopQnaConversationDto.setWriterName(userId);
                    shopQnaConversationDto.setQuestionType("상품문의");
                    shopQnaConversationDto.setQuestionId(questionNumber);
                    shopQnaConversationDto.setSubject("");
                    shopQnaConversationDto.setContent(content);
                    shopQnaConversationDto.setConversationId(Long.parseLong(cmtId));
                    shopQnaConversationDto.setPostAt(convertQnaPostAt(questionTime));

                    shopQnA.getShopQnaConversationDtoList().add(shopQnaConversationDto);

                    replyIndex++;
                }
                //마지막 글의 내용이 답변일 경우 QnA를 답변완료 처리
                if(shopQnA.getShopQnaConversationDtoList().size() > 0
                        && "ANSWER".equals(shopQnA.getShopQnaConversationDtoList().get(shopQnA.getShopQnaConversationDtoList().size()-1).getType())
                ){
                    shopQnA.setQuestionStatus(ShopQnaJobDto.QuestionStatus.COMPLETE);
                }else{
                    shopQnA.setQuestionStatus(ShopQnaJobDto.QuestionStatus.READY);
                }
                shopQnAList.add(shopQnA);
            }
        }else{
            log.info("Over comments-list size:{}", elements.size());
        }

        return shopQnAList;
    }

    /*
     * QNA Page html을 파싱한다.
     */
    public List<ShopQnaDto.Request.PostCallback> parseQnaByQuestionId(String htmlContents, String questionId){
        List<ShopQnaDto.Request.PostCallback> shopQnAList = new ArrayList<>();
        Document document = Jsoup.parse(htmlContents);
        Elements elements = document.getElementsByClass("comments-list");
        if(elements.size() == 1){
            //각 문의 블럭단위
            Elements commentBlockElements = elements.first().getElementsByClass("block-comment-list");
            log.info("commentBlockElements: {}",commentBlockElements);
            for(Element commentBlockElement : commentBlockElements){
                log.info("=============comment block start==================");
                //문의글 번호
                String questionNumber = commentBlockElement.attr("data-id");
                if(!questionNumber.equals(questionId)){
                    log.info("Another Question number - questionNumber:{}, questionId:{}", questionNumber, questionId);
                    continue;
                }
                //class이름에 status에 따라 달라지기 때문에 div 마지막 태그로 뽑아낸다.
                Element headerElement = commentBlockElement.getElementsByClass("comment-header").first();
                String userEmail = headerElement.getElementsByClass("user-email").first().html();
                String productName = headerElement.getElementsByClass("product-name").first().html();
                String status = headerElement.getElementsByTag("div").last().html();
                log.info("status:{}",status);

                //body 블럭
                Element bodyElement = commentBlockElement.getElementsByClass("comment-body").first();

                //상품영역
                Element productElement = bodyElement.getElementsByClass("area-product").first();
                String thumbnailLink = productElement.getElementsByClass("product-img")
                        .first().getElementsByTag("img").first().attr("src");
                log.info("thumbnailLink:{}",thumbnailLink);

                Element productInfoElement = productElement.getElementsByClass("product-info").first();
                String productLink = productInfoElement.getElementsByTag("a").first().attr("href");
                String productId = PatternExtractor.REEBONZ_PRODUCT_NUMBER.extract(productLink);
                String productBrand = productInfoElement.getElementsByClass("brand").first().html();
                log.info("productLink:{}",productLink);
                log.info("productBrand:{}",productBrand);

                //SKU의 경우 존재하는 경우와 없는 경우가 존재한다.
//                String sellerSku = productInfoElement.getElementsByClass("seller-sku").first().html();
//                log.info("sellerSku:{}",sellerSku);

                ShopQnaDto.Request.PostCallback shopQnA = new ShopQnaDto.Request.PostCallback();
                shopQnA.setQuestionId(questionNumber);
                shopQnA.setShopProductSubject(productName);
                shopQnA.setShopProductId(productId);
                shopQnA.setShopProductLink(productLink);

                //제일 첫 질문글에서 입력해야 한다.
                shopQnA.setQuestionWriter(userEmail);
                shopQnA.setPostAt("");
                shopQnA.setQuestionTitle("");

                //질문답변 글 영역
                Element replyElement = bodyElement.getElementsByClass("area-reply").first();
                //답변 작성영역 제거
                replyElement.getElementsByClass("reply-box-input").first().remove();
                //코멘트 영역 추출
                Elements qnaElements = replyElement.getElementsByClass("reply-box");
                //질문 답변 루프
                int replyIndex = 1;
                String cmtId = questionNumber;
                for(Element qnaElement : qnaElements){
                    String type = "QUESTION";
                    if(qnaElement.attr("class").indexOf("answer") > 0){
                        type = "ANSWER";
                    }
                    String userId = qnaElement.attr("data-user-id");
                    String content = qnaElement.getElementsByClass("content").first().html();
                    String questionTime = qnaElement.getElementsByClass("time").first().html();

                    //첫번째 질문의 경우 문의 번호가 코멘트 ID가 된다. 그리고 QNA메인의 시간과 작성자를 세팅한다.
                    if(replyIndex > 1){
                        cmtId = qnaElement.getElementsByClass("cmt-id").first().html().replace("(","").replace(")","");
                    }else{ //QNA메인의 시간과 작성자를 세팅. 작성자의 경우 USERID를 추가로 기입한다.
                        shopQnA.setPostAt(convertQnaPostAt(questionTime));
                        shopQnA.setQuestionWriter(shopQnA.getQuestionWriter() +"(" + userId +")");
                        shopQnA.setQuestionTitle(content);
                    }

                    ShopQnaConversationDto.Request.PostCallback shopQnaConversationDto = new ShopQnaConversationDto.Request.PostCallback();
                    shopQnaConversationDto.setType(type);
                    shopQnaConversationDto.setWriterId(userId);
                    shopQnaConversationDto.setWriterName(userId);
                    shopQnaConversationDto.setQuestionType("상품문의");
                    shopQnaConversationDto.setQuestionId(questionNumber);
                    shopQnaConversationDto.setSubject("");
                    shopQnaConversationDto.setContent(content);
                    shopQnaConversationDto.setConversationId(Long.parseLong(cmtId));
                    shopQnaConversationDto.setPostAt(convertQnaPostAt(questionTime));

                    shopQnA.getShopQnaConversationDtoList().add(shopQnaConversationDto);

                    replyIndex++;
                }
                //마지막 글의 내용이 답변일 경우 QnA를 답변완료 처리
                if(shopQnA.getShopQnaConversationDtoList().size() > 0
                    && "ANSWER".equals(shopQnA.getShopQnaConversationDtoList().get(shopQnA.getShopQnaConversationDtoList().size()-1).getType())
                ){
                    shopQnA.setQuestionStatus(ShopQnaJobDto.QuestionStatus.COMPLETE);
                }else{
                    shopQnA.setQuestionStatus(ShopQnaJobDto.QuestionStatus.READY);
                }
                shopQnAList.add(shopQnA);
            }
        }else{
            log.info("Over comments-list size:{}", elements.size());
        }

        return shopQnAList;
    }

    private String convertQnaPostAt(String postAt){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy. MM. dd HH:mm:ss");
        LocalDateTime postDateTime = LocalDateTime.parse(postAt, formatter);
        DateTimeFormatter resultFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return postDateTime.format(resultFormatter);
    }

}
