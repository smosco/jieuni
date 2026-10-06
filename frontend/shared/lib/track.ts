// 분석 이벤트(SPEC 8절). MVP는 도구 없이 이 함수 하나만 두고, 도구는 공개 후 여기에 붙인다(PLAN 확정 15).
export type TrackEvent =
  | "feed_view"
  | "feed_end"
  | "card_open"
  | "save"
  | "follow"
  | "suggestion_submit"
  | "newsletter_subscribe"
  | "share_click";

export function track(event: TrackEvent, props?: Record<string, string | number>): void {
  if (process.env.NODE_ENV === "development") {
    console.debug("[track]", event, props ?? {});
  }
}
