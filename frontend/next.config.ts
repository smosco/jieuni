import type { NextConfig } from "next";

// 브라우저는 백엔드를 직접 부르지 않는다. 같은 출처로 받아 백엔드로 넘긴다(세션 쿠키·CSRF 쿠키가 도메인과 상관없이 동작하게).
// 배열로 주면 afterFiles: 파일 라우트(app/api/revalidate)가 먼저 잡힌다.
const nextConfig: NextConfig = {
  output: "standalone",
  poweredByHeader: false,
  async headers() {
    return [
      {
        source: "/:path*",
        headers: [
          { key: "Content-Security-Policy", value: contentSecurityPolicy() },
          { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
          { key: "X-Content-Type-Options", value: "nosniff" },
          { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
        ],
      },
    ];
  },
  async rewrites() {
    const apiUrl = backendUrl();
    return [
      { source: "/api/:path*", destination: `${apiUrl}/api/:path*` },
      { source: "/oauth2/:path*", destination: `${apiUrl}/oauth2/:path*` },
      { source: "/login/oauth2/:path*", destination: `${apiUrl}/login/oauth2/:path*` },
    ];
  },
};

// rewrites 목적지는 빌드 결과물에 굳는다. 운영 빌드에서 빠지면 localhost로 굳은 채 배포되므로 멈춘다.
// next typegen도 같은 phase·NODE_ENV로 rewrites까지 부르므로 build와 구분할 수 없다. 그래서 typecheck 스크립트가 API_URL 자리 값을 준다.
function backendUrl(): string {
  if (process.env.API_URL) {
    return process.env.API_URL;
  }
  if (process.env.NODE_ENV === "production") {
    throw new Error("API_URL이 없습니다. 운영 빌드에는 백엔드 주소가 필요합니다(.env.example 참고).");
  }
  return "http://localhost:8080";
}

export default nextConfig;

// nonce를 쓰면 모든 페이지를 요청마다 그려야 해서(Next CSP 안내) 태그 캐시와 맞지 않는다. 그래서 인라인 스크립트는 'unsafe-inline'.
// 개발 서버만 React가 eval을 쓴다. 헤더는 빌드 때 굳으므로 사진 주소도 빌드 환경 변수에서 읽는다.
function contentSecurityPolicy(): string {
  const dev = process.env.NODE_ENV === "development";
  return [
    "default-src 'self'",
    `script-src 'self' 'unsafe-inline'${dev ? " 'unsafe-eval'" : ""}`,
    "style-src 'self' 'unsafe-inline'",
    // blob: 관리자 사진 미리보기, i.ytimg.com: 유튜브 임베드 썸네일, lh3: 구글 프로필 사진
    `img-src 'self' data: blob: ${photoOrigin()} https://i.ytimg.com https://lh3.googleusercontent.com`,
    "font-src 'self'",
    // 관리자 사진은 R2 서명 URL로 브라우저가 직접 올린다(계정마다 호스트가 다르다).
    "connect-src 'self' https://*.r2.cloudflarestorage.com",
    "frame-src https://www.youtube-nocookie.com https://www.instagram.com",
    "object-src 'none'",
    "base-uri 'self'",
    "form-action 'self'",
    "frame-ancestors 'none'",
  ].join("; ");
}

// 운영은 R2 커스텀 도메인, 값이 없으면(로컬) 개발 버킷의 r2.dev 주소.
// Vercel 운영 빌드에서 빠지면 사진이 CSP에 막힌 채 배포되므로 멈춘다(로컬 Docker 빌드에는 VERCEL_ENV가 없다).
function photoOrigin(): string {
  const base = process.env.PHOTO_PUBLIC_BASE_URL;
  if (base) {
    return new URL(base).origin;
  }
  if (process.env.VERCEL_ENV === "production") {
    throw new Error("PHOTO_PUBLIC_BASE_URL이 없습니다. 운영 빌드의 CSP img-src에 사진 주소가 필요합니다.");
  }
  return "https://*.r2.dev";
}
