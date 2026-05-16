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
  Select,
  Stack,
  Table,
  Text,
  TextInput,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { notifications } from "@mantine/notifications";
import { IconPencil, IconSearch, IconTrash } from "@tabler/icons-react";
import { useAdminPagedList } from "@/src/features/admin/hooks/useAdminPagedList";
import { cleanText, formatDate, wrapTextStyle } from "@/src/features/admin/lib/adminUi";
import MyEditor from "@/src/shared/components/MyEditor";
import { readBackendError } from "@/src/shared/lib/readBackendError";
import { slugify } from "@/src/shared/lib/utils";
import { toUserFriendlyBackendError } from "@/src/shared/lib/userFriendlyBackendError";

type ChallengeStatus = "DRAFT" | "PRIVATE" | "PUBLIC";
type ChallengeDifficulty = "BEGINNER" | "EASY" | "MEDIUM" | "HARD" | "EXPERT";

type AdminLabListItem = {
  id: string;
  title: string;
  description: string | null;
  status: ChallengeStatus;
  difficulty: ChallengeDifficulty;
  isSoftDeleted: boolean;
  canHardDelete: boolean;
  dockerImage: string | null;
  courseCount: number;
  createdAt: string;
  updatedAt: string;
  creatorId: string | null;
  creatorName: string | null;
  creatorUsername: string | null;
};

const PAGE_SIZE = 10;

