"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import {
  Badge,
  Button,
  Group,
  Loader,
  Paper,
  ScrollArea,
  Stack,
  Table,
  Tabs,
  Text,
  TextInput,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { notifications } from "@mantine/notifications";
import AppButton from "@/src/shared/components/AppButton";

type CreateUserPayload = {
  email: string;
  username: string;
  firstName: string;
  lastName: string;
  title?: string;
  pictureUrl?: string;
};

type CreateUserResponse = {
  userId: string;
  temporaryPassword: string;
};

type AdminUserListResponse = {
  id: string;
  email?: string;
  username?: string;
  firstName?: string;
  lastName?: string;
  enabled: boolean;
  provisioned: boolean;
  deletedAt?: string | null;
  anonymizedAt?: string | null;
  roles?: string[];
}[];

type AdminActiveSessionResponse = {
  sessionId: string;
  userId?: string;
  username?: string;
  ipAddress?: string;
  start?: number;
  lastAccess?: number;
}[];

export default function AdminUserManagement({ keycloakAdminUrl }: { keycloakAdminUrl?: string }) {
  const router = useRouter();
  const [created, setCreated] = useState<CreateUserResponse | null>(null);
  const [createError, setCreateError] = useState<string | null>(null);
  const [listQuery, setListQuery] = useState("");
  const [users, setUsers] = useState<AdminUserListResponse | null>(null);
  const [loadingUsers, setLoadingUsers] = useState(false);
  const [creatingUser, setCreatingUser] = useState(false);
  const [activeSessions, setActiveSessions] = useState<AdminActiveSessionResponse | null>(null);
  const [loadingActiveSessions, setLoadingActiveSessions] = useState(false);
  const [loggingOutSessionId, setLoggingOutSessionId] = useState<string | null>(null);

  const form = useForm<CreateUserPayload>({
    mode: "controlled",
    initialValues: {
      email: "",
      username: "",
      firstName: "",
      lastName: "",
      title: "",
      pictureUrl: "",
    },
    validate: {
      email: (v) => (/^\S+@\S+\.\S+$/.test(v.trim()) ? null : "Valid email required"),
      username: (v) => (v.trim().length >= 3 ? null : "Min 3 characters"),
      firstName: (v) => (v.trim().length > 0 ? null : "Required"),
      lastName: (v) => (v.trim().length > 0 ? null : "Required"),
      pictureUrl: (v) => {
        const value = v?.trim();
        if (!value) return null;
        if (value.length > 2048) return "Max 2048 characters";
        if (!/^https?:\/\//i.test(value)) return "Must start with http:// or https://";
        return null;
      },
      title: (v) => (v && v.length > 255 ? "Max 255 characters" : null),
    },
  });

  const loadUsers = useCallback(async (queryValue: string) => {
    try {
      setLoadingUsers(true);
      const query = queryValue.trim();
      const url = new URL("/api/backend/api/admin/users/directory", window.location.origin);
      if (query) url.searchParams.set("q", query);
      url.searchParams.set("first", "0");
      url.searchParams.set("max", "50");

      const res = await fetch(url.toString(), { cache: "no-store" });
      if (!res.ok) {
        throw new Error(await safeErrorMessage(res));
      }
      const data = (await res.json()) as AdminUserListResponse;
      setUsers(data);
    } catch (e) {
      notifications.show({
        title: "Error",
        message: "Failed to load users: " + (e as Error).message,
        color: "red",
      });
    } finally {
      setLoadingUsers(false);
    }
  }, []);

  const formatEpoch = (value?: number) => {
    if (!value) return "-";
    try {
      return new Date(value).toLocaleString();
    } catch {
      return String(value);
    }
  };

  useEffect(() => {
    void loadUsers("");
  }, [loadUsers]);

  const onSubmit = async (values: CreateUserPayload) => {
    const notificationId = "create-user";
    let slowNotificationShown = false;
    const slowTimer = window.setTimeout(() => {
      slowNotificationShown = true;
      notifications.show({
        id: notificationId,
        loading: true,
        title: "Creating user...",
        message: "Talking to Keycloak. This can take a few seconds.",
        autoClose: false,
        withCloseButton: false,
      });
    }, 1500);

    const abortController = new AbortController();
    const abortTimer = window.setTimeout(() => abortController.abort(), 90000);

    try {
      setCreatingUser(true);
      setCreateError(null);
      setCreated(null);
      const payload: CreateUserPayload = {
        email: values.email.trim(),
        username: values.username.trim(),
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        title: values.title?.trim() || undefined,
        pictureUrl: values.pictureUrl?.trim() || undefined,
      };

      const res = await fetch("/api/backend/api/admin/users", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
        signal: abortController.signal,
      });
      if (!res.ok) {
        const msg = await safeErrorMessage(res);
        throw new Error(msg);
      }
      const data = (await res.json()) as CreateUserResponse;
      setCreated(data);

      if (slowNotificationShown) {
        notifications.update({
          id: notificationId,
          loading: false,
          title: "User created",
          message: "Temporary password generated. Copy it now; it won't be shown again.",
          color: "green",
          autoClose: 3000,
        });
      } else {
        notifications.show({
          title: "User created",
          message: "Temporary password generated. Copy it now; it won't be shown again.",
          color: "green",
        });
      }
      form.reset();

      try {
        window.scrollTo({ top: 0, behavior: "smooth" });
      } catch {
        // ignore
      }
    } catch (e) {
      const msg =
        e instanceof DOMException && e.name === "AbortError"
          ? "Request timed out while waiting for Keycloak. Try again."
          : (e as Error).message;

      setCreateError(msg);

      if (slowNotificationShown) {
        notifications.update({
          id: notificationId,
          loading: false,
          title: "Error",
          message: msg,
          color: "red",
          autoClose: 6000,
        });
      } else {
        notifications.show({ title: "Error", message: msg, color: "red" });
      }
    } finally {
      window.clearTimeout(slowTimer);
      window.clearTimeout(abortTimer);
      setCreatingUser(false);
    }
  };

  const loadActiveSessions = async () => {
    try {
      setLoadingActiveSessions(true);
      const res = await fetch("/api/backend/api/admin/sessions", { cache: "no-store" });
      if (!res.ok) throw new Error(await safeErrorMessage(res));
      const data = (await res.json()) as AdminActiveSessionResponse;
      setActiveSessions(data);
    } catch (e) {
      notifications.show({
        title: "Error",
        message: "Failed to load active sessions: " + (e as Error).message,
        color: "red",
      });
    } finally {
      setLoadingActiveSessions(false);
    }
  };

  const logoutActiveSession = async (sessionId: string) => {
    const id = sessionId.trim();
    if (!id) return;
    try {
      setLoggingOutSessionId(id);
      const res = await fetch(`/api/backend/api/admin/sessions/${encodeURIComponent(id)}`, {
        method: "DELETE",
      });
      if (!res.ok) throw new Error(await safeErrorMessage(res));
      notifications.show({ title: "Done", message: "Session logged out.", color: "green" });
      void loadActiveSessions();
    } catch (e) {
      notifications.show({
        title: "Error",
        message: "Failed to logout session: " + (e as Error).message,
        color: "red",
      });
    } finally {
      setLoggingOutSessionId(null);
    }
  };

  return (
    <Tabs defaultValue="users">
      <Tabs.List mb="lg">
        <Tabs.Tab value="users">User Management</Tabs.Tab>
        <Tabs.Tab value="create">Create User</Tabs.Tab>
        <Tabs.Tab value="sessions">Sessions</Tabs.Tab>
        <Tabs.Tab value="keycloak">Keycloak</Tabs.Tab>
      </Tabs.List>

      <Tabs.Panel value="users">
        <Stack gap="lg">
          <Paper
            p="md"
            radius="md"
            style={{
              background: "rgba(255,255,255,0.03)",
              border: "1px solid rgba(255,255,255,0.08)",
              maxWidth: 900,
            }}
          >
            <Stack gap={6}>
              <Text fw={700} style={{ color: "#e2e8f0" }}>
                What these actions do
              </Text>
              <Text size="sm" c="dimmed">
                <b>Provision</b>: Creates the ISTP database user record for an existing Keycloak
                account (idempotent). Required for profile/role management in the app.
              </Text>
              <Text size="sm" c="dimmed">
                <b>Disable</b>: Temporarily disables the account (Keycloak login blocked) and marks
                the user as disabled in ISTP. Can be reverted with <b>Restore</b>.
              </Text>
              <Text size="sm" c="dimmed">
                <b>Soft delete</b>: Permanently anonymizes email/username and disables the account
                to free identifiers for reuse. This cannot be undone.
              </Text>
            </Stack>
          </Paper>

          <Paper
            p="lg"
            radius="md"
            style={{
              background: "rgba(255,255,255,0.03)",
              border: "1px solid rgba(255,255,255,0.08)",
            }}
          >
            <Stack gap="md">
              <Group justify="space-between" align="flex-end">
                <Text fw={700} style={{ color: "#e2e8f0" }}>
                  Users
                </Text>
                {loadingUsers ? <Loader size="sm" /> : null}
              </Group>

              <Group align="flex-end" grow>
                <TextInput
                  label="Search"
                  placeholder="Name, email, username..."
                  value={listQuery}
                  onChange={(e) => setListQuery(e.currentTarget.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" && !loadingUsers) {
                      e.preventDefault();
                      void loadUsers(listQuery);
                    }
                  }}
                  disabled={loadingUsers}
                />
                <AppButton onClick={() => void loadUsers(listQuery)} loading={loadingUsers}>
                  Search
                </AppButton>
              </Group>

              <ScrollArea h="max(560px, calc(100vh - 340px))">
                <Table highlightOnHover withTableBorder>
                  <Table.Thead>
                    <Table.Tr>
                      <Table.Th>Name</Table.Th>
                      <Table.Th>Email</Table.Th>
                      <Table.Th>Roles</Table.Th>
                      <Table.Th style={{ width: 120 }}>Action</Table.Th>
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {(users ?? []).map((u) => {
                      const displayName =
                        [u.firstName, u.lastName].filter(Boolean).join(" ").trim() ||
                        u.username ||
                        u.email ||
                        u.id;
                      const profileHref = `/dashboard/admin/users/${encodeURIComponent(u.id)}`;
                      return (
                        <Table.Tr
                          key={u.id}
                          onClick={() => router.push(profileHref)}
                          style={{ cursor: "pointer" }}
                        >
                          <Table.Td>
                            <Group gap={8}>
                              <Link
                                href={profileHref}
                                style={{ textDecoration: "none" }}
                                onClick={(e) => e.stopPropagation()}
                              >
                                <Text size="sm" style={{ color: "#e2e8f0" }}>
                                  {displayName}
                                </Text>
                              </Link>
                              {u.anonymizedAt ? (
                                <Badge color="red" variant="light">
                                  soft deleted
                                </Badge>
                              ) : !u.enabled ? (
                                <Badge color="red" variant="light">
                                  disabled
                                </Badge>
                              ) : null}
                              {u.provisioned ? (
                                <Badge color="green" variant="light">
                                  provisioned
                                </Badge>
                              ) : (
                                <Badge color="yellow" variant="light">
                                  not provisioned
                                </Badge>
                              )}
                            </Group>
                          </Table.Td>
                          <Table.Td>{u.email ?? "-"}</Table.Td>
                          <Table.Td>
                            <Group gap={6}>
                              {(u.roles ?? []).slice(0, 3).map((r) => (
                                <Badge key={r} variant="light" color="gray">
                                  {r}
                                </Badge>
                              ))}
                            </Group>
                          </Table.Td>
                          <Table.Td>
                            <Button
                              size="xs"
                              variant="subtle"
                              component={Link}
                              href={profileHref}
                              onClick={(e) => e.stopPropagation()}
                            >
                              Open
                            </Button>
                          </Table.Td>
                        </Table.Tr>
                      );
                    })}
                    {users && users.length === 0 ? (
                      <Table.Tr>
                        <Table.Td colSpan={4}>
                          <Text size="sm" c="dimmed">
                            No users returned from Keycloak.
                          </Text>
                        </Table.Td>
                      </Table.Tr>
                    ) : null}
                  </Table.Tbody>
                </Table>
              </ScrollArea>
            </Stack>
          </Paper>
        </Stack>
      </Tabs.Panel>

      <Tabs.Panel value="create">
        <Stack gap="lg" style={{ maxWidth: 720, marginInline: "auto", width: "100%" }}>
          {creatingUser ? (
            <Paper
              p="md"
              radius="md"
              style={{
                background: "rgba(59, 130, 246, 0.08)",
                border: "1px solid rgba(59, 130, 246, 0.25)",
                maxWidth: 720,
              }}
            >
              <Group gap="sm">
                <Loader size="sm" />
                <Text size="sm" style={{ color: "#e2e8f0" }}>
                  Creating user in Keycloak...
                </Text>
              </Group>
            </Paper>
          ) : null}

          {createError ? (
            <Paper
              p="md"
              radius="md"
              style={{
                background: "rgba(239, 68, 68, 0.08)",
                border: "1px solid rgba(239, 68, 68, 0.25)",
                maxWidth: 720,
              }}
            >
              <Stack gap={6}>
                <Text fw={700} style={{ color: "#e2e8f0" }}>
                  Create failed
                </Text>
                <Text size="sm" c="dimmed">
                  {createError}
                </Text>
              </Stack>
            </Paper>
          ) : null}

          {created ? (
            <Paper
              p="md"
              radius="md"
              style={{
                background: "rgba(16, 185, 129, 0.08)",
                border: "1px solid rgba(16, 185, 129, 0.25)",
              }}
            >
              <Stack gap={6}>
                <Text fw={700} style={{ color: "#e2e8f0" }}>
                  Temporary password (copy now)
                </Text>
                <Text size="sm" style={{ fontFamily: "monospace", color: "#e2e8f0" }}>
                  {created.temporaryPassword}
                </Text>
                <Group gap="sm">
                  <Button
                    size="xs"
                    radius="md"
                    variant="subtle"
                    onClick={() => void navigator.clipboard.writeText(created.temporaryPassword)}
                  >
                    Copy password
                  </Button>
                  <Button
                    size="xs"
                    radius="md"
                    variant="subtle"
                    onClick={() => void navigator.clipboard.writeText(created.userId)}
                  >
                    Copy userId
                  </Button>
                </Group>
                <Text size="sm" c="dimmed">
                  User ID: {created.userId}
                </Text>
              </Stack>
            </Paper>
          ) : null}

          <form onSubmit={form.onSubmit((v) => void onSubmit(v))}>
            <Stack gap="md" style={{ maxWidth: 720 }}>
              <Text fw={700} style={{ color: "#e2e8f0" }}>
                Create user (admin-managed)
              </Text>
              <Group grow>
                <TextInput
                  label="Email"
                  required
                  placeholder="user@example.com"
                  disabled={creatingUser}
                  {...form.getInputProps("email")}
                />
                <TextInput
                  label="Username"
                  required
                  placeholder="username"
                  disabled={creatingUser}
                  {...form.getInputProps("username")}
                />
              </Group>

              <Group grow>
                <TextInput
                  label="First name"
                  required
                  disabled={creatingUser}
                  {...form.getInputProps("firstName")}
                />
                <TextInput
                  label="Last name"
                  required
                  disabled={creatingUser}
                  {...form.getInputProps("lastName")}
                />
              </Group>

              <Group grow>
                <TextInput label="Title" disabled={creatingUser} {...form.getInputProps("title")} />
                <TextInput
                  label="Picture URL"
                  placeholder="https://..."
                  disabled={creatingUser}
                  {...form.getInputProps("pictureUrl")}
                />
              </Group>

              <Group justify="flex-end" mt="xs">
                <AppButton type="submit" loading={creatingUser}>
                  Create user
                </AppButton>
              </Group>
            </Stack>
          </form>
        </Stack>
      </Tabs.Panel>

      <Tabs.Panel value="sessions">
        <Stack gap="lg">
          <Paper
            p="lg"
            radius="md"
            style={{
              background: "rgba(255,255,255,0.03)",
              border: "1px solid rgba(255,255,255,0.08)",
            }}
          >
            <Stack gap="md">
              <Group justify="space-between" align="flex-end">
                <div>
                  <Text fw={700} style={{ color: "#e2e8f0" }}>
                    Active Sessions (ISTP)
                  </Text>
                  <Text c="dimmed" size="sm">
                    Shows Keycloak sessions for the configured app client.
                  </Text>
                </div>
                <AppButton
                  onClick={() => void loadActiveSessions()}
                  loading={loadingActiveSessions}
                >
                  Refresh
                </AppButton>
              </Group>

              <ScrollArea h="max(560px, calc(100vh - 340px))">
                <Table highlightOnHover withTableBorder>
                  <Table.Thead>
                    <Table.Tr>
                      <Table.Th>User</Table.Th>
                      <Table.Th>IP</Table.Th>
                      <Table.Th>Last access</Table.Th>
                      <Table.Th style={{ width: 120 }}>Action</Table.Th>
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {(activeSessions ?? []).map((s) => (
                      <Table.Tr key={s.sessionId}>
                        <Table.Td>
                          <Text size="sm" style={{ color: "#e2e8f0" }}>
                            {s.username || s.userId || "-"}
                          </Text>
                          <Text size="xs" c="dimmed">
                            {s.userId ?? "-"}
                          </Text>
                        </Table.Td>
                        <Table.Td>
                          <Text size="sm" c="dimmed" style={{ fontFamily: "monospace" }}>
                            {s.ipAddress ?? "-"}
                          </Text>
                        </Table.Td>
                        <Table.Td>
                          <Text size="sm" c="dimmed">
                            {formatEpoch(s.lastAccess)}
                          </Text>
                        </Table.Td>
                        <Table.Td>
                          <Button
                            radius="md"
                            color="red"
                            variant="subtle"
                            onClick={() => void logoutActiveSession(s.sessionId)}
                            loading={loggingOutSessionId === s.sessionId}
                            disabled={loadingActiveSessions || !!loggingOutSessionId}
                          >
                            Logout
                          </Button>
                        </Table.Td>
                      </Table.Tr>
                    ))}
                    {activeSessions && activeSessions.length === 0 ? (
                      <Table.Tr>
                        <Table.Td colSpan={4}>
                          <Text size="sm" c="dimmed">
                            No active sessions.
                          </Text>
                        </Table.Td>
                      </Table.Tr>
                    ) : null}
                    {activeSessions == null ? (
                      <Table.Tr>
                        <Table.Td colSpan={4}>
                          <Text size="sm" c="dimmed">
                            Click Refresh to load sessions.
                          </Text>
                        </Table.Td>
                      </Table.Tr>
                    ) : null}
                  </Table.Tbody>
                </Table>
              </ScrollArea>
            </Stack>
          </Paper>
        </Stack>
      </Tabs.Panel>

      <Tabs.Panel value="keycloak">
        <Paper
          p="lg"
          radius="md"
          style={{
            background: "rgba(255,255,255,0.03)",
            border: "1px solid rgba(255,255,255,0.08)",
            maxWidth: 720,
            marginInline: "auto",
          }}
        >
          <Stack gap="sm">
            <Text fw={700} style={{ color: "#e2e8f0" }}>
              Keycloak Management
            </Text>
            <Text c="dimmed" size="sm">
              Normally you manage users through this app. The Keycloak admin console is useful for
              realm/client setup and troubleshooting.
            </Text>
            <Paper
              p="sm"
              radius="md"
              style={{
                background: "rgba(239, 68, 68, 0.08)",
                border: "1px solid rgba(239, 68, 68, 0.25)",
              }}
            >
              <Stack gap={4}>
                <Group gap="xs">
                  <Badge color="red" variant="light">
                    Sensitive
                  </Badge>
                  <Text size="sm" style={{ color: "#e2e8f0" }}>
                    Keycloak changes can break authentication for everyone.
                  </Text>
                </Group>
                <Text size="sm" c="dimmed">
                  Only project owners should use the Keycloak admin console. Having the app role
                  `ROLE_ADMINISTRATOR` is not enough — you also need the Keycloak admin group /
                  admin permissions.
                </Text>
              </Stack>
            </Paper>
            {keycloakAdminUrl ? (
              <Button
                component="a"
                href={keycloakAdminUrl}
                target="_blank"
                rel="noopener noreferrer"
                radius="md"
                variant="subtle"
                color="gray"
              >
                Open Keycloak Admin Console
              </Button>
            ) : (
              <Text c="dimmed" size="sm">
                `KEYCLOAK_ADMIN_URL` is not configured.
              </Text>
            )}
          </Stack>
        </Paper>
      </Tabs.Panel>
    </Tabs>
  );
}

async function safeErrorMessage(res: Response): Promise<string> {
  try {
    const data = (await res.json()) as { error?: string; message?: string };
    return data?.error || data?.message || res.statusText || `HTTP ${res.status}`;
  } catch {
    return res.statusText || `HTTP ${res.status}`;
  }
}
