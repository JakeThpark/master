package com.wanpan.app.service.reebonz.parser;

import com.wanpan.app.dto.reebonz.ReebonzProductSaleStatus;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ReebonzProductParser {
    /*
     * 판매중 목록 페이지로부터 판매상태를 파싱한다.
     */
    public static ReebonzProductSaleStatus parseSaleStatus(String htmlContents){
        Document document = Jsoup.parse(htmlContents);
        Element saleStatusElement = document.select("div.table-cell.product-state > span.available").first();
        if (saleStatusElement == null) {
            return new ReebonzProductSaleStatus("삭제된글", 0);
        }
        String saleStatus = saleStatusElement.html();
        int quantity = Integer.parseInt(document.select("div.table-cell.product-state > span.qty").first().html());
        return new ReebonzProductSaleStatus(saleStatus, quantity);
    }
}
