package com.wanpan.app.service.gorda;

import com.wanpan.app.config.gateway.GordaClient;
import com.wanpan.app.dto.BrandDto;
import com.wanpan.app.dto.godra.GordaUserBaseResponse;
import com.wanpan.app.dto.godra.GordaUserBrandDto;
import com.wanpan.app.service.gorda.constant.GordaGenderType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@AllArgsConstructor
public class GordaBrandService {
    private final GordaClient gordaClient;
    private final ModelMapper modelMapper;

    public List<BrandDto> getBrandList() {
        Set<String> brandNameSet = new HashSet<>();
        List<BrandDto> brandDtoList = new ArrayList<>();

        for (GordaGenderType gordaGenderType : GordaGenderType.values()) {
            int currentPageNumber = 0; // 고르다 API는 0페이지부터 시작
            String gender = gordaGenderType.name();

            // 0페이지 호출
            GordaUserBaseResponse<GordaUserBrandDto.Response.GetBrandPage> brandListPageResponse =
                    gordaClient.getBrands(currentPageNumber, gender);


            int totalResultCount = brandListPageResponse.getResponse().getTotalCount();
            int pageSize = brandListPageResponse.getResponse().getPageSize();

            if (totalResultCount > 0) {
                List<GordaUserBrandDto.Response.GetBrand> brandList = brandListPageResponse.getResponse().getResult();
                for (GordaUserBrandDto.Response.GetBrand brand : brandList) {
                    String brandName = brand.getBrandName();
                    if (!brandNameSet.contains(brandName)) {
                        brandNameSet.add(brandName);
                        BrandDto brandDto = modelMapper.map(brand, BrandDto.class);
                        // 리스트에 추가
                        brandDtoList.add(brandDto);
                    }
                }

                // 다음 페이지 호출하는 경우
                int pageCount = getPageCount(totalResultCount, pageSize);
                for (currentPageNumber = 1; currentPageNumber < pageCount; currentPageNumber++) {
                    GordaUserBaseResponse<GordaUserBrandDto.Response.GetBrandPage> nextBrandListPageResponse =
                            gordaClient.getBrands(currentPageNumber, gender);
                    List<GordaUserBrandDto.Response.GetBrand> nextBrandList = nextBrandListPageResponse.getResponse().getResult();
                    for (GordaUserBrandDto.Response.GetBrand nextBrand : nextBrandList) {
                        String nextBrandName = nextBrand.getBrandName();
                        if (!brandNameSet.contains(nextBrandName)) {
                            brandNameSet.add(nextBrandName);
                            BrandDto nextBrandDto = modelMapper.map(nextBrand, BrandDto.class);
                            // 리스트에 추가
                            brandDtoList.add(nextBrandDto);
                        }
                    }
                }
            }
        }

        return brandDtoList;
    }

    /**
     * 총 페이지 수를 구한다.
     */
    private int getPageCount(int resultCount, int pageSize) {
        if (resultCount < 0) {
            throw new IllegalArgumentException("유효하지 않은 resultCount(<0)");
        } else if (pageSize <= 0) {
            throw new IllegalArgumentException("유효하지 않은 pageSize(<=0)");
        }

        int pageCount;
        if (resultCount == 0) {
            pageCount = 1;
        } else {
            if (resultCount%pageSize == 0) {
                pageCount = resultCount/pageSize;
            } else {
                pageCount = resultCount/pageSize + 1;
            }
        }

        return pageCount;
    }
}
