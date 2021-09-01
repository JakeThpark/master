package com.wanpan.app.service;

import com.wanpan.app.entity.Shop;
import com.wanpan.app.service.feelway.FeelwayService;
import com.wanpan.app.service.gorda.GordaService;
import com.wanpan.app.service.mustit.MustitService;
import com.wanpan.app.service.reebonz.ReebonzService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ShopServiceFactory {
    private FeelwayService feelwayService;
    private MustitService mustitService;
    private ReebonzService reebonzService;
    private GordaService gordaService;

    public ShopService getShopService(String shopType) {
        if ("FEELWAY".equals(shopType)) {
            return feelwayService;
        } else if ("MUSTIT".equals(shopType)) {
            return mustitService;
        } else if ("REEBONZ".equals(shopType)) {
            return reebonzService;
        } else if ("GORDA".equals(shopType)) {
            return gordaService;
        }

        return null;
    }
}
