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
  NumberInput,
  Pagination,
  Select,
  Stack,
  Table,
  Text,
  TextInput,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { IconPencil, IconSearch, IconTrash, IconX } from "@tabler/icons-react";
import { useAdminPagedList } from "@/src/features/admin/hooks/useAdminPagedList";
import { cleanText, formatDate, wrapTextStyle } from "@/src/features/admin/lib/adminUi";
import MyEditor from "@/src/shared/components/MyEditor";
import { useToast } from "@/src/shared/hooks/useToast";
import { readBackendError } from "@/src/shared/lib/readBackendError";
import { toUserFriendlyBackendError } from "@/src/shared/lib/userFriendlyBackendError";

type ChallengeStatus = "DRAFT" | "PRIVATE" | "PUBLIC";
type ChallengeDifficulty = "BEGINNER" | "EASY" | "MEDIUM" | "HARD" | "EXPERT";

type AdminChallengeListItem = {
  id: string;
  title: string;
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

const PAGE_SIZE = 10;

export default function AdminChallengeManagement() {
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
    items: challenges,
    totalPages,
    loading,
    error,
    setError,
    refresh,
  } = useAdminPagedList<AdminChallengeListItem>({
    endpoint: "/api/backend/api/admin/challenges",
    label: "labs",
    pageSize: PAGE_SIZE,
    sort: "updatedAt,desc",
  });

  const [editOpened, setEditOpened] = useState(false);
  const [deleteOpened, setDeleteOpened] = useState(false);
  const [selected, setSelected] = useState<AdminChallengeListItem | null>(null);
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
      maxScore: 0,
    },
    validate: {
      title: (v) => (v.trim().length === 0 ? "Title is required" : null),
      maxScore: (v) => (v < 0 ? "Max score must be >= 0" : null),
    },
  });

  const selectedTitle = useMemo(() => selected?.title ?? "", [selected]);

  function openEdit(challenge: AdminChallengeListItem) {
    setSelected(challenge);
    form.setValues({
      title: challenge.title ?? "",
      description: challenge.description ?? "<p>Add a description...</p>",
      status: challenge.status ?? "DRAFT",
      difficulty: challenge.difficulty ?? "BEGINNER",
      maxScore: challenge.maxScore ?? 0,
    });
    setEditOpened(true);
  }

  function openDelete(challenge: AdminChallengeListItem) {
    setSelected(challenge);
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
          description: cleanText(values.description),
          status: values.status,
          difficulty: values.difficulty,
          maxScore: values.maxScore,
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
      const res = await fetch(`/api/backend/api/admin/challenges/${selected.id}`, {
        method: "DELETE",
      });
      if (!res.ok) {
        const raw = await readBackendError(res);
        const msg = toUserFriendlyBackendError(raw);
        const color = res.status >= 500 ? "red" : "orange";
        showToast(color, "Failed to delete lab", msg ?? "Please try again.");
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
      showToast("red", "Failed to delete lab", "Please try again.");
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
                  No labs found.
                </Text>
              </Table.Td>
            </Table.Tr>
          ) : (
            challenges.map((c) => (
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
                      color="red"
                      aria-label="Delete lab"
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
            <NumberInput
              label="Max score"
              min={0}
              value={form.values.maxScore}
              onChange={(v) => form.setFieldValue("maxScore", Number(v ?? 0))}
            />

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
        title="Delete Lab"
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
            <Button
              variant="default"
              radius="md"
              onClick={() => setDeleteOpened(false)}
              disabled={saving}
            >
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
