"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  ActionIcon,
  Badge,
  Button,
  Group,
  Loader,
  Modal,
  NumberInput,
  Pagination,
  Select,
  Stack,
  Table,
  Text,
  TextInput,
  Textarea,
} from "@mantine/core";
import { useDebouncedCallback } from "@mantine/hooks";
import { useForm } from "@mantine/form";
import { IconPencil, IconSearch, IconTrash } from "@tabler/icons-react";
import { readBackendError } from "@/src/shared/lib/readBackendError";

type ChallengeStatus = "DRAFT" | "PRIVATE" | "PUBLIC";
type ChallengeDifficulty = "BEGINNER" | "EASY" | "MEDIUM" | "HARD" | "EXPERT";

type AdminChallengeListItem = {
  id: string;
  title: string;
  shortDescription: string | null;
  description: string | null;
  status: ChallengeStatus;
  difficulty: ChallengeDifficulty;
  maxScore: number;
  courseCount: number;
  createdAt: string;
  updatedAt: string;
  creatorId: string | null;
  creatorName: string | null;
  creatorUsername: string | null;
};

type PageResponse<T> = {
  content?: T[];
  totalPages?: number;
};

const PAGE_SIZE = 10;

const wrapTextStyle: React.CSSProperties = {
  whiteSpace: "pre-wrap",
  overflowWrap: "anywhere",
  wordBreak: "break-word",
};

function cleanText(v: string) {
  const t = v.trim();
  return t.length === 0 ? null : t;
}

function formatDate(value?: string | null) {
  if (!value) return "-";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleString();
}

