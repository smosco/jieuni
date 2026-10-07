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

**사진 위 글자의 옅은 색**(01 피드 카드): `--color-on-photo`를 투명도로 섞은 토큰을 쓴다. `--on-photo-80`(○요일의 공간) · `--on-photo-75`(지역·종류) · `--on-photo-70`(팔로우 테두리) · `--on-photo-60`(역할, 저장 버튼 테두리) · `--on-photo-55`(열린 날 막대) · `--on-photo-20`(아직 안 열린 날 막대).

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
| `--shell-max-width` | 480px (데스크톱에서 공개 화면을 가운데 두는 최대 너비, SPEC 4절) |
| `--tabbar-gutter` | 40px (탭 바 좌우 패딩) |
| `--tab-indicator-width` | 16px (켜진 탭 위 선) |
| `--tab-indicator-gap` | 6px (선과 글자 사이) |
| `--dot-size` | 5px (내 공간 옆 점) |
| `--row-padding` | 14px (CreditRow·감각 한 줄의 위아래 패딩) |
| `--row-label-width` | 40px (감각·방문 정보 한 줄의 왼쪽 라벨) |
| `--credit-role-width` | 56px (CreditRow의 역할 칸) |
| `--top-offset` | 52px (상세·프로필에서 뒤로 가기가 놓이는 위쪽 거리) |
| `--icon-size` | 24px (뒤로 가기 화살표) |
| `--icon-stroke` | 1.25px (아이콘 선 굵기) |
| `--detail-photo-height` | 440px (공간 상세 사진 높이) |
| `--detail-body-top` | 28px (상세 사진 아래 본문 위 여백) |
| `--save-button-padding` | 10px 16px |
| `--follow-button-padding` | 5px 10px |
| `--badge-padding` | 4px 8px |
| `--badge-dot-size` | 4px (머물러 봄 앞 원) |
| `--chip-padding` | 6px 12px |
| `--chip-gap` | 6px (필터 칩 사이) |
| `--top-tab-indicator-gap` | 10px (상단 탭 글자와 아래 선 사이) |
| `--head-bottom` | 20px (둘러보기·내 공간 머리 아래) |
| `--feed-top-height` | 56px (피드 카드 상단 줄) |
| `--progress-bar-width` | 14px (진행 막대 하나) |
| `--feed-block-gap` | 14px (피드 카드 하단 블록 항목 사이) |
| `--feed-line-width` | 300px (피드 카드의 한 줄 최대 너비) |
| `--shade-photo-height` | 520px (사진 위 그림자 높이) |
| `--shade-top-height` | 140px (피드 카드 위 그림자 높이) |
| `--end-block-top` | 260px (피드 마지막 화면 블록 위치) |
| `--end-rule-width` | 32px (마지막 화면의 짧은 선) |
| `--big-button-padding` | 14px 16px |
| `--sheet-padding` | 12px 24px 40px (바텀 시트 안쪽) |
| `--sheet-gap` | 20px (바텀 시트 항목 사이) |
| `--sheet-handle-width` | 32px (바텀 시트 손잡이) |
| `--sheet-handle-height` | 2px |
| `--login-button-padding` | 15px (로그인 시트의 구글 버튼) |

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
- 휴업: 캡션에 "· 휴업 중". 모든 격자에 붙인다.
- 새로운: 왼쪽 위 6px에 작은 꼬리표(바탕 `paper`, 패딩 3px 6px, 글자 `label-sm` `earth` "새로운").

### CreditRow (크레딧 한 줄)

- 너비 327px, 위아래 패딩 14px, 아래만 1px `line`.
- 왼쪽부터: 역할(56px 고정, `label-sm` `ink-tertiary`) · 이름(`body-md` `ink`)과 그 아래 확인 상태(`body-sm` `ink-tertiary`: "출처 확인됨 ↗" / "확인 중") · 오른쪽 끝에 FollowButton.
- 이름(프로필로)과 "출처 확인됨 ↗"(외부 출처, 새 창)는 **글자 너비만** 누를 수 있다. 줄 전체로 늘리지 않아 이름 근처를 누르다 외부로 나가지 않게 한다. "↗"는 밖으로 나가는 링크라는 표시다.

