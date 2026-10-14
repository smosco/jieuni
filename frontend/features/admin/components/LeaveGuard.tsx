"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useRef, type ReactNode } from "react";

const MESSAGE = "저장하지 않은 변경이 있어요. 이 화면을 떠날까요?";

type Guard = { setDirty: (dirty: boolean) => void; confirmLeave: () => boolean };

const GuardContext = createContext<Guard>({ setDirty: () => {}, confirmLeave: () => true });

// 폼이 "저장 안 됨"을 알리고, 관리자 메뉴 링크가 떠나기 전에 묻는다.
// App Router에는 이동을 막는 전역 훅이 없어 Link의 onNavigate에서 막는다.
export function LeaveGuardProvider({ children }: { children: ReactNode }) {
  const dirtyRef = useRef(false);
  const setDirty = useCallback((dirty: boolean) => {
    dirtyRef.current = dirty;
  }, []);
  const confirmLeave = useCallback(() => !dirtyRef.current || window.confirm(MESSAGE), []);
  const value = useMemo(() => ({ setDirty, confirmLeave }), [setDirty, confirmLeave]);
  return <GuardContext.Provider value={value}>{children}</GuardContext.Provider>;
}

export function useConfirmLeave() {
  return useContext(GuardContext).confirmLeave;
}

// 새로고침·창 닫기·주소 입력은 브라우저 기본 확인창으로 막는다(문구는 브라우저가 정한다).
// 뒤로 가기는 막지 않는다. 폼이 입력을 브라우저에 보관해 다시 열면 불러올 수 있다.
export function useLeaveGuard(dirty: boolean) {
  const { setDirty } = useContext(GuardContext);
  useEffect(() => {
    setDirty(dirty);
    if (!dirty) return;
    const onBeforeUnload = (e: BeforeUnloadEvent) => {
      e.preventDefault();
      // preventDefault만으로는 확인창을 띄우지 않는 브라우저가 있다(MDN beforeunload, 레거시 지원).
      e.returnValue = "";
    };
    window.addEventListener("beforeunload", onBeforeUnload);
    return () => {
      setDirty(false);
      window.removeEventListener("beforeunload", onBeforeUnload);
    };
  }, [setDirty, dirty]);
}
