package com.wanpan.app.config.gateway;

import com.wanpan.app.config.FeignConfiguration;
import com.wanpan.app.dto.godra.GordaUserBaseResponse;
import com.wanpan.app.dto.godra.GordaUserBrandDto;
import com.wanpan.app.dto.godra.GordaUserCategoryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "gorda", url = "${gorda.client.url}", configuration = FeignConfiguration.class, decode404 = true) //https://api.gordastyle.com"
public interface GordaClient {
    @GetMapping("/categories")
    GordaUserBaseResponse<List<GordaUserCategoryDto.Response.GetCategory>> getCategories(
            @RequestParam("shoppingGender") String shoppingGender
    );

    @GetMapping("/designers")
    GordaUserBaseResponse<GordaUserBrandDto.Response.GetBrandPage> getBrands(
            @RequestParam("page") int page,
            @RequestParam("shoppingGender") String shoppingGender
    );
}
