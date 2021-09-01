package com.wanpan.app.service.mustit.parser;

import com.wanpan.app.config.PatternExtractor;
import com.wanpan.app.dto.ShopAccountDto;
import com.wanpan.app.dto.job.qna.ShopQnaDto;
import com.wanpan.app.dto.job.qna.ShopQnaConversationDto;
import com.wanpan.app.dto.job.qna.ShopQnaJobDto;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class MustitQnAParser {

    public static List<ShopQnaDto.Request.CollectCallback> parseQna(String contents, ShopAccountDto.Request request){
        Document document = Jsoup.parse(contents);
        //메인 리스트 블럭
        Element element = document.getElementsByClass("productQuestionList").first();
        Elements questionBlocks = element.getElementsByClass("mi-card-inquiry-wrapper");
        if(questionBlocks.size() == 0)
            log.info("문의가 존재하지 않습니다.");

        List<ShopQnaDto.Request.CollectCallback> shopQnAList = new ArrayList<>();
        for(Element eachQuestionBlock : questionBlocks){
//            log.info("==============================================");

            //문의상품 설명
            Element productElement = eachQuestionBlock.getElementsByClass("mi-column-border-box").first();
            Elements productFieldElements = productElement.getElementsByClass("mi-column-item");
            String thumbnail = productFieldElements.get(0).getElementsByClass("mi-image-border").first().getElementsByTag("img").attr("src");
//            log.info("thumbnail:{}", thumbnail);
            String price = productFieldElements.get(0).getElementsByClass("mi-roboto h3 mi-bold mi-font-darkblack").first().html();
//            log.info("price:{}", price);
            String productName = productFieldElements.get(0).getElementsByClass("mi-group-b40").first().getElementsByTag("p").first().html();
            String productId = productFieldElements.get(1).getElementsByTag("span").last().html();
            String questionWriter = productFieldElements.get(2).getElementsByTag("span").last().html();
            String questionNumber = PatternExtractor.MUSTIT_QNA_NUMBER.extract(
                    productFieldElements.get(3).getElementsByTag("input").attr("onclick"),
                    0
            ).replace("'", "");
            //문의 내용 블럭
            Element questionBlockElement = eachQuestionBlock.getElementsByClass("mi-accordion").first();
            //상단 타이틀(제목,문의상태,최초문의시간을 가지고 있음)
            Element questionTitleBlockElement = questionBlockElement.getElementsByClass("mi-accordion-title").first();
            Elements questionTitleFieldElements = questionTitleBlockElement.getElementsByClass("mi-column-layout mi-table").first().getElementsByClass("mi-column-item");
            //상태
            String questionStatus = questionTitleFieldElements.get(0).getElementsByTag("span").first().html();
            //문의제목
            String questionSubject = questionTitleFieldElements.get(1).getElementsByTag("span").first().html();
            //시간
            String questionTime = questionTitleFieldElements.get(2).getElementsByTag("span").first().html();

            ShopQnaDto.Request.CollectCallback shopQnA = new ShopQnaDto.Request.CollectCallback();
            shopQnA.setQuestionId(questionNumber);
            shopQnA.setShopProductSubject(productName);
            shopQnA.setShopProductId(productId);
            shopQnA.setShopProductLink("https://mustit.co.kr/product/product_detail/"+productId);
            shopQnA.setQuestionTitle(questionSubject);
            shopQnA.setPostAt(questionTime+":00");
            shopQnA.setQuestionWriter(questionWriter);
            if("답변대기".equals(questionStatus)){
                shopQnA.setQuestionStatus(ShopQnaJobDto.QuestionStatus.READY);
            }else{
                shopQnA.setQuestionStatus(ShopQnaJobDto.QuestionStatus.COMPLETE);
            }


            //문의,답변 대화 내용
            Element conversationBlockElement = questionBlockElement.getElementsByClass("mi-accordion-contents").first();
            //문의, 답변 리스트의 한줄 한줄에 해당되는 내용
            Elements conversationElements = conversationBlockElement.getElementsByClass("mi-card-inquiry-talklist");

            int conversationId = 1;
            //문의 대화 for
            for(Element eachConversationElement : conversationElements){ //각줄의 내용을 읽어온다.
                Elements fieldElements = eachConversationElement.getElementsByClass("mi-column-layout mi-table").first().getElementsByClass("mi-column-item");
                //질문 답변 타입(Q,A)
                String type = fieldElements.get(0).getElementsByTag("span").first().html().replace(".","");
                //인풋박스를 포함하고 있는경우 스킵한다.
                if(fieldElements.get(1).getElementsByClass("mi-inputbox").size() > 0){
//                    log.info("input box block");
                    continue;
                }

                //img 태그가 두개일 경우 첫째 태그는 첨부사진을 의미한다.
                Elements contentImgElements =  fieldElements.get(1).getElementsByTag("img");
                String contentImg = null;
                if(contentImgElements.size() == 2){
                    contentImg = contentImgElements.first().attr("src");
                }

                //비밀글 여부에 따라서 뒤에 이미지가 붙고 안붙고 해서 스트링 파싱을 추가
                String content = fieldElements.get(1).getElementsByClass("mi-font-basic").first().html().split("<")[0].trim();
                String time = fieldElements.get(2).getElementsByTag("span").first().html();

                ShopQnaConversationDto.Request.CollectCallback shopQnaConversationDto = new ShopQnaConversationDto.Request.CollectCallback();
                //Q일 경우는 제목이 존재하고 A일 경우는 제목이 존재하지 않는다.
                if("Q".equals(type)){
                    shopQnaConversationDto.setType("QUESTION");
                    shopQnaConversationDto.setWriterId(questionWriter);
                    shopQnaConversationDto.setWriterName(questionWriter);
                    shopQnaConversationDto.setSubject(questionSubject);
                }else{
                    shopQnaConversationDto.setType("ANSWER");
                    shopQnaConversationDto.setWriterId(request.getLoginId());
                    shopQnaConversationDto.setWriterName(request.getLoginId());
                    shopQnaConversationDto.setSubject("");
                }
                shopQnaConversationDto.setQuestionType("상품문의");
                shopQnaConversationDto.setQuestionId(questionNumber);
//                shopQnaConversationDto.setSubject(questionSubject);
                shopQnaConversationDto.setContentImagePath(contentImg);
                shopQnaConversationDto.setContent(content);
                shopQnaConversationDto.setConversationId(conversationId);
                shopQnaConversationDto.setPostAt(time+":00");

                shopQnA.getShopQnaConversationDtoList().add(shopQnaConversationDto);
                conversationId++;
            }
//            log.info("==============================================");

            shopQnAList.add(shopQnA);
        }


        return shopQnAList;
    }

    public static List<ShopQnaDto.Request.PostCallback> parseQnaByQuestionId(final String contents, ShopAccountDto.Request request, final String questionId){
        Document document = Jsoup.parse(contents);
        //메인 리스트 블럭
        Element element = document.getElementsByClass("productQuestionList").first();
        Elements questionBlocks = element.getElementsByClass("mi-card-inquiry-wrapper");
        if(questionBlocks.size() == 0)
            log.info("문의가 존재하지 않습니다.");

        List<ShopQnaDto.Request.PostCallback> shopQnAList = new ArrayList<>();
        for(Element eachQuestionBlock : questionBlocks){
//            log.info("==============================================");

            //문의상품 설명
            Element productElement = eachQuestionBlock.getElementsByClass("mi-column-border-box").first();
            Elements productFieldElements = productElement.getElementsByClass("mi-column-item");
            String questionNumber = PatternExtractor.MUSTIT_QNA_NUMBER.extract(
                    productFieldElements.get(3).getElementsByTag("input").attr("onclick"),
                    0
            ).replace("'", "");
            log.info("questionNumber:{}",questionNumber);
            if(!questionNumber.equals(questionId)){
                continue;
            }

            String thumbnail = productFieldElements.get(0).getElementsByClass("mi-image-border").first().getElementsByTag("img").attr("src");
            log.info("thumbnail:{}", thumbnail);
            String price = productFieldElements.get(0).getElementsByClass("mi-roboto h3 mi-bold mi-font-darkblack").first().html();
            log.info("price:{}", price);
            String productName = productFieldElements.get(0).getElementsByClass("mi-group-b40").first().getElementsByTag("p").first().html();
            String productId = productFieldElements.get(1).getElementsByTag("span").last().html();
            String questionWriter = productFieldElements.get(2).getElementsByTag("span").last().html();

            //문의 내용 블럭
            Element questionBlockElement = eachQuestionBlock.getElementsByClass("mi-accordion").first();
            //상단 타이틀(제목,문의상태,최초문의시간을 가지고 있음)
            Element questionTitleBlockElement = questionBlockElement.getElementsByClass("mi-accordion-title").first();
            Elements questionTitleFieldElements = questionTitleBlockElement.getElementsByClass("mi-column-layout mi-table").first().getElementsByClass("mi-column-item");
            //상태
            String questionStatus = questionTitleFieldElements.get(0).getElementsByTag("span").first().html();
            //문의제목
            String questionSubject = questionTitleFieldElements.get(1).getElementsByTag("span").first().html();
            //시간
            String questionTime = questionTitleFieldElements.get(2).getElementsByTag("span").first().html();

            ShopQnaDto.Request.PostCallback shopQnA = new ShopQnaDto.Request.PostCallback();
            shopQnA.setQuestionId(questionNumber);
            shopQnA.setShopProductSubject(productName);
            shopQnA.setShopProductId(productId);
            shopQnA.setShopProductLink("https://mustit.co.kr/product/product_detail/"+productId);
            shopQnA.setQuestionTitle(questionSubject);
            shopQnA.setPostAt(questionTime+":00");
            shopQnA.setQuestionWriter(questionWriter);
            if("답변대기".equals(questionStatus)){
                shopQnA.setQuestionStatus(ShopQnaJobDto.QuestionStatus.READY);
            }else{
                shopQnA.setQuestionStatus(ShopQnaJobDto.QuestionStatus.COMPLETE);
            }

            //문의,답변 대화 내용
            Element conversationBlockElement = questionBlockElement.getElementsByClass("mi-accordion-contents").first();
            //문의, 답변 리스트의 한줄 한줄에 해당되는 내용
            Elements conversationElements = conversationBlockElement.getElementsByClass("mi-card-inquiry-talklist");

            int conversationId = 1;
            //문의 대화 for
            for(Element eachConversationElement : conversationElements){ //각줄의 내용을 읽어온다.
                Elements fieldElements = eachConversationElement.getElementsByClass("mi-column-layout mi-table").first().getElementsByClass("mi-column-item");
                //질문 답변 타입(Q,A)
                String type = fieldElements.get(0).getElementsByTag("span").first().html().replace(".","");
                //인풋박스를 포함하고 있는경우 스킵한다.
                if(fieldElements.get(1).getElementsByClass("mi-inputbox").size() > 0){
//                    log.info("input box block");
                    continue;
                }

                //img 태그가 두개일 경우 첫째 태그는 첨부사진을 의미한다.
                Elements contentImgElements =  fieldElements.get(1).getElementsByTag("img");
                String contentImg = null;
                if(contentImgElements.size() == 2){
                    contentImg = contentImgElements.first().attr("src");
                }

                //비밀글 여부에 따라서 뒤에 이미지가 붙고 안붙고 해서 스트링 파싱을 추가
                String content = fieldElements.get(1).getElementsByClass("mi-font-basic").first().html().split("<")[0].trim();
                String time = fieldElements.get(2).getElementsByTag("span").first().html();

                ShopQnaConversationDto.Request.PostCallback shopQnaConversationDto = new ShopQnaConversationDto.Request.PostCallback();
                if("Q".equals(type)){
                    shopQnaConversationDto.setType("QUESTION");
                    shopQnaConversationDto.setWriterId(questionWriter);
                    shopQnaConversationDto.setWriterName(questionWriter);
                    shopQnaConversationDto.setSubject(questionSubject);
                }else{
                    shopQnaConversationDto.setType("ANSWER");
                    shopQnaConversationDto.setWriterId(request.getLoginId());
                    shopQnaConversationDto.setWriterName(request.getLoginId());
                    shopQnaConversationDto.setSubject("");
                }
                shopQnaConversationDto.setQuestionType("상품문의");
                shopQnaConversationDto.setQuestionId(questionNumber);
//                shopQnaConversationDto.setSubject(questionSubject);
                shopQnaConversationDto.setContentImagePath(contentImg);
                shopQnaConversationDto.setContent(content);
                shopQnaConversationDto.setConversationId(conversationId);
                shopQnaConversationDto.setPostAt(time+":00");

                shopQnA.getShopQnaConversationDtoList().add(shopQnaConversationDto);
                conversationId++;
            }
//            log.info("==============================================");

            shopQnAList.add(shopQnA);
        }


        return shopQnAList;
    }
}
