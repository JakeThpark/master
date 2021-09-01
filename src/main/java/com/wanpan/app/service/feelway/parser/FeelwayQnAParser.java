package com.wanpan.app.service.feelway.parser;

import com.wanpan.app.config.PatternExtractor;
import com.wanpan.app.dto.job.qna.ShopQnaDto;
import com.wanpan.app.dto.job.qna.ShopQnaConversationDto;
import com.wanpan.app.dto.job.qna.ShopQnaJobDto;
import com.wanpan.app.service.feelway.util.FeelwayResponseConverter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class FeelwayQnAParser {

    /**
     * 상품문의 수집된 html을 파싱해서 목록 전체를 뽑아낸다
     *
     * @param contentsHtml : html page contents
     * @return : CollectCallback
     */
    public static Collection<ShopQnaDto.Request.CollectCallback> parseQna(final String contentsHtml){
        log.info("parseQna CAll");
        Document document = Jsoup.parse(contentsHtml);
        Element trList = document.body()
                .select("#middle .inner > table")
                .get(3)
                .select(" > tbody > tr")
                .last()
                .select(" > td > table > tbody")
                .first();
        if(trList == null) {
            return Collections.emptyList();
        }
        Elements questionTrElements =
                trList
                .select(" > tr > td");

        int conversationId = 1;
        int questionCount = 0;
        int qnaCount = 0;
        String questionNumber = "";
        Map<String, ShopQnaDto.Request.CollectCallback> shopQnADtoMap = new HashMap<>();
        for(Element questionTrElement : questionTrElements){
            //Question과 Answer 구분자
            String tdClassName = questionTrElement.className();
            if("link".equals(tdClassName)){
                questionCount++;
                conversationId = 1; //질문단위로 오더링 초기화
                String productName = PatternExtractor.FEELWAY_QUESTION_BRAND_PRODUCT.extract(questionTrElement.html(),1);
//                String productName = questionTrElement.getElementsByTag("font").last().html();
                String questionSubject = questionTrElement.getElementsByTag("a").html();

                //html파싱 불가사항에 대해 string파싱(1:time, 2:writerId, 3:questionNumber, 4:productId, 5:questionContents)
                Map<Integer,String> fieldMap = PatternExtractor.FEELWAY_QUESTION_FIELDS.extractGroups(questionTrElement.html());
                questionNumber = fieldMap.get(4);
                //새 질문 객체를 만든다
                ShopQnaDto.Request.CollectCallback shopQnA = new ShopQnaDto.Request.CollectCallback();
                shopQnA.setQuestionId(questionNumber);
                shopQnA.setShopProductSubject(productName);
                shopQnA.setShopProductId(fieldMap.get(5));
                shopQnA.setShopProductLink("https://www.feelway.com/view_goods.php?g_no="+fieldMap.get(5));
                shopQnA.setQuestionTitle(questionSubject);
                shopQnA.setPostAt(FeelwayResponseConverter.timeConvert(fieldMap.get(1)));
                shopQnA.setQuestionWriter(fieldMap.get(2));
                shopQnA.setQuestionStatus(ShopQnaJobDto.QuestionStatus.READY);
                shopQnA.setPossibleSubjectInputFlag(true);

                ShopQnaConversationDto.Request.CollectCallback shopQnaConversationDto = new ShopQnaConversationDto.Request.CollectCallback();
                shopQnaConversationDto.setType("QUESTION");
                shopQnaConversationDto.setQuestionType("상품문의");
                shopQnaConversationDto.setQuestionId(questionNumber);
                shopQnaConversationDto.setWriterId(fieldMap.get(2));
                shopQnaConversationDto.setWriterName(fieldMap.get(2));
                shopQnaConversationDto.setSubject(questionSubject);
                shopQnaConversationDto.setContent(fieldMap.get(6));
                shopQnaConversationDto.setConversationId(conversationId);
                shopQnaConversationDto.setPostAt(FeelwayResponseConverter.timeConvert(fieldMap.get(1)));

                shopQnA.getShopQnaConversationDtoList().add(shopQnaConversationDto);

                //만들어진 질문 객체를 저장한다.
                shopQnADtoMap.put(questionNumber, shopQnA);
            }else if("un2".equals(tdClassName)){
                String questionSubject = questionTrElement.getElementsByTag("a").html();

                Map<Integer,String> fieldMap = PatternExtractor.FEELWAY_ANSWER_FIELDS.extractGroups(questionTrElement.html());
                ShopQnaConversationDto.Request.CollectCallback shopQnaConversationDto = new ShopQnaConversationDto.Request.CollectCallback();
                shopQnaConversationDto.setType("ANSWER");
                shopQnaConversationDto.setQuestionType("상품문의");
                shopQnaConversationDto.setQuestionId(questionNumber);
                shopQnaConversationDto.setWriterId(fieldMap.get(2));
                shopQnaConversationDto.setWriterName(fieldMap.get(2));
                shopQnaConversationDto.setSubject(questionSubject);
                shopQnaConversationDto.setContent(fieldMap.get(3).replace("&nbsp;&nbsp;&amp;nbsp",""));
                shopQnaConversationDto.setConversationId(conversationId);
                shopQnaConversationDto.setPostAt(FeelwayResponseConverter.timeConvert(fieldMap.get(1)));

                //답변을 기존 질문에 연결한다.
                shopQnADtoMap.get(questionNumber).setQuestionStatus(ShopQnaJobDto.QuestionStatus.COMPLETE);
                shopQnADtoMap.get(questionNumber).getShopQnaConversationDtoList().add(shopQnaConversationDto);
            }else{
                continue;
            }

            conversationId++;
            qnaCount++;
        }

        log.info("total qna count:{}",qnaCount);
        log.info("Question count:{}",questionCount);

        return shopQnADtoMap.values();
    }

    /**
     * 상품문의 수집된 html을 파싱해서 특정 질문에 해당하는 질의와 응답을 뽑아낸다
     *
     * @param contentsHtml : html page contents
     * @param questionId : 특정 질문 ID
     * @return : PostCallback
     */
    public static Collection<ShopQnaDto.Request.PostCallback> parseQnaByQuestionId(final String contentsHtml, final String questionId){
        log.info("parseQna CAll");
        Document document = Jsoup.parse(contentsHtml);
        Elements questionTrElements = document.body()
                .select("#middle .inner > table")
                .get(3)
                .select(" > tbody > tr")
                .last()
                .select(" > td > table > tbody")
                .first()
                .select(" > tr > td");

        int conversationId = 1;
        int questionCount = 0;
        int qnaCount = 0;
        String questionNumber = "";
        Map<String, ShopQnaDto.Request.PostCallback> shopQnADtoMap = new HashMap<>();
        for(Element questionTrElement : questionTrElements){
            //Question과 Answer 구분자
            String tdClassName = questionTrElement.className();
            if("link".equals(tdClassName)){
                questionCount++;
                conversationId = 1; //질문단위로 오더링 초기화
                //html파싱 불가사항에 대해 string파싱(1:time, 2:writerId, 3:questionNumber, 4:productId, 5:questionContents)
                Map<Integer,String> fieldMap = PatternExtractor.FEELWAY_QUESTION_FIELDS.extractGroups(questionTrElement.html());
                questionNumber = fieldMap.get(4);
                //질문번호가 같을 경우만 수행한다.
                if(!questionNumber.equals(questionId)){
                    continue;
                }
                String productName = PatternExtractor.FEELWAY_QUESTION_BRAND_PRODUCT.extract(questionTrElement.html(),1);
//                String productName = questionTrElement.getElementsByTag("font").last().html();
                String questionSubject = questionTrElement.getElementsByTag("a").html();

                //새 질문 객체를 만든다
                ShopQnaDto.Request.PostCallback shopQnA = new ShopQnaDto.Request.PostCallback();
                shopQnA.setQuestionId(questionNumber);
                shopQnA.setShopProductSubject(productName);
                shopQnA.setShopProductId(fieldMap.get(5));
                shopQnA.setShopProductLink("https://www.feelway.com/view_goods.php?g_no="+fieldMap.get(5));
                shopQnA.setQuestionTitle(questionSubject);
                shopQnA.setPostAt(FeelwayResponseConverter.timeConvert(fieldMap.get(1)));
                shopQnA.setQuestionWriter(fieldMap.get(2));
                shopQnA.setQuestionStatus(ShopQnaJobDto.QuestionStatus.READY);

                ShopQnaConversationDto.Request.PostCallback shopQnaConversationDto = new ShopQnaConversationDto.Request.PostCallback();
                shopQnaConversationDto.setType("QUESTION");
                shopQnaConversationDto.setQuestionType("상품문의");
                shopQnaConversationDto.setQuestionId(questionNumber);
                shopQnaConversationDto.setWriterId(fieldMap.get(2));
                shopQnaConversationDto.setWriterName(fieldMap.get(2));
                shopQnaConversationDto.setSubject(questionSubject);
                shopQnaConversationDto.setContent(fieldMap.get(6));
                shopQnaConversationDto.setConversationId(conversationId);
                shopQnaConversationDto.setPostAt(FeelwayResponseConverter.timeConvert(fieldMap.get(1)));

                shopQnA.getShopQnaConversationDtoList().add(shopQnaConversationDto);

                //만들어진 질문 객체를 저장한다.
                shopQnADtoMap.put(questionNumber, shopQnA);
            }else if("un2".equals(tdClassName)){
                //질문번호가 같을 경우만 수행한다.
                if(!questionNumber.equals(questionId)){
                    continue;
                }
                String questionSubject = questionTrElement.getElementsByTag("a").html();

                Map<Integer,String> fieldMap = PatternExtractor.FEELWAY_ANSWER_FIELDS.extractGroups(questionTrElement.html());
                ShopQnaConversationDto.Request.PostCallback shopQnaConversationDto = new ShopQnaConversationDto.Request.PostCallback();
                shopQnaConversationDto.setType("ANSWER");
                shopQnaConversationDto.setQuestionType("상품문의");
                shopQnaConversationDto.setQuestionId(questionNumber);
                shopQnaConversationDto.setWriterId(fieldMap.get(2));
                shopQnaConversationDto.setWriterName(fieldMap.get(2));
                shopQnaConversationDto.setSubject(questionSubject);
                shopQnaConversationDto.setContent(fieldMap.get(3).replace("&nbsp;&nbsp;&amp;nbsp",""));
                shopQnaConversationDto.setConversationId(conversationId);
                shopQnaConversationDto.setPostAt(FeelwayResponseConverter.timeConvert(fieldMap.get(1)));

                //답변을 기존 질문에 연결한다.
                shopQnADtoMap.get(questionNumber).setQuestionStatus(ShopQnaJobDto.QuestionStatus.COMPLETE);
                shopQnADtoMap.get(questionNumber).getShopQnaConversationDtoList().add(shopQnaConversationDto);
            }else{
                continue;
            }

            conversationId++;
            qnaCount++;
        }

        log.info("total qna count:{}",qnaCount);
        log.info("Question count:{}",questionCount);

        return shopQnADtoMap.values();
    }
}