### SectionLabel (섹션 제목)

- `label-sm` `ink-tertiary` 글자 + 12px 띄우고 남은 너비를 채우는 1px `line` 선.
- 예: "에디터의 감상", "감각", "만든 사람들", "방문 정보".

### 감각 한 줄 / 방문 정보 한 줄

- 위아래 패딩 14px(방문 정보 12px), 아래만 1px `line`.
- 왼쪽 라벨 40px 고정: 감각은 `label-sm` **`earth`**(빛·소리·향·촉감), 방문 정보는 `label-sm` `ink-tertiary`.
- 오른쪽 내용 `body-md` `ink`.

### Toast (안내 한 줄) — F2에서 정함 (가정)

- 탭 바 위 16px, 가로 가운데, 최대 너비는 본문 너비(327px).
- 바탕 `ink`, 글자 `paper` `label-md`, 패딩 12px 16px, 모서리 2px. 그림자 없음.
- 2초 뒤 사라진다. 한 번에 하나만(새 안내가 이전 것을 바꾼다). 스크린 리더에 `aria-live="polite"`로 읽힌다.
- 문구 예: "링크를 복사했어요", "주소를 복사했어요".

### 상태 띠 (휴업·폐업) — F2에서 정함 (가정)

- 상세 사진 바로 아래, 화면 너비 전체. 바탕 `paper-raised`, 아래만 1px `line`, 위아래 12px · 좌우 24px.
- 글자 `body-sm` `ink-secondary`. 휴업 "지금은 쉬고 있어요", 폐업 "이제는 만날 수 없는 공간이에요"(SPEC 5절).

### 큰 버튼 (피드 마지막 화면 등)

- 놓인 영역의 너비를 채운다(좌우 24px 화면이면 327px, 피드 마지막 화면은 좌우 32px라 311px). 패딩 14px 16px, 모서리 2px, 글자 `label-md`, 오른쪽 끝에 "→".
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
- 순서(F6에서 정함, 가정): 손잡이 → 제목(`display-md`, 동작별) → 설명 한 줄(`body-md` `ink-secondary`) → "만 14세 이상입니다" 체크(`label-md`, 체크 전에는 버튼이 꺼짐: 테두리 `line`, 글자 `ink-tertiary`) → 구글 버튼 → 약관 안내.
- 설명 문구(가정): "로그인하면 저장한 공간과 팔로우한 사람을 내 공간에서 볼 수 있어요". 약관 안내: "계속하면 이용약관과 개인정보처리방침에 동의하는 것으로 봐요"(두 단어는 밑줄 링크).
- 네이티브 `<dialog>` 모달로 그린다. Esc·딤 누르기로 닫히고, 열려 있는 동안 뒤 화면은 스크롤되지 않는다. 폭은 셸과 같이 최대 480px 가운데.

---

## 5. 화면별 배치

### 01 홈 — 오늘의 공간 카드

- 사진이 화면 전체(탭 바 제외). 아래 520px에 사진 위 그림자.
- 위에도 옅은 그림자(`--shade-photo-top`, 높이 140px): 상단 줄(56px)까지 `rgba(23,20,18,.55)`로 유지하고 140px에서 0. 밝은 사진이나 placeholder 위에서도 상단 글자(10px)가 읽히게 한다. placeholder 위 상단 글자 대비 5.0:1(그림자 없으면 1.4:1).
- 상단(위 56px): 왼쪽 요일 영문(`label-sm`, WEDNESDAY), 가운데 진행 막대 5개(14×1px, 4px 간격: 지금 카드 100%, 열린 카드 55%, 아직 안 열린 날 20%), 오른쪽 "03 / 05".
- 요일·순서·막대는 **그 카드의 pickDate**로 정한다. 순서는 주 안의 자리(월 01 · 화 02 · 수 03 · 목 04 · 금 05), 분모는 **05 고정**이다. 편성이 빈 날이 있어도 분모는 05이고, 막대 5개도 월~금 자리 그대로다. 프론트는 API가 준 날짜(`picks[].date`, `weekday`)를 표시용으로 바꾸기만 하고 시간 계산은 하지 않는다.
- 하단 블록(좌우 24px, 항목 사이 14px, 탭 바 위 32px에서 끝남): "수요일의 공간"(`label-sm` 80%) → 공간 이름(`display-xl`) → 지역·종류(`body-sm` 75%) → 한 줄(`body-lg`, 너비 300px) → 만든 사람 줄(역할 `label-sm` 60% · 이름 `body-md` · 팔로우) → 저장 버튼 두 개.
- 사진 위 글자는 모두 `on-photo`.

