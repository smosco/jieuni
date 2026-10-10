import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";

// tsconfig의 "@/*" 경로를 테스트에서도 같게 푼다.
export default defineConfig({
  resolve: { alias: { "@": fileURLToPath(new URL(".", import.meta.url)) } },
});
