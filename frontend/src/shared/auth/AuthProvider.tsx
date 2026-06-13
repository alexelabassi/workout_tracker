import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import {
  apiPost,
  registerRefreshHandler,
  setAccessToken,
} from "../api/client";

export type Role = "USER" | "COACH" | "ADMIN";

export interface User {
  id: string;
  email: string;
  displayName: string | null;
  role: Role;
}

interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

type Status = "loading" | "authenticated" | "unauthenticated";

interface AuthContextValue {
  user: User | null;
  status: Status;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [status, setStatus] = useState<Status>("loading");

  const applySession = useCallback((data: AuthResponse) => {
    setAccessToken(data.accessToken);
    setUser(data.user);
    setStatus("authenticated");
  }, []);

  const clearSession = useCallback(() => {
    setAccessToken(null);
    setUser(null);
    setStatus("unauthenticated");
  }, []);

  // Exchanges the HttpOnly refresh cookie for a new access token. Used both to bootstrap a
  // session on load and to recover from an expired access token mid-session.
  const performRefresh = useCallback(async (): Promise<boolean> => {
    try {
      const data = await apiPost<AuthResponse>("/auth/refresh", undefined, {
        auth: false,
        retryOnUnauthorized: false,
      });
      applySession(data);
      return true;
    } catch {
      clearSession();
      return false;
    }
  }, [applySession, clearSession]);

  useEffect(() => {
    registerRefreshHandler(performRefresh);
    return () => registerRefreshHandler(null);
  }, [performRefresh]);

  useEffect(() => {
    let active = true;
    void performRefresh().then((ok) => {
      if (active && !ok) {
        setStatus("unauthenticated");
      }
    });
    return () => {
      active = false;
    };
  }, [performRefresh]);

  const login = useCallback(
    async (email: string, password: string) => {
      const data = await apiPost<AuthResponse>(
        "/auth/login",
        { email, password },
        { auth: false, retryOnUnauthorized: false },
      );
      applySession(data);
    },
    [applySession],
  );

  const register = useCallback(
    async (email: string, password: string, displayName: string) => {
      const data = await apiPost<AuthResponse>(
        "/auth/register",
        { email, password, displayName },
        { auth: false, retryOnUnauthorized: false },
      );
      applySession(data);
    },
    [applySession],
  );

  const logout = useCallback(async () => {
    try {
      await apiPost("/auth/logout", undefined, { auth: false, retryOnUnauthorized: false });
    } finally {
      clearSession();
    }
  }, [clearSession]);

  const value = useMemo<AuthContextValue>(
    () => ({ user, status, login, register, logout }),
    [user, status, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
