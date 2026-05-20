"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  ActionIcon,
  Alert,
  Badge,
  Group,
  Modal,
  Pagination,
  ScrollArea,
  Select,
  Stack,
  Table,
  Text,
  TextInput,
  Textarea,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { notifications } from "@mantine/notifications";
import { IconPencil, IconTrash } from "@tabler/icons-react";
import { useAdminPagedList } from "@/src/features/admin/hooks/useAdminPagedList";
import { cleanText, formatDate, wrapTextStyle } from "@/src/features/admin/lib/adminUi";
import AdminListSearch from "@/src/features/admin/components/AdminListSearch";
import AppButton from "@/src/shared/components/AppButton";
import { useCourseTopicOptions } from "@/src/features/course/hooks/useCourseTopicOptions";
import MyEditor from "@/src/shared/components/MyEditor";
import { useApiClient } from "@/src/shared/lib/api/client";
import { apiErrorText } from "@/src/shared/lib/api";
import { slugify } from "@/src/shared/lib/utils";
import {
  COURSE_SHORT_DESCRIPTION_MAX_CHARS,
  normalizeShortDescription,
} from "@/src/features/course/utils/courseText";
import { toUserFriendlyBackendError } from "@/src/shared/lib/userFriendlyBackendError";
import type { CourseVisibility } from "@/src/shared/types/course";

type AdminCourseListItem = {
  id: string;
  title: string;
  description: string | null;
  shortDescription: string | null;
  status: CourseVisibility;
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
  const client = useApiClient();
  const topicOptions = useCourseTopicOptions();
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

  const showToast = useCallback((color: "red" | "orange", title: string, message: string) => {
    notifications.show({
      id: `admin-course-management:${color}:${slugify(title)}`,
      color,
      title,
      message,
    });
  }, []);

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
      status: "DRAFT" as CourseVisibility,
      topic: "" as string,
      imageUrl: "",
    },
    validate: {
      title: (v) => (v.trim().length === 0 ? "Title is required" : null),
    },
  });

  const selectedTitle = useMemo(() => selected?.title ?? "", [selected]);

  function openEdit(course: AdminCourseListItem) {
    setSelected(course);
    form.setValues({
      title: course.title ?? "",
      description: course.description ?? "<p>Add a description...</p>",
      shortDescription: course.shortDescription ?? "",
      status: course.status ?? "DRAFT",
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
      const { error, response } = await client.PUT("/api/admin/courses/{id}", {
        params: { path: { id: selected.id } },
        body: {
          title: values.title.trim(),
          description: cleanText(values.description) ?? "",
          shortDescription: cleanText(normalizedShortDescription) ?? "",
          status: values.status,
          topic: cleanText(values.topic) ?? undefined,
          imageUrl: cleanText(values.imageUrl) ?? undefined,
        },
      });
      if (error || !response.ok) {
        const raw = apiErrorText(error);
        const msgLower = raw?.toLowerCase() ?? "";
        if (msgLower.includes("invalid topic")) {
          showToast("orange", "Invalid topic", "Please select a topic from the list.");
          return;
        }
        const msg = toUserFriendlyBackendError(raw);
        const color = response.status >= 500 ? "red" : "orange";
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
      const { error, response } = await client.DELETE("/api/admin/courses/{id}", {
        params: { path: { id: selected.id } },
      });
      if (error || !response.ok) {
        const msg = toUserFriendlyBackendError(apiErrorText(error));
        const color = response.status >= 500 ? "red" : "orange";
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
      <AdminListSearch
        query={query}
        onQueryChange={onQueryChange}
        applyQueryNow={applyQueryNow}
        loading={loading}
      />
      <Alert color="orange" title="Delete" variant="light">
        <Text size="sm">
          Deleting a course removes it from active lists so students and instructors can no longer
          access it.
        </Text>
      </Alert>

      <ScrollArea>
        <Table
          highlightOnHover
          withTableBorder
          withColumnBorders={false}
          striped={false}
          miw={820}
          style={{ tableLayout: "fixed" }}
        >
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Title</Table.Th>
              <Table.Th style={{ width: 240 }}>Owner</Table.Th>
              <Table.Th style={{ width: 190 }}>Visibility</Table.Th>
              <Table.Th style={{ width: 190 }}>Updated</Table.Th>
              <Table.Th style={{ width: 130 }} />
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
                      <Text
                        size="sm"
                        lineClamp={1}
                        style={wrapTextStyle}
                        title={c.ownerName ?? "-"}
                      >
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
                      <Badge
                        variant="light"
                        color={
                          c.status === "PRIVATE"
                            ? "yellow"
                            : c.status === "PUBLIC"
                              ? "green"
                              : "gray"
                        }
                      >
                        {c.status === "PRIVATE"
                          ? "Private"
                          : c.status === "PUBLIC"
                            ? "Public"
                            : "Draft"}
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
      </ScrollArea>

      {totalPages > 1 && (
        <Group justify="center">
          <Pagination
            radius="md"
            total={totalPages}
            value={page + 1}
            onChange={(v) => setPage(v - 1)}
          />
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
              styles={{ input: { overflowY: "auto" } }}
              required
            />
            <MyEditor
              label="Description"
              required
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

            <Select
              label="Visibility"
              value={form.values.status}
              onChange={(value) => {
                if (value) form.setFieldValue("status", value as CourseVisibility);
              }}
              data={[
                { value: "DRAFT", label: "Draft" },
                { value: "PUBLIC", label: "Public" },
                { value: "PRIVATE", label: "Private" },
              ]}
              description="Choose exactly one state. Draft keeps it hidden, Public shows in catalog, Private is join-by-code only."
              allowDeselect={false}
            />

            <Group justify="flex-end" mt="xs">
              <AppButton tone="ghost" onClick={() => setEditOpened(false)} disabled={saving}>
                Cancel
              </AppButton>
              <AppButton type="submit" loading={saving}>
                Save
              </AppButton>
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
            ? Students and instructors will no longer see it in active lists.
          </Text>
          <Group justify="flex-end">
            <AppButton tone="ghost" onClick={() => setDeleteOpened(false)} disabled={saving}>
              Cancel
            </AppButton>
            <AppButton tone="danger" onClick={() => void confirmDelete()} loading={saving}>
              Delete
            </AppButton>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  );
}
