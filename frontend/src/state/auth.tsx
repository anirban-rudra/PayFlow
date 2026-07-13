import { createContext, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { setAuthToken, setUnauthorizedHandler } from "../api/http";
import type { AuthUser, Role } from "../api/types";

interface AuthContextValue {
  token: string | null;
  user: AuthUser | null;
  isAuthenticated: boolean;
  setSession: (token: string) => void;
  logout: () => void;
}

const STORAGE_KEY = "payflow.session";
const AuthContext = createContext<AuthContextValue | undefined>(undefined);

interface JwtPayload {
  userId?: number | string;
  sub?: string;
  role?: Role;
  exp?: number;
}

function decodeBase64Url(value: string) {
  const base64 = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=");
  return atob(padded);
}

function decodeToken(token: string): AuthUser | null {
  try {
    const payload = JSON.parse(decodeBase64Url(token.split(".")[1] ?? "")) as JwtPayload;
    const userId = Number(payload.userId);
    const email = String(payload.sub ?? "");
    const role = String(payload.role ?? "ROLE_USER") as Role;

    if (payload.exp && payload.exp * 1000 <= Date.now()) {
      return null;
    }

    if (!Number.isFinite(userId) || !email || !["ROLE_USER", "ROLE_ADMIN"].includes(role)) {
      return null;
    }

    return { userId, email, role };
  } catch {
    return null;
  }
}

function readStoredToken() {
  const storedToken = sessionStorage.getItem(STORAGE_KEY);
  if (!storedToken) {
    setAuthToken(null);
    return null;
  }

  if (!decodeToken(storedToken)) {
    sessionStorage.removeItem(STORAGE_KEY);
    setAuthToken(null);
    return null;
  }

  setAuthToken(storedToken);
  return storedToken;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => readStoredToken());
  const user = useMemo(() => (token ? decodeToken(token) : null), [token]);

  useEffect(() => {
    if (token && !user) {
      setToken(null);
    }
  }, [token, user]);

  useEffect(() => {
    setAuthToken(token);
    if (token) {
      sessionStorage.setItem(STORAGE_KEY, token);
    } else {
      sessionStorage.removeItem(STORAGE_KEY);
    }
  }, [token]);

  useEffect(() => {
    setUnauthorizedHandler(() => setToken(null));
    return () => setUnauthorizedHandler(null);
  }, []);

  const value = useMemo<AuthContextValue>(() => {
    return {
      token,
      user,
      isAuthenticated: Boolean(token && user),
      setSession: setToken,
      logout: () => setToken(null)
    };
  }, [token, user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
