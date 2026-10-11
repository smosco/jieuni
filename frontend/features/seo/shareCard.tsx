import { ImageResponse } from "next/og";
import { SITE_NAME } from "@/shared/lib/site";

export const SHARE_CARD_SIZE = { width: 1200, height: 630 };

// ImageResponse는 CSS 변수를 읽지 못한다. tokens.css와 같은 값이고 배치는 DESIGN.md "공유 카드".
const COLOR = { paper: "#F4F0E8", ink: "#2A2622", inkSecondary: "#6E665C", earth: "#8A5A3C" };

// 한글 글꼴 전체는 ImageResponse 번들 한도(500KB)를 넘는다. 카드에 쓰는 글자만 Google Fonts에서 받는다.
// 브라우저 User-Agent 없이 부르면 CSS가 woff2가 아닌 TrueType 주소를 준다(satori는 woff2를 못 읽는다).
async function loadFont(family: string, weight: number, text: string): Promise<ArrayBuffer> {
  const query = `family=${family.replace(/ /g, "+")}:wght@${weight}&text=${encodeURIComponent(text)}`;
  const css = await fetch(`https://fonts.googleapis.com/css2?${query}`, { cache: "force-cache" }).then((r) => r.text());
  const url = css.match(/src: url\((.+?)\) format\('(opentype|truetype)'\)/)?.[1];
  if (!url) {
    throw new Error(`공유 카드 글꼴을 받지 못했어요: ${family} ${weight}`);
  }
  return fetch(url, { cache: "force-cache" }).then((r) => r.arrayBuffer());
}

// 실패하면 그대로 던진다. 한글이 빈 네모로 그려진 카드가 캐시에 남는 것보다 카드가 없는 편이 낫다.
export async function shareCard({ kicker, title }: { kicker: string; title: string }): Promise<ImageResponse> {
  const [serif, sans] = await Promise.all([
    loadFont("Noto Serif KR", 300, title),
    loadFont("IBM Plex Sans KR", 400, SITE_NAME + kicker),
  ]);
  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          justifyContent: "space-between",
          padding: 96,
          background: COLOR.paper,
          fontFamily: "Sans",
        }}
      >
        <div style={{ display: "flex", fontSize: 28, color: COLOR.inkSecondary }}>{SITE_NAME}</div>
        <div style={{ display: "flex", flexDirection: "column" }}>
          <div style={{ width: 48, height: 2, background: COLOR.earth, marginBottom: 32 }} />
          <div style={{ display: "flex", fontSize: 28, color: COLOR.inkSecondary, marginBottom: 20 }}>{kicker}</div>
          <div style={{ display: "flex", fontFamily: "Serif", fontSize: 80, lineHeight: 1.25, color: COLOR.ink }}>
            {title}
          </div>
        </div>
      </div>
    ),
    {
      ...SHARE_CARD_SIZE,
      fonts: [
        { name: "Serif", data: serif, weight: 300, style: "normal" },
        { name: "Sans", data: sans, weight: 400, style: "normal" },
      ],
    },
  );
}