**01의 세부 (가정, F5에서 정함)**

| 항목 | 규칙 |
| --- | --- |
| 막대 | 그 카드의 요일 자리는 100%(`on-photo`), 이번 주에 열린 다른 날은 55%, 열리지 않은 날(편성이 비었거나 아직 안 열림)은 20% |
| 카드 누르기 | 사진·이름·한 줄 등 빈 곳을 누르면 상세로. 만든 사람 이름(프로필)·팔로우·저장 버튼은 각자 동작 |
| 만든 사람 줄 | `firstMaker`가 없으면 줄을 숨긴다. slug가 없으면 이름에 링크와 팔로우 버튼이 없다 |
| 폐업 공간 | 지역 줄 아래 "이제는 만날 수 없는 공간이에요"(`body-sm` `on-photo`), 저장 버튼 숨김 |
| 사진 없음 | 바탕 `photo-placeholder` 위에 같은 그림자와 글자 |
| 돌아오기 | 카드에서 상세·프로필로 갔다가 뒤로 오면 그 카드에서 다시 보인다. 탭 바로 "오늘"에 오면 맨 위 |

### 02 홈 — 피드 마지막 화면

- 바탕 `paper`. 위에서 260px부터 블록(좌우 32px, 항목 사이 20px).
- "WED · 03 / 05"(`label-sm` `ink-tertiary`, 가장 최근에 열린 카드 = 피드 맨 위 카드의 pickDate 기준, 01과 같은 규칙. 열린 카드가 없으면 숨김) → 제목(`display-xl`) → 안내 한 줄(`body-lg` `ink-secondary`) → 32×1px `ink` 선 → 큰 버튼 두 개(전체 둘러보기 주 버튼, 뉴스레터 구독 보조 버튼, 사이 12px).
- 제목·안내는 API의 `nextRelative`로 고른다(SPEC 5절 홈 표).
- 뉴스레터 버튼은 `NEXT_PUBLIC_NEWSLETTER_URL`이 없으면 숨긴다(가정, F5).
    - `today`: "오늘의 공간은 아직이에요." / "오전 8시에 열려요."
    - `tomorrow`: "오늘은 여기까지예요." / "내일은 ○요일의 공간이 와요."
    - `later`: "이번 주는 여기까지예요." / "월요일 오전 8시에 새 공간이 와요."

### 03 로그인 시트

- 4절 바텀 시트. 제목 "저장하려면 로그인이 필요해요", 설명, 구글 버튼(너비 327px, 패딩 15px), 약관 안내(`body-sm` `ink-tertiary`).

### 04 공간 상세

