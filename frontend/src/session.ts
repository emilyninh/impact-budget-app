import type { AuthResponse, AuthUser } from "./types";

// JWT + user persisted in localStorage so a refresh keeps you logged in.
const TOKEN_KEY = "ib_token";
const USER_KEY = "ib_user";

// Registered by AuthProvider so a 401 anywhere forces a logout.
let onUnauthorized: (() => void) | null = null;

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function getStoredUser(): AuthUser | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AuthUser;
  } catch {
    return null;
  }
}

export function saveSession(auth: AuthResponse): AuthUser {
  const user: AuthUser = {
    userId: auth.userId,
    email: auth.email,
    displayName: auth.displayName,
  };
  localStorage.setItem(TOKEN_KEY, auth.token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
  return user;
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function setUnauthorizedHandler(fn: (() => void) | null) {
  onUnauthorized = fn;
}

export function handleUnauthorized() {
  clearSession();
  if (onUnauthorized) onUnauthorized();
}
