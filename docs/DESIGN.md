# 디자인 기준

이 문서는 화면이 **어떻게 보이는지** 적습니다. 원칙, 토큰, 컴포넌트, 화면별 핵심 수치, 접근성 기준이 있습니다. 화면을 고칠 때와 프론트 구현을 볼 때 읽습니다. 화면이 **무엇을 하는지**(동작, 문구, 빈 상태)는 [SPEC.md](SPEC.md)에 있습니다.

## 1. 원칙

인테리어·건축 스튜디오의 포트폴리오처럼, 고요하고 섬세하게.

- 종이색 바탕에 먹색 글자. 포인트 색은 흙색(`--color-earth`) 하나입니다. 오류 색은 입력 오류에만 씁니다.
- 사진이 주인공입니다. 선은 1px 헤어라인, 모서리는 2px. 그림자·그라데이션 버튼·둥근 카드는 쓰지 않습니다(사진 위 글자를 읽히게 하는 그림자만 예외).
- 숫자 배지, 아이콘 남발, 이모지를 쓰지 않습니다. 좋아요·저장 수 같은 숫자는 화면에 없습니다.
- 기준 너비는 375px, 좌우 여백 24px(본문 327px)입니다. 공개 화면은 데스크톱에서 최대 480px로 가운데 두고, 관리자 화면만 최대 960px입니다.
- 작은 영문 라벨은 대문자에 자간을 넓힙니다(WEDNESDAY, BROWSE).

## 2. 토큰

값의 원본은 [frontend/app/tokens.css](../frontend/app/tokens.css) 하나이고, [design/tokens.json](../design/tokens.json)은 같은 값의 JSON입니다. 컴포넌트 CSS는 이 변수와 글자 클래스만 씁니다. 색·간격·글자 크기를 숫자로 직접 쓰지 않고, 필요한 값이 없으면 토큰을 먼저 추가합니다. 토큰을 바꿀 때는 두 파일과 이 문서를 함께 고칩니다.

| 분류 | 토큰 | 역할 |
| --- | --- | --- |
| 바탕 | `--color-paper` · `--color-paper-raised` · `--color-photo-placeholder` | 화면·탭 바·시트 바탕 / 입력칸처럼 살짝 뜬 면 / 사진이 없을 때 자리 |
| 선 | `--color-line` | 헤어라인, 꺼진 버튼 테두리 |
| 글자 | `--color-ink` · `--color-ink-secondary` · `--color-ink-tertiary` | 제목·본문 / 설명 / 라벨·보조 정보. 세 단계로 위계를 만듭니다 |
| 꺼진 것 | `--color-ink-disabled` | 꺼진 버튼·링크, 고를 수 없는 항목에만 |
| 포인트 | `--color-earth` | 머물러 봄 배지, 감각 라벨, "새로운", 내 공간 옆 점 |
| 오류 | `--color-error` | 입력 오류 메시지와 오류 칸 밑줄에만 |
| 사진 위 | `--color-on-photo`, `--on-photo-80`~`20`, `--shade-photo`, `--shade-photo-top`, `--shade-tile` | 사진 위 글자와 투명도 단계, 글자를 읽히게 하는 그림자 |
| 글꼴 | `display-xl` · `display-lg` · `display-md` | Noto Serif KR. 공간·프로필 이름(30px) / 화면 제목(24px) / 시트·구역 제목(19px) |
| 글꼴 | `body-lg` · `body-md` · `body-sm` | IBM Plex Sans KR. 감상·한 줄(15px) / 본문·이름(14px) / 지역·출처·안내(12px) |
| 글꼴 | `label-md` · `label-sm` | 버튼·탭(12px, Medium) / 섹션 라벨·역할·영문 라벨(10px) |
| 간격 | `--space-xs`~`--space-4xl` | 4 · 8 · 12 · 16 · 24 · 32 · 48 · 64px. 화면별 값은 같은 파일의 이름 있는 토큰(`--detail-photo-height` 등) |
| 크기 | `--page-gutter` · `--content-width` · `--shell-max-width` · `--admin-max-width` · `--touch-min` | 24px · 327px · 480px · 960px · 44px |
| 움직임 | `--duration-state` · `--duration-sheet` · `--ease-out` | 상태 전환 120ms, 바텀 시트 240ms |

