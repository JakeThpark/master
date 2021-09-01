package com.wanpan.app.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import com.wanpan.app.dto.BucketFolder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@Slf4j
@AllArgsConstructor
public class ImageService {
//    private final AmazonS3 s3Client;
//    private final BucketFolder bucketFolder;
//
//    public void uploadObject(MultipartFile multipartFile, String storedFileName) throws IOException{
//        log.info("Call uploadObject");
//        ObjectMetadata omd = new ObjectMetadata();
//        omd.setContentType(multipartFile.getContentType());
//        omd.setContentLength(multipartFile.getSize());
//        omd.addUserMetadata("title", "someTitle");
//        omd.setHeader("filename", multipartFile.getOriginalFilename());
//        log.info("created metadata : multipartFile name:{}", multipartFile.getOriginalFilename());
//        log.info("bucketFolder:{}", bucketFolder);
////        log.info("bucketDir:{}", bucketDir);
//        log.info("s3Client:{}", s3Client);
//        log.info("multipartFile.getInputStream():{}", multipartFile.getInputStream());
//        PutObjectRequest request = new PutObjectRequest(
//                bucketFolder.getBucketName(),
//                bucketFolder.getFileObjKeyName(storedFileName),
//                multipartFile.getInputStream(),
//                omd
//        );
//        log.info("create PutObjectRequest");
//        PutObjectResult putObjectResult = s3Client.putObject(request);
//        log.info("putObjectResult : {}", putObjectResult);
//    }
//
//    public void deleteObject(String storedFileName) throws AmazonServiceException {
//        DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(
//                bucketFolder.getBucketName(),
//                bucketFolder.getFileObjKeyName(storedFileName)
//        );
//
//        s3Client.deleteObject(deleteObjectRequest);
//    }
//
//    public Resource getObject(String storedFileName) throws IOException{
//        String requestFilePath = bucketFolder.getFileObjKeyName(storedFileName);
//        log.info("requestFilePath:{}",requestFilePath);
//        GetObjectRequest getObjectRequest = new GetObjectRequest(
//                bucketFolder.getBucketName(),
//                requestFilePath
//        );
//        S3Object s3Object = s3Client.getObject(getObjectRequest);
//
//        S3ObjectInputStream s3ObjectInputStream = s3Object.getObjectContent();
//        byte[] bytes = IOUtils.toByteArray(s3ObjectInputStream);
//
//        Resource resource = new ByteArrayResource(bytes);
//
//        return resource;
//    }
//
//    public void copyObject(String storedFileName, String targetFilename) throws IOException{
//        CopyObjectRequest copyObjRequest = new CopyObjectRequest(
//                bucketFolder.getBucketName(),
//                bucketFolder.getFileObjKeyName(storedFileName),
//                bucketFolder.getBucketName(),
//                bucketFolder.getFileObjKeyName(targetFilename)
//        );
//        CopyObjectResult copyObjectResult = s3Client.copyObject(copyObjRequest);
//        log.info("CopyObjectResult:{}",copyObjectResult);
//    }
//
//    public void moveObject(String storedFileName, String targetFilename) throws IOException{
//        CopyObjectRequest copyObjRequest = new CopyObjectRequest(
//                bucketFolder.getBucketName(),
//                bucketFolder.getFileObjKeyName(storedFileName),
//                bucketFolder.getBucketName(),
//                bucketFolder.getFileObjKeyName(targetFilename)
//        );
//        CopyObjectResult copyObjectResult = s3Client.copyObject(copyObjRequest);
//        log.info("CopyObjectResult:{}",copyObjectResult);
//
//        deleteObject(storedFileName);
//    }

}
