"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { resumeFollow } from "@/features/follow/queries";
import { useMe } from "@/features/me/queries";
import { resumeSave } from "@/features/save/queries";
import { useToast } from "@/shared/ui/Toast";
import { pathOf, stripLoginError } from "../login";
import { clearPending, takePending } from "../pendingAction";
import type { LoginRequest } from "../types";
import { LoginSheet } from "./LoginSheet";

const LoginContext = createContext<{ openLogin: (request: LoginRequest) => void }>({
  openLogin: () => {},
});

export function useLoginSheet() {
  return useContext(LoginContext);
}

export function LoginProvider({ children }: { children: ReactNode }) {
  const [request, setRequest] = useState<LoginRequest | null>(null);
  const { data: me } = useMe();
  const toast = useToast();
  const queryClient = useQueryClient();

  // 로그인 실패·취소로 돌아오면 서버가 login=error를 붙인다. 전체 새로고침으로만 들어오므로 처음 한 번만 본다.
  useEffect(() => {
    const { pathname, search, hash } = window.location;
    if (new URLSearchParams(search).get("login") !== "error") return;
    clearPending();
    toast("로그인하지 못했어요. 다시 시도해 주세요");
    window.history.replaceState(null, "", pathname + stripLoginError(search) + hash);
  }, [toast]);

  // 로그인 상태가 되면(구글에서 돌아와 /api/me를 받았거나 개발용 로그인) 누르던 동작을 한 번 이어서 한다.
  // /api/me 응답이 새 CSRF 쿠키를 심은 뒤라 PUT이 통과한다.
  useEffect(() => {
    if (!me) return;
    const pending = takePending();
    if (!pending || pathOf(pending.returnTo) !== window.location.pathname) return;
    // 실패하면 mutation이 되돌리고 토스트를 띄운다.
    if (pending.action === "save") resumeSave(queryClient, toast, pending.placeId, pending.status);
    if (pending.action === "follow") resumeFollow(queryClient, toast, pending.makerId);
  }, [me, queryClient, toast]);

  const openLogin = useCallback((next: LoginRequest) => setRequest(next), []);
  const value = useMemo(() => ({ openLogin }), [openLogin]);

  return (
    <LoginContext.Provider value={value}>
      {children}
      <LoginSheet request={request} onClose={() => setRequest(null)} />
    </LoginContext.Provider>
  );
}
