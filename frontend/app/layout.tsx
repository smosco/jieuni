import type { Metadata } from "next";
import { IBM_Plex_Sans_KR, Noto_Serif_KR } from "next/font/google";
import { QueryProvider } from "@/shared/api/QueryProvider";
import { EntryPathRecorder } from "@/shared/lib/entry";
import { ToastProvider } from "@/shared/ui/Toast";
import "./tokens.css";
import "./globals.css";

// 한글은 Google Fonts가 unicode-range 조각 100여 개로 주고 subsets에 'korean'이 없다.
// 조각은 전부 self-host되고 브라우저가 쓰이는 글자의 조각만 받는다. preload는 끈다:
// Turbopack 빌드가 subsets와 상관없이 조각 전부(188개)에 preload 링크를 달았다(F1에서 확인).
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

export const metadata: Metadata = {
  title: "공간의 지은이",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="ko" className={`${serif.variable} ${sans.variable}`}>
      <body>
        <QueryProvider>
          <ToastProvider>{children}</ToastProvider>
          <EntryPathRecorder />
        </QueryProvider>
      </body>
    </html>
  );
}
