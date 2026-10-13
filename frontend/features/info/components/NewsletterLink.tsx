"use client";

import { track } from "@/shared/lib/track";

const NEWSLETTER_URL = process.env.NEXT_PUBLIC_NEWSLETTER_URL;

export function NewsletterLink({ className }: { className: string }) {
  if (!NEWSLETTER_URL) {
    return null;
  }
  return (
    <a
      href={NEWSLETTER_URL}
      target="_blank"
      rel="noopener noreferrer"
      className={className}
      onClick={() => track("newsletter_subscribe")}
    >
      뉴스레터 구독 <span aria-hidden="true">→</span>
    </a>
  );
}
