"use client";

import { useInfiniteQuery, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { followKey } from "@/features/follow/queries";
import { fetchSavesPage } from "@/features/save/api";
import { saveKey } from "@/features/save/queries";
import type { SaveStatus } from "@/features/save/types";
import { fetchFollowingPlacesPage, getFollowingMakers, getMe, getMeState, getMySuggestions } from "./api";

export function useMe() {
  return useQuery({ queryKey: ["me"], queryFn: getMe, retry: false, staleTime: 5 * 60 * 1000 });
}

// 공개 화면 위의 저장·팔로우 상태를 한 번에 받아 공간·만든 사람별 키에 나눠 넣는다.
// 버튼은 자기 키만 읽으므로 화면이 어떤 id를 그리는지 몰라도 된다.
export function useMeStateLoader(places: number[], makers: number[]) {
  const { data: me } = useMe();
  const queryClient = useQueryClient();
  const { data } = useQuery({
    queryKey: ["me", "state", places, makers],
    queryFn: () => getMeState(places, makers),
    enabled: !!me && places.length + makers.length > 0,
  });

  useEffect(() => {
    if (!data) return;
    for (const id of places) queryClient.setQueryData(saveKey(id), data.saves[id] ?? null);
    for (const id of makers) queryClient.setQueryData(followKey(id), data.follows.includes(id));
    // places·makers는 data가 바뀔 때 함께 바뀐다(같은 쿼리 키).
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data, queryClient]);
}

export function useSavesPages(status: SaveStatus) {
  const { data: me } = useMe();
  return useInfiniteQuery({
    queryKey: ["me", "saves", status],
    queryFn: ({ pageParam }) => fetchSavesPage(status, pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: (last) => last.nextCursor,
    enabled: !!me,
  });
}

export function useFollowingPlacesPages() {
  const { data: me } = useMe();
  return useInfiniteQuery({
    queryKey: ["me", "following", "places"],
    queryFn: ({ pageParam }) => fetchFollowingPlacesPage(pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: (last) => last.nextCursor,
    enabled: !!me,
    // 탭을 열 때마다 새로 받고, 보는 동안에는 다시 받지 않는다. 열자마자 본 것으로 남기므로
    // 다시 받으면 "새로운"이 사라진다(화면을 떠날 때까지 남겨야 한다).
    staleTime: Infinity,
    gcTime: 0,
  });
}

export function useFollowingMakers() {
  const { data: me } = useMe();
  const queryClient = useQueryClient();
  // 팔로잉 탭을 열 때마다 새로 받는다(떠날 때 지운다). 남겨 두면 다른 화면에서 해제한 사람이 다음에 열 때 잠깐 보인다.
  const query = useQuery({
    queryKey: ["me", "following", "makers"],
    queryFn: getFollowingMakers,
    enabled: !!me,
    gcTime: 0,
  });
  const { data } = query;

  // 이 목록은 모두 팔로우 중이다. 버튼이 같은 키를 읽게 채운다.
  useEffect(() => {
    for (const maker of data?.items ?? []) queryClient.setQueryData(followKey(maker.id), true);
  }, [data, queryClient]);
  return query;
}

export function useMySuggestions() {
  const { data: me } = useMe();
  return useQuery({ queryKey: ["me", "suggestions"], queryFn: getMySuggestions, enabled: !!me });
}
