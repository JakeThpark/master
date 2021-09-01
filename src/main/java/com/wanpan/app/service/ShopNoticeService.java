package com.wanpan.app.service;

import com.wanpan.app.dto.ShopNoticeDto;
import com.wanpan.app.entity.Shop;
import com.wanpan.app.entity.ShopNotice;
import com.wanpan.app.repository.ShopNoticeRepository;
import com.wanpan.app.repository.ShopRepository;
import com.wanpan.app.service.feelway.FeelwayRequestPageService;
import com.wanpan.app.service.feelway.parser.FeelwayNoticeParser;
import com.wanpan.app.service.mustit.MustitService;
import com.wanpan.app.service.mustit.parser.MustitNoticeParser;
import com.wanpan.app.service.reebonz.ReebonzWebPageService;
import com.wanpan.app.service.reebonz.parser.ReebonzNoticeParser;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@AllArgsConstructor
public class ShopNoticeService {
    private FeelwayRequestPageService feelwayRequestPageService;
    private ReebonzWebPageService reebonzWebPageService;
    private MustitService mustitService;
    private ShopNoticeRepository shopNoticeRepository;
    private ShopRepository shopRepository;
    private ModelMapper modelMapper;

    private final String REEBONZ_ADMIN_ACCOUNT = "amhoking@gmail.com";
    private final String REEBONZ_ADMIN_PASSWORD = "!bitcoin369";

    public void parseFeelwayNotice() throws IOException {
        //현재 존재하는 공지사항의 마지막을 가져온다.
        Shop shop = shopRepository.findById("FEELWAY");
        String finalShopNoticeId = getMaxNoticeIdByShop(shop);
        log.info("finalShopNoticeId:{}", finalShopNoticeId);

        //Notice의 첫페이지 리스트를 가져온다.
        String noticeHtmlContents = feelwayRequestPageService.collectShopNotice();
        List<ShopNoticeDto> shopNoticeDtoList = FeelwayNoticeParser.parseShopNotice(noticeHtmlContents);

        List<ShopNotice> requireSaveNoticeList = new ArrayList<>();
        //가져온 리스트를 가지고 가지고 오지 않은 공지사항에 대해서 상세를 가져온다.
        for(ShopNoticeDto shopNoticeDto : shopNoticeDtoList){
            //DB의 마지막 공지보다 작거나 같은 경우는 스킵처리
            if(finalShopNoticeId != null
                    && finalShopNoticeId.compareTo(shopNoticeDto.getShopNoticeId()) >= 0){
                continue;
            }
            //새로운 공지사항이므로 처리한다.
            log.info("shopNoticeDto:{}",shopNoticeDto);
            String noticeDetailHtmlContents = feelwayRequestPageService.collectShopNoticeDetail(shopNoticeDto.getShopNoticeId());
            String noticeDetail = FeelwayNoticeParser.parseShopNoticeDetail(noticeDetailHtmlContents);
            shopNoticeDto.setContents(noticeDetail);
            log.info("{}",shopNoticeDto);

            //Entity Data를 만든다
            ShopNotice shopNotice = modelMapper.map(shopNoticeDto, ShopNotice.class);
            shopNotice.setShop(shop);
            requireSaveNoticeList.add(shopNotice);
        }

        shopNoticeRepository.saveAll(requireSaveNoticeList);
    }

