# 공간의 지은이 — 디자인 기준 (DESIGN v0.1)

> 2026-10-06 · 화면을 구현할 때 피그마를 다시 열지 않아도 되도록, 정한 값을 숫자까지 전부 적는다.
> 피그마 원본: [공간 서비스 — 디자인 v0.1](https://www.figma.com/design/d3wAmslKf6WOFoA8rr50Hc)
> 무엇을 보여주는지는 `SPEC.md`, 어떻게 보이는지는 이 문서.

## 0. 방향

**인테리어·건축 스튜디오의 포트폴리오처럼, 고요하고 섬세하게.**

- 따뜻한 종이 바탕 위에 먹색 글자. 색은 흙색 하나만 포인트로 쓴다.
- 사진이 주인공이다. UI는 사진을 방해하지 않게 물러선다.
- 선은 1px 헤어라인만. 그림자, 그라데이션 버튼, 둥근 카드는 쓰지 않는다.
- 모서리는 거의 각지게(2px). 건축 도면 같은 정확함.
- 작은 라벨은 자간을 넓힌다. 영문 라벨은 대문자(WEDNESDAY, BROWSE).
- 여백은 넉넉하게. 화면 좌우 여백 24px가 기본.
- 하지 않는 것: 아이콘 남발, 이모지, 숫자 배지, 컬러 칩 여러 색, 애니메이션 과잉.

---

## 1. 색

모두 CSS 변수로 둔다. 다크 모드는 MVP에서 하지 않는다.

| 토큰 | 값 | 쓰는 곳 |
| --- | --- | --- |
| `--color-paper` | `#F4F0E8` | 화면 바탕, 탭 바, 시트 |
| `--color-paper-raised` | `#EBE5DA` | 살짝 떠 있는 면(입력칸 바탕 등) |
| `--color-photo-placeholder` | `#D9CFBF` | 사진이 오기 전 자리 |
| `--color-line` | `#D8D0C3` | 헤어라인, 꺼진 버튼 테두리 |
| `--color-ink-tertiary` | `#A39B8F` | 라벨, 보조 정보, 꺼진 탭 |
| `--color-ink-secondary` | `#6E665C` | 본문 보조, 설명 |
| `--color-ink` | `#2A2622` | 제목, 본문, 켜진 버튼 바탕 |
| `--color-earth` | `#8A5A3C` | 유일한 포인트: 머물러 봄 배지, 감각 라벨, "새로운", 탭의 점 |
| `--color-on-photo` | `#F7F4EE` | 사진 위 글자 |

**사진 위 그림자**: 사진 아래쪽에 글자를 올릴 때 `linear-gradient(to bottom, rgba(23,20,18,0) 0%, rgba(23,20,18,.35) 45%, rgba(23,20,18,.85) 100%)`를 화면 아래 520px에 깐다.

**대비 확인** (paper 바탕 기준): ink 13.2:1, ink-secondary 5.0:1(본문 가능), earth 5.1:1, ink-tertiary 2.4:1이라 **ink-tertiary는 10~12px 라벨과 보조 정보에만** 쓰고 읽어야 하는 문장에는 쓰지 않는다.

---

## 2. 글꼴

| 용도 | 글꼴 |
| --- | --- |
| 제목 | Noto Serif KR (Light, Regular) |
| 본문·라벨·버튼 | IBM Plex Sans KR (Light, Regular, Medium) |

두 글꼴 모두 Google Fonts에 있다. `next/font/google`로 불러오고 필요한 굵기만 쓴다.

| 토큰 | 글꼴 · 굵기 | 크기 | 줄 높이 | 자간 | 쓰는 곳 |
| --- | --- | --- | --- | --- | --- |
| `display-xl` | Serif Light | 30px | 1.4 | -1% | 공간 이름(피드, 상세), 프로필 이름 |
| `display-lg` | Serif Light | 24px | 1.4 | -0.5% | 화면 제목(둘러보기, 내 공간) |
| `display-md` | Serif Regular | 19px | 1.5 | -0.3% | 시트 제목 |
| `body-lg` | Sans Light | 15px | 1.75 | 0 | 에디터의 감상, 한 줄 소개 |
| `body-md` | Sans Regular | 14px | 1.65 | 0 | 일반 본문, 이름, 감각 내용 |
| `body-sm` | Sans Regular | 12px | 1.6 | 0 | 지역·종류, 사진 출처, 안내문 |
| `label-md` | Sans Medium | 12px | 1.4 | +2% | 버튼, 탭 |
| `label-sm` | Sans Regular | 10px | 1.4 | +2.5% | 섹션 라벨, 역할, 영문 대문자 라벨 |

---

## 3. 간격·크기

| 토큰 | 값 |
| --- | --- |
| `--space-xs` | 4px |
| `--space-sm` | 8px |
| `--space-md` | 12px |
| `--space-lg` | 16px |
| `--space-xl` | 24px |
| `--space-2xl` | 32px |
| `--space-3xl` | 48px |
| `--space-4xl` | 64px |
| `--radius-sm` | 2px |

- 기준 너비 375px, 좌우 여백 24px(본문 너비 327px).
- 공간 상세의 섹션 사이 간격 48px.
- 하단 탭 바 높이 64px. 상단 안전 영역을 고려해 화면 첫 요소는 위에서 52~64px.
- 누르는 요소는 최소 44px 높이(작은 버튼은 터치 영역을 패딩으로 늘린다).

---

## 4. 컴포넌트

### SaveButton (저장 버튼)

- 종류: `want`(가고 싶어요), `been`(다녀왔어요). 상태: `off`, `on`.
- 패딩 10px 16px, 모서리 2px, 글자 `label-md`.
- off: 바탕 없음, 테두리 1px `line`, 글자 `ink`.
- on: 바탕 `ink`, 테두리 `ink`, 글자 `paper`.
- 사진 위(피드 카드): 테두리 `on-photo` 60%, 글자 `on-photo`.
- 두 버튼은 8px 간격으로 나란히. 한 번에 하나만 on.

### FollowButton (팔로우 버튼)

- 상태: `follow`(팔로우), `following`(팔로잉).
- 패딩 5px 10px, 모서리 2px, 글자 `label-sm`.
- follow: 테두리 1px `ink`, 글자 `ink`.
- following: 테두리 없음, 글자 `ink-tertiary`.
- 피드 카드에서는 follow 상태일 때만 보인다(SPEC 5절). 사진 위에서는 테두리 `on-photo` 70%.

### Badge — 머물러 봄

- 패딩 4px 8px, 테두리 1px `earth`, 앞에 4px 원(`earth`), 글자 `label-sm` `earth`.

### TabBar (하단 탭 바)

- 높이 64px, 바탕 `paper`, 위쪽만 1px `line`. 좌우 패딩 40px, 세 항목 양 끝 정렬.
- 항목: **오늘 · 둘러보기 · 내 공간**. 글자 `label-md`. 켜진 탭은 `ink`, 꺼진 탭은 `ink-tertiary`.
- 켜진 탭은 글자 위 6px에 16×1px `ink` 선.
- 내 공간 글자 오른쪽에 5px 원(`earth`): 팔로우한 사람의 새 공간이 있을 때만.
- 아이콘은 쓰지 않는다.

### PlaceTile (사진 격자 한 칸)

- 정사각형. 3열 격자에서 124×124px, 칸 사이 1.5px(화면 끝까지 꽉 채움).
- 사진 위 왼쪽 아래 8px에 캡션(`label-sm`, `on-photo`): 역할 또는 공간 이름.
- 밝은 사진에서도 캡션이 읽히도록 칸 아래 48px에 그림자: `linear-gradient(to bottom, rgba(23,20,18,0), rgba(23,20,18,.35))`.
- 폐업: 투명도 40%, 캡션에 "· 폐업".
- 새로운: 왼쪽 위 6px에 작은 꼬리표(바탕 `paper`, 패딩 3px 6px, 글자 `label-sm` `earth` "새로운").

### CreditRow (크레딧 한 줄)

- 너비 327px, 위아래 패딩 14px, 아래만 1px `line`.
- 왼쪽부터: 역할(56px 고정, `label-sm` `ink-tertiary`) · 이름(`body-md` `ink`)과 그 아래 확인 상태(`body-sm` `ink-tertiary`: "출처 확인됨" / "확인 중") · 오른쪽 끝에 FollowButton.

### SectionLabel (섹션 제목)

- `label-sm` `ink-tertiary` 글자 + 12px 띄우고 남은 너비를 채우는 1px `line` 선.
- 예: "에디터의 감상", "감각", "만든 사람들", "방문 정보".

### 감각 한 줄 / 방문 정보 한 줄

- 위아래 패딩 14px(방문 정보 12px), 아래만 1px `line`.
- 왼쪽 라벨 40px 고정: 감각은 `label-sm` **`earth`**(빛·소리·향·촉감), 방문 정보는 `label-sm` `ink-tertiary`.
- 오른쪽 내용 `body-md` `ink`.

### 큰 버튼 (피드 마지막 화면 등)

- 너비 327px, 패딩 14px 16px, 모서리 2px, 글자 `label-md`, 오른쪽 끝에 "→".
- 주 버튼: 바탕 `ink`, 글자 `paper`. 보조 버튼: 테두리 1px `ink`, 글자 `ink`.

### 필터 칩

- 패딩 6px 12px, 모서리 2px, 글자 `label-sm`.
- 켜짐: 바탕 `ink`, 글자 `paper`. 꺼짐: 테두리 1px `line`, 글자 `ink-secondary`. 칩 사이 6px.

### 상단 탭 (둘러보기, 내 공간)

- 좌우 패딩 24px, 항목 사이 24px, 아래만 1px `line`.
- 글자 `label-md`. 켜짐 `ink` + 글자 아래 10px에 글자 너비만큼 1px `ink` 선. 꺼짐 `ink-tertiary`.

### 바텀 시트 (로그인)

- 바탕 `paper`, 위 모서리 없음(각짐). 위쪽에 32×2px `line` 손잡이.
- 패딩 12px 24px 40px, 항목 사이 20px, 모두 가운데 정렬.
- 뒤 배경 `ink` 55%.
- 로그인 버튼은 구글 하나(카카오는 공개 후). 4절 큰 버튼의 보조 버튼 모양(테두리 1px `ink`, 글자 `ink`). `--color-kakao`는 카카오 로그인을 넣을 때까지 쓰지 않는다.

---

## 5. 화면별 배치

### 01 홈 — 오늘의 공간 카드

- 사진이 화면 전체(탭 바 제외). 아래 520px에 사진 위 그림자.
- 상단(위 56px): 왼쪽 요일 영문(`label-sm`, WEDNESDAY), 가운데 진행 막대 5개(14×1px, 4px 간격: 지금 카드 100%, 열린 카드 55%, 아직 안 열린 날 20%), 오른쪽 "01 / 03".
- 하단 블록(좌우 24px, 항목 사이 14px, 탭 바 위 32px에서 끝남): "수요일의 공간"(`label-sm` 80%) → 공간 이름(`display-xl`) → 지역·종류(`body-sm` 75%) → 한 줄(`body-lg`, 너비 300px) → 만든 사람 줄(역할 `label-sm` 60% · 이름 `body-md` · 팔로우) → 저장 버튼 두 개.
- 사진 위 글자는 모두 `on-photo`.

### 02 홈 — 오늘은 여기까지

- 바탕 `paper`. 위에서 260px부터 블록(좌우 32px, 항목 사이 20px).
- "WED · 03 / 05"(`label-sm` `ink-tertiary`) → "오늘은 여기까지예요."(`display-xl`) → 안내 두 줄(`body-lg` `ink-secondary`) → 32×1px `ink` 선 → 큰 버튼 두 개(전체 둘러보기 주 버튼, 뉴스레터 구독 보조 버튼, 사이 12px).

### 03 로그인 시트

- 4절 바텀 시트. 제목 "저장하려면 로그인이 필요해요", 설명, 구글 버튼(너비 327px, 패딩 15px), 약관 안내(`body-sm` `ink-tertiary`).

### 04 공간 상세

- 사진 375×440px. 왼쪽 위(16, 52)에 뒤로 가기 화살표(24px, 선 1.25px). 아래쪽 32px에 왼쪽 "사진: ○○", 오른쪽 "1 / 4"(둘 다 `label-sm` `on-photo`).
- 본문(좌우 24px, 위 28px, 섹션 사이 48px):
  1. 제목 블록(사이 12px): 만남 출처 또는 머물러 봄 배지 → 이름(`display-xl`) → "지역 · 종류 · 방문 방식"(`body-sm` `ink-secondary`) → 저장 버튼 두 개 + "공유"(`label-md` `ink-secondary`)
  2. 에디터의 감상: SectionLabel + 감상(`body-lg`)
  3. 감각: SectionLabel + 감각 한 줄 ×4
  4. 만든 사람들: SectionLabel + CreditRow 목록 + "이 공간을 만드셨나요? 크레딧을 알려주세요 →"(`body-sm` `ink-secondary`)
  5. 방문 정보: SectionLabel + 방문 정보 한 줄(주소, 방문, 메모, 지도)
  6. "○○의 다른 공간": SectionLabel + 3열 격자(본문 너비 안, 106×106px, 사이 4px)

### 05 만든 사람 프로필 (검수 완료)

- 위(좌우 24px, 위 52px, 사이 12px): 뒤로 가기 → 영문 라벨("STUDIO · 서울 종로") → 이름(`display-xl`)과 오른쪽 끝 FollowButton → 소개(`body-md` `ink-secondary`, 327px) → 링크·소속(`body-sm` `ink-tertiary`).
- SectionLabel "○○가 만든 공간" 다음에 **화면 끝까지 꽉 찬 3열 격자**(124px, 사이 1.5px). 캡션은 역할.

### 06 둘러보기 (검수 완료)

- 머리(좌우 24px, 위 64px): "BROWSE"(`label-sm`) → "둘러보기"(`display-lg`).
- 상단 탭 "공간 | 만든 사람" → 필터 칩 줄(위아래 16px) → 꽉 찬 3열 격자(캡션은 공간 이름).

### 07 내 공간 — 팔로잉 (검수 완료)

- 머리: 닉네임(`label-sm`) → "내 공간"(`display-lg`).
- 상단 탭 "가고 싶어요 · 다녀왔어요 · 팔로잉".
- SectionLabel "팔로우한 사람들의 공간" → 꽉 찬 3열 격자(캡션은 만든 사람 이름, 새로 공개된 칸에 "새로운").
- SectionLabel "팔로우한 만든 사람" → 줄 목록(이름 `body-md` + 종류 `label-sm` `ink-tertiary`, 오른쪽 FollowButton following).

---

## 6. 움직임

- 피드 넘기기: CSS scroll-snap, 한 장씩 멈춤.
- 저장·팔로우 상태 전환: 색만 120ms ease-out으로 바뀐다. 크기 변화나 튀는 효과 없음.
- 바텀 시트: 아래에서 240ms ease-out으로 올라오고, 배경은 같은 시간에 서서히 어두워진다.
- `prefers-reduced-motion`이면 전환 시간을 0으로.

---

## 7. CSS 변수 (실제 파일: design/tokens.css)

```css
:root {
  --color-paper: #F4F0E8;
  --color-paper-raised: #EBE5DA;
  --color-photo-placeholder: #D9CFBF;
  --color-line: #D8D0C3;
  --color-ink-tertiary: #A39B8F;
  --color-ink-secondary: #6E665C;
  --color-ink: #2A2622;
  --color-earth: #8A5A3C;
  --color-on-photo: #F7F4EE;

  --space-xs: 4px;  --space-sm: 8px;  --space-md: 12px; --space-lg: 16px;
  --space-xl: 24px; --space-2xl: 32px; --space-3xl: 48px; --space-4xl: 64px;
  --radius-sm: 2px;
  --tabbar-height: 64px;
  --page-gutter: 24px;

  --font-serif: 'Noto Serif KR', serif;
  --font-sans: 'IBM Plex Sans KR', sans-serif;
}
```

---

## 8. 남은 것

- [x] 05~07 화면 검수 (피그마 원본과 맞추는 것은 한도가 풀린 뒤)
- [ ] 관리자 화면(등록 폼, 편성) 배치. 데스크톱 너비, 같은 토큰
- [ ] 빈 상태 화면(저장 없음, 이번 주 편성 없음)
- [x] 하단 탭 이름 "오늘"로 확정, SPEC.md 반영 (2026-10-06)
