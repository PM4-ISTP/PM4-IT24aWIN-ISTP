"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  ActionIcon,
  Alert,
  Button,
  Group,
  Loader,
  Modal,
  Pagination,
  Stack,
  Table,
  Text,
  TextInput,
  Title,
} from "@mantine/core";
import { notifications } from "@mantine/notifications";
import { IconSearch, IconTrash } from "@tabler/icons-react";
import { useAdminPagedList } from "@/src/features/admin/hooks/useAdminPagedList";
import { formatDate, wrapTextStyle } from "@/src/features/admin/lib/adminUi";
import { readBackendError } from "@/src/shared/lib/readBackendError";
import { slugify } from "@/src/shared/lib/utils";
import { toUserFriendlyBackendError } from "@/src/shared/lib/userFriendlyBackendError";

type DeleteCheckBlocker = { relation: string; count: number };
type DeleteCheckResponse = { hardDeleteAllowed: boolean; blockers: DeleteCheckBlocker[] };

type AdminCourseListItem = {
  id: string;
  title: string;
  updatedAt: string;
  ownerName: string | null;
  ownerUsername: string | null;
};

type AdminLabListItem = {
  id: string;
  title: string;
  updatedAt: string;
  creatorName: string | null;
  creatorUsername: string | null;
};

const PAGE_SIZE = 10;

async function fetchDeleteCheck(url: string): Promise<DeleteCheckResponse | null> {
  try {
    const res = await fetch(url, { method: "GET" });
    if (!res.ok) return null;
    return (await res.json()) as DeleteCheckResponse;
  } catch {
    return null;
  }
}

function blockersToHint(blockers: DeleteCheckBlocker[]): string {
  if (!blockers || blockers.length === 0) return "";
  return blockers
    .map((b) => (b.count > 1 ? `${b.relation} (${b.count})` : b.relation))
    .join(" • ");
}

