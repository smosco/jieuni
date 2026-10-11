import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";
import { CARD_COLOR } from "./cardColor";

const tokens = readFileSync(new URL("../../app/tokens.css", import.meta.url), "utf8");

function token(name: string): string | undefined {
  return tokens.match(new RegExp(`--color-${name}:\\s*(#[0-9A-Fa-f]{6})`))?.[1];
}

describe("공유 카드 색", () => {
  it.each(Object.entries(CARD_COLOR))("%s는 tokens.css의 같은 이름 변수와 같다", (name, value) => {
    expect(token(name)?.toUpperCase()).toBe(value.toUpperCase());
  });
});
