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
import { settled } from "@/features/save/queries";
import { isUnauthorized } from "@/shared/api/client";
import { track } from "@/shared/lib/track";
import { useToast } from "@/shared/ui/Toast";
import { deleteFollow, putFollow } from "./api";

// 만든 사람 하나의 팔로우 여부. 채우는 쪽은 useMeStateLoader와 팔로잉 탭. undefined는 아직 모름.
export const followKey = (makerId: number) => ["me", "follow", makerId] as const;

export function useFollowing(makerId: number): boolean | undefined {
  return useQuery<boolean>({ queryKey: followKey(makerId), queryFn: skipToken }).data;
}

// 누른 순간의 값(currentSaveStatus와 같은 이유).
export function currentFollowing(queryClient: QueryClient, makerId: number): boolean {
  return queryClient.getQueryData<boolean>(followKey(makerId)) ?? false;
}

function followOptions(queryClient: QueryClient, toast: (message: string) => void, makerId: number) {
  return mutationOptions({
    mutationKey: ["me-write", "follow"],
    // 같은 사람에 대한 요청은 순서대로 보낸다(저장과 같은 이유).
    scope: { id: `follow-${makerId}` },
    mutationFn: (following: boolean) => (following ? putFollow(makerId) : deleteFollow(makerId)),
    onMutate: async (following) => {
      // 저장과 같은 이유로 기다리기 전에 바꾼다.
      const previous = currentFollowing(queryClient, makerId);
      queryClient.setQueryData(followKey(makerId), following);
      await queryClient.cancelQueries({ queryKey: ["me", "state"] });
      return { previous };
    },
    onError: (error, _following, context) => {
      queryClient.setQueryData(followKey(makerId), context?.previous ?? false);
      if (!isUnauthorized(error)) toast("팔로우하지 못했어요. 다시 시도해 주세요");
    },
    onSuccess: (_, following) => {
      if (following) track("follow");
    },
    // 탭 바의 점(hasNewFromFollowing)이 팔로우에 따라 바뀐다.
    // 팔로잉 탭의 목록은 보는 중에 줄이 사라지지 않게 다시 받지 않는다. 다음에 열 때 새로 받는다.
    onSettled: () => settled(queryClient, "follow", () => queryClient.invalidateQueries({ queryKey: ["me"], exact: true })),
  });
}

export function useFollowMutation(makerId: number) {
  const queryClient = useQueryClient();
  const toast = useToast();
  return useMutation(followOptions(queryClient, toast, makerId));
}

// 로그인 뒤 이어서 할 팔로우. resumeSave와 같은 이유.
export function resumeFollow(queryClient: QueryClient, toast: (message: string) => void, makerId: number) {
  new MutationObserver(queryClient, followOptions(queryClient, toast, makerId)).mutate(true).catch(() => {});
}