    public void parseMustitNotice() throws IOException {
        //현재 존재하는 공지사항의 마지막을 가져온다.
        Shop shop = shopRepository.findById("MUSTIT");
        String finalShopNoticeId = getMaxNoticeIdByShop(shop);
        log.info("finalShopMustItNoticeId:{}", finalShopNoticeId);

        //Notice의 첫페이지 리스트를 가져온다.
        String noticeHtmlContents = mustitService.collectShopNotice();
        List<ShopNoticeDto> shopNoticeDtoList = MustitNoticeParser.parseShopNotice(noticeHtmlContents);
//
        List<ShopNotice> requireSaveNoticeList = new ArrayList<>();
        //가져온 리스트를 가지고 가지고 오지 않은 공지사항에 대해서 상세를 가져온다.
        for(ShopNoticeDto shopNoticeDto : shopNoticeDtoList){
            //DB의 마지막 공지보다 작거나 같은 경우는 스킵처리
            if(finalShopNoticeId != null
                    && finalShopNoticeId.compareTo(shopNoticeDto.getShopNoticeId()) >= 0){
                continue;
            }
            //새로운 공지사항이므로 처리한다.
            log.info("shopNoticeDto:{}",shopNoticeDto);
            String noticeDetailHtmlContents = mustitService.collectShopNoticeDetail(shopNoticeDto.getShopNoticeId());
            String noticeDetail = MustitNoticeParser.parseShopNoticeDetail(noticeDetailHtmlContents);
            shopNoticeDto.setContents(noticeDetail);
            log.info("{}",shopNoticeDto);

            //Entity Data를 만든다
            ShopNotice shopNotice = modelMapper.map(shopNoticeDto, ShopNotice.class);
            shopNotice.setShop(shop);
            requireSaveNoticeList.add(shopNotice);
        }
        shopNoticeRepository.saveAll(requireSaveNoticeList);
    }

    public void parseReebonzNotice() throws IOException {
        //현재 존재하는 공지사항의 마지막을 가져온다.
        Shop shop = shopRepository.findById("REEBONZ");
        String finalShopNoticeId = getMaxNoticeIdByShop(shop);
        log.info("finalShopMustItNoticeId:{}", finalShopNoticeId);

        //리본즈의 경우 세션을 가지고 가져와야 하기 때문에 세션을 체크한다.
        String webToken = reebonzWebPageService.getToken(REEBONZ_ADMIN_ACCOUNT, REEBONZ_ADMIN_PASSWORD);
        //Notice의 첫페이지 리스트를 가져온다.
        String noticeHtmlContents = reebonzWebPageService.collectShopNotice(webToken);
        log.info("noticeHtmlContents: {}",noticeHtmlContents);
        List<ShopNoticeDto> shopNoticeDtoList = ReebonzNoticeParser.parseShopNotice(noticeHtmlContents);
//
        List<ShopNotice> requireSaveNoticeList = new ArrayList<>();
        //가져온 리스트를 가지고 가지고 오지 않은 공지사항에 대해서 상세를 가져온다.
        for(ShopNoticeDto shopNoticeDto : shopNoticeDtoList){
            //DB의 마지막 공지보다 작거나 같은 경우는 스킵처리
            if(finalShopNoticeId != null
                    && finalShopNoticeId.compareTo(shopNoticeDto.getShopNoticeId()) >= 0){
                continue;
            }
            //새로운 공지사항이므로 처리한다.(Reebonz의 경우 상세포함 목록에서 파싱하기 때문에 별도 상세 파싱이 필요없다.
            log.info("shopNoticeDto:{}",shopNoticeDto);

            //Entity Data를 만든다
            ShopNotice shopNotice = modelMapper.map(shopNoticeDto, ShopNotice.class);
            shopNotice.setShop(shop);
            requireSaveNoticeList.add(shopNotice);
        }
        shopNoticeRepository.saveAll(requireSaveNoticeList);
    }

    private String getMaxNoticeIdByShop(Shop shop){
        //현재 존재하는 공지사항의 마지막을 가져온다.
        List<ShopNotice> findShopList = shopNoticeRepository.findByShop(shop);
        if(!ObjectUtils.isEmpty(findShopList)){
            ShopNotice finalShopNotice = findShopList.stream().max(Comparator.comparing(ShopNotice::getShopNoticeId)).orElseThrow(NoSuchElementException::new);
            return finalShopNotice.getShopNoticeId();
        }else{
            return null;
        }
    }

}
