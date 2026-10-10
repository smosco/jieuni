import { putToStorage, requestUpload } from "./api";
import { fitWidth } from "./photoSchema";

const SIZES = [1600, 800] as const;
const QUALITY = 0.85;

// 화면에 그대로 보이는 문구를 담는다.
export class PhotoUploadError extends Error {}

export type Converted = { w1600: Blob; w800: Blob; width: number; height: number };

// 브라우저에서 WebP 두 크기로 바꾼다. createImageBitmap은 기본으로 EXIF 방향을 적용한다(imageOrientation "from-image").
export async function toWebp(file: File): Promise<Converted> {
  let bitmap: ImageBitmap;
  try {
    bitmap = await createImageBitmap(file, { imageOrientation: "from-image" });
  } catch {
    throw new PhotoUploadError("사진을 읽지 못했어요. JPEG·PNG·WebP로 올려 주세요");
  }
  try {
    const [large, small] = await Promise.all(SIZES.map((max) => encode(bitmap, max)));
    return { w1600: large.blob, w800: small.blob, width: large.width, height: large.height };
  } finally {
    bitmap.close();
  }
}

async function encode(bitmap: ImageBitmap, max: number): Promise<{ blob: Blob; width: number; height: number }> {
  const size = fitWidth(bitmap.width, bitmap.height, max);
  const canvas = document.createElement("canvas");
  canvas.width = size.width;
  canvas.height = size.height;
  const context = canvas.getContext("2d");
  if (!context) throw new PhotoUploadError("사진을 바꾸지 못했어요. 다시 올려 주세요");
  context.imageSmoothingQuality = "high";
  context.drawImage(bitmap, 0, 0, size.width, size.height);
  const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, "image/webp", QUALITY));
  // WebP로 쓸 수 없는 브라우저는 조용히 PNG를 준다.
  if (!blob || blob.type !== "image/webp") {
    throw new PhotoUploadError("이 브라우저는 WebP로 바꿀 수 없어요. 크롬에서 올려 주세요");
  }
  return { blob, ...size };
}

// 서명 URL을 받아 두 크기를 저장소에 올린다. 등록은 하지 않는다(임시저장 때).
export async function uploadConverted(placeId: number, converted: Converted): Promise<string> {
  try {
    const { objectKey, uploadUrls } = await requestUpload(placeId);
    await Promise.all([putToStorage(uploadUrls.w1600, converted.w1600), putToStorage(uploadUrls.w800, converted.w800)]);
    return objectKey;
  } catch {
    throw new PhotoUploadError("올리지 못했어요");
  }
}
