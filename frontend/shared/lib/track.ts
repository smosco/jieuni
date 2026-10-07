// 분석 이벤트. 도구를 아직 고르지 않아 이 함수 하나만 두고, 도구를 고르면 여기에만 붙인다.
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