글꼴은 `next/font/google`로 불러오고, `--font-serif`·`--font-sans` 변수는 `app/layout.tsx`가 `<html>`에 정의합니다.

## 3. 컴포넌트

| 컴포넌트 | 상태 | 모양 |
| --- | --- | --- |
| SaveButton | `want`·`been` × off·on | 패딩 10·16px, `label-md`. off는 1px `line` 테두리, on은 `ink` 바탕에 `paper` 글자. 두 버튼 사이 8px, 한 번에 하나만 on. 사진 위에서는 `on-photo` |
| FollowButton | `follow` · `following` | 패딩 5·10px, `label-sm`. follow는 1px `ink` 테두리, following은 테두리 없이 `ink-tertiary`. 피드 카드에서는 follow 상태에서만 보임 |
| Badge(머물러 봄) | — | 패딩 4·8px, 1px `earth` 테두리, 앞에 4px 원, `label-sm` `earth` |
| TabBar(하단) | 켜짐 · 꺼짐 · 점 | 높이 64px, 위 1px `line`, 좌우 40px. 오늘 · 둘러보기 · 내 공간. 켜진 탭은 글자 위 6px에 16×1px 선. 내 공간 옆 5px `earth` 점 |
| 상단 탭 | 켜짐 · 꺼짐 | `label-md`, 항목 사이 24px. 켜진 탭은 글자 아래 10px에 1px `ink` 선 |
| PlaceTile | 기본 · 휴업 · 폐업 · 새로운 | 정사각형 3열 격자, 375px 화면 끝까지 채우면 124px·사이 1.5px, 본문 안에서는 사이 4px. 캡션은 왼쪽 아래 8px `label-sm`, 아래 48px에 옅은 그림자. 폐업은 투명도 40%. "새로운"은 왼쪽 위 6px 꼬리표 |
| CreditRow | 확인됨 · 확인 중 | 위아래 14px, 아래 1px `line`. 역할 칸 56px · 이름과 "출처 확인됨 ↗"/"확인 중" · 오른쪽 FollowButton |
| SectionLabel | — | `label-sm` `ink-tertiary` 글자 + 12px 뒤 남은 너비를 채우는 1px 선 |
| 감각·방문 정보 한 줄 | — | 왼쪽 라벨 40px(감각은 `earth`), 오른쪽 `body-md`, 아래 1px `line` |
| 큰 버튼 | 주 · 보조 · 꺼짐 | 놓인 영역을 채움, 패딩 14·16px, `label-md`, 오른쪽 "→". 주는 `ink` 바탕, 보조는 1px `ink` 테두리 |
| 필터 칩 | 켜짐 · 꺼짐 | 패딩 6·12px, `label-sm`, 사이 6px. 켜짐은 `ink` 바탕 |
| Toast | — | 탭 바 위 16px 가운데, `ink` 바탕 `paper` 글자, 2초 뒤 사라짐, `aria-live="polite"` |
| 상태 띠(휴업·폐업) | 휴업 · 폐업 | 상세 사진 아래 전체 너비, `paper-raised` 바탕, `body-sm` `ink-secondary` |
| 바텀 시트 | 열림 · 닫힘 | 네이티브 `<dialog>`. 위에 32×2px 손잡이, 패딩 12·24·40px, 항목 사이 20px, 뒤 배경 `ink` 55%. Esc·바깥 누르기로 닫힘 |
| 입력칸 | 기본 · 포커스 · 오류 | 공개 화면은 1px 밑줄(포커스 `ink`, 오류 `error`). 관리자 폼은 `paper-raised` 바탕 + 밑줄. 글자 16px |

## 4. 화면별 핵심 수치