- 사진 375×440px. 왼쪽 위(16, 52)에 뒤로 가기 화살표(24px, 선 1.25px). 아래쪽 32px에 왼쪽 "사진: ○○", 오른쪽 "1 / 4"(둘 다 `label-sm` `on-photo`).
- 본문(좌우 24px, 위 28px, 섹션 사이 48px):
  1. 제목 블록(사이 12px): 만남 출처 또는 머물러 봄 배지 → 이름(`display-xl`) → "지역 · 종류 · 방문 방식"(`body-sm` `ink-secondary`) → 저장 버튼 두 개 + "공유"(`label-md` `ink-secondary`)
  2. 에디터의 감상: SectionLabel + 감상(`body-lg`)
  3. 감각: SectionLabel + 감각 한 줄 ×4
  4. 만든 사람들: SectionLabel + CreditRow 목록 + "이 공간을 만드셨나요? 크레딧을 알려주세요 →"(`body-sm` `ink-secondary`)
  5. 에세이(있을 때만, SPEC 5절 7번): SectionLabel "에세이" + 본문. 만든 사람들과 방문 정보 사이. (F2에서 정함)
     - 마크다운 중 `h2` · `h3` · `p` · `ul` · `ol` · `li` · `strong` · `em` · `a` · `blockquote`만 그린다. `h1`은 `h2`로 그린다(에세이 안의 가장 큰 제목이 페이지 제목 `h1`과 겹치지 않게). 이미지·raw HTML은 그리지 않는다(허용 밖 요소는 안의 글자만 남김).
     - 글자: 문단·목록 `body-lg` `ink`, `h2` `display-md`, `h3` `label-md`, 인용 `body-lg` `ink-secondary` + 왼쪽 1px `line` 선과 16px 안쪽 여백. `strong`은 Medium(500).
     - 간격: 요소 사이 16px, 제목 위 24px, 목록 왼쪽 16px.
     - 링크: 밑줄(글자 아래 4px), 새 창(`noopener noreferrer`).
  6. 방문 정보: SectionLabel + 방문 정보 한 줄(주소, 방문, 메모, 지도)
  7. "○○의 다른 공간": SectionLabel + 3열 격자(본문 너비 안, 106×106px, 사이 4px)

**04의 문구 (가정, F2에서 정함)** — 코드 값은 SPEC 3절, 역할 문구는 API.md 1절(B4)을 따른다.

| 코드 | 화면 문구 |
| --- | --- |
| `kind` | cafe 카페 · bar 바 · restaurant 식당 · stay 스테이 · garden 정원 · museum 미술관 · gallery 갤러리 · bookstore 서점 · shop 숍 · religious 종교 공간 · public 공공 공간 · other 그 밖 |
| `access` | walk_in 바로 방문 · reservation 예약 · ticket 입장권 · guests_only 투숙객만 |
| 만남의 출처(`sources[0].type`) | article "기사로 만남" · video "영상으로 만남" · instagram "인스타그램으로 만남" · official "공식 소개로 만남". 첫 출처에 링크 |
| 머물러 봄 | `visitedByEditorOn`이 있으면 배지 "머물러 봄"(년·월은 배지 옆 `body-sm` `ink-tertiary`로 "2026년 5월") |
| 방문 정보 라벨 | 주소(오른쪽 "복사") · 방문 · 메모 · 지도("네이버지도 · 카카오맵") |
| 크레딧 확인 상태 | verified면 "출처 확인됨 ↗"(출처가 있을 때 외부 링크, 새 창. 출처가 없으면 "출처 확인됨" 글자만) · 아니면 "확인 중" |
| 같은 만든 사람의 다른 공간 | 크레딧이 한 명이면 "○○의 다른 공간", 둘 이상이면 "만든 사람들의 다른 공간" |

- 사진이 없고 임베드만 있으면 사진 자리에 임베드를 같은 크기로 넣는다. 둘 다 있으면 사진 뒤에 임베드가 이어진다(같은 가로 넘기기, "1 / n"에 포함).
- 임베드는 처음에 썸네일만(유튜브는 `i.ytimg.com` 썸네일, 인스타그램은 placeholder 색 + "인스타그램 게시물 보기"), 누르면 iframe을 불러온다.
- 사진이 하나도 없으면 placeholder 색만 두고 "사진: ○○"과 "1 / n"은 숨긴다. 뒤로 가기는 이때 `ink` 색.

### 05 만든 사람 프로필 (검수 완료)

- 위(좌우 24px, 위 52px, 사이 12px): 뒤로 가기 → 영문 라벨("STUDIO · 서울 종로") → 이름(`display-xl`)과 오른쪽 끝 FollowButton → 소개(`body-md` `ink-secondary`, 327px) → 링크·소속(`body-sm` `ink-tertiary`).
- SectionLabel "○○가 만든 공간" 다음에 **화면 끝까지 꽉 찬 3열 격자**(124px, 사이 1.5px). 캡션은 역할.
- 머리 아래 28px, SectionLabel 아래 12px 뒤에 격자.

