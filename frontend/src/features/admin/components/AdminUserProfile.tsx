"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import {
  Badge,
  Button,
  Checkbox,
  Divider,
  Group,
  Loader,
  PasswordInput,
  Paper,
  Radio,
  RadioGroup,
  Stack,
  Text,
  TextInput,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { notifications } from "@mantine/notifications";

type AdminUserDetailResponse = {
  id: string;
  name?: string;
  email?: string;
  username?: string;
  firstName?: string;
  lastName?: string;
  title?: string;
  picture?: string;
  roles?: string[];
  deletedAt?: string | null;
  anonymizedAt?: string | null;
  provisioned?: boolean;
  keycloak?: {
    enabled?: boolean;
    emailVerified?: boolean;
  } | null;
};

const ALL_APP_ROLES = ["ROLE_STUDENT", "ROLE_INSTRUCTOR", "ROLE_ADMINISTRATOR"] as const;

export default function AdminUserProfile({ userId }: { userId: string }) {
  const [loading, setLoading] = useState(true);
  const [user, setUser] = useState<AdminUserDetailResponse | null>(null);
  const [savingProfile, setSavingProfile] = useState(false);
  const [savingRoles, setSavingRoles] = useState(false);
  const [sendingResetEmail, setSendingResetEmail] = useState(false);
  const [settingPassword, setSettingPassword] = useState(false);
  const [provisioningUser, setProvisioningUser] = useState(false);
  const [disablingUser, setDisablingUser] = useState(false);
  const [restoringUser, setRestoringUser] = useState(false);
  const [softDeletingUser, setSoftDeletingUser] = useState(false);
  const [passwordTemporary, setPasswordTemporary] = useState(true);
  const [passwordSuccess, setPasswordSuccess] = useState<string | null>(null);

  const roles = useMemo(
    () => (user?.roles ?? []).filter((r) => ALL_APP_ROLES.includes(r as never)),
    [user]
  );

  const profileForm = useForm<{
    email: string;
    username: string;
    firstName: string;
    lastName: string;
    title: string;
    pictureUrl: string;
  }>({
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
      firstName: (v) => (v.trim().length > 0 ? null : "Required"),
      lastName: (v) => (v.trim().length > 0 ? null : "Required"),
      title: (v) => (v && v.length > 255 ? "Max 255 characters" : null),
      pictureUrl: (v) => {
        const value = v.trim();
        if (!value) return null; // empty means "remove"
        if (value.length > 2048) return "Max 2048 characters";
        if (!/^https?:\/\//i.test(value)) return "Must start with http:// or https://";
        return null;
      },
    },
  });

  const rolesForm = useForm<{ role: string }>({
    mode: "controlled",
    initialValues: { role: "ROLE_STUDENT" },
  });

  const passwordForm = useForm<{ password: string }>({
    mode: "controlled",
    initialValues: { password: "" },
    validate: {
      password: (v) => (v.trim().length > 0 ? null : "Required"),
    },
  });

  const load = async () => {
    try {
      setLoading(true);
      const res = await fetch(`/api/backend/api/admin/users/${encodeURIComponent(userId)}`, {
        cache: "no-store",
      });
      if (!res.ok) throw new Error(await safeErrorMessage(res));
      const data = (await res.json()) as AdminUserDetailResponse;
      setUser(data);

      profileForm.setValues({
        email: data.email ?? "",
        username: data.username ?? "",
        firstName: data.firstName ?? "",
        lastName: data.lastName ?? "",
        title: data.title ?? "",
        pictureUrl: data.picture ?? "",
      });

      const normalizedRoles = (data.roles ?? []).filter((r) => ALL_APP_ROLES.includes(r as never));
      const preferred =
        normalizedRoles.find((r) => r === "ROLE_ADMINISTRATOR") ??
        normalizedRoles.find((r) => r === "ROLE_INSTRUCTOR") ??
        normalizedRoles.find((r) => r === "ROLE_STUDENT") ??
        "ROLE_STUDENT";
      rolesForm.setValues({ role: preferred });
    } catch (e) {
      notifications.show({
        title: "Error",
        message: "Failed to load user: " + (e as Error).message,
        color: "red",
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userId]);

  const saveProfile = async () => {
    const valid = profileForm.validate();
    if (valid.hasErrors) return;
    if (user?.anonymizedAt) {
      notifications.show({
        title: "Soft-deleted",
        message: "This user is soft-deleted and cannot be edited.",
        color: "yellow",
      });
      return;
    }
    if (!user?.provisioned) {
      notifications.show({
        title: "Not provisioned",
        message: "Provision the user first (DB row missing).",
        color: "yellow",
      });
      return;
    }
    try {
      setSavingProfile(true);

      const v = profileForm.getValues();
      const payload: Record<string, unknown> = {
        firstName: v.firstName.trim(),
        lastName: v.lastName.trim(),
        title: v.title.trim() || undefined,
      };
      const picture = v.pictureUrl.trim();
      if (picture) payload.pictureUrl = picture;

      const res = await fetch(`/api/backend/api/v1/users/${encodeURIComponent(userId)}/profile`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      if (!res.ok) throw new Error(await safeErrorMessage(res));
      notifications.show({ title: "Saved", message: "Profile updated.", color: "green" });
      await load();
    } catch (e) {
      notifications.show({
        title: "Error",
        message: "Failed to update profile: " + (e as Error).message,
        color: "red",
      });
    } finally {
      setSavingProfile(false);
    }
  };

  const saveRoles = async () => {
    if (user?.anonymizedAt) {
      notifications.show({
        title: "Soft-deleted",
        message: "This user is soft-deleted and cannot be edited.",
        color: "yellow",
      });
      return;
    }
    try {
      setSavingRoles(true);
      const payload = { roles: [rolesForm.getValues().role] };
      const res = await fetch(`/api/backend/api/admin/users/${encodeURIComponent(userId)}/roles`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      if (!res.ok) throw new Error(await safeErrorMessage(res));
      notifications.show({ title: "Saved", message: "Roles updated.", color: "green" });
      await load();
    } catch (e) {
      notifications.show({
        title: "Error",
        message: "Failed to update roles: " + (e as Error).message,
        color: "red",
      });
    } finally {
      setSavingRoles(false);
    }
  };

  const sendResetEmail = async () => {
    if (user?.anonymizedAt) {
      notifications.show({
        title: "Soft-deleted",
        message: "This user is soft-deleted. Password reset is not available.",
        color: "yellow",
      });
      return;
    }
    try {
      setSendingResetEmail(true);
      const res = await fetch(
        `/api/backend/api/admin/users/${encodeURIComponent(userId)}/password-reset-email`,
        { method: "POST" }
      );
      if (!res.ok) throw new Error(await safeErrorMessage(res));
      notifications.show({ title: "Sent", message: "Reset email triggered.", color: "green" });
    } catch (e) {
      notifications.show({
        title: "Error",
        message: "Failed to send reset email: " + (e as Error).message,
        color: "red",
      });
    } finally {
      setSendingResetEmail(false);
    }
  };

  const setPassword = async () => {
    if (user?.anonymizedAt) {
      notifications.show({
        title: "Soft-deleted",
        message: "This user is soft-deleted. Password changes are not available.",
        color: "yellow",
      });
      return;
    }
    const valid = passwordForm.validate();
    if (valid.hasErrors) {
      setPasswordSuccess(null);
      return;
    }
    try {
      setSettingPassword(true);
      setPasswordSuccess(null);
      passwordForm.clearFieldError("password");
      const res = await fetch(
        `/api/backend/api/admin/users/${encodeURIComponent(userId)}/password`,
        {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            password: passwordForm.getValues().password.trim(),
            temporary: passwordTemporary,
          }),
        }
      );
      if (!res.ok) {
        const raw = await safeErrorMessage(res);
        passwordForm.setFieldError("password", toFriendlyPasswordError(raw));
        return;
      }
      setPasswordSuccess(
        passwordTemporary
          ? "Temporary password set (user must change on next login)."
          : "Password set."
      );
      passwordForm.reset();
    } catch (e) {
      setPasswordSuccess(null);
      passwordForm.setFieldError("password", (e as Error).message);
    } finally {
      setSettingPassword(false);
    }
  };

  const provision = async () => {
    if (user?.anonymizedAt) {
      notifications.show({
        title: "Soft-deleted",
        message: "This user is soft-deleted and cannot be provisioned.",
        color: "yellow",
      });
      return;
    }
    try {
      setProvisioningUser(true);
      const res = await fetch(
        `/api/backend/api/admin/users/${encodeURIComponent(userId)}/provision`,
        {
          method: "POST",
        }
      );
      if (!res.ok) throw new Error(await safeErrorMessage(res));
      notifications.show({ title: "Done", message: "Provisioned (if needed).", color: "green" });
      await load();
    } catch (e) {
      notifications.show({
        title: "Error",
        message: "Failed to provision: " + (e as Error).message,
        color: "red",
      });
    } finally {
      setProvisioningUser(false);
    }
  };

  const disable = async () => {
    try {
      setDisablingUser(true);
      const res = await fetch(
        `/api/backend/api/admin/users/${encodeURIComponent(userId)}/disable`,
        {
          method: "POST",
        }
      );
      if (!res.ok) throw new Error(await safeErrorMessage(res));
      notifications.show({ title: "Done", message: "User disabled.", color: "green" });
      await load();
    } catch (e) {
      notifications.show({
        title: "Error",
        message: "Failed to disable: " + (e as Error).message,
        color: "red",
      });
    } finally {
      setDisablingUser(false);
    }
  };

  const restore = async () => {
    try {
      setRestoringUser(true);
      const res = await fetch(
        `/api/backend/api/admin/users/${encodeURIComponent(userId)}/restore`,
        {
          method: "POST",
        }
      );
      if (!res.ok) throw new Error(await safeErrorMessage(res));
      notifications.show({ title: "Done", message: "User restored.", color: "green" });
      await load();
    } catch (e) {
      notifications.show({
        title: "Error",
        message: "Failed to restore: " + (e as Error).message,
        color: "red",
      });
    } finally {
      setRestoringUser(false);
    }
  };

  const softDelete = async () => {
    const confirmed = window.confirm(
      "Soft-delete is irreversible.\n\nThis will:\n- anonymize email + username in Keycloak and DB\n- disable the Keycloak account\n- set deletedAt\n\nContinue?"
    );
    if (!confirmed) return;

    try {
      setSoftDeletingUser(true);
      const res = await fetch(
        `/api/backend/api/admin/users/${encodeURIComponent(userId)}/soft-delete`,
        {
          method: "POST",
        }
      );
      if (!res.ok) throw new Error(await safeErrorMessage(res));
      notifications.show({ title: "Done", message: "User soft-deleted.", color: "green" });
      await load();
    } catch (e) {
      notifications.show({
        title: "Error",
        message: "Failed to soft-delete: " + (e as Error).message,
        color: "red",
      });
    } finally {
      setSoftDeletingUser(false);
    }
  };

  if (loading && !user) {
    return (
      <Group>
        <Loader size="sm" />
        <Text c="dimmed">Loading user…</Text>
      </Group>
    );
  }

  const isSoftDeleted = Boolean(user?.anonymizedAt);
  const isDisabled = Boolean(user?.deletedAt) || user?.keycloak?.enabled === false;
  const canRestore = !isSoftDeleted && isDisabled;
  const emailVerifiedBadge =
    user?.keycloak?.emailVerified === true ? (
      <Badge color="green">EMAIL VERIFIED</Badge>
    ) : user?.keycloak?.emailVerified === false ? (
      <Badge color="yellow">EMAIL NOT VERIFIED</Badge>
    ) : null;
  const statusBadge = (() => {
    if (isSoftDeleted) return <Badge color="red">SOFT DELETED</Badge>;
    if (user?.deletedAt) return <Badge color="red">DELETED</Badge>;
    if (user?.keycloak?.enabled === false) return <Badge color="orange">DISABLED</Badge>;
    return null;
  })();

  return (
    <Stack gap="lg">
      <Group justify="space-between">
        <Group gap="sm">
          <Button component={Link} href="/dashboard/admin/users" variant="subtle" radius="md">
            Back
          </Button>
          <Text fw={700} style={{ color: "#e2e8f0" }}>
            User Profile
          </Text>
          {statusBadge}
          {user?.provisioned ? (
            <Badge color="green">PROVISIONED</Badge>
          ) : (
            <Badge color="yellow">NOT PROVISIONED</Badge>
          )}
          {emailVerifiedBadge}
        </Group>
        <Group gap="sm">
          {!isSoftDeleted && !isDisabled ? (
            <Button
              variant="subtle"
              radius="md"
              onClick={() => void provision()}
              loading={provisioningUser}
              disabled={disablingUser || restoringUser || softDeletingUser}
            >
              Provision
            </Button>
          ) : null}

          {!isSoftDeleted && !isDisabled ? (
            <>
              <Button
                color="red"
                variant="filled"
                radius="md"
                onClick={() => void disable()}
                loading={disablingUser}
                disabled={provisioningUser || restoringUser || softDeletingUser}
              >
                Disable
              </Button>
              <Button
                color="red"
                variant="outline"
                radius="md"
                onClick={() => void softDelete()}
                loading={softDeletingUser}
                disabled={provisioningUser || disablingUser || restoringUser}
              >
                Soft delete
              </Button>
            </>
          ) : null}

          {canRestore ? (
            <Button
              color="green"
              variant="filled"
              radius="md"
              onClick={() => void restore()}
              loading={restoringUser}
              disabled={provisioningUser || disablingUser || softDeletingUser}
            >
              Restore
            </Button>
          ) : null}
        </Group>
      </Group>

      <Paper
        p="lg"
        radius="md"
        style={{
          background: "rgba(255,255,255,0.03)",
          border: "1px solid rgba(255,255,255,0.08)",
          maxWidth: 900,
        }}
      >
        <Stack gap="md">
          <Text fw={700} style={{ color: "#e2e8f0" }}>
            Profile
          </Text>
          {isSoftDeleted ? (
            <Text size="sm" c="dimmed">
              This account is soft-deleted (identifiers anonymized). Editing is disabled.
            </Text>
          ) : null}

          <Group grow>
            <TextInput label="Email (read-only)" value={profileForm.values.email} disabled />
            <TextInput label="Username (read-only)" value={profileForm.values.username} disabled />
          </Group>

          <Group grow>
            <TextInput label="First name" required {...profileForm.getInputProps("firstName")} />
            <TextInput label="Last name" required {...profileForm.getInputProps("lastName")} />
          </Group>

          <Group grow>
            <TextInput label="Title" {...profileForm.getInputProps("title")} />
            <TextInput
              label="Profile picture URL"
              placeholder="https://..."
              {...profileForm.getInputProps("pictureUrl")}
            />
          </Group>

          {profileForm.values.pictureUrl.trim() ? (
            <Group gap="md" align="center">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={profileForm.values.pictureUrl.trim()}
                alt="profile"
                style={{
                  width: 56,
                  height: 56,
                  borderRadius: 999,
                  objectFit: "cover",
                  border: "1px solid rgba(255,255,255,0.12)",
                }}
              />
              <Button
                variant="subtle"
                radius="md"
                onClick={() => profileForm.setFieldValue("pictureUrl", "")}
              >
                Remove picture
              </Button>
            </Group>
          ) : null}

          <Group justify="flex-end">
            <Button
              radius="md"
              onClick={() => void saveProfile()}
              loading={savingProfile}
              disabled={isSoftDeleted}
              style={{
                background: "linear-gradient(90deg, #2563eb, #4f46e5)",
                border: "none",
                fontFamily: "var(--font-space-grotesk), sans-serif",
                fontWeight: 600,
                boxShadow: "0 2px 12px rgba(79,70,229,0.3)",
              }}
            >
              Save profile
            </Button>
          </Group>
          {!user?.provisioned ? (
            <Text size="xs" c="dimmed">
              This user has no DB row yet. Click &quot;Provision&quot; first to enable profile
              editing in the app.
            </Text>
          ) : null}
        </Stack>
      </Paper>

      <Paper
        p="lg"
        radius="md"
        style={{
          background: "rgba(255,255,255,0.03)",
          border: "1px solid rgba(255,255,255,0.08)",
          maxWidth: 900,
        }}
      >
        <Stack gap="md">
          <Text fw={700} style={{ color: "#e2e8f0" }}>
            Roles
          </Text>
          <RadioGroup
            value={rolesForm.values.role}
            onChange={(v) => rolesForm.setFieldValue("role", v)}
          >
            <Group gap="md">
              {ALL_APP_ROLES.map((r) => (
                <Radio key={r} value={r} label={r} />
              ))}
            </Group>
          </RadioGroup>
          <Group justify="flex-end">
            <Button
              radius="md"
              onClick={() => void saveRoles()}
              loading={savingRoles}
              disabled={isSoftDeleted}
              style={{
                background: "linear-gradient(90deg, #2563eb, #4f46e5)",
                border: "none",
                fontFamily: "var(--font-space-grotesk), sans-serif",
                fontWeight: 600,
                boxShadow: "0 2px 12px rgba(79,70,229,0.3)",
              }}
            >
              Save roles
            </Button>
          </Group>
          <Text size="xs" c="dimmed">
            Current roles: {(roles.length ? roles : ["(none)"]).join(", ")}
          </Text>
          {roles.length > 1 ? (
            <Text size="xs" c="dimmed">
              Multiple roles detected. Saving will normalize this user to a single role.
            </Text>
          ) : null}
        </Stack>
      </Paper>

      <Paper
        p="lg"
        radius="md"
        style={{
          background: "rgba(255,255,255,0.03)",
          border: "1px solid rgba(255,255,255,0.08)",
          maxWidth: 900,
        }}
      >
        <Stack gap="md">
          <Text fw={700} style={{ color: "#e2e8f0" }}>
            Password
          </Text>

          <Group gap="sm">
            <Button
              radius="md"
              variant="subtle"
              onClick={() => void sendResetEmail()}
              loading={sendingResetEmail}
              disabled={isSoftDeleted}
            >
              Send reset email
            </Button>
          </Group>

          <Divider />

          <Group align="flex-end" grow>
            <PasswordInput
              label="Set password (manual)"
              placeholder="New password"
              value={passwordForm.values.password}
              error={passwordForm.errors.password}
              onChange={(e) => {
                passwordForm.setFieldValue("password", e.currentTarget.value);
                passwordForm.clearFieldError("password");
                if (passwordSuccess) setPasswordSuccess(null);
              }}
            />
            <Checkbox
              label="Temporary"
              checked={passwordTemporary}
              onChange={(e) => setPasswordTemporary(e.currentTarget.checked)}
              style={{ alignSelf: "flex-end", paddingBottom: 6 }}
            />
          </Group>
          <Group justify="flex-end">
            <Button
              radius="md"
              onClick={() => void setPassword()}
              loading={settingPassword}
              disabled={isSoftDeleted}
              style={{
                background: "linear-gradient(90deg, #2563eb, #4f46e5)",
                border: "none",
                fontFamily: "var(--font-space-grotesk), sans-serif",
                fontWeight: 600,
                boxShadow: "0 2px 12px rgba(79,70,229,0.3)",
              }}
            >
              Save password
            </Button>
          </Group>
          {passwordSuccess ? (
            <Text size="sm" style={{ color: "#86efac" }}>
              {passwordSuccess}
            </Text>
          ) : null}
          <Text size="xs" c="dimmed">
            Temporary = user must change password on next login.
          </Text>
        </Stack>
      </Paper>
    </Stack>
  );
}

function toFriendlyPasswordError(raw: string): string {
  const msg = (raw ?? "").trim();
  if (!msg) return "Keycloak rejected the password.";

  const lower = msg.toLowerCase();

  // Keycloak password policy error codes
  if (lower.includes("invalidpasswordminspecialcharsmessage")) {
    return "Password must contain at least 1 special character.";
  }
  if (lower.includes("invalidpasswordminuppercasemessage")) {
    return "Password must contain at least 1 uppercase letter.";
  }
  if (lower.includes("invalidpasswordminlowercasemessage")) {
    return "Password must contain at least 1 lowercase letter.";
  }
  if (lower.includes("invalidpasswordmindigitsmessage")) {
    return "Password must contain at least 1 digit.";
  }
  if (
    lower.includes("invalidpasswordminlengthmessage") ||
    lower.includes("password must be at least")
  ) {
    const m = msg.match(/at least\s+(\d+)\s+characters?/i);
    if (m?.[1]) return `Password must be at least ${m[1]} characters long.`;
    return "Password is too short.";
  }

  // German Keycloak messages (depends on realm locale)
  if (lower.includes("passwort") && lower.includes("mindestens") && lower.includes("zeichen")) {
    const m = msg.match(/mindestens\s+(\d+)\s+zeichen/i);
    if (m?.[1]) return `Password must be at least ${m[1]} characters long.`;
    return "Password is too short.";
  }
  if (lower.includes("sonderzeichen")) {
    const m = msg.match(/mindestens\s+(\d+)\s+sonderzeichen/i);
    const n = m?.[1] ?? "1";
    return `Password must contain at least ${n} special character(s).`;
  }
  if (lower.includes("großbuch") || lower.includes("grossbuch")) {
    const m = msg.match(/mindestens\s+(\d+)\s+/i);
    const n = m?.[1] ?? "1";
    return `Password must contain at least ${n} uppercase letter(s).`;
  }
  if (lower.includes("kleinbuch")) {
    const m = msg.match(/mindestens\s+(\d+)\s+/i);
    const n = m?.[1] ?? "1";
    return `Password must contain at least ${n} lowercase letter(s).`;
  }
  if (lower.includes("zahl") || lower.includes("ziffer")) {
    const m = msg.match(/mindestens\s+(\d+)\s+/i);
    const n = m?.[1] ?? "1";
    return `Password must contain at least ${n} digit(s).`;
  }

  // Generic heuristic parsing
  if (lower.includes("must contain") && lower.includes("special")) {
    const m = msg.match(/at least\s+(\d+)\s+special/i);
    const n = m?.[1] ?? "1";
    return `Password must contain at least ${n} special character(s).`;
  }
  if (lower.includes("must contain") && (lower.includes("upper") || lower.includes("uppercase"))) {
    return "Password must contain at least 1 uppercase letter.";
  }
  if (lower.includes("must contain") && (lower.includes("lower") || lower.includes("lowercase"))) {
    return "Password must contain at least 1 lowercase letter.";
  }
  if (lower.includes("must contain") && (lower.includes("digit") || lower.includes("number"))) {
    return "Password must contain at least 1 digit.";
  }

  return msg;
}

async function safeErrorMessage(res: Response): Promise<string> {
  try {
    const data = (await res.json()) as { error?: string; message?: string };
    return data?.error || data?.message || res.statusText || `HTTP ${res.status}`;
  } catch {
    return res.statusText || `HTTP ${res.status}`;
  }
}