export default function AdminRemovedManagement() {
  const {
    query: courseQuery,
    onQueryChange: onCourseQueryChange,
    applyQueryNow: applyCourseQueryNow,
    page: coursePage,
    setPage: setCoursePage,
    items: removedCourses,
    totalPages: courseTotalPages,
    loading: courseLoading,
    error: courseError,
    setError: setCourseError,
    refresh: refreshCourses,
  } = useAdminPagedList<AdminCourseListItem>({
    endpoint: "/api/backend/api/admin/courses/removed",
    label: "removed courses",
    pageSize: PAGE_SIZE,
  });

  const {
    query: labQuery,
    onQueryChange: onLabQueryChange,
    applyQueryNow: applyLabQueryNow,
    page: labPage,
    setPage: setLabPage,
    items: removedLabs,
    totalPages: labTotalPages,
    loading: labLoading,
    error: labError,
    setError: setLabError,
    refresh: refreshLabs,
  } = useAdminPagedList<AdminLabListItem>({
    endpoint: "/api/backend/api/admin/labs/removed",
    label: "removed labs",
    pageSize: PAGE_SIZE,
    sort: "updatedAt,desc",
  });

  const [courseChecks, setCourseChecks] = useState<Record<string, DeleteCheckResponse>>({});
  const [labChecks, setLabChecks] = useState<Record<string, DeleteCheckResponse>>({});

  const [deleteOpened, setDeleteOpened] = useState(false);
  const [deleteKind, setDeleteKind] = useState<"course" | "lab">("course");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [selectedTitle, setSelectedTitle] = useState<string>("");
  const [saving, setSaving] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const showToast = useCallback((color: "red" | "orange", title: string, message: string) => {
    notifications.show({
      id: `admin-removed:${color}:${slugify(title)}`,
      color,
      title,
      message,
    });
  }, []);

  useEffect(() => {
    if (!courseError) return;
    const message = courseError.replace(/^Failed to load removed courses\\.?\\s*/i, "").trim();
    showToast("red", "Failed to load removed courses", message || "Please try again.");
    setCourseError(null);
  }, [courseError, setCourseError, showToast]);

  useEffect(() => {
    if (!labError) return;
    const message = labError.replace(/^Failed to load removed labs\\.?\\s*/i, "").trim();
    showToast("red", "Failed to load removed labs", message || "Please try again.");
    setLabError(null);
  }, [labError, setLabError, showToast]);

  const removedCourseIds = useMemo(
    () => removedCourses.map((c) => c.id).filter(Boolean),
    [removedCourses]
  );
  const removedLabIds = useMemo(() => removedLabs.map((l) => l.id).filter(Boolean), [removedLabs]);

  useEffect(() => {
    let cancelled = false;
    async function loadChecks() {
      const entries = await Promise.all(
        removedCourseIds.map(async (id) => [id, await fetchDeleteCheck(`/api/backend/api/admin/delete-check/course/${id}`)] as const)
      );
      if (cancelled) return;
      setCourseChecks((prev) => {
        const next = { ...prev };
        entries.forEach(([id, check]) => {
          if (check) next[id] = check;
        });
        return next;
      });
    }
    if (removedCourseIds.length > 0) void loadChecks();
    return () => {
      cancelled = true;
    };
  }, [removedCourseIds]);

  useEffect(() => {
    let cancelled = false;
    async function loadChecks() {
      const entries = await Promise.all(
        removedLabIds.map(async (id) => [id, await fetchDeleteCheck(`/api/backend/api/admin/delete-check/lab/${id}`)] as const)
      );
      if (cancelled) return;
      setLabChecks((prev) => {
        const next = { ...prev };
        entries.forEach(([id, check]) => {
          if (check) next[id] = check;
        });
        return next;
      });
    }
    if (removedLabIds.length > 0) void loadChecks();
    return () => {
      cancelled = true;
    };
  }, [removedLabIds]);

  function openHardDelete(kind: "course" | "lab", id: string, title: string) {
    setDeleteKind(kind);
    setSelectedId(id);
    setSelectedTitle(title || (kind === "course" ? "this course" : "this lab"));
    setDeleteError(null);
    setDeleteOpened(true);
  }

  async function confirmHardDelete() {
    if (!selectedId) return;
    setSaving(true);
    setDeleteError(null);
    try {
      const endpoint =
        deleteKind === "course"
          ? `/api/backend/api/admin/courses/${selectedId}`
          : `/api/backend/api/admin/labs/${selectedId}`;
      const res = await fetch(endpoint, { method: "DELETE" });
      if (!res.ok) {
        const raw = await readBackendError(res);
        const msg = toUserFriendlyBackendError(raw);
        setDeleteError(msg ?? "Please try again.");
        return;
      }
      setDeleteOpened(false);
      setSelectedId(null);
      if (deleteKind === "course") {
        if (coursePage !== 0) setCoursePage(0);
        else refreshCourses();
      } else {
        if (labPage !== 0) setLabPage(0);
        else refreshLabs();
      }
    } catch {
      setDeleteError("Please try again.");
    } finally {
      setSaving(false);
    }
  }

  const selectedCheck = useMemo(() => {
    if (!selectedId) return null;
    return deleteKind === "course" ? courseChecks[selectedId] : labChecks[selectedId];
  }, [courseChecks, labChecks, deleteKind, selectedId]);

  return (
    <Stack gap="xl">
      <Alert color="orange" title="Hard delete rules" variant="light">
        <Text size="sm">
          Hard delete is only available when no related database dependencies exist. If hard delete
          is disabled, related database records must be removed first.
        </Text>
      </Alert>

      <Modal
        opened={deleteOpened}
        onClose={() => setDeleteOpened(false)}
        title={deleteKind === "course" ? "Hard Delete Course" : "Hard Delete Lab"}
        centered
      >
        <Stack gap="md">
          <Text size="sm">
            Permanently delete <strong>{selectedTitle}</strong>? This cannot be undone.
          </Text>
          {selectedCheck && !selectedCheck.hardDeleteAllowed ? (
            <Alert color="orange" title="Hard delete is blocked" variant="light">
              <Text size="sm">Hard delete is blocked because related data still exists.</Text>
              {selectedCheck.blockers?.length ? (
                <Text size="sm" mt={6}>
                  {blockersToHint(selectedCheck.blockers)}
                </Text>
              ) : null}
            </Alert>
          ) : null}
          {deleteError ? (
            <Alert color="red" title="Could not hard-delete" variant="light">
              {deleteError}
            </Alert>
          ) : null}
          <Group justify="flex-end">
            <Button variant="default" onClick={() => setDeleteOpened(false)} disabled={saving}>
              Cancel
            </Button>
            <Button
              color="red"
              onClick={() => void confirmHardDelete()}
              loading={saving}
              disabled={saving || (selectedCheck ? !selectedCheck.hardDeleteAllowed : false)}
            >
              Hard Delete
            </Button>
          </Group>
        </Stack>
      </Modal>

      <Stack gap="md">
        <Group justify="space-between" align="flex-end" wrap="wrap">
          <div>
            <Title order={3} size="h4">
              Soft-deleted Courses
            </Title>
            <Text size="sm" c="dimmed">
              Courses removed by instructors (soft delete).
            </Text>
          </div>
          <Group gap="sm" wrap="wrap">
            <TextInput
              label="Search"
              placeholder="Title / description..."
              leftSection={<IconSearch size={16} />}
              value={courseQuery}
              onChange={(e) => onCourseQueryChange(e.currentTarget.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  applyCourseQueryNow();
                }
              }}
              w={420}
            />
            {courseLoading && (
              <Group gap="xs">
                <Loader size="sm" />
                <Text size="sm" c="dimmed">
                  Loading
                </Text>
              </Group>
            )}
          </Group>
        </Group>

        <Table highlightOnHover withTableBorder striped={false} style={{ tableLayout: "fixed" }}>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Title</Table.Th>
              <Table.Th style={{ width: 240 }}>Owner</Table.Th>
              <Table.Th style={{ width: 190 }}>Updated</Table.Th>
              <Table.Th style={{ width: 220 }} />
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {removedCourses.length === 0 ? (
              <Table.Tr>
                <Table.Td colSpan={4}>
                  <Text size="sm" c="dimmed" ta="center" py="md">
                    No removed courses found.
                  </Text>
                </Table.Td>
              </Table.Tr>
            ) : (
              removedCourses.map((c) => {
                const check = courseChecks[c.id];
                const blocked = check ? !check.hardDeleteAllowed : true;
                return (
                  <Table.Tr key={c.id}>
                    <Table.Td>
                      <Text fw={600} size="sm" lineClamp={2} style={wrapTextStyle} title={c.title}>
                        {c.title}
                      </Text>
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
                      <Text size="sm">{formatDate(c.updatedAt)}</Text>
                    </Table.Td>
                    <Table.Td>
                      <Stack gap={6} align="flex-end">
                        <ActionIcon
                          variant="filled"
                          color="red"
                          aria-label="Hard delete course"
                          onClick={() => openHardDelete("course", c.id, c.title)}
                          disabled={blocked}
                        >
                          <IconTrash size={16} />
                        </ActionIcon>
                        {blocked && check?.blockers?.length ? (
                          <Text size="xs" c="orange" ta="right">
                            Hard delete is blocked because related data still exists.
                            <br />
                            {blockersToHint(check.blockers)}
                          </Text>
                        ) : blocked ? (
                          <Text size="xs" c="orange" ta="right">
                            Hard delete is blocked because related data still exists.
                          </Text>
                        ) : null}
                      </Stack>
                    </Table.Td>
                  </Table.Tr>
                );
              })
            )}
          </Table.Tbody>
        </Table>

        {courseTotalPages > 1 && (
          <Group justify="center">
            <Pagination
              radius="md"
              total={courseTotalPages}
              value={coursePage + 1}
              onChange={(v) => setCoursePage(v - 1)}
            />
          </Group>
        )}
      </Stack>

      <Stack gap="md">
        <Group justify="space-between" align="flex-end" wrap="wrap">
          <div>
            <Title order={3} size="h4">
              Soft-deleted Labs
            </Title>
            <Text size="sm" c="dimmed">
              Labs removed by instructors (soft delete).
            </Text>
          </div>
          <Group gap="sm" wrap="wrap">
            <TextInput
              label="Search"
              placeholder="Title / description..."
              leftSection={<IconSearch size={16} />}
              value={labQuery}
              onChange={(e) => onLabQueryChange(e.currentTarget.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  applyLabQueryNow();
                }
              }}
              w={420}
            />
            {labLoading && (
              <Group gap="xs">
                <Loader size="sm" />
                <Text size="sm" c="dimmed">
                  Loading
                </Text>
              </Group>
            )}
          </Group>
        </Group>

        <Table highlightOnHover withTableBorder striped={false} style={{ tableLayout: "fixed" }}>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Title</Table.Th>
              <Table.Th style={{ width: 240 }}>Creator</Table.Th>
              <Table.Th style={{ width: 190 }}>Updated</Table.Th>
              <Table.Th style={{ width: 220 }} />
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {removedLabs.length === 0 ? (
              <Table.Tr>
                <Table.Td colSpan={4}>
                  <Text size="sm" c="dimmed" ta="center" py="md">
                    No removed labs found.
                  </Text>
                </Table.Td>
              </Table.Tr>
            ) : (
              removedLabs.map((l) => {
                const check = labChecks[l.id];
                const blocked = check ? !check.hardDeleteAllowed : true;
                return (
                  <Table.Tr key={l.id}>
                    <Table.Td>
                      <Text fw={600} size="sm" lineClamp={2} style={wrapTextStyle} title={l.title}>
                        {l.title}
                      </Text>
                    </Table.Td>
                    <Table.Td>
                      <Stack gap={2}>
                        <Text
                          size="sm"
                          lineClamp={1}
                          style={wrapTextStyle}
                          title={l.creatorName ?? "-"}
                        >
                          {l.creatorName ?? "-"}
                        </Text>
                        <Text
                          size="xs"
                          c="dimmed"
                          lineClamp={1}
                          style={wrapTextStyle}
                          title={l.creatorUsername ? `@${l.creatorUsername}` : ""}
                        >
                          {l.creatorUsername ? `@${l.creatorUsername}` : ""}
                        </Text>
                      </Stack>
                    </Table.Td>
                    <Table.Td>
                      <Text size="sm">{formatDate(l.updatedAt)}</Text>
                    </Table.Td>
                    <Table.Td>
                      <Stack gap={6} align="flex-end">
                        <ActionIcon
                          variant="filled"
                          color="red"
                          aria-label="Hard delete lab"
                          onClick={() => openHardDelete("lab", l.id, l.title)}
                          disabled={blocked}
                        >
                          <IconTrash size={16} />
                        </ActionIcon>
                        {blocked && check?.blockers?.length ? (
                          <Text size="xs" c="orange" ta="right">
                            Hard delete is blocked because related data still exists.
                            <br />
                            {blockersToHint(check.blockers)}
                          </Text>
                        ) : blocked ? (
                          <Text size="xs" c="orange" ta="right">
                            Hard delete is blocked because related data still exists.
                          </Text>
                        ) : null}
                      </Stack>
                    </Table.Td>
                  </Table.Tr>
                );
              })
            )}
          </Table.Tbody>
        </Table>

        {labTotalPages > 1 && (
          <Group justify="center">
            <Pagination
              radius="md"
              total={labTotalPages}
              value={labPage + 1}
              onChange={(v) => setLabPage(v - 1)}
            />
          </Group>
        )}
      </Stack>
    </Stack>
  );
}

