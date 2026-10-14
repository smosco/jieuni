import { describe, expect, it } from "vitest";
import { embedTypeOf } from "@/shared/lib/embed";
import { checkLists, move, rowKey, saveOrder, toRows, withoutBlankRows, type ListRows } from "./listSchema";
import type { AdminPlace } from "./types";

const PLACE = {
  credits: [
    {
      position: 0,
      role: "architecture",
      verified: true,
      sourceUrl: "https://example.com/c",
      maker: { id: 3, slug: "studio", name: "스튜디오", kind: "studio" },
      makerId: 3,
    },
  ],
  sources: [{ title: "기사", url: "https://example.com/a", type: "article" }],
  embeds: [{ type: "instagram", url: "https://www.instagram.com/p/abc/" }],
} as unknown as AdminPlace;

const credit = (over: Partial<ListRows["credits"][number]> = {}) => ({
  key: rowKey(),
  makerId: 3,
  makerName: "스튜디오",
  role: "architecture" as const,
  verified: true,
  sourceUrl: "https://example.com/c",
  ...over,
});

describe("목록 변경 판단", () => {
  it("불러온 그대로면 변경 없음", () => {
    expect(checkLists(toRows(PLACE), PLACE)).toEqual({
      changes: {},
      dirty: { credits: false, sources: false, embeds: false },
      errors: {},
    });
  });

  it("앞뒤 공백은 변경이 아니다", () => {
    const rows = toRows(PLACE);
    rows.sources[0].title = "  기사 ";
    expect(checkLists(rows, PLACE).dirty.sources).toBe(false);
  });

  it("순서를 바꾸면 그 목록만 변경, 본문은 새 순서", () => {
    const rows = toRows(PLACE);
    rows.credits = [credit({ makerId: 9, role: "interior", verified: false, sourceUrl: "" }), ...rows.credits];
    const result = checkLists(rows, PLACE);
    expect(result.dirty).toEqual({ credits: true, sources: false, embeds: false });
    expect(result.changes.credits?.map((c) => c.makerId)).toEqual([9, 3]);
    expect(result.changes.credits?.[0].sourceUrl).toBeNull();
    expect(result.changes.sources).toBeUndefined();
  });
});

describe("크레딧 검증", () => {
  it("확인됨이면 출처 링크가 필요하다", () => {
    const rows = { ...toRows(PLACE), credits: [credit(), credit({ sourceUrl: "  " })] };
    expect(checkLists(rows, PLACE).errors).toEqual({ "credits[1].sourceUrl": "확인됨이면 출처 링크가 필요해요" });
  });

  it("만든 사람·역할을 골라야 하고 링크는 https만", () => {
    const rows = { ...toRows(PLACE), credits: [credit({ makerId: null, role: "", verified: false, sourceUrl: "javascript:x" })] };
    expect(checkLists(rows, PLACE).errors).toEqual({
      "credits[0].makerId": "만든 사람을 골라 주세요",
      "credits[0].role": "역할을 골라 주세요",
      "credits[0].sourceUrl": "https://로 시작하는 주소를 넣어 주세요",
    });
  });

  it("형식이 틀린 목록은 보내지 않고 변경으로 남는다", () => {
    const rows = { ...toRows(PLACE), credits: [credit({ makerId: null })] };
    const result = checkLists(rows, PLACE);
    expect(result.dirty.credits).toBe(true);
    expect(result.changes.credits).toBeUndefined();
  });
});

