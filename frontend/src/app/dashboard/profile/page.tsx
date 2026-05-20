"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Avatar, Group, Stack, Text, TextInput } from "@mantine/core";
import { useForm } from "@mantine/form";
import { notifications } from "@mantine/notifications";
import { useAsyncAction } from "@/src/shared/hooks/useAsyncAction";
import { useApiClient } from "@/src/shared/lib/api/client";
import { apiErrorText } from "@/src/shared/lib/api";
import { httpUrlValidator } from "@/src/shared/lib/validation";
import PageHeader from "@/src/shared/components/PageHeader";
import AppButton from "@/src/shared/components/AppButton";
import { SurfaceCard } from "@/src/shared/components/SurfaceCard";

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
  const client = useApiClient();
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
        if (value.trim().length > 2048) return "Max 2048 characters";
        return httpUrlValidator()(value);
      },
    },
  });
  const formRef = useRef(form);
  formRef.current = form;

  const loadProfile = useCallback(async () => {
    setLoadingProfile(true);
    try {
      setStatusMessage(null);
      const { data, error } = await client.GET("/api/v1/users/me/profile");
      if (error || !data) {
        throw new Error(apiErrorText(error) ?? "Could not load profile");
      }
      setProfile(data as UserProfile);
      formRef.current.setValues({
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
  }, [client]);

  useEffect(() => {
    void loadProfile();
  }, [loadProfile]);

  const submitAction = useAsyncAction(
    async (values: UpdateProfilePayload) => {
      setStatusMessage(null);
      const payload: UpdateProfilePayload = {
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        title: values.title?.trim() || undefined,
        pictureUrl: values.pictureUrl?.trim() || undefined,
      };

      const { data, error } = await client.PUT("/api/v1/users/me/profile", { body: payload });
      if (error || !data) {
        throw new Error(apiErrorText(error) ?? "Could not update profile");
      }
      return data as UserProfile;
    },
    {
      id: "user-profile-save",
      successTitle: "Saved",
      successMessage: "Your profile has been updated.",
      errorTitle: "Error",
      errorMessage: (e) => `Failed to update profile: ${e.message}`,
      onSuccess: (updated) => {
        setProfile(updated);
        setStatusMessage({ kind: "success", text: "Profile saved." });
        router.refresh();
      },
      onError: (e) => {
        setStatusMessage({
          kind: "error",
          text: `Failed to update profile: ${e.message}`,
        });
      },
    }
  );

  const displayName =
    profile?.name ||
    [form.values.firstName, form.values.lastName].filter((s) => s.trim().length > 0).join(" ") ||
    "Profile";

  return (
    <Stack p="xl" gap="xl" style={{ maxWidth: 720 }}>
      <PageHeader title="Profile" subtitle="Update your name, title, and profile picture URL." />

      <SurfaceCard variant="default" padding="1.5rem">
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

        <form onSubmit={form.onSubmit((v) => void submitAction.run(v))}>
          <Stack gap="md" mt="lg">
            <Group grow>
              <TextInput
                label="First name"
                required
                placeholder="First name"
                disabled={loadingProfile || submitAction.loading}
                {...form.getInputProps("firstName")}
              />
              <TextInput
                label="Last name"
                required
                placeholder="Last name"
                disabled={loadingProfile || submitAction.loading}
                {...form.getInputProps("lastName")}
              />
            </Group>

            <TextInput
              label="Title"
              placeholder="e.g. Dr., Student, ..."
              disabled={loadingProfile || submitAction.loading}
              {...form.getInputProps("title")}
            />

            <TextInput
              label="Profile picture URL"
              placeholder="https://..."
              disabled={loadingProfile || submitAction.loading}
              {...form.getInputProps("pictureUrl")}
            />

            {statusMessage ? (
              <Text size="sm" c={statusMessage.kind === "error" ? "red" : "green"}>
                {statusMessage.text}
              </Text>
            ) : null}

            <Group justify="flex-end" mt="xs">
              <AppButton type="submit" loading={submitAction.loading} disabled={loadingProfile}>
                Save changes
              </AppButton>
            </Group>
          </Stack>
        </form>
      </SurfaceCard>
    </Stack>
  );
}
