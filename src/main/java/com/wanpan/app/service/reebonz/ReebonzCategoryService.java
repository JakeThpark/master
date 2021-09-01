package com.wanpan.app.service.reebonz;

import com.wanpan.app.config.gateway.ReebonzClient;
import com.wanpan.app.dto.reebonz.Categories;
import com.wanpan.app.dto.reebonz.ReebonzBaseResponse;
import com.wanpan.app.dto.reebonz.ReebonzCategory;
import com.wanpan.app.entity.ShopAccount;
import com.wanpan.app.entity.ShopCategory;
import com.wanpan.app.repository.ShopAccountRepository;
import com.wanpan.app.repository.ShopCategoryRepository;
import com.wanpan.app.service.ShopAccountService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class ReebonzCategoryService {
    private final ShopAccountService shopAccountService;
    private final ReebonzClient reebonzClient;
    private final ShopCategoryRepository shopCategoryRepository;
    private final ShopAccountRepository shopAccountRepository;
    /*
     * 최초 기본이 되는 모든 카테고리를 입력한다.
     */
    public List<ReebonzCategory> createCategory(final long shopAccountId) throws IOException {

        log.info("CALL ReebonzCategory createCate");
        try {
            Optional<ShopAccount> shopAccount = shopAccountRepository.findById(shopAccountId);
            if(shopAccount.isEmpty()){
                return null;
            }

            //해당 사이트에서 성별 카테고리를 Get 한다.
            List<ReebonzCategory> genderCategoryList = getCategoryListByShopAccount(shopAccount.get(), true, 1);
            //제품 카테고리를 모두 가져온다.(parent_id:4)
            List<ReebonzCategory> mainCategoryList = getCategoryListByShopAccount(shopAccount.get(), true, 4);

            //모든 성별 타입에 대해서 category 복사본을 형성한다.(리본즈의 경우 성별에 상관없이 하위 카테고리는 동일하다)
            for(ReebonzCategory genderCategory : genderCategoryList){
                //중복체크
                ShopCategory findShopCategory = shopCategoryRepository.findByShopIdAndNameAndParentId(
                        shopAccount.get().getShop().getId(), genderCategory.getNameForHangle(), null);
                if (ObjectUtils.isEmpty(findShopCategory)) {
                    ShopCategory shopCategory = new ShopCategory();
                    shopCategory.setShopId(shopAccount.get().getShop().getId());
                    shopCategory.setName(genderCategory.getNameForHangle());
                    shopCategory.setShopCategoryCode(String.valueOf(genderCategory.getCategoryId()));
                    shopCategory.setParentId(null);
                    shopCategory.setDescription(genderCategory.getCategoryName());

                    findShopCategory = shopCategoryRepository.save(shopCategory);
                }

                //각 성별마다 제품카테고리를 모두 카피해서 입력한다.
                subRecursiveProcess(mainCategoryList, findShopCategory);
            }

            return mainCategoryList;
        }catch (IOException ie){
            throw ie;
        }catch (Exception e){
            return null;
        }
    }

    private void subRecursiveProcess(List<ReebonzCategory> reebonzCategoryList, ShopCategory parentShopCategory){
        for (ReebonzCategory reebonzCategory : reebonzCategoryList) {
            //중복체크(같은 부모 카테고리 아래에 같은 이름이 있는지 체크)
            ShopCategory findShopCategory = shopCategoryRepository.findByShopIdAndNameAndParentId(parentShopCategory.getShopId(), reebonzCategory.getNameForHangle(), parentShopCategory.getId());
            if (ObjectUtils.isEmpty(findShopCategory)) {
                ShopCategory shopCategory = new ShopCategory();
                shopCategory.setShopId(parentShopCategory.getShopId());
                shopCategory.setName(reebonzCategory.getNameForHangle());
                shopCategory.setShopCategoryCode(String.valueOf(reebonzCategory.getCategoryId()));
                shopCategory.setParentId(parentShopCategory.getId());
                shopCategory.setDescription(reebonzCategory.getCategoryName());

                findShopCategory = shopCategoryRepository.save(shopCategory);
            }

            //하위 작업이 필요한지 확인
            if(!ObjectUtils.isEmpty(reebonzCategory.getSubCategories())){
                subRecursiveProcess(reebonzCategory.getSubCategories(), findShopCategory);
            }
        }
    }


    public List<ReebonzCategory> getCategoryListByShopAccount(ShopAccount shopAccount, boolean newCategory, int parentId) throws IOException {
        try{
            //shopAccountId를 가지고 Token을 받아온다
            String shopToken = shopAccountService.getTokenByShopAccount(shopAccount);
            log.info("shopToken: {}", shopToken);

            return getCategoryListByShopToken(shopToken, newCategory, parentId);
        }catch(IOException ioe){
            log.error("Fail~",ioe);
            throw ioe;

        }catch(Exception e){
            log.error("Fail~",e);
            return null;
        }
    }

    public List<ReebonzCategory> getCategoryListByShopAccountId(long shopAccountId, boolean newCategory, int parentId) throws IOException {
        try {
            //shopAccountId를 가지고 Token을 받아온다
            String shopToken = shopAccountService.getTokenByShopAccountId(shopAccountId);
            log.info("shopToken: {}", shopToken);

            return getCategoryListByShopToken(shopToken, newCategory, parentId);

        }catch(IOException ioe){
            log.error("Fail~",ioe);
            throw ioe;

        }catch(Exception e){
            log.error("Fail~",e);
            return null;
        }
    }

    /*
     * 리본즈 category parameter newCategory: value = true, new version(3depth)
     * 리본즈 category parameter parent_id: Gender: 1, Category: 4
     */
    public List<ReebonzCategory> getCategoryListByShopToken(String shopToken, boolean newCategory, int parentId) {
        ResponseEntity<ReebonzBaseResponse<Categories>> categoriesResponseEntity
                = reebonzClient.getCategories(shopToken, newCategory, parentId);

        if (categoriesResponseEntity.getStatusCode() == HttpStatus.OK) {
            if (!ObjectUtils.isEmpty(categoriesResponseEntity.getBody())
                    && "success".equals(categoriesResponseEntity.getBody().getResult())) {
                log.info("Result Category:{}", categoriesResponseEntity.getBody().getData().getCategories());
                return categoriesResponseEntity.getBody().getData().getCategories();
            }else {
                //Http200으로 결과는 받았지만 해당 동작에 대한 실패 메세지를 받은 경우
                log.error("getData Fail. Message: {}",categoriesResponseEntity.getBody().getMessage());
                return null;
            }
        } else {
            log.error("Reebonz getCategory Fail - {}", categoriesResponseEntity);
            return null;
        }
    }

}
