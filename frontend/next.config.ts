import type { NextConfig } from "next";

// 브라우저는 백엔드를 직접 부르지 않는다. 같은 출처로 받아 백엔드로 넘긴다(세션 쿠키·CSRF 쿠키가 도메인과 상관없이 동작하게).
// 배열로 주면 afterFiles: 파일 라우트(app/api/revalidate)가 먼저 잡힌다.
const nextConfig: NextConfig = {
  output: "standalone",
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
