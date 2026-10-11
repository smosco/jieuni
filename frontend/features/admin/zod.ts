import { z } from "zod";

// Zod 4는 빠른 검증 경로를 위해 new Function을 시도한다. CSP에 'unsafe-eval'이 없어 막히고, 실패를 삼켜도
// 브라우저는 CSP 위반으로 보고한다. jitless면 시도하지 않는다(검증 결과는 같다). 스키마는 이 z만 쓴다.
z.config({ jitless: true });

export { z };
