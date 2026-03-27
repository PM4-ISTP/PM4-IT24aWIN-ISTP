"use server";

import { fetchBackend } from "@/src/lib/api";

export async function postTest() {
  const res = await fetchBackend("/api/v1/tests", { method: "POST" });

  if (!res.ok) {
    throw new Error(`Backend returned ${res.status}: ${res.statusText}`);
  }

  return (await res.json()) as unknown;
}

// ─── Keycloak Admin actions ───────────────────────────────────────────────────

export interface KeycloakUser {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  enabled: boolean;
  createdTimestamp: number;
}

export interface KeycloakSession {
  id: string;
  userId: string;
  username: string;
  ipAddress: string;
  start: number;
  lastAccess: number;
  clients: Record<string, string>;
}

/** Returns the total number of users registered in the Keycloak realm. */
export async function getKeycloakUserCount(): Promise<number> {
  const res = await fetchBackend("/api/v1/admin/keycloak/users/count");
  if (!res.ok) {
    throw new Error(`Backend returned ${res.status}: ${res.statusText}`);
  }
  return (await res.json()) as number;
}

/** Returns a paginated list of users from the Keycloak realm. */
export async function getKeycloakUsers(
  first = 0,
  max = 50,
): Promise<KeycloakUser[]> {
  const res = await fetchBackend(
    `/api/v1/admin/keycloak/users?first=${first}&max=${max}`,
  );
  if (!res.ok) {
    throw new Error(`Backend returned ${res.status}: ${res.statusText}`);
  }
  return (await res.json()) as KeycloakUser[];
}

/** Returns the total number of active client sessions across the Keycloak realm. */
export async function getKeycloakActiveSessionCount(): Promise<number> {
  const res = await fetchBackend("/api/v1/admin/keycloak/sessions/count");
  if (!res.ok) {
    throw new Error(`Backend returned ${res.status}: ${res.statusText}`);
  }
  return (await res.json()) as number;
}

/** Returns all active sessions for the given Keycloak user ID. */
export async function getKeycloakUserSessions(
  userId: string,
): Promise<KeycloakSession[]> {
  const res = await fetchBackend(`/api/v1/admin/keycloak/sessions/${userId}`);
  if (!res.ok) {
    throw new Error(`Backend returned ${res.status}: ${res.statusText}`);
  }
  return (await res.json()) as KeycloakSession[];
}
