import { ApiError } from "@/shared/api/client";
import { putToStorage, requestUpload } from "./api";
import { fitWidth } from "./photoSchema";

const SIZES = [1600, 800] as const;
const QUALITY = 0.85;

// 고를 때 보는 원본 한도. 바꾼 WebP 한도(5MB)는 서버가 본다.
const MAX_FILE_BYTES = 20 * 1024 * 1024;
const TYPES = ["image/jpeg", "image/png", "image/webp"];
// 공개 화면 너비(최대 480px)를 2·3배 화면에서 채우려면 이 정도는 있어야 한다.
const SHARP_WIDTH = 1200;

// 바꾸기 전에 거를 파일. 이유를 돌려주고, 올려도 되면 null.
export function checkPhotoFile(file: { size: number; type: string }): string | null {
  if (!TYPES.includes(file.type)) return "JPEG·PNG·WebP로 올려 주세요";
  if (file.size > MAX_FILE_BYTES) {
    return `20MB보다 커서 올릴 수 없어요(${(file.size / 1024 / 1024).toFixed(1)}MB)`;
  }
  return null;
}

// 막지는 않고 알린다. 너비를 아직 모르면(0) 알리지 않는다.
export function photoWarning(width: number): string | null {
  return width > 0 && width < SHARP_WIDTH ? "작아서 흐릿하게 보일 수 있어요" : null;
}

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
    const { objectKey, uploadUrls } = await requestUpload(placeId, {
      w1600: converted.w1600.size,
      w800: converted.w800.size,
    });
    await Promise.all([putToStorage(uploadUrls.w1600, converted.w1600), putToStorage(uploadUrls.w800, converted.w800)]);
    return objectKey;
  } catch (err) {
    // 크기 한도처럼 서버가 이유를 준 거절은 그대로 보인다.
    const reason = err instanceof ApiError && err.fieldErrors ? Object.values(err.fieldErrors)[0] : null;
    throw new PhotoUploadError(reason ?? "올리지 못했어요");
  }
}
