// 이름 뒤 "이/가". 끝 글자가 한글이고 받침이 있으면 "이", 그 밖은 "가".
export function subjectParticle(name: string): "이" | "가" {
  const code = name.trim().charCodeAt(name.trim().length - 1) - 0xac00;
  const isHangulSyllable = code >= 0 && code <= 0xd7a3 - 0xac00;
  return isHangulSyllable && code % 28 !== 0 ? "이" : "가";
}