describe("출처·임베드 검증", () => {
  it("반쯤 채운 출처 줄은 빈 칸마다 오류", () => {
    const rows = { ...toRows(PLACE), sources: [{ key: rowKey(), title: "기사", url: "", type: "" as const }] };
    expect(checkLists(rows, PLACE).errors).toEqual({
      "sources[0].url": "링크를 입력해 주세요",
      "sources[0].type": "종류를 골라 주세요",
    });
  });

  it("링크는 https만 받는다", () => {
    const rows = { ...toRows(PLACE), sources: [{ key: rowKey(), title: "기사", url: "http://example.com/a", type: "article" as const }] };
    expect(checkLists(rows, PLACE).errors).toEqual({ "sources[0].url": "https://로 시작하는 주소를 넣어 주세요" });
  });

  it("임베드 종류는 주소로 정하고, 영상·게시물 주소만 받는다", () => {
    const rows = {
      ...toRows(PLACE),
      embeds: [
        { key: rowKey(), url: "https://youtu.be/xyz" },
        { key: rowKey(), url: "https://www.instagram.com/someone/" },
        { key: rowKey(), url: "https://example.com/p/abc/" },
      ],
    };
    const result = checkLists(rows, PLACE);
    expect(result.errors).toEqual({
      "embeds[1].url": "유튜브 영상이나 인스타그램 게시물 주소를 넣어 주세요",
      "embeds[2].url": "유튜브 영상이나 인스타그램 게시물 주소를 넣어 주세요",
    });
  });

  it("도메인으로 종류를 고른다", () => {
    expect(embedTypeOf("https://m.youtube.com/watch?v=a")).toBe("youtube");
    expect(embedTypeOf("https://instagram.com/reel/a/")).toBe("instagram");
    expect(embedTypeOf("https://vimeo.com/1")).toBeNull();
    expect(embedTypeOf("주소 아님")).toBeNull();
  });

  it("바른 임베드는 종류가 붙어 보내진다", () => {
    const rows = { ...toRows(PLACE), embeds: [{ key: rowKey(), url: "https://www.youtube.com/watch?v=ok" }] };
    expect(checkLists(rows, PLACE).changes.embeds).toEqual([{ type: "youtube", url: "https://www.youtube.com/watch?v=ok" }]);
  });
});

describe("순서 옮기기", () => {
  it("한 칸을 다른 자리로", () => {
    expect(move(["a", "b", "c"], 0, 2)).toEqual(["b", "c", "a"]);
    expect(move(["a", "b", "c"], 2, 0)).toEqual(["c", "a", "b"]);
  });

  it("범위 밖이면 그대로", () => {
    expect(move(["a", "b"], 0, 5)).toEqual(["a", "b"]);
    expect(move(["a", "b"], -1, 0)).toEqual(["a", "b"]);
  });
});

describe("임시저장 순서", () => {
  const source = { title: "기사", url: "https://example.com/a", type: "article" as const };

  it("바뀐 것만, 기본 값 → 크레딧 → 출처 → 임베드", () => {
    expect(saveOrder(true, { credits: [], embeds: [] })).toEqual(["edit", "credits", "embeds"]);
    expect(saveOrder(false, {})).toEqual([]);
  });

  // 머물러 본 달을 비우며 출처를 더하면, 출처가 먼저 들어가야 공개 검증을 통과한다.
  it("새 출처가 있으면 출처를 기본 값보다 먼저", () => {
    expect(saveOrder(true, { credits: [], sources: [source] })).toEqual(["sources", "edit", "credits"]);
  });

  // 출처를 모두 빼며 머물러 본 달을 채우면, 기본 값이 먼저 들어가야 한다.
  it("출처를 모두 빼면 기본 값 다음에", () => {
    expect(saveOrder(true, { sources: [], embeds: [] })).toEqual(["edit", "sources", "embeds"]);
  });
});

describe("빈 줄", () => {
  const blankCredit = () => credit({ makerId: null, makerName: "", role: "", verified: false, sourceUrl: "" });
  const blankSource = () => ({ key: rowKey(), title: "", url: "", type: "" as const });

  it("아무것도 입력하지 않은 줄은 보내지 않고 변경도 아니다", () => {
    const rows = toRows(PLACE);
    const result = checkLists(
      { ...rows, credits: [...rows.credits, blankCredit()], sources: [blankSource(), ...rows.sources], embeds: [...rows.embeds, { key: rowKey(), url: " " }] },
      PLACE,
    );
    expect(result).toEqual({ changes: {}, dirty: { credits: false, sources: false, embeds: false }, errors: {} });
  });

  it("빈 줄을 빼도 오류는 화면의 줄 번호를 가리킨다", () => {
    const rows = { ...toRows(PLACE), credits: [blankCredit(), credit(), credit({ makerId: null })] };
    const result = checkLists(rows, PLACE);
    expect(result.errors).toEqual({ "credits[2].makerId": "만든 사람을 골라 주세요" });
  });

  it("빈 줄 사이의 순서는 남은 줄 그대로 보낸다", () => {
    const rows = { ...toRows(PLACE), credits: [credit({ makerId: 9 }), blankCredit(), credit({ makerId: 3 })] };
    expect(checkLists(rows, PLACE).changes.credits?.map((c) => c.makerId)).toEqual([9, 3]);
  });

  it("공개 검증에도 빈 줄은 없는 것으로 본다", () => {
    const rows = { ...toRows(PLACE), sources: [blankSource()] };
    expect(withoutBlankRows(rows).sources).toEqual([]);
  });
});