**05의 문구와 세부 (가정, F3에서 정함)**

| 항목 | 규칙 |
| --- | --- |
| 뒤로 가기 | 머리 흐름 안 첫 줄(왼쪽 24px, 위 52px), 영문 라벨까지 36px(머리의 사이 12px + 24px). 터치 영역은 44px |
| 영문 라벨 | `kind` 대문자(PERSON · STUDIO · OWNER) + `region`이 있으면 " · 지역". 지역은 응답 그대로 |
| 링크·소속 줄 | 있는 것만 " · "로 잇는다: 웹사이트(프로토콜·`www.`·끝 `/`를 뺀 주소, 새 창) · 인스타그램("@아이디", 새 창) · 소속("소속 ○○ →", 소속 프로필로). 셋 다 없으면 줄을 숨긴다 |
| SectionLabel | 이름 끝 글자에 받침이 있으면 "○○이 만든 공간", 없으면 "○○가 만든 공간". 한글이 아니면 "○○가 만든 공간" |
| 소개 | 없으면 숨긴다 |

### 06 둘러보기 (검수 완료)

- 머리(좌우 24px, 위 64px): "BROWSE"(`label-sm`) → "둘러보기"(`display-lg`).
- 상단 탭 "공간 | 만든 사람" → 필터 칩 줄(위아래 16px) → 꽉 찬 3열 격자(캡션은 공간 이름).
- 머리: 사이 8px, 아래 20px. 상단 탭 글자 위 4px.

**06의 세부 (가정, F4에서 정함)**

| 항목 | 규칙 |
| --- | --- |
| 필터 칩 | 맨 앞 "전체", 그다음 정원 · 스테이 · 카페 · 미술관 · 바 · 갤러리 · 서점 · 식당 · 숍 · 종교 공간 · 공공 공간 · 그 밖. 줄이 넘치면 가로로 넘긴다. 종류 칩은 누를 때마다 켜고 끈다(여러 개). "전체"는 모두 끈다. 켜진 종류가 없으면 "전체"가 켜짐 |
| URL | `?kind=garden,cafe`(칩 순서). 모르는 값은 무시한다 |
| 만든 사람 탭 | 칩 없음. 줄 목록: 왼쪽 48px 정사각형 사진(대표 공간 사진, 없으면 placeholder 색) · 사이 12px · 이름(`body-md`)과 그 아래 영문 라벨(`label-sm` `ink-tertiary`, 05와 같은 "STUDIO · 지역"). 줄 위아래 14px, 아래만 1px `line`, 좌우 24px. 줄 전체가 프로필 링크 |
| 빈 결과 | 격자 자리에 좌우 24px · 위 48px: "아직 이 조건의 공간이 없어요"(`body-md` `ink-secondary`) → 16px → "필터 초기화"와 "공간 소개하기 →"(`label-md` `ink`, 사이 24px) |
| 이어 받기 | 목록 끝이 화면 아래 400px 안에 오면 다음 페이지를 받는다. 받는 동안 따로 표시하지 않는다 |

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

## 7. CSS 변수 (실제 파일: frontend/app/tokens.css)

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
  --shell-max-width: 480px;

  /* --font-serif · --font-sans는 app/layout.tsx의 next/font가 <html>에 정의한다(같은 이름을 여기서도 선언하면 순서에 따라 뒤집힌다). */
}
```

---

## 8. 남은 것

- [x] 05~07 화면 검수 (피그마 원본과 맞추는 것은 한도가 풀린 뒤)
- [ ] 관리자 화면(등록 폼, 편성) 배치. 데스크톱 너비, 같은 토큰
- [ ] 빈 상태 화면(저장 없음, 이번 주 편성 없음)
- [x] 하단 탭 이름 "오늘"로 확정, SPEC.md 반영 (2026-10-06)
