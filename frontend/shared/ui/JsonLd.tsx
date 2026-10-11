// 검색 엔진용 구조화 데이터. 값에 든 "<"를 이스케이프해 스크립트 태그를 닫지 못하게 한다(Next JSON-LD 안내).
export function JsonLd({ data }: { data: Record<string, unknown> }) {
  return (
    <script
      type="application/ld+json"
      dangerouslySetInnerHTML={{ __html: JSON.stringify({ "@context": "https://schema.org", ...data }).replace(/</g, "\\u003c") }}
    />
  );
}
