package com.wanpan.app.dto;

import lombok.Data;

@Data
public class BucketFolder {
    private String bucketName;
    private String prefix;

    public String getFileObjKeyName(String objectName) {
        return prefix + objectName;
    }
}
