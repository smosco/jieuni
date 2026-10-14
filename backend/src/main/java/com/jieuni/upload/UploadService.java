package com.jieuni.upload;

import com.jieuni.common.ApiException;
import com.jieuni.place.PlaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;
import java.util.Map;

@Service
public class UploadService {

	static final String WEBP = "image/webp";
	// 바꾼 WebP 한 파일의 한도. 너비 1600px WebP는 보통 1MB 아래라 넉넉히 둔다(원본 20MB 한도는 브라우저가 본다).
	static final long MAX_BYTES = 5L * 1024 * 1024;
	static final String TOO_LARGE = "사진 파일이 너무 커요(5MB까지)";
	private static final Duration EXPIRES = Duration.ofMinutes(5);

	private final S3Presigner presigner;
	private final PlaceRepository places;
	private final String bucket;

	UploadService(S3Presigner presigner, PlaceRepository places, S3Properties props) {
		this.presigner = presigner;
		this.places = places;
		this.bucket = props.bucket();
	}

	public record Upload(String objectKey, Urls uploadUrls) {

		public record Urls(String w1600, String w800) {
		}

	}

	// 서버는 파일을 받지 않고 PUT 서명 URL만 준다. Content-Type과 Content-Length가 서명에 들어가
	// 브라우저는 image/webp로, 요청 때 밝힌 크기로만 올릴 수 있다(R2도 다른 길이를 403으로 거절한다).
	@Transactional(readOnly = true)
	public Upload issue(long placeId, String contentType, long w1600Bytes, long w800Bytes) {
		if (!WEBP.equals(contentType)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "입력한 값을 확인해 주세요",
					Map.of("contentType", "WebP 사진만 올릴 수 있어요"));
		}
		if (!places.existsById(placeId)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "공간을 찾을 수 없어요");
		}
		String key = PhotoFiles.newKey(placeId);
		return new Upload(key, new Upload.Urls(presign(PhotoFiles.w1600(key), w1600Bytes), presign(PhotoFiles.w800(key), w800Bytes)));
	}

	private String presign(String file, long bytes) {
		return presigner.presignPutObject(r -> r.signatureDuration(EXPIRES)
				.putObjectRequest(p -> p.bucket(bucket).key(file).contentType(WEBP).contentLength(bytes)))
				.url().toString();
	}

}
