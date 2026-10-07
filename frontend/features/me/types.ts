// GET /api/me
export type Me = {
  id: number;
  nickname: string;
  profileImageUrl: string | null;
  role: "user" | "admin";
  hasNewFromFollowing: boolean;
};