| 화면 | 수치 |
| --- | --- |
| 01 피드 카드 | 사진이 화면 전체(탭 바 제외). 아래 520px 그림자, 위 140px 그림자. 상단 줄 56px: 요일 영문 · 진행 막대 5개(14×1px, 사이 4px, 지금 100% · 열린 날 55% · 안 열린 날 20%) · "03 / 05". 하단 블록은 좌우 24px, 항목 사이 14px, 탭 바 위 32px에서 끝남. 한 줄 최대 300px |
| 02 피드 마지막 화면 | 위 260px부터, 좌우 32px, 항목 사이 20px. 제목 `display-xl` → 안내 `body-lg` → 32×1px 선 → 큰 버튼 둘(사이 12px) → 소개·약관·방침 링크 줄 |
| 03 로그인 시트 | 바텀 시트. 구글 버튼 327px, 패딩 15px. 만 14세 확인 전에는 버튼이 꺼짐 |
| 04 공간 상세 | 사진 375×440px, 뒤로 가기는 (16, 52)에 24px. 본문 위 28px, 좌우 24px, 섹션 사이 48px. 같은 만든 사람의 다른 공간은 본문 안 3열 격자 |
| 05 만든 사람 프로필 | 머리 위 52px, 항목 사이 12px. 머리 아래 28px, 화면 끝까지 채운 3열 격자(캡션은 역할) |
| 06 둘러보기 | 머리 위 64px · 사이 8px · 아래 20px. 칩 줄 위아래 16px. 목록 끝이 화면 아래 400px 안에 오면 다음 페이지를 받음 |
| 07 내 공간 | 머리는 둘러보기와 같음. 탭 아래 20px부터 격자. 팔로잉 탭의 두 번째 섹션 위 28px |
| 08 공간 소개하기 | 좌우 24px, 위 24px, 항목 사이 24px. 메시지는 칸 아래 `body-sm` `error` |
| 09 관리자 셸 | 최대 960px, 왼쪽 메뉴 160px + 본문. 본문이 327px보다 좁아지면 본문이 메뉴 아래로 내려감 |
| 10 관리자 공간 폼 | 폼 최대 560px 한 열. 구역 사이 48px+48px, 항목 사이 24px. 아래 고정 줄(상태 · ⋯ 44px · 큰 버튼 하나). 사진 미리보기 3:2 |
| 13 공유 카드 | 1200×630 PNG, 안쪽 96px. 서비스 이름 28px → 아래쪽 `earth` 선 48×2px → 보조 줄 28px → 제목 Serif 80px. 사진은 넣지 않음. 색은 `features/seo/cardColor.ts`에 값으로 두고, 토큰과 같은지 테스트로 확인 |
| 14 404·오류, 15 소개·약관·방침 | 위 64px(문서 페이지는 52px), 좌우 24px. 본문 절 사이 32px |

## 5. 움직임

- 피드는 CSS scroll-snap으로 한 장씩 멈춥니다. 라이브러리는 쓰지 않습니다.
- 저장·팔로우는 색만 120ms로 바뀝니다. 크기 변화나 튀는 효과는 없습니다. 바텀 시트는 240ms로 올라옵니다.
- `prefers-reduced-motion`이면 전환 시간을 0으로 둡니다.

## 6. 접근성

- **대비**(paper 바탕): ink 13.2:1, ink-secondary 7.0:1, error 6.3:1, earth 5.1:1, ink-tertiary 4.6:1. 읽는 글자는 모두 WCAG AA(4.5:1) 이상입니다. ink-disabled(2.4:1)는 꺼진 컨트롤에만 씁니다(WCAG는 꺼진 컨트롤을 대비 기준에서 뺍니다). 피드 상단 글자는 위 그림자 덕분에 사진 자리 색 위에서도 5.0:1입니다.
- **누르는 영역**: 44px 이상(`--touch-min`). 작은 글자 링크·버튼은 보이는 크기를 두고 누르는 영역만 넓힙니다(`hit-area` 클래스나 `::before`). 예외: 상세 크레딧 줄의 이름과 "출처 확인됨"은 위아래로 붙어 각 30px 남짓, 관리자 순서 버튼은 가로 39px입니다.
- **사진 설명**: 모든 사진에 `alt`가 있어야 공개할 수 있습니다.
- **입력칸 글자 16px**: iOS Safari가 16px보다 작은 입력칸을 누르면 화면을 확대하기 때문입니다.
- **스크린 리더**: 토스트는 `aria-live="polite"`, 오류 칸은 `aria-invalid`, 메시지·도움말은 `aria-describedby`로 칸에 묶습니다. 관리자 ⋯ 버튼은 `aria-expanded`, 꺼진 메뉴 항목은 `aria-disabled`입니다.
