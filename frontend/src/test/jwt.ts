import type { Role } from "../api/types";

function base64Url(value: unknown) {
  return btoa(JSON.stringify(value)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

export function createTestToken({
  userId = 1,
  email = "user@example.com",
  role = "ROLE_USER",
  exp = Math.floor(Date.now() / 1000) + 3600
}: {
  userId?: number;
  email?: string;
  role?: Role;
  exp?: number;
} = {}) {
  return `${base64Url({ alg: "none", typ: "JWT" })}.${base64Url({ userId, sub: email, role, exp })}.signature`;
}
