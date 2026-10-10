package com.jieuni.upload;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("jieuni.s3")
record S3Properties(String endpoint, String region, String bucket, String accessKey, String secretKey) {
}
