import type { Metadata } from "next";
import { IBM_Plex_Sans_KR, Noto_Serif_KR } from "next/font/google";
import { LoginProvider } from "@/features/auth/components/LoginProvider";
import { QueryProvider } from "@/shared/api/QueryProvider";
import { EntryPathRecorder } from "@/shared/lib/entry";
import { SITE_DESCRIPTION, SITE_NAME, SITE_URL } from "@/shared/lib/site";
import { ToastProvider } from "@/shared/ui/Toast";
import "./tokens.css";
import "./globals.css";

// 한글은 Google Fonts가 unicode-range 조각 100여 개로 주고 subsets에 'korean'이 없다.
// 조각은 전부 self-host되고 브라우저가 쓰이는 글자의 조각만 받는다. preload는 끈다:
// Turbopack 빌드가 subsets와 상관없이 조각 전부(188개)에 preload 링크를 달았다.
const serif = Noto_Serif_KR({
  weight: ["300", "400"],
  preload: false,
  display: "swap",
  variable: "--font-serif",
});

const sans = IBM_Plex_Sans_KR({
  weight: ["300", "400", "500"],
  preload: false,
  display: "swap",
  variable: "--font-sans",
});

// 자식 경로에서 상대 경로로 쓴 canonical·og:url·공유 카드 주소가 이 주소에 붙는다.
// title은 두지 않는다: 페이지마다 제목이 있고, 여기 두면 없는 주소의 404 화면에서 이 제목이 404 제목보다 먼저 나온다.
export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  description: SITE_DESCRIPTION,
  openGraph: { type: "website", siteName: SITE_NAME, locale: "ko_KR" },
  twitter: { card: "summary_large_image" },
  // 네이버 서치어드바이저 소유 확인.
  verification: { other: { "naver-site-verification": "eb5807c3de9b05433c83e248f0d39aeb549e81f7" } },
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="ko" className={`${serif.variable} ${sans.variable}`}>
      <body>
        <QueryProvider>
          <ToastProvider>
            <LoginProvider>{children}</LoginProvider>
          </ToastProvider>
          <EntryPathRecorder />
        </QueryProvider>
      </body>
    </html>
  );
}
