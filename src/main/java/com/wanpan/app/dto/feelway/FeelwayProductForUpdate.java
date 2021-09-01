package com.wanpan.app.dto.feelway;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@EqualsAndHashCode(callSuper = true)
@Data
public class FeelwayProductForUpdate extends FeelwayProduct {
    @JsonProperty("bosang_text_now")
    String bosangTextNow;
    @JsonProperty("brand_name_check")
    String brandNameCheck;
    @JsonProperty("brand_no_check")
    String brandNoCheck;

    @JsonProperty("g_photo2_del")
    String photo2DeleteFlag = null;
    @JsonProperty("g_photo3_del")
    String photo3DeleteFlag = null;
    @JsonProperty("g_photo4_del")
    String photo4DeleteFlag = null;
    @JsonProperty("g_photo5_del")
    String photo5DeleteFlag = null;
    @JsonProperty("g_photo6_del")
    String photo6DeleteFlag = null;
    @JsonProperty("g_photo8_del")
    String photo8DeleteFlag = null;
    @JsonProperty("g_photo9_del")
    String photo9DeleteFlag = null;
    @JsonProperty("g_photo10_del")
    String photo10DeleteFlag = null;

    @JsonProperty("g_photo7_del")
    String warrantyPhotoDeleteFlag = null;

    @JsonIgnore
    int uploadedImageCount;

    @JsonIgnore
    int updatePhotoCount;

    public void setImage(FeelwayProduct feelwayProductImage) {
        super.setImage(feelwayProductImage);

        if (updatePhotoCount < uploadedImageCount) {
            for (int i = uploadedImageCount; i > updatePhotoCount; i--) {
                Class<? extends FeelwayProductForUpdate> feelwayProductClass = this.getClass();
                try {
                    String methodName = "setPhoto" + i + "DeleteFlag";
                    if (i == 7) {
                        methodName = "setWarrantyPhoto";
                    }
                    Method method = feelwayProductClass.getMethod(methodName, String.class);
                    method.invoke(this, "1");
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
