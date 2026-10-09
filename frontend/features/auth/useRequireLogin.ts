"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useCallback } from "react";
import { forgetUser } from "@/features/me/cache";
import { useMe } from "@/features/me/queries";
import { isUnauthorized } from "@/shared/api/client";
import { useLoginSheet } from "./components/LoginProvider";
import type { LoginRequest } from "./types";

// 비로그인이면 로그인 시트, 로그인 상태면 run을 실행한다.
// 세션이 끝나 401이 오면 이전 사용자의 값을 지우고 시트를 연다(resume이면 로그인 뒤 이어서 완료된다).
// 그 밖의 실패는 run(저장·팔로우 mutation)이 되돌리고 토스트를 띄운다.
export function useRequireLogin() {
  const { data: me } = useMe();
  const { openLogin } = useLoginSheet();
  const queryClient = useQueryClient();

  return useCallback(
    async (request: LoginRequest, run: () => Promise<unknown>) => {
      if (me === null) {
        openLogin(request);
        return;
      }
      try {
        await run();
      } catch (e) {
        if (isUnauthorized(e)) {
          forgetUser(queryClient);
          openLogin(request);
        }
      }
    },
    [me, openLogin, queryClient],
  );
}
