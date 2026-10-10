"use client";

import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMe } from "@/features/me/queries";
import {
  changePlaceStatus,
  createAdminMaker,
  createAdminPlace,
  editAdminMaker,
  editAdminPlace,
  fetchAdminMaker,
  fetchAdminMakersPage,
  fetchAdminPlace,
  fetchAdminPlacesPage,
  replacePlaceList,
} from "./api";
import { saveOrder } from "./listSchema";
import type { AdminPlace, AdminPlaceStatus, MakerEdit, PlaceEdit, PlaceLists } from "./types";

const ADMIN_PLACES = ["admin", "places"] as const;
const ADMIN_MAKERS = ["admin", "makers"] as const;

export const adminPlaceKey = (id: number) => ["admin", "place", id] as const;

// 관리자 셸 안에서만 쓰지만, 역할이 확인되기 전에 403을 부르지 않게 한 번 더 막는다.
export function useAdminPlacesPages(status: AdminPlaceStatus | null, q: string) {
  const { data: me } = useMe();
  return useInfiniteQuery({
    queryKey: [...ADMIN_PLACES, status, q],
    queryFn: ({ pageParam }) => fetchAdminPlacesPage(status, q, pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: (last) => last.nextCursor,
    enabled: me?.role === "admin",
  });
}

export function useAdminPlace(id: number) {
  const { data: me } = useMe();
  return useQuery({
    queryKey: adminPlaceKey(id),
    queryFn: () => fetchAdminPlace(id),
    enabled: me?.role === "admin",
    // 기본 재시도는 404에도 세 번 다시 불러 "찾을 수 없는 공간"이 몇 초 늦게 뜬다.
    retry: false,
  });
}

export function useCreateAdminPlace() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createAdminPlace,
    onSuccess: (place) => {
      queryClient.setQueryData(adminPlaceKey(place.id), place);
      queryClient.invalidateQueries({ queryKey: ADMIN_PLACES });
    },
  });
}

export type PlaceSave = { edit?: PlaceEdit; lists: Partial<PlaceLists> };

// 임시저장 한 번: 바뀐 것만 saveOrder 순서로 보낸다.
// 요청마다 응답으로 캐시를 바꿔, 중간에 실패해도 이미 저장된 것은 "저장됨"으로 보인다.
export function useSavePlace(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ edit, lists }: PlaceSave): Promise<AdminPlace | undefined> => {
      let saved: AdminPlace | undefined;
      const keep = (place: AdminPlace) => {
        saved = place;
        queryClient.setQueryData(adminPlaceKey(id), place);
      };
      for (const step of saveOrder(edit !== undefined, lists)) {
        if (step === "edit") keep(await editAdminPlace(id, edit as PlaceEdit));
        else keep(await replacePlaceList(id, step, lists[step] as PlaceLists[typeof step]));
      }
      return saved;
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: ADMIN_PLACES }),
  });
}

export function useChangePlaceStatus(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (to: AdminPlaceStatus) => changePlaceStatus(id, to),
    onSuccess: (place: AdminPlace) => {
      queryClient.setQueryData(adminPlaceKey(id), place);
      queryClient.invalidateQueries({ queryKey: ADMIN_PLACES });
    },
  });
}

export function useAdminMakersPages(q: string) {
  const { data: me } = useMe();
  return useInfiniteQuery({
    queryKey: [...ADMIN_MAKERS, "list", q],
    queryFn: ({ pageParam }) => fetchAdminMakersPage(q, pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: (last) => last.nextCursor,
    enabled: me?.role === "admin",
  });
}

// 크레딧 콤보박스: 첫 페이지만 쓴다.
export function useMakerSearch(q: string, enabled: boolean) {
  const { data: me } = useMe();
  return useQuery({
    queryKey: [...ADMIN_MAKERS, "search", q],
    queryFn: () => fetchAdminMakersPage(q, null),
    enabled: enabled && me?.role === "admin",
    staleTime: 30 * 1000,
  });
}

export function useAdminMaker(id: number) {
  const { data: me } = useMe();
  return useQuery({
    queryKey: [...ADMIN_MAKERS, "one", id],
    queryFn: () => fetchAdminMaker(id),
    enabled: me?.role === "admin",
    retry: false,
  });
}

export function useCreateAdminMaker() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createAdminMaker,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ADMIN_MAKERS }),
  });
}

export function useEditAdminMaker(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: MakerEdit) => editAdminMaker(id, body),
    onSuccess: (maker) => {
      queryClient.setQueryData([...ADMIN_MAKERS, "one", id], maker);
      queryClient.invalidateQueries({ queryKey: [...ADMIN_MAKERS, "list"] });
      queryClient.invalidateQueries({ queryKey: [...ADMIN_MAKERS, "search"] });
      // 공간 폼의 크레딧 이름이 이 사람의 이름을 쓴다.
      queryClient.invalidateQueries({ queryKey: ["admin", "place"] });
    },
  });
}
