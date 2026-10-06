// API.md 2절 GET /api/me
export type Me = {
  id: number;
  nickname: string;
  profileImageUrl: string | null;
  role: "user" | "admin";
  hasNewFromFollowing: boolean;
};
