"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { ActionIcon, Button, Group, Loader, Stack, Table, Text, TextInput } from "@mantine/core";
import { notifications } from "@mantine/notifications";
import { IconPlus, IconTrash } from "@tabler/icons-react";
import { readBackendError } from "@/src/shared/lib/readBackendError";
import { toUserFriendlyBackendError } from "@/src/shared/lib/userFriendlyBackendError";
import { slugify } from "@/src/shared/lib/utils";
import { ConfirmModal } from "@/src/shared/components/ConfirmModal";

const MIN_TOPIC_LENGTH = 3;
const MAX_TOPIC_LENGTH = 24;
const TOPIC_PATTERN = /^[A-Za-z][A-Za-z0-9-]*$/;

export default function AdminTopicManagement() {
  const [topics, setTopics] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [newTopic, setNewTopic] = useState("");
  const [saving, setSaving] = useState(false);

  const [deleteOpened, setDeleteOpened] = useState(false);
  const [selected, setSelected] = useState<string | null>(null);

  const sortedTopics = useMemo(() => [...topics].sort((a, b) => a.localeCompare(b)), [topics]);

  const trimmedTopic = newTopic.trim();
  const topicTooShort = trimmedTopic.length > 0 && trimmedTopic.length < MIN_TOPIC_LENGTH;
  const topicTooLong = trimmedTopic.length > MAX_TOPIC_LENGTH;
  const topicInvalidFormat = trimmedTopic.length > 0 && !TOPIC_PATTERN.test(trimmedTopic);

  const inputError = useMemo(() => {
    if (topicTooShort) return `Minimum ${MIN_TOPIC_LENGTH} characters`;
    if (topicTooLong) return `Maximum ${MAX_TOPIC_LENGTH} characters`;
    if (topicInvalidFormat) return "Single word only (letters, numbers, '-')";
    return undefined;
  }, [topicTooShort, topicTooLong, topicInvalidFormat]);

  const loadTopics = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch("/api/backend/api/admin/topics", { method: "GET" });
      if (!res.ok) {
        const msg = toUserFriendlyBackendError(await readBackendError(res));
        setError(`Failed to load topics.${msg ? ` ${msg}` : ""}`);
        return;
      }
      const data = (await res.json()) as string[];
      setTopics(Array.isArray(data) ? data : []);
    } catch {
      setError("Failed to load topics");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadTopics();
  }, [loadTopics]);

  const showToast = useCallback(
    (color: "green" | "red" | "orange", title: string, message: string) => {
      notifications.show({
        id: `admin-topic-management:${color}:${slugify(title)}`,
        color,
        title,
        message,
      });
    },
    []
  );

  useEffect(() => {
    if (!error) return;
    showToast("red", "Error", error);
    setError(null);
  }, [error, showToast]);

  async function addTopic() {
    const value = trimmedTopic;
    if (!value) return;

    if (value.length < MIN_TOPIC_LENGTH) {
      showToast(
        "orange",
        "Topic too short",
        `Topic must be at least ${MIN_TOPIC_LENGTH} characters.`
      );
      return;
    }
    if (value.length > MAX_TOPIC_LENGTH) {
      showToast(
        "orange",
        "Topic too long",
        `Topic must be at most ${MAX_TOPIC_LENGTH} characters.`
      );
      return;
    }
    if (!TOPIC_PATTERN.test(value)) {
      showToast("orange", "Invalid topic", "Use a single word (letters, numbers, '-'). No spaces.");
      return;
    }

    setSaving(true);
    setError(null);
    try {
      const res = await fetch("/api/backend/api/admin/topics", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ value }),
      });
      if (!res.ok) {
        const msg = await readBackendError(res);
        const msgLower = msg?.toLowerCase() ?? "";
        if (msgLower.includes("already exists")) {
          showToast(
            "orange",
            "Topic already exists",
            "This topic already exists. Choose a different name."
          );
          return;
        }
        if (msgLower.includes("at least") || msgLower.includes("between")) {
          showToast(
            "orange",
            "Topic too short",
            `Topic must be at least ${MIN_TOPIC_LENGTH} characters.`
          );
          return;
        }
        if (msgLower.includes("at most")) {
          showToast(
            "orange",
            "Topic too long",
            `Topic must be at most ${MAX_TOPIC_LENGTH} characters.`
          );
          return;
        }
        if (msgLower.includes("single word")) {
          showToast(
            "orange",
            "Invalid topic",
            "Use a single word (letters, numbers, '-'). No spaces."
          );
          return;
        }
        if (msgLower.includes("limit reached")) {
          showToast(
            "orange",
            "Topic limit reached",
            "You reached the maximum number of topics. Please delete unused topics first."
          );
          return;
        }
        showToast(
          "red",
          "Failed to add topic",
          toUserFriendlyBackendError(msg) ?? "Please try again."
        );
        return;
      }
      setNewTopic("");
      showToast("green", "Topic added", "Topic created successfully.");
      await loadTopics();
    } catch {
      setError("Failed to add topic");
    } finally {
      setSaving(false);
    }
  }

  function openDelete(value: string) {
    setSelected(value);
    setDeleteOpened(true);
  }

  async function confirmDelete() {
    if (!selected) return;
    setSaving(true);
    setError(null);
    try {
      const res = await fetch(`/api/backend/api/admin/topics/${encodeURIComponent(selected)}`, {
        method: "DELETE",
      });
      if (!res.ok) {
        const msg = await readBackendError(res);
        showToast(
          "red",
          "Failed to delete topic",
          toUserFriendlyBackendError(msg) ?? "Please try again."
        );
        return;
      }
      setDeleteOpened(false);
      setSelected(null);
      showToast("green", "Topic deleted", "Topic deleted successfully.");
      await loadTopics();
    } catch {
      setError("Failed to delete topic");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-end" wrap="wrap">
        <Group gap="sm" wrap="wrap">
          <TextInput
            label="New topic"
            placeholder="e.g. network"
            value={newTopic}
            onChange={(e) => setNewTopic(e.currentTarget.value)}
            w={320}
            error={inputError}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                e.preventDefault();
                void addTopic();
              }
            }}
          />
          <Button
            leftSection={<IconPlus size={16} />}
            mt={22}
            radius="md"
            onClick={() => void addTopic()}
            loading={saving}
            disabled={!trimmedTopic || topicTooShort || topicTooLong || topicInvalidFormat}
            style={{
              background: "linear-gradient(90deg, #2563eb, #4f46e5)",
              border: "none",
              fontFamily: "var(--font-space-grotesk), sans-serif",
              fontWeight: 600,
              boxShadow: "0 2px 12px rgba(79,70,229,0.3)",
            }}
          >
            Add
          </Button>
        </Group>

        {loading && (
          <Group gap="xs">
            <Loader size="sm" />
            <Text size="sm" c="dimmed">
              Loading
            </Text>
          </Group>
        )}
      </Group>

      <Text size="xs" c="dimmed">
        Topics are short tags used for filtering. Keep them short (max {MAX_TOPIC_LENGTH} chars) and
        use a single word (letters, numbers, &apos;-&apos;).
      </Text>

      {error && (
        <Text c="red" size="sm">
          {error}
        </Text>
      )}

      <Table highlightOnHover withTableBorder striped={false} style={{ tableLayout: "fixed" }}>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Topic</Table.Th>
            <Table.Th style={{ width: 96 }} />
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {sortedTopics.length === 0 ? (
            <Table.Tr>
              <Table.Td colSpan={2}>
                <Text size="sm" c="dimmed" ta="center" py="md">
                  No topics found.
                </Text>
              </Table.Td>
            </Table.Tr>
          ) : (
            sortedTopics.map((t) => (
              <Table.Tr key={t}>
                <Table.Td>
                  <Text size="sm" fw={600}>
                    {t}
                  </Text>
                </Table.Td>
                <Table.Td>
                  <Group justify="flex-end" gap="xs" wrap="nowrap">
                    <ActionIcon
                      variant="subtle"
                      color="red"
                      aria-label="Delete topic"
                      onClick={() => openDelete(t)}
                    >
                      <IconTrash size={16} />
                    </ActionIcon>
                  </Group>
                </Table.Td>
              </Table.Tr>
            ))
          )}
        </Table.Tbody>
      </Table>

      <ConfirmModal
        opened={deleteOpened}
        onClose={() => setDeleteOpened(false)}
        onConfirm={confirmDelete}
        title="Delete Topic"
        message={
          <>
            Delete{" "}
            <Text span fw={700}>
              {selected ?? ""}
            </Text>
            ? This will also clear the topic from any courses using it.
          </>
        }
        confirmLabel="Delete"
        loading={saving}
        danger
      />
    </Stack>
  );
}