export default function AdminChallengeManagement() {
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);

  const [challenges, setChallenges] = useState<AdminChallengeListItem[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [editOpened, setEditOpened] = useState(false);
  const [deleteOpened, setDeleteOpened] = useState(false);
  const [selected, setSelected] = useState<AdminChallengeListItem | null>(null);
  const [saving, setSaving] = useState(false);

  const form = useForm({
    initialValues: {
      title: "",
      shortDescription: "",
      description: "",
      status: "DRAFT" as ChallengeStatus,
      difficulty: "BEGINNER" as ChallengeDifficulty,
      maxScore: 0,
    },
    validate: {
      title: (v) => (v.trim().length === 0 ? "Title is required" : null),
      maxScore: (v) => (v < 0 ? "Max score must be >= 0" : null),
    },
  });

  const fetchPage = useCallback(async (q: string, p: number) => {
    setLoading(true);
    setError(null);
    try {
      const url = new URL("/api/backend/api/admin/challenges", window.location.origin);
      const qTrim = q.trim();
      if (qTrim) url.searchParams.set("q", qTrim);
      url.searchParams.set("page", String(p));
      url.searchParams.set("size", String(PAGE_SIZE));
      url.searchParams.set("sort", "updatedAt,desc");

      const res = await fetch(url.toString(), { method: "GET" });
      if (!res.ok) {
        const msg = await readBackendError(res);
        setError(`Failed to load challenges (HTTP ${res.status})${msg ? ` — ${msg}` : ""}`);
        return;
      }
      const data = (await res.json()) as PageResponse<AdminChallengeListItem>;
      setChallenges(data.content ?? []);
      setTotalPages(data.totalPages ?? 0);
    } catch {
      setError("Failed to load challenges");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchPage(query, page);
  }, [fetchPage, query, page]);

  const debouncedSearch = useDebouncedCallback((nextQ: string) => {
    setPage(0);
    void fetchPage(nextQ, 0);
  }, 300);

  function onQueryChange(next: string) {
    setQuery(next);
    debouncedSearch(next);
  }

  const selectedTitle = useMemo(() => selected?.title ?? "", [selected]);

  function openEdit(ch: AdminChallengeListItem) {
    setSelected(ch);
    form.setValues({
      title: ch.title ?? "",
      shortDescription: ch.shortDescription ?? "",
      description: ch.description ?? "",
      status: ch.status ?? "DRAFT",
      difficulty: ch.difficulty ?? "BEGINNER",
      maxScore: ch.maxScore ?? 0,
    });
    setEditOpened(true);
  }

  function openDelete(ch: AdminChallengeListItem) {
    setSelected(ch);
    setDeleteOpened(true);
  }

  async function submitEdit(values: typeof form.values) {
    if (!selected?.id) return;
    setSaving(true);
    setError(null);
    try {
      const res = await fetch(`/api/backend/api/admin/challenges/${selected.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: values.title.trim(),
          shortDescription: cleanText(values.shortDescription),
          description: cleanText(values.description),
          status: values.status,
          difficulty: values.difficulty,
          maxScore: values.maxScore,
        }),
      });
      if (!res.ok) {
        const msg = await readBackendError(res);
        setError(`Failed to update challenge (HTTP ${res.status})${msg ? ` — ${msg}` : ""}`);
        return;
      }
      setEditOpened(false);
      setSelected(null);
      void fetchPage(query, page);
    } catch {
      setError("Failed to update challenge");
    } finally {
      setSaving(false);
    }
  }

  async function confirmDelete() {
    if (!selected?.id) return;
    setSaving(true);
    setError(null);
    try {
      const res = await fetch(`/api/backend/api/admin/challenges/${selected.id}`, {
        method: "DELETE",
      });
      if (!res.ok) {
        const msg = await readBackendError(res);
        setError(`Failed to delete challenge (HTTP ${res.status})${msg ? ` — ${msg}` : ""}`);
        return;
      }
      setDeleteOpened(false);
      setSelected(null);
      setPage(0);
      void fetchPage(query, 0);
    } catch {
      setError("Failed to delete challenge");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-end" wrap="wrap">
        <Group gap="sm" wrap="wrap">
          <TextInput
            label="Search"
            placeholder="Title / description..."
            leftSection={<IconSearch size={16} />}
            value={query}
            onChange={(e) => onQueryChange(e.currentTarget.value)}
            w={420}
          />
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

      {error && (
        <Text c="red" size="sm">
          {error}
        </Text>
      )}

      <Table highlightOnHover withTableBorder striped={false} style={{ tableLayout: "fixed" }}>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Title</Table.Th>
            <Table.Th style={{ width: 240 }}>Creator</Table.Th>
            <Table.Th>Status</Table.Th>
            <Table.Th style={{ width: 190 }}>Updated</Table.Th>
            <Table.Th style={{ width: 96 }} />
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {challenges.length === 0 ? (
            <Table.Tr>
              <Table.Td colSpan={5}>
                <Text size="sm" c="dimmed" ta="center" py="md">
                  No challenges found.
                </Text>
              </Table.Td>
            </Table.Tr>
          ) : (
            challenges.map((c) => (
              <Table.Tr key={c.id}>
                <Table.Td>
                  <Stack gap={2}>
                    <Text fw={600} size="sm" lineClamp={1} style={wrapTextStyle} title={c.title}>
                      {c.title}
                    </Text>
                    {c.shortDescription ? (
                      <Text
                        size="xs"
                        c="dimmed"
                        lineClamp={2}
                        style={wrapTextStyle}
                        title={c.shortDescription}
                      >
                        {c.shortDescription}
                      </Text>
                    ) : null}
                  </Stack>
                </Table.Td>
                <Table.Td>
                  <Stack gap={2}>
                    <Text
                      size="sm"
                      lineClamp={1}
                      style={wrapTextStyle}
                      title={c.creatorName ?? "-"}
                    >
                      {c.creatorName ?? "-"}
                    </Text>
                    <Text
                      size="xs"
                      c="dimmed"
                      lineClamp={1}
                      style={wrapTextStyle}
                      title={c.creatorUsername ? `@${c.creatorUsername}` : ""}
                    >
                      {c.creatorUsername ? `@${c.creatorUsername}` : ""}
                    </Text>
                  </Stack>
                </Table.Td>
                <Table.Td>
                  <Group gap="xs">
                    <Badge
                      variant="light"
                      color={
                        c.status === "PUBLIC" ? "green" : c.status === "PRIVATE" ? "yellow" : "gray"
                      }
                    >
                      {c.status}
                    </Badge>
                    <Badge variant="light" color="blue">
                      {c.difficulty}
                    </Badge>
                    <Badge variant="light" color="gray">
                      Score {c.maxScore ?? 0}
                    </Badge>
                    <Badge variant="light" color="gray">
                      In {c.courseCount ?? 0} courses
                    </Badge>
                  </Group>
                </Table.Td>
                <Table.Td>
                  <Text size="sm">{formatDate(c.updatedAt)}</Text>
                </Table.Td>
                <Table.Td>
                  <Group justify="flex-end" gap="xs" wrap="nowrap">
                    <ActionIcon
                      variant="subtle"
                      color="gray"
                      aria-label="Edit challenge"
                      onClick={() => openEdit(c)}
                    >
                      <IconPencil size={16} />
                    </ActionIcon>
                    <ActionIcon
                      variant="subtle"
                      color="red"
                      aria-label="Delete challenge"
                      onClick={() => openDelete(c)}
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

      {totalPages > 1 && (
        <Group justify="center">
          <Pagination total={totalPages} value={page + 1} onChange={(v) => setPage(v - 1)} />
        </Group>
      )}

      <Modal
        opened={editOpened}
        onClose={() => setEditOpened(false)}
        title={`Edit Challenge: ${selectedTitle}`}
        centered
        size="lg"
      >
        <form onSubmit={form.onSubmit((values) => void submitEdit(values))}>
          <Stack gap="sm">
            <TextInput label="Title" required {...form.getInputProps("title")} />
            <Textarea
              label="Short description"
              autosize
              minRows={2}
              maxRows={4}
              {...form.getInputProps("shortDescription")}
            />
            <Textarea
              label="Description"
              autosize
              minRows={3}
              maxRows={8}
              {...form.getInputProps("description")}
            />
            <Group grow>
              <Select
                label="Status"
                data={[
                  { value: "DRAFT", label: "DRAFT" },
                  { value: "PRIVATE", label: "PRIVATE" },
                  { value: "PUBLIC", label: "PUBLIC" },
                ]}
                value={form.values.status}
                onChange={(v) => form.setFieldValue("status", (v ?? "DRAFT") as ChallengeStatus)}
              />
              <Select
                label="Difficulty"
                data={[
                  { value: "BEGINNER", label: "BEGINNER" },
                  { value: "EASY", label: "EASY" },
                  { value: "MEDIUM", label: "MEDIUM" },
                  { value: "HARD", label: "HARD" },
                  { value: "EXPERT", label: "EXPERT" },
                ]}
                value={form.values.difficulty}
                onChange={(v) =>
                  form.setFieldValue("difficulty", (v ?? "BEGINNER") as ChallengeDifficulty)
                }
              />
            </Group>
            <NumberInput
              label="Max score"
              min={0}
              value={form.values.maxScore}
              onChange={(v) => form.setFieldValue("maxScore", Number(v ?? 0))}
            />

            <Group justify="flex-end" mt="xs">
              <Button variant="default" onClick={() => setEditOpened(false)} disabled={saving}>
                Cancel
              </Button>
              <Button type="submit" loading={saving}>
                Save
              </Button>
            </Group>
          </Stack>
        </form>
      </Modal>

      <Modal
        opened={deleteOpened}
        onClose={() => setDeleteOpened(false)}
        title="Delete Challenge"
        centered
      >
        <Stack gap="md">
          <Text size="sm">
            Delete{" "}
            <Text span fw={700}>
              {selectedTitle}
            </Text>
            ? This cannot be undone.
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
