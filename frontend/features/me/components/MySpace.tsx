"use client";

import Link from "next/link";
import { useState } from "react";
import { useLoginSheet } from "@/features/auth/components/LoginProvider";
import { TopTabs } from "@/shared/ui/TopTabs";
import { useMe } from "../queries";
import { AccountSheet } from "./AccountSheet";
import { FollowingTab } from "./FollowingTab";
import { MySuggestionsLink } from "./MySuggestionList";
import { SavedTab } from "./SavedTab";
import form from "@/shared/ui/Form.module.css";
import styles from "./MySpace.module.css";

export type MyTab = "want" | "been" | "following";

export function MySpace({ tab }: { tab: MyTab }) {
  const { data: me } = useMe();
  const { openLogin } = useLoginSheet();
  const [accountOpen, setAccountOpen] = useState(false);

  return (
    <>
      <header className={styles.head}>
        <div className={styles.headRow}>
          {/* 닉네임이 오기 전에도 머리 높이가 바뀌지 않게 자리를 둔다. */}
          <p className={`label-sm ${styles.nickname}`}>{me?.nickname ?? " "}</p>
          {me && (
            <button type="button" className={`label-md ${styles.settings}`} onClick={() => setAccountOpen(true)}>
              설정
            </button>
          )}
        </div>
        <h1 className={`display-lg ${styles.title}`}>내 공간</h1>
        {me && (
          <div className={`label-md ${styles.headLinks}`}>
            <MySuggestionsLink />
            <Link href="/suggest" className={styles.headLink}>
              공간 소개하기 →
            </Link>
          </div>
        )}
      </header>

      {me === null && (
        <div className={styles.empty}>
          <p className={`body-md ${styles.emptyText}`}>로그인하면 저장한 공간과 팔로우한 사람을 여기서 볼 수 있어요</p>
          <button type="button" className={`label-md ${form.secondary} ${styles.loginButton}`} onClick={() => openLogin({ action: { action: "me" } })}>
            로그인
          </button>
        </div>
      )}

      {me && (
        <>
          <TopTabs
            items={[
              { href: "/me", label: "가고 싶어요", active: tab === "want" },
              { href: "/me?tab=been", label: "다녀왔어요", active: tab === "been" },
              { href: "/me?tab=following", label: "팔로잉", active: tab === "following" },
            ]}
          />
          {tab === "following" ? <FollowingTab /> : <SavedTab key={tab} status={tab} />}
          <AccountSheet open={accountOpen} me={me} onClose={() => setAccountOpen(false)} />
        </>
      )}
    </>
  );
}
