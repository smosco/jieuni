"use client";

import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMe } from "@/features/me/queries";
import { createAdminPlace, editAdminPlace, fetchAdminPlace, fetchAdminPlacesPage } from "./api";
import type { AdminPlace, AdminPlaceStatus, PlaceEdit } from "./types";

const ADMIN_PLACES = ["admin", "places"] as const;

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

export function useEditAdminPlace(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: PlaceEdit) => editAdminPlace(id, body),
    onSuccess: (place: AdminPlace) => {
      queryClient.setQueryData(adminPlaceKey(id), place);
      queryClient.invalidateQueries({ queryKey: ADMIN_PLACES });
    },
  });
}
