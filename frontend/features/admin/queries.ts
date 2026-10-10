"use client";

import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMe } from "@/features/me/queries";
import { ApiError } from "@/shared/api/client";
import {
  addPhoto,
  changePlaceStatus,
  createAdminMaker,
  createAdminPlace,
  deletePhoto,
  editAdminMaker,
  editAdminPlace,
  editPhoto,
  fetchAdminMaker,
  fetchAdminMakersPage,
  fetchAdminPlace,
  fetchAdminPlacesPage,
  reorderPhotos,
  replacePlaceList,
} from "./api";
import { saveOrder } from "./listSchema";
import { orderIds, photoSteps, toFormPhotoKey } from "./photoSchema";
import type { AdminPlace, AdminPlaceStatus, MakerEdit, PhotoBody, PlaceEdit, PlaceLists } from "./types";

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

export type PlaceSave = { edit?: PlaceEdit; lists: Partial<PlaceLists>; photos?: PhotoBody[] };

// 사진 한 장 요청의 칸 오류는 키에 목록 자리가 없다("sourceUrl"). 폼의 그 줄에 보이도록 "photos[i].sourceUrl"로 바꾼다.
// 공개 검증 오류("photos[N].alt")는 서버 순서(serverKeys)의 자리라 폼 줄 번호로 바꾼다.
// 같은 사진을 두 번 등록한 409에는 칸이 없어 그 줄의 업로드 자리(objectKey)에 단다.
function atPhoto(index: number, serverKeys: string[], desired: PhotoBody[]) {
  return (err: unknown): never => {
    if (err instanceof ApiError && err.status === 409) {
      throw new ApiError(err.status, err.message, { [`photos[${index}].objectKey`]: err.message }, err.body);
    }
    if (err instanceof ApiError && err.fieldErrors) {
      const fieldErrors = Object.fromEntries(
        Object.entries(err.fieldErrors).map(([key, message]) => [
          key.startsWith("photos") ? toFormPhotoKey(key, serverKeys, desired) : `photos[${index}].${key}`,
          message,
        ]),
      );
      throw new ApiError(err.status, err.message, fieldErrors, err.body);
    }
    throw err;
  };
}

// 임시저장 한 번: 바뀐 것만 보낸다. 공개 페이지가 있는 공간은 요청마다 공개 검증을 거치므로
// 사진 등록·수정을 먼저(사진이 늘어난 뒤 임베드를 빼도 되게), 사진 삭제·순서는 마지막에(임베드를 더한 뒤 마지막 사진을 빼도 되게) 보낸다.
// 요청마다 응답으로 캐시를 바꿔, 중간에 실패해도 이미 저장된 것은 "저장됨"으로 보인다.
export function useSavePlace(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ edit, lists, photos }: PlaceSave): Promise<AdminPlace | undefined> => {
      let saved: AdminPlace | undefined;
      const keep = (place: AdminPlace) => {
        saved = place;
        queryClient.setQueryData(adminPlaceKey(id), place);
      };
      const current = () => saved ?? (queryClient.getQueryData(adminPlaceKey(id)) as AdminPlace);
      const steps = photos ? photoSteps(photos, current().photos) : null;
      const serverKeys = () => current().photos.map((photo) => photo.objectKey);
      // 등록은 서버가 새 사진을 맨 뒤에 붙인 채 공개 검증한다.
      for (const { index, body } of steps?.add ?? []) {
        keep(await addPhoto(id, body).catch(atPhoto(index, [...serverKeys(), body.objectKey], photos ?? [])));
      }
      for (const { index, id: photoId, body } of steps?.edit ?? []) {
        keep(await editPhoto(id, photoId, body).catch(atPhoto(index, serverKeys(), photos ?? [])));
      }
      for (const step of saveOrder(edit !== undefined, lists)) {
        if (step === "edit") keep(await editAdminPlace(id, edit as PlaceEdit));
        else keep(await replacePlaceList(id, step, lists[step] as PlaceLists[typeof step]));
      }
      if (photos && steps) {
        for (const photoId of steps.remove) await deletePhoto(id, photoId);
        // 삭제는 본문 없는 204라 다시 받아 남은 사진의 id·순서를 안다.
        if (steps.remove.length > 0) keep(await fetchAdminPlace(id));
        const order = orderIds(photos, current().photos);
        if (order) keep(await reorderPhotos(id, order));
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
