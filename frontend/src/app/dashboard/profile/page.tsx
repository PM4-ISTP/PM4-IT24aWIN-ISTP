"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Avatar, Button, Group, Paper, Stack, Text, TextInput, Title } from "@mantine/core";
import { useForm } from "@mantine/form";
import { notifications } from "@mantine/notifications";

type UserProfile = {
  id: string;
  name?: string;
  email?: string;
  username?: string;
  firstName?: string;
  lastName?: string;
  picture?: string;
  title?: string;
};

type UpdateProfilePayload = {
  firstName: string;
  lastName: string;
  title?: string;
  pictureUrl?: string;
};

export const dynamic = "force-dynamic";

export default function ProfilePage() {
  const router = useRouter();
  const [loadingProfile, setLoadingProfile] = useState(true);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [statusMessage, setStatusMessage] = useState<{
    kind: "error" | "success";
    text: string;
  } | null>(null);

  const form = useForm<UpdateProfilePayload>({
    mode: "controlled",
    initialValues: {
      firstName: "",
      lastName: "",
      title: "",
      pictureUrl: "",
    },
    validate: {
      firstName: (value) => (value.trim().length === 0 ? "First name is required" : null),
      lastName: (value) => (value.trim().length === 0 ? "Last name is required" : null),
      title: (value) => (value && value.length > 255 ? "Max 255 characters" : null),
      pictureUrl: (value) => {
        if (!value) return null;
        const trimmed = value.trim();
        if (trimmed.length > 2048) return "Max 2048 characters";
        if (!/^https?:\/\//i.test(trimmed)) return "Must start with http:// or https://";
        return null;
      },
    },
  });

  useEffect(() => {
    const load = async () => {
      try {
        setStatusMessage(null);
        const res = await fetch("/api/backend/api/v1/users/me/profile", { cache: "no-store" });
        if (!res.ok) {
          throw new Error(await safeErrorMessage(res));
        }
        const data = (await res.json()) as UserProfile;
        setProfile(data);
        form.setValues({
          firstName: data.firstName ?? "",
          lastName: data.lastName ?? "",
          title: data.title ?? "",
          pictureUrl: data.picture ?? "",
        });
      } catch (e) {
        setStatusMessage({
          kind: "error",
          text: "Failed to load profile: " + (e as Error).message,
        });
        notifications.show({
          title: "Error",
          message: "Failed to load profile: " + (e as Error).message,
          color: "red",
        });
      } finally {
        setLoadingProfile(false);
      }
    };
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSubmit = async (values: UpdateProfilePayload) => {
    try {
      setStatusMessage(null);
      const payload: UpdateProfilePayload = {
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        title: values.title?.trim() || undefined,
        pictureUrl: values.pictureUrl?.trim() || undefined,
      };

      const res = await fetch("/api/backend/api/v1/users/me/profile", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      if (!res.ok) {
        throw new Error(await safeErrorMessage(res));
      }

      const updated = (await res.json()) as UserProfile;
      setProfile(updated);

      setStatusMessage({ kind: "success", text: "Profile saved." });
      notifications.show({
        title: "Saved",
        message: "Your profile has been updated.",
        color: "green",
      });

      router.refresh();
    } catch (e) {
      setStatusMessage({
        kind: "error",
        text: "Failed to update profile: " + (e as Error).message,
      });
      notifications.show({
        title: "Error",
        message: "Failed to update profile: " + (e as Error).message,
        color: "red",
      });
    }
  };

  const displayName =
    profile?.name ||
    [form.values.firstName, form.values.lastName].filter((s) => s.trim().length > 0).join(" ") ||
    "Profile";

  return (
    <Stack p="xl" gap="xl" style={{ maxWidth: 720 }}>
      <div>
        <Title
          order={1}
          style={{
            color: "#f1f5f9",
            fontFamily: "var(--font-space-grotesk), sans-serif",
            fontWeight: 700,
          }}
        >
          Profile
        </Title>
        <Text style={{ color: "#94a3b8" }} mt={4}>
          Update your name, title, and profile picture URL.
        </Text>
      </div>

      <Paper
        p="lg"
        radius="md"
        style={{
          background: "rgba(255,255,255,0.03)",
          border: "1px solid rgba(255,255,255,0.08)",
        }}
      >
        <Group gap="md" align="center">
          <Avatar radius="xl" size={56} src={form.values.pictureUrl || undefined}>
            {displayName
              .split(" ")
              .map((p) => p[0])
              .join("")
              .slice(0, 2)
              .toUpperCase()}
          </Avatar>
          <div>
            <Text fw={700} style={{ color: "#e2e8f0" }}>
              {displayName}
            </Text>
            <Text size="sm" c="dimmed">
              {profile?.email ?? ""}
            </Text>
          </div>
        </Group>

        <form onSubmit={form.onSubmit((v) => void handleSubmit(v))}>
          <Stack gap="md" mt="lg">
            <Group grow>
              <TextInput
                label="First name"
                required
                placeholder="First name"
                disabled={loadingProfile || form.submitting}
                {...form.getInputProps("firstName")}
              />
              <TextInput
                label="Last name"
                required
                placeholder="Last name"
                disabled={loadingProfile || form.submitting}
                {...form.getInputProps("lastName")}
              />
            </Group>

            <TextInput
              label="Title"
              placeholder="e.g. Dr., Student, ..."
              disabled={loadingProfile || form.submitting}
              {...form.getInputProps("title")}
            />

            <TextInput
              label="Profile picture URL"
              placeholder="https://..."
              disabled={loadingProfile || form.submitting}
              {...form.getInputProps("pictureUrl")}
            />

            {statusMessage ? (
              <Text size="sm" c={statusMessage.kind === "error" ? "red" : "green"}>
                {statusMessage.text}
              </Text>
            ) : null}

            <Group justify="flex-end" mt="xs">
              <Button
                type="submit"
                loading={form.submitting}
                disabled={loadingProfile}
                radius="md"
                style={{
                  background: "linear-gradient(90deg, #2563eb, #4f46e5)",
                  border: "none",
                  fontFamily: "var(--font-space-grotesk), sans-serif",
                  fontWeight: 600,
                  boxShadow: "0 2px 12px rgba(79,70,229,0.3)",
                }}
              >
                Save changes
              </Button>
            </Group>
          </Stack>
        </form>
      </Paper>
    </Stack>
  );
}

async function safeErrorMessage(res: Response): Promise<string> {
  try {
    const data = (await res.json()) as { error?: string };
    return data?.error || res.statusText || `HTTP ${res.status}`;
  } catch {
    return res.statusText || `HTTP ${res.status}`;
  }
}
