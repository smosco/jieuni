# 공간의 지은이

줄여서 **지은이**. 공간에서 감동받는 사람들을 위한 서비스. 좋은 공간을 발견하고, 그 공간을 만든 사람을 따라가고, 자기만의 취향을 쌓는다.

> 정식 이름은 "공간의 지은이", 줄여 부를 때 "지은이". 페이지 제목과 계정에는 정식 이름을 쓴다(검색 노출 때문, SPEC 8절).

## 문서

| 문서 | 내용 |
| --- | --- |
| [docs/SPEC.md](docs/SPEC.md) | 무엇을 만드는가: 데이터, 화면, 규칙, 예외 |
| [docs/DECISIONS.md](docs/DECISIONS.md) | 어떤 기술로, 왜 |
| [docs/DESIGN.md](docs/DESIGN.md) | 어떻게 보이는가: 색, 글꼴, 간격, 컴포넌트, 화면 배치 |
| [docs/API.md](docs/API.md) | API 경로·요청·응답·상태 코드, 서비스 사이 내부 계약 |
| [CLAUDE.md](CLAUDE.md) | 작업 기준(제약·결정·작업 방식) |

## 디자인 자산

| 파일 | 내용 |
| --- | --- |
| [design/tokens.css](design/tokens.css) | CSS 변수와 글꼴 클래스. 프론트엔드에 그대로 가져다 쓴다 |
| [design/tokens.json](design/tokens.json) | 같은 값의 JSON |
| [피그마 원본](https://www.figma.com/design/d3wAmslKf6WOFoA8rr50Hc) | Foundations, 화면 01~07 |

토큰을 바꿀 때는 `docs/DESIGN.md`, `tokens.css`, `tokens.json`을 함께 고친다.

## 상태

- [x] 기획(SPEC), 기술 결정(DECISIONS), 디자인 기준(DESIGN), API 목록(API)
- [x] 작업 기준(CLAUDE.md)
- [ ] 구현
