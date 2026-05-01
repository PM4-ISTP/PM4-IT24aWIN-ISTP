"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  ActionIcon,
  Affix,
  Badge,
  Button,
  Group,
  Loader,
  Modal,
  Notification,
  Pagination,
  Select,
  Stack,
  Switch,
  Table,
  Text,
  TextInput,
  Textarea,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { IconPencil, IconSearch, IconTrash, IconX } from "@tabler/icons-react";
import { useAdminPagedList } from "@/src/features/admin/hooks/useAdminPagedList";
import { cleanText, formatDate, wrapTextStyle } from "@/src/features/admin/lib/adminUi";
import { useCourseTopicOptions } from "@/src/features/course/hooks/useCourseTopicOptions";
import MyEditor from "@/src/shared/components/MyEditor";
import { useToast } from "@/src/shared/hooks/useToast";
import { readBackendError } from "@/src/shared/lib/readBackendError";
import {
  COURSE_SHORT_DESCRIPTION_MAX_CHARS,
  normalizeShortDescription,
} from "@/src/features/course/utils/courseText";
import { toUserFriendlyBackendError } from "@/src/shared/lib/userFriendlyBackendError";

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

const PAGE_SIZE = 10;

export default function AdminCourseManagement() {
  const topicOptions = useCourseTopicOptions();
  const {
    visible: toastVisible,
    show: showToastNotification,
    hide: hideToastNotification,
  } = useToast();
  const [toastConfig, setToastConfig] = useState<{
    color: "red" | "orange";
    title: string;
    message: string;
  } | null>(null);
  const {
    query,
    onQueryChange,
    applyQueryNow,
    page,
    setPage,
    items: courses,
    totalPages,
    loading,
    error,
    setError,
    refresh,
  } = useAdminPagedList<AdminCourseListItem>({
    endpoint: "/api/backend/api/admin/courses",
    label: "courses",
    pageSize: PAGE_SIZE,
  });

  const [editOpened, setEditOpened] = useState(false);
  const [deleteOpened, setDeleteOpened] = useState(false);
  const [selected, setSelected] = useState<AdminCourseListItem | null>(null);
  const [saving, setSaving] = useState(false);

  const showToast = useCallback(
    (color: "red" | "orange", title: string, message: string) => {
      setToastConfig({ color, title, message });
      showToastNotification();
    },
    [showToastNotification]
  );

  useEffect(() => {
    if (!error) return;
    const message = error.replace(/^Failed to load courses\.\s*/i, "").trim();
    showToast("red", "Failed to load courses", message || "Please try again.");
    setError(null);
  }, [error, setError, showToast]);

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

  const selectedTitle = useMemo(() => selected?.title ?? "", [selected]);

  function openEdit(course: AdminCourseListItem) {
    setSelected(course);
    form.setValues({
      title: course.title ?? "",
      description: course.description ?? "<p>Add a description...</p>",
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
      const normalizedShortDescription = normalizeShortDescription(values.shortDescription);
      const res = await fetch(`/api/backend/api/admin/courses/${selected.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: values.title.trim(),
          description: cleanText(values.description),
          shortDescription: cleanText(normalizedShortDescription),
          isPublished: values.isPublished,
          isPrivate: values.isPrivate,
          topic: cleanText(values.topic),
          imageUrl: cleanText(values.imageUrl),
        }),
      });
      if (!res.ok) {
        const raw = await readBackendError(res);
        const msgLower = raw?.toLowerCase() ?? "";
        if (msgLower.includes("invalid topic")) {
          showToast("orange", "Invalid topic", "Please select a topic from the list.");
          return;
        }
        const msg = toUserFriendlyBackendError(raw);
        const color = res.status >= 500 ? "red" : "orange";
        showToast(color, "Failed to update course", msg ?? "Please try again.");
        return;
      }
      setEditOpened(false);
      setSelected(null);
      refresh();
    } catch {
      showToast("red", "Failed to update course", "Please try again.");
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
        const raw = await readBackendError(res);
        const msg = toUserFriendlyBackendError(raw);
        const color = res.status >= 500 ? "red" : "orange";
        showToast(color, "Failed to delete course", msg ?? "Please try again.");
        return;
      }
      setDeleteOpened(false);
      setSelected(null);
      if (page !== 0) {
        setPage(0);
      } else {
        refresh();
      }
    } catch {
      showToast("red", "Failed to delete course", "Please try again.");
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
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                e.preventDefault();
                applyQueryNow();
              }
            }}
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
          <Pagination radius="md" total={totalPages} value={page + 1} onChange={(v) => setPage(v - 1)} />
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
              value={form.values.shortDescription}
              onChange={(e) => {
                const next = e.currentTarget.value;
                if (next.length > COURSE_SHORT_DESCRIPTION_MAX_CHARS) {
                  showToast(
                    "orange",
                    "Character limit reached",
                    `Short description cannot exceed ${COURSE_SHORT_DESCRIPTION_MAX_CHARS} characters.`
                  );
                  return;
                }
                form.setFieldValue("shortDescription", next);
              }}
              description={`${form.values.shortDescription.length}/${COURSE_SHORT_DESCRIPTION_MAX_CHARS} characters.`}
            />
            <MyEditor
              description={form.values.description}
              setDescription={(value) => form.setFieldValue("description", value)}
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
              <Button variant="default" radius="md" onClick={() => setEditOpened(false)} disabled={saving}>
                Cancel
              </Button>
              <Button
                type="submit"
                radius="md"
                loading={saving}
                style={{
                  background: "linear-gradient(90deg, #2563eb, #4f46e5)",
                  border: "none",
                  fontFamily: "var(--font-space-grotesk), sans-serif",
                  fontWeight: 600,
                  boxShadow: "0 2px 12px rgba(79,70,229,0.3)",
                }}
              >
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
            <Button variant="default" radius="md" onClick={() => setDeleteOpened(false)} disabled={saving}>
              Cancel
            </Button>
            <Button color="red" radius="md" onClick={() => void confirmDelete()} loading={saving}>
              Delete
            </Button>
          </Group>
        </Stack>
      </Modal>

      {toastVisible && toastConfig && (
        <Affix position={{ bottom: 20, right: 20 }} style={{ zIndex: 3000 }}>
          <Notification
            color={toastConfig.color}
            title={toastConfig.title}
            onClose={hideToastNotification}
            withCloseButton
            icon={<IconX size={18} />}
          >
            {toastConfig.message}
          </Notification>
        </Affix>
      )}
    </Stack>
  );
}
