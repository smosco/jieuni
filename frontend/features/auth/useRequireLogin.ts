"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useCallback } from "react";
import { useMe } from "@/features/me/queries";
import { ApiError } from "@/shared/api/client";
import { useToast } from "@/shared/ui/Toast";
import { useLoginSheet } from "./components/LoginProvider";
import { FAILURE_MESSAGE, runAction } from "./pendingAction";
import type { PendingAction } from "./types";

// 비로그인이면 로그인 시트, 로그인 상태면 바로 실행.
// 세션이 끝나 401이 오면 비로그인으로 바꾸고 같은 동작으로 시트를 연다(로그인하면 이어서 완료된다).
// onLeave: 구글 로그인으로 페이지를 떠나기 직전에 부른다(피드가 보던 카드를 남긴다).
export function useRequireLogin() {
  const { data: me } = useMe();
  const { openLogin } = useLoginSheet();
  const queryClient = useQueryClient();
  const toast = useToast();

  return useCallback(
    async (action: PendingAction, onLeave?: () => void) => {
      if (me === null) {
        openLogin(action, onLeave);
        return;
      }
      try {
        await runAction(action, queryClient);
      } catch (e) {
        if (e instanceof ApiError && e.status === 401) {
          queryClient.setQueryData(["me"], null);
          openLogin(action, onLeave);
          return;
        }
        toast(FAILURE_MESSAGE[action.action]);
      }
    },
    [me, openLogin, queryClient, toast],
  );
}
