package com.jieuni.upload;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

// R2 설정은 Cloudflare의 Java SDK 예제를 따른다(버킷을 경로에 두는 주소, region auto).
@Configuration
@EnableConfigurationProperties(S3Properties.class)
class S3Config {

	@Bean(destroyMethod = "close")
	S3Client s3Client(S3Properties props) {
		return S3Client.builder()
				.endpointOverride(URI.create(props.endpoint()))
				.region(Region.of(props.region()))
				.credentialsProvider(credentials(props))
				// R2 공식 Java 예제 설정을 따른다(path style, 청크 전송 끔).
				.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).chunkedEncodingEnabled(false).build())
				// SDK 2.30부터 기본으로 붙는 CRC32 체크섬을 필요한 작업에만 쓴다(S3 호환 저장소용).
				.requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
				.responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
				.build();
	}

	// 서명 URL에는 청크·체크섬 설정이 적용되지 않는다(S3Presigner.Builder javadoc). 주소 모양만 맞춘다.
	@Bean(destroyMethod = "close")
	S3Presigner s3Presigner(S3Properties props) {
		return S3Presigner.builder()
				.endpointOverride(URI.create(props.endpoint()))
				.region(Region.of(props.region()))
				.credentialsProvider(credentials(props))
				.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
				.build();
	}

	private static StaticCredentialsProvider credentials(S3Properties props) {
		return StaticCredentialsProvider.create(AwsBasicCredentials.create(props.accessKey(), props.secretKey()));
	}

}
