package com.wanpan.app.config;


import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.wanpan.app.dto.BucketFolder;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Setter
@ConfigurationProperties("aws")
public class AWSConfiguration {
//    private AWSCredentialProperty credential;
//    private String region;
//    private BucketFolder bucket;
//
//    @Bean
//    public AmazonS3 amazonS3() {
//        return AmazonS3ClientBuilder.standard()
//                .withCredentials(credential.createCredentialProvider())
//                .withRegion(region)
//                .build();
//    }
//
//    @Setter
//    private static class AWSCredentialProperty {
//        private String accessKey;
//        private String secretKey;
//
//        public AWSStaticCredentialsProvider createCredentialProvider() {
//            return new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
//        }
//    }
//
//    @Bean
//    public BucketFolder getBucketFolder() {
//        return bucket;
//    }
}
