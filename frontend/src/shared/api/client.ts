const API_BASE = "/api";

export class ApiError extends Error {
  readonly status: number;
  readonly code?: string;

  constructor(status: number, message: string, code?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

// The access token lives only in memory (never localStorage), so a closed tab forgets it
// and a fresh tab re-establishes a session from the HttpOnly refresh cookie.
let accessToken: string | null = null;

// AuthProvider registers a refresh callback so the client can transparently recover from a
// single 401 (expired access token) by rotating the refresh cookie and retrying once.
let refreshHandler: (() => Promise<boolean>) | null = null;

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function registerRefreshHandler(handler: (() => Promise<boolean>) | null): void {
  refreshHandler = handler;
}

interface RequestOptions {
  method?: string;
  body?: unknown;
  /** Attach the bearer access token (default true). */
  auth?: boolean;
  /** Attempt a refresh-and-retry on a 401 (default true). */
  retryOnUnauthorized?: boolean;
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = "GET", body, auth = true, retryOnUnauthorized = true } = options;

  const headers: Record<string, string> = { Accept: "application/json" };
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  if (auth && accessToken) {
    headers.Authorization = `Bearer ${accessToken}`;
  }

  let response: Response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      // Required so the browser stores/sends the HttpOnly refresh cookie.
      credentials: "include",
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, "Network error: backend unreachable");
  }

  if (response.status === 401 && auth && retryOnUnauthorized && refreshHandler) {
    const refreshed = await refreshHandler();
    if (refreshed) {
      return request<T>(path, { ...options, retryOnUnauthorized: false });
    }
  }

  if (!response.ok) {
    throw await toApiError(response);
  }

  return parseBody<T>(response);
}

async function parseBody<T>(response: Response): Promise<T> {
  if (response.status === 204) {
    return undefined as T;
  }
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const data = (await response.json()) as { message?: unknown; error?: unknown };
    const message =
      typeof data.message === "string"
        ? data.message
        : `Request failed with status ${response.status}`;
    const code = typeof data.error === "string" ? data.error : undefined;
    return new ApiError(response.status, message, code);
  } catch {
    return new ApiError(response.status, `Request failed with status ${response.status}`);
  }
}

export function apiGet<T>(path: string, options?: Pick<RequestOptions, "auth">): Promise<T> {
  return request<T>(path, { method: "GET", auth: options?.auth });
}

export function apiPost<T>(
  path: string,
  body?: unknown,
  options?: Pick<RequestOptions, "auth" | "retryOnUnauthorized">,
): Promise<T> {
  return request<T>(path, {
    method: "POST",
    body,
    auth: options?.auth,
    retryOnUnauthorized: options?.retryOnUnauthorized,
  });
}

export function apiPut<T>(path: string, body?: unknown): Promise<T> {
  return request<T>(path, { method: "PUT", body });
}

export function apiDelete<T>(path: string): Promise<T> {
  return request<T>(path, { method: "DELETE" });
}
