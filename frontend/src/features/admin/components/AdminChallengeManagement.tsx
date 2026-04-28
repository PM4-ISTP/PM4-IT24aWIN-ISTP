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
  Textarea,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { IconPencil, IconSearch, IconTrash, IconX } from "@tabler/icons-react";
import { useAdminPagedList } from "@/src/features/admin/hooks/useAdminPagedList";
import { cleanText, formatDate, wrapTextStyle } from "@/src/features/admin/lib/adminUi";
import MyEditor from "@/src/shared/components/MyEditor";
import { useToast } from "@/src/shared/hooks/useToast";
import { readBackendError } from "@/src/shared/lib/readBackendError";
import { CHALLENGE_SHORT_DESCRIPTION_MAX_CHARS } from "@/src/features/course/constants/challengeConstants";
import { normalizeShortDescription } from "@/src/features/course/utils/courseText";
import { toUserFriendlyBackendError } from "@/src/shared/lib/userFriendlyBackendError";

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
    label: "challenges",
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
    const message = error.replace(/^Failed to load challenges\.\s*/i, "").trim();
    showToast("red", "Failed to load challenges", message || "Please try again.");
    setError(null);
  }, [error, setError, showToast]);

  const form = useForm({
    initialValues: {
      title: "",
      shortDescription: "",
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
      shortDescription: challenge.shortDescription ?? "",
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
      const normalizedShortDescription = normalizeShortDescription(values.shortDescription);
      const res = await fetch(`/api/backend/api/admin/challenges/${selected.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: values.title.trim(),
          shortDescription: cleanText(normalizedShortDescription),
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
        showToast(color, "Failed to update challenge", msg ?? "Please try again.");
        return;
      }
      setEditOpened(false);
      setSelected(null);
      refresh();
    } catch {
      showToast("red", "Failed to update challenge", "Please try again.");
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
        showToast(color, "Failed to delete challenge", msg ?? "Please try again.");
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
      showToast("red", "Failed to delete challenge", "Please try again.");
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
              value={form.values.shortDescription}
              onChange={(e) => {
                const next = e.currentTarget.value;
                if (next.length > CHALLENGE_SHORT_DESCRIPTION_MAX_CHARS) {
                  showToast(
                    "orange",
                    "Character limit reached",
                    `Short description cannot exceed ${CHALLENGE_SHORT_DESCRIPTION_MAX_CHARS} characters.`
                  );
                  return;
                }
                form.setFieldValue("shortDescription", next);
              }}
              description={`${form.values.shortDescription.length}/${CHALLENGE_SHORT_DESCRIPTION_MAX_CHARS} characters.`}
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