export default function AdminChallengeManagement() {
  const {
    query,
    onQueryChange,
    applyQueryNow,
    page,
    setPage,
    items: labs,
    totalPages,
    loading,
    error,
    setError,
    refresh,
  } = useAdminPagedList<AdminLabListItem>({
    endpoint: "/api/backend/api/admin/labs",
    label: "labs",
    pageSize: PAGE_SIZE,
    sort: "updatedAt,desc",
  });

  const [editOpened, setEditOpened] = useState(false);
  const [deleteOpened, setDeleteOpened] = useState(false);
  const [deleteMode, setDeleteMode] = useState<"soft" | "hard">("soft");
  const [selected, setSelected] = useState<AdminLabListItem | null>(null);
  const [saving, setSaving] = useState(false);

  const showToast = useCallback((color: "red" | "orange", title: string, message: string) => {
    notifications.show({
      id: `admin-challenge-management:${color}:${slugify(title)}`,
      color,
      title,
      message,
    });
  }, []);

  useEffect(() => {
    if (!error) return;
    const message = error.replace(/^Failed to load labs\.\s*/i, "").trim();
    showToast("red", "Failed to load labs", message || "Please try again.");
    setError(null);
  }, [error, setError, showToast]);

  const form = useForm({
    initialValues: {
      title: "",
      description: "<p>Add a description...</p>",
      status: "DRAFT" as ChallengeStatus,
      difficulty: "BEGINNER" as ChallengeDifficulty,
    },
    validate: {
      title: (v) => (v.trim().length === 0 ? "Title is required" : null),
    },
  });

  const selectedTitle = useMemo(() => selected?.title ?? "", [selected]);

  function openEdit(lab: AdminLabListItem) {
    setSelected(lab);
    form.setValues({
      title: lab.title ?? "",
      description: lab.description ?? "<p>Add a description...</p>",
      status: lab.status ?? "DRAFT",
      difficulty: lab.difficulty ?? "BEGINNER",
    });
    setEditOpened(true);
  }

  function openDelete(lab: AdminLabListItem, mode: "soft" | "hard") {
    setSelected(lab);
    setDeleteMode(mode);
    setDeleteOpened(true);
  }

  async function submitEdit(values: typeof form.values) {
    if (!selected?.id) return;
    setSaving(true);
    setError(null);
    try {
      const res = await fetch(`/api/backend/api/admin/labs/${selected.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: values.title.trim(),
          description: cleanText(values.description),
          status: values.status,
          difficulty: values.difficulty,
        }),
      });
      if (!res.ok) {
        const raw = await readBackendError(res);
        const msg = toUserFriendlyBackendError(raw);
        const color = res.status >= 500 ? "red" : "orange";
        showToast(color, "Failed to update lab", msg ?? "Please try again.");
        return;
      }
      setEditOpened(false);
      setSelected(null);
      refresh();
    } catch {
      showToast("red", "Failed to update lab", "Please try again.");
    } finally {
      setSaving(false);
    }
  }

  async function confirmDelete() {
    if (!selected?.id) return;
    setSaving(true);
    setError(null);
    try {
      const res = await fetch(`/api/backend/api/admin/labs/${selected.id}`, {
        method: "DELETE",
      });
      if (!res.ok) {
        const raw = await readBackendError(res);
        const msg = toUserFriendlyBackendError(raw);
        const color = res.status >= 500 ? "red" : "orange";
        showToast(
          color,
          deleteMode === "hard" ? "Failed to hard-delete lab" : "Failed to remove lab",
          msg ?? "Please try again."
        );
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
      showToast(
        "red",
        deleteMode === "hard" ? "Failed to hard-delete lab" : "Failed to remove lab",
        "Please try again."
      );
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
      <Text size="sm" c="dimmed">
        Instructors can only soft-delete labs. Hard delete is enabled after soft delete.
      </Text>

      <Table highlightOnHover withTableBorder striped={false} style={{ tableLayout: "fixed" }}>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Title</Table.Th>
            <Table.Th style={{ width: 240 }}>Creator</Table.Th>
            <Table.Th>Status</Table.Th>
            <Table.Th style={{ width: 150 }}>Deletion</Table.Th>
            <Table.Th style={{ width: 190 }}>Updated</Table.Th>
            <Table.Th style={{ width: 130 }} />
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {labs.length === 0 ? (
            <Table.Tr>
              <Table.Td colSpan={6}>
                <Text size="sm" c="dimmed" ta="center" py="md">
                  No labs found.
                </Text>
              </Table.Td>
            </Table.Tr>
          ) : (
            labs.map((c) => (
              <Table.Tr key={c.id}>
                <Table.Td>
                  <Text fw={600} size="sm" lineClamp={2} style={wrapTextStyle} title={c.title}>
                    {c.title}
                  </Text>
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
                    <Badge variant="light" color="grape">
                      Courses: {c.courseCount}
                    </Badge>
                  </Group>
                </Table.Td>
                <Table.Td>
                  <Badge variant="light" color={c.isSoftDeleted ? "orange" : "teal"}>
                    {c.isSoftDeleted ? "Soft Deleted" : "Active"}
                  </Badge>
                </Table.Td>
                <Table.Td>
                  <Text size="sm">{formatDate(c.updatedAt)}</Text>
                </Table.Td>
                <Table.Td>
                  <Group justify="flex-end" gap="xs" wrap="nowrap">
                    <ActionIcon
                      variant="subtle"
                      color="gray"
                      aria-label="Edit lab"
                      onClick={() => openEdit(c)}
                    >
                      <IconPencil size={16} />
                    </ActionIcon>
                    <ActionIcon
                      variant="subtle"
                      color="orange"
                      aria-label="Soft delete lab"
                      onClick={() => openDelete(c, "soft")}
                      disabled={c.isSoftDeleted}
                    >
                      <IconTrash size={16} />
                    </ActionIcon>
                    <ActionIcon
                      variant="subtle"
                      color="red"
                      aria-label="Hard delete lab"
                      onClick={() => openDelete(c, "hard")}
                      disabled={!c.canHardDelete}
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
        title={`Edit Lab: ${selectedTitle}`}
        centered
        size="lg"
      >
        <form onSubmit={form.onSubmit((values) => void submitEdit(values))}>
          <Stack gap="sm">
            <TextInput label="Title" required {...form.getInputProps("title")} />
            <TextInput
              label="Docker Image"
              value={selected?.dockerImage ?? ""}
              readOnly
              styles={{ input: { fontFamily: "monospace", fontSize: "0.85rem" } }}
            />
            <MyEditor
              description={form.values.description}
              setDescription={(value) => form.setFieldValue("description", value)}
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
            <Group justify="flex-end" mt="xs">
              <Button
                variant="default"
                radius="md"
                onClick={() => setEditOpened(false)}
                disabled={saving}
              >
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
        title={deleteMode === "hard" ? "Hard Delete Lab" : "Remove Lab"}
        centered
      >
        <Stack gap="md">
          <Text size="sm">
            {deleteMode === "hard" ? "Permanently delete" : "Soft-delete"}{" "}
            <Text span fw={700}>
              {selectedTitle}
            </Text>
            ?{" "}
            {deleteMode === "hard"
              ? "This cannot be undone."
              : "The lab will be hidden from active instructor and student lists."}
          </Text>
          {deleteMode === "hard" && !(selected?.canHardDelete ?? false) && (
            <Text size="sm" c="dimmed">
              Hard delete is only possible after soft delete.
            </Text>
          )}
          <Group justify="flex-end">
            <Button
              variant="default"
              radius="md"
              onClick={() => setDeleteOpened(false)}
              disabled={saving}
            >
              Cancel
            </Button>
            <Button
              color={deleteMode === "hard" ? "red" : "orange"}
              radius="md"
              onClick={() => void confirmDelete()}
              loading={saving}
              disabled={deleteMode === "hard" && !(selected?.canHardDelete ?? false)}
            >
              {deleteMode === "hard" ? "Hard Delete" : "Remove"}
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  );
}
