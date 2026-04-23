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
} from "@mantine/core";
import { useDebouncedCallback } from "@mantine/hooks";
import { useForm } from "@mantine/form";
import { IconPencil, IconSearch, IconTrash } from "@tabler/icons-react";

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
  const [query, setQuery] = useState("");
  const [owner, setOwner] = useState("");
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
      topic: "",
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

  const fetchPage = useCallback(async (q: string, o: string, p: number) => {
    setLoading(true);
    setError(null);
    try {
      const url = new URL("/api/backend/api/admin/courses", window.location.origin);
      const qTrim = q.trim();
      const oTrim = o.trim();
      if (qTrim) url.searchParams.set("q", qTrim);
      if (oTrim) url.searchParams.set("owner", oTrim);
      url.searchParams.set("page", String(p));
      url.searchParams.set("size", String(PAGE_SIZE));
      url.searchParams.set("sort", "updatedAt,desc");

      const res = await fetch(url.toString(), { method: "GET" });
      if (!res.ok) {
        setError("Failed to load courses");
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
    void fetchPage(query, owner, page);
  }, [fetchPage, query, owner, page]);

  const debouncedSearch = useDebouncedCallback((nextQ: string, nextOwner: string) => {
    setPage(0);
    void fetchPage(nextQ, nextOwner, 0);
  }, 300);

  function onQueryChange(next: string) {
    setQuery(next);
    debouncedSearch(next, owner);
  }

  function onOwnerChange(next: string) {
    setOwner(next);
    debouncedSearch(query, next);
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
        setError("Failed to update course");
        return;
      }
      setEditOpened(false);
      setSelected(null);
      void fetchPage(query, owner, page);
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
        setError("Failed to delete course");
        return;
      }
      setDeleteOpened(false);
      setSelected(null);
      setPage(0);
      void fetchPage(query, owner, 0);
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
            w={320}
          />
          <TextInput
            label="Owner"
            placeholder="Name or username..."
            value={owner}
            onChange={(e) => onOwnerChange(e.currentTarget.value)}
            w={260}
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

      <Table highlightOnHover withTableBorder withColumnBorders={false} striped={false}>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Title</Table.Th>
            <Table.Th>Owner</Table.Th>
            <Table.Th>Visibility</Table.Th>
            <Table.Th>Updated</Table.Th>
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
                    <Text fw={600} size="sm">
                      {c.title}
                    </Text>
                    {c.shortDescription ? (
                      <Text size="xs" c="dimmed" lineClamp={2}>
                        {c.shortDescription}
                      </Text>
                    ) : null}
                  </Stack>
                </Table.Td>
                <Table.Td>
                  <Stack gap={2}>
                    <Text size="sm">{c.ownerName ?? "-"}</Text>
                    <Text size="xs" c="dimmed">
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
            <TextInput label="Topic" {...form.getInputProps("topic")} />
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
