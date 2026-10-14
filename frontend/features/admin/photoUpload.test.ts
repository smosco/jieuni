import { describe, expect, it } from "vitest";
import { checkPhotoFile, photoWarning } from "./photoUpload";

const MB = 1024 * 1024;

describe("고를 때 사진 검사", () => {
  it("JPEG·PNG·WebP는 20MB까지 받는다", () => {
    expect(checkPhotoFile({ type: "image/jpeg", size: 20 * MB })).toBeNull();
    expect(checkPhotoFile({ type: "image/png", size: 1 })).toBeNull();
    expect(checkPhotoFile({ type: "image/webp", size: 1 })).toBeNull();
  });

  it("20MB를 넘으면 크기와 함께 거절한다", () => {
    expect(checkPhotoFile({ type: "image/jpeg", size: 20 * MB + 1 })).toBe("20MB보다 커서 올릴 수 없어요(20.0MB)");
    expect(checkPhotoFile({ type: "image/jpeg", size: 23.4 * MB })).toBe("20MB보다 커서 올릴 수 없어요(23.4MB)");
  });

  it("다른 형식은 크기보다 먼저 형식을 알린다", () => {
    for (const type of ["image/heic", "image/heif", "image/gif", ""]) {
      expect(checkPhotoFile({ type, size: 30 * MB })).toBe("JPEG·PNG·WebP로 올려 주세요");
    }
  });
});

describe("작은 사진 경고", () => {
  it("너비 1200px 미만만 알리고, 모르는 너비(0)는 알리지 않는다", () => {
    expect(photoWarning(1199)).toBe("작아서 흐릿하게 보일 수 있어요");
    expect(photoWarning(1200)).toBeNull();
    expect(photoWarning(0)).toBeNull();
  });
});
