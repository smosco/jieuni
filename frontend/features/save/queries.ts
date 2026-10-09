"use client";

import {
  MutationObserver,
  mutationOptions,
  skipToken,
  useMutation,
  useQuery,
  useQueryClient,
  type QueryClient,
} from "@tanstack/react-query";
import { isUnauthorized } from "@/shared/api/client";
import { track } from "@/shared/lib/track";
import { useToast } from "@/shared/ui/Toast";
import { deleteSave, putSave } from "./api";
import type { SaveStatus } from "./types";

// 공간 하나의 저장 상태. 화면이 /api/me/state로 한 번에 받아 채우고(useMeStateLoader), 버튼은 읽기만 한다.
// undefined는 아직 모름(비로그인 또는 받는 중).
export const saveKey = (placeId: number) => ["me", "save", placeId] as const;

export function useSaveStatus(placeId: number): SaveStatus | null | undefined {
  return useQuery<SaveStatus | null>({ queryKey: saveKey(placeId), queryFn: skipToken }).data;
}

// 누른 순간의 값. 화면이 다시 그려지기 전에 또 눌러도 앞의 누름이 반영된 값을 준다.
export function currentSaveStatus(queryClient: QueryClient, placeId: number): SaveStatus | null {
  return queryClient.getQueryData<SaveStatus | null>(saveKey(placeId)) ?? null;
}

function saveOptions(queryClient: QueryClient, toast: (message: string) => void, placeId: number) {
  return mutationOptions({
    // 저장·팔로우는 같은 /api/me/state를 고친다. 둘 다 끝났을 때만 그 상태를 다시 받으려고 접두를 같게 둔다.
    mutationKey: ["me-write", "save"],
    // 같은 공간의 요청은 앞의 것이 끝난 뒤에 보낸다. 켬→끔이 뒤바뀌어 도착하면 서버에 켜진 채 남는다.
    // 화면 반영(onMutate)은 기다리지 않는다.
    scope: { id: `save-${placeId}` },
    mutationFn: async (status: SaveStatus | null) => {
      if (status) await putSave(placeId, status);
      else await deleteSave(placeId);
    },
    onMutate: async (status) => {
      // 기다리기 전에 바꾼다. 같은 순간에 두 번 눌러도 두 번째가 바뀐 값을 읽는다.
      const previous = currentSaveStatus(queryClient, placeId);
      queryClient.setQueryData(saveKey(placeId), status);
      // 받는 중인 상태가 낙관적 값을 덮지 않게 멈춘다.
      await queryClient.cancelQueries({ queryKey: ["me", "state"] });
      return { previous };
    },
    onError: (error, _status, context) => {
      queryClient.setQueryData(saveKey(placeId), context?.previous ?? null);
      // 401은 로그인 시트가 대신 뜬다.
      if (!isUnauthorized(error)) toast("저장하지 못했어요. 다시 시도해 주세요");
    },
    onSuccess: (_, status) => {
      if (status) track("save", { status });
    },
    // 연달아 누르는 동안에는 다시 받지 않는다. 먼저 끝난 응답이 나중에 누른 값을 덮지 않게.
    onSettled: () => settled(queryClient, "save", () => queryClient.invalidateQueries({ queryKey: ["me", "saves"] })),
  });
}

// 이 종류의 마지막 요청이 끝나면 afterKind, 저장·팔로우 모두 끝나면 화면 상태(/api/me/state)를 다시 받는다.
export function settled(queryClient: QueryClient, kind: "save" | "follow", afterKind: () => void): void {
  if (queryClient.isMutating({ mutationKey: ["me-write", kind] }) === 1) afterKind();
  if (queryClient.isMutating({ mutationKey: ["me-write"] }) === 1) {
    queryClient.invalidateQueries({ queryKey: ["me", "state"] });
  }
}

export function useSaveMutation(placeId: number) {
  const queryClient = useQueryClient();
  const toast = useToast();
  return useMutation(saveOptions(queryClient, toast, placeId));
}

// 로그인 뒤 이어서 할 저장. 대상이 그때 정해져 훅 대신 같은 옵션으로 한 번 실행한다.
export function resumeSave(queryClient: QueryClient, toast: (message: string) => void, placeId: number, status: SaveStatus) {
  new MutationObserver(queryClient, saveOptions(queryClient, toast, placeId)).mutate(status).catch(() => {});
}
