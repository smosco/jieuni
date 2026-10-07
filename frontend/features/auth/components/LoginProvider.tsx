"use client";

import { useQueryClient } from "@tanstack/react-query";
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { useMe } from "@/features/me/queries";
import { useToast } from "@/shared/ui/Toast";
import { pathOf, stripLoginError } from "../login";
import { FAILURE_MESSAGE, clearPending, runAction, takePending } from "../pendingAction";
import type { PendingAction } from "../types";
import { LoginSheet } from "./LoginSheet";

type Request = { action: PendingAction; onLeave?: () => void };

const LoginContext = createContext<{ openLogin: (action: PendingAction, onLeave?: () => void) => void }>({
  openLogin: () => {},
});

export function useLoginSheet() {
  return useContext(LoginContext);
}

export function LoginProvider({ children }: { children: ReactNode }) {
  const [request, setRequest] = useState<Request | null>(null);
  const { data: me } = useMe();
  const queryClient = useQueryClient();
  const toast = useToast();

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
    runAction(pending, queryClient).catch(() => toast(FAILURE_MESSAGE[pending.action]));
  }, [me, queryClient, toast]);

  const openLogin = useCallback((action: PendingAction, onLeave?: () => void) => setRequest({ action, onLeave }), []);
  const value = useMemo(() => ({ openLogin }), [openLogin]);

  return (
    <LoginContext.Provider value={value}>
      {children}
      <LoginSheet request={request} onClose={() => setRequest(null)} />
    </LoginContext.Provider>
  );
}
