"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  ActionIcon,
  Button,
  Group,
  Loader,
  Modal,
  Stack,
  Table,
  Text,
  TextInput,
} from "@mantine/core";
import { IconPlus, IconTrash } from "@tabler/icons-react";

async function readBackendError(res: Response): Promise<string | null> {
  try {
    const body = await res.text();
    if (!body) return null;
    try {
      const json = JSON.parse(body) as unknown;
      if (
        json &&
        typeof json === "object" &&
        "error" in json &&
        typeof (json as { error?: unknown }).error === "string"
      ) {
        return (json as { error: string }).error;
      }
    } catch {
      // ignore JSON parse errors
    }
    return body;
  } catch {
    return null;
  }
}

export default function AdminTopicManagement() {
  const [topics, setTopics] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [newTopic, setNewTopic] = useState("");
  const [saving, setSaving] = useState(false);

  const [deleteOpened, setDeleteOpened] = useState(false);
  const [selected, setSelected] = useState<string | null>(null);

  const sortedTopics = useMemo(() => [...topics].sort((a, b) => a.localeCompare(b)), [topics]);

  const loadTopics = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch("/api/backend/api/admin/topics", { method: "GET" });
      if (!res.ok) {
        const msg = await readBackendError(res);
        setError(`Failed to load topics (HTTP ${res.status})${msg ? ` — ${msg}` : ""}`);
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

  async function addTopic() {
    const value = newTopic.trim();
    if (!value) return;
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
        setError(`Failed to add topic (HTTP ${res.status})${msg ? ` — ${msg}` : ""}`);
        return;
      }
      setNewTopic("");
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
        setError(`Failed to delete topic (HTTP ${res.status})${msg ? ` — ${msg}` : ""}`);
        return;
      }
      setDeleteOpened(false);
      setSelected(null);
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
            placeholder="e.g. Security"
            value={newTopic}
            onChange={(e) => setNewTopic(e.currentTarget.value)}
            w={320}
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
            onClick={() => void addTopic()}
            loading={saving}
            disabled={!newTopic.trim()}
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
        Deleting a topic removes it from the selection list and clears it from existing courses.
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

      <Modal
        opened={deleteOpened}
        onClose={() => setDeleteOpened(false)}
        title="Delete Topic"
        centered
      >
        <Stack gap="md">
          <Text size="sm">
            Delete{" "}
            <Text span fw={700}>
              {selected ?? ""}
            </Text>
            ? This will also clear the topic from any courses using it.
          </Text>
          <Group justify="flex-end">
            <Button variant="default" onClick={() => setDeleteOpened(false)} disabled={saving}>
              Cancel
            </Button>
            <Button color="red" onClick={() => void confirmDelete()} loading={saving}>
              Delete
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  );
}
