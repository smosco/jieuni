"use client";

import { track } from "@/shared/lib/track";
import { useToast } from "@/shared/ui/Toast";
import styles from "./PlaceDetailView.module.css";

// Web Share API가 있으면 시스템 공유, 없으면 링크 복사(PLAN F2).
export function ShareButton({ title }: { title: string }) {
  const toast = useToast();

  async function share() {
    track("share_click");
    const url = window.location.href;
    if (navigator.share) {
      try {
        await navigator.share({ title, url });
      } catch {
        // 사용자가 공유 창을 닫은 경우. 안내하지 않는다.
      }
      return;
    }
    toast((await copyText(url)) ? "링크를 복사했어요" : "링크를 복사하지 못했어요");
  }

  return (
    <button type="button" className={`label-md ${styles.textButton}`} onClick={share}>
      공유
    </button>
  );
}

export function CopyAddressButton({ address }: { address: string }) {
  const toast = useToast();

  async function copy() {
    toast((await copyText(address)) ? "주소를 복사했어요" : "주소를 복사하지 못했어요");
  }

  return (
    <button type="button" className={`label-sm ${styles.copyButton}`} onClick={copy} aria-label="주소 복사">
      복사
    </button>
  );
}

// 클립보드 API는 보안 출처(https·localhost)에서만 있다.
async function copyText(text: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    return false;
  }
}
