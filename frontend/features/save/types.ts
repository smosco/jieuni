export type SaveStatus = "want" | "been";

// 버튼을 눌렀을 때 바뀔 상태. 켜진 버튼을 다시 누르면 해제(null), 다른 버튼이면 그 상태로.
export function nextSaveStatus(current: SaveStatus | null, pressed: SaveStatus): SaveStatus | null {
  return current === pressed ? null : pressed;
}
