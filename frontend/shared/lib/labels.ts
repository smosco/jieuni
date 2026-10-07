import type { CreditRole, PlaceKind } from "@/shared/api/types";

// 코드 값 → 화면 문구. DESIGN.md 5절 04 문구 표, 역할은 API.md 1절(B4)과 같다.
export const KIND_LABEL: Record<PlaceKind, string> = {
  cafe: "카페",
  bar: "바",
  restaurant: "식당",
  stay: "스테이",
  garden: "정원",
  museum: "미술관",
  gallery: "갤러리",
  bookstore: "서점",
  shop: "숍",
  religious: "종교 공간",
  public: "공공 공간",
  other: "그 밖",
};

export const ROLE_LABEL: Record<CreditRole, string> = {
  planning: "기획",
  branding: "브랜딩",
  architecture: "건축",
  interior: "인테리어",
  landscape: "조경",
  lighting: "조명",
  furniture: "가구",
  scent: "향",
  photography: "사진",
  owner: "주인",
  other: "그 밖",
};
