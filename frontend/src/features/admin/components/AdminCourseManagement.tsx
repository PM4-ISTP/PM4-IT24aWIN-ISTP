"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  ActionIcon,
  Badge,
  Button,
  Group,
  Loader,
  Modal,
  Pagination,
  Stack,
  Table,
  Text,
  TextInput,
  Textarea,
  Switch,
  Select,
} from "@mantine/core";
import { useDebouncedCallback } from "@mantine/hooks";
import { useForm } from "@mantine/form";
import { IconPencil, IconSearch, IconTrash } from "@tabler/icons-react";
import { useCourseTopicOptions } from "@/src/features/course/hooks/useCourseTopicOptions";

type AdminCourseListItem = {
  id: string;
  title: string;
  description: string | null;
  shortDescription: string | null;
  isPublished: boolean;
  isPrivate: boolean;
  createdAt: string;
  updatedAt: string;
  topic: string | null;
  imageUrl: string | null;
  ownerId: string | null;
  ownerName: string | null;
  ownerUsername: string | null;
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

export default function AdminCourseManagement() {
  const topicOptions = useCourseTopicOptions();
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [courses, setCourses] = useState<AdminCourseListItem[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [editOpened, setEditOpened] = useState(false);
  const [deleteOpened, setDeleteOpened] = useState(false);
  const [selected, setSelected] = useState<AdminCourseListItem | null>(null);
  const [saving, setSaving] = useState(false);

  const form = useForm({
    initialValues: {
      title: "",
      description: "",
      shortDescription: "",
      isPublished: false,
      isPrivate: false,
      topic: "" as string,
      imageUrl: "",
    },
    validate: {
      title: (v) => (v.trim().length === 0 ? "Title is required" : null),
      isPrivate: (_v, values) =>
        values.isPrivate && values.isPublished
          ? "A course cannot be private and published at the same time"
          : null,
      isPublished: (_v, values) =>
        values.isPrivate && values.isPublished
          ? "A course cannot be private and published at the same time"
          : null,
    },
  });

  const fetchPage = useCallback(async (q: string, p: number) => {
    setLoading(true);
    setError(null);
    try {
      const url = new URL("/api/backend/api/admin/courses", window.location.origin);
      const qTrim = q.trim();
      if (qTrim) url.searchParams.set("q", qTrim);
      url.searchParams.set("page", String(p));
      url.searchParams.set("size", String(PAGE_SIZE));
      url.searchParams.set("sort", "updatedAt,desc");

      const res = await fetch(url.toString(), { method: "GET" });
      if (!res.ok) {
        const msg = await readBackendError(res);
        setError(
          `Failed to load courses (HTTP ${res.status})${msg ? ` — ${msg}` : ""}`
        );
        return;
      }
      const data = (await res.json()) as PageResponse<AdminCourseListItem>;
      setCourses(data.content ?? []);
      setTotalPages(data.totalPages ?? 0);
    } catch {
      setError("Failed to load courses");
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

  function openEdit(course: AdminCourseListItem) {
    setSelected(course);
    form.setValues({
      title: course.title ?? "",
      description: course.description ?? "",
      shortDescription: course.shortDescription ?? "",
      isPublished: !!course.isPublished,
      isPrivate: !!course.isPrivate,
      topic: course.topic ?? "",
      imageUrl: course.imageUrl ?? "",
    });
    setEditOpened(true);
  }

  function openDelete(course: AdminCourseListItem) {
    setSelected(course);
    setDeleteOpened(true);
  }

  async function submitEdit(values: typeof form.values) {
    if (!selected?.id) return;
    setSaving(true);
    setError(null);
    try {
      const res = await fetch(`/api/backend/api/admin/courses/${selected.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: values.title.trim(),
          description: cleanText(values.description),
          shortDescription: cleanText(values.shortDescription),
          isPublished: values.isPublished,
          isPrivate: values.isPrivate,
          topic: cleanText(values.topic),
          imageUrl: cleanText(values.imageUrl),
        }),
      });
      if (!res.ok) {
        const msg = await readBackendError(res);
        setError(`Failed to update course (HTTP ${res.status})${msg ? ` — ${msg}` : ""}`);
        return;
      }
      setEditOpened(false);
      setSelected(null);
      void fetchPage(query, page);
    } catch {
      setError("Failed to update course");
    } finally {
      setSaving(false);
    }
  }

  async function confirmDelete() {
    if (!selected?.id) return;
    setSaving(true);
    setError(null);
    try {
      const res = await fetch(`/api/backend/api/admin/courses/${selected.id}`, {
        method: "DELETE",
      });
      if (!res.ok) {
        const msg = await readBackendError(res);
        setError(`Failed to delete course (HTTP ${res.status})${msg ? ` — ${msg}` : ""}`);
        return;
      }
      setDeleteOpened(false);
      setSelected(null);
      setPage(0);
      void fetchPage(query, 0);
    } catch {
      setError("Failed to delete course");
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

      <Table
        highlightOnHover
        withTableBorder
        withColumnBorders={false}
        striped={false}
        style={{ tableLayout: "fixed" }}
      >
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Title</Table.Th>
            <Table.Th style={{ width: 240 }}>Owner</Table.Th>
            <Table.Th style={{ width: 190 }}>Visibility</Table.Th>
            <Table.Th style={{ width: 190 }}>Updated</Table.Th>
            <Table.Th style={{ width: 96 }} />
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {courses.length === 0 ? (
            <Table.Tr>
              <Table.Td colSpan={5}>
                <Text size="sm" c="dimmed" ta="center" py="md">
                  No courses found.
                </Text>
              </Table.Td>
            </Table.Tr>
          ) : (
            courses.map((c) => (
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
                    <Text size="sm" lineClamp={1} style={wrapTextStyle} title={c.ownerName ?? "-"}>
                      {c.ownerName ?? "-"}
                    </Text>
                    <Text
                      size="xs"
                      c="dimmed"
                      lineClamp={1}
                      style={wrapTextStyle}
                      title={c.ownerUsername ? `@${c.ownerUsername}` : ""}
                    >
                      {c.ownerUsername ? `@${c.ownerUsername}` : ""}
                    </Text>
                  </Stack>
                </Table.Td>
                <Table.Td>
                  <Group gap="xs">
                    <Badge variant="light" color={c.isPublished ? "green" : "gray"}>
                      {c.isPublished ? "Published" : "Draft"}
                    </Badge>
                    <Badge variant="light" color={c.isPrivate ? "yellow" : "blue"}>
                      {c.isPrivate ? "Private" : "Public"}
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
                      aria-label="Edit course"
                      onClick={() => openEdit(c)}
                    >
                      <IconPencil size={16} />
                    </ActionIcon>
                    <ActionIcon
                      variant="subtle"
                      color="red"
                      aria-label="Delete course"
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
        title={`Edit Course: ${selectedTitle}`}
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
            <Select
              label="Topic"
              placeholder="Select a topic"
              data={topicOptions.options}
              value={form.values.topic || null}
              onChange={(value) => form.setFieldValue("topic", value ?? "")}
              clearable
              searchable
              disabled={topicOptions.loading}
            />
            <TextInput label="Image URL" {...form.getInputProps("imageUrl")} />

            <Group justify="space-between" wrap="wrap">
              <Switch
                label="Published"
                checked={form.values.isPublished}
                onChange={(e) => {
                  const next = e.currentTarget.checked;
                  form.setFieldValue("isPublished", next);
                  if (next) form.setFieldValue("isPrivate", false);
                }}
              />
              <Switch
                label="Private"
                checked={form.values.isPrivate}
                onChange={(e) => {
                  const next = e.currentTarget.checked;
                  form.setFieldValue("isPrivate", next);
                  if (next) form.setFieldValue("isPublished", false);
                }}
              />
            </Group>

            {form.errors.isPrivate ? (
              <Text c="red" size="sm">
                {form.errors.isPrivate}
              </Text>
            ) : null}

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
        title="Delete Course"
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
