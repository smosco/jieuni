import Markdown, { type Components } from "react-markdown";
import styles from "./Essay.module.css";

// DESIGN 5절 04 에세이. raw HTML은 그리지 않고(skipHtml, rehype-raw 없음), 허용 밖 요소는 글자만 남긴다.
// h1은 페이지 제목(공간 이름)과 겹치지 않게 h2로 그린다.
const ALLOWED = ["h1", "h2", "h3", "p", "ul", "ol", "li", "strong", "em", "a", "blockquote"];

const components: Components = {
  h1: ({ children }) => <h2 className="display-md">{children}</h2>,
  h2: ({ children }) => <h2 className="display-md">{children}</h2>,
  h3: ({ children }) => <h3 className="label-md">{children}</h3>,
  p: ({ children }) => <p className="body-lg">{children}</p>,
  ul: ({ children }) => <ul className="body-lg">{children}</ul>,
  ol: ({ children }) => <ol className="body-lg">{children}</ol>,
  blockquote: ({ children }) => <blockquote>{children}</blockquote>,
  // 안전하지 않은 주소(javascript: 등)는 기본 urlTransform이 비운다. 그때는 글자만 남긴다.
  a: ({ href, children }) =>
    href ? (
      <a href={href} target="_blank" rel="noopener noreferrer">
        {children}
      </a>
    ) : (
      <>{children}</>
    ),
};

export function Essay({ markdown }: { markdown: string }) {
  return (
    <div className={`body-lg ${styles.essay}`}>
      <Markdown allowedElements={ALLOWED} unwrapDisallowed skipHtml components={components}>
        {markdown}
      </Markdown>
    </div>
  );
}
