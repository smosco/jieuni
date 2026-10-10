package com.jieuni.upload;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
class S3ObjectStorage implements ObjectStorage {

	// DeleteObjects 한 번에 보낼 수 있는 최대 키 수.
	private static final int DELETE_BATCH = 1000;

	private final S3Client s3;
	private final String bucket;

	S3ObjectStorage(S3Client s3, S3Properties props) {
		this.s3 = s3;
		this.bucket = props.bucket();
	}

	@Override
	public List<StoredObject> list(String prefix) {
		return s3.listObjectsV2Paginator(b -> b.bucket(bucket).prefix(prefix)).contents().stream()
				.map(o -> new StoredObject(o.key(), o.lastModified()))
				.toList();
	}

	@Override
	public boolean exists(String key) {
		try {
			s3.headObject(b -> b.bucket(bucket).key(key));
			return true;
		} catch (S3Exception e) {
			// HEAD 응답에는 본문이 없어 오류 코드 대신 상태 코드로 본다.
			if (e.statusCode() == 404) {
				return false;
			}
			throw e;
		}
	}

	@Override
	public void delete(Collection<String> keys) {
		List<ObjectIdentifier> ids = keys.stream().map(k -> ObjectIdentifier.builder().key(k).build()).toList();
		for (int from = 0; from < ids.size(); from += DELETE_BATCH) {
			List<ObjectIdentifier> batch = new ArrayList<>(ids.subList(from, Math.min(from + DELETE_BATCH, ids.size())));
			s3.deleteObjects(b -> b.bucket(bucket).delete(Delete.builder().objects(batch).quiet(true).build()));
		}
	}

}
