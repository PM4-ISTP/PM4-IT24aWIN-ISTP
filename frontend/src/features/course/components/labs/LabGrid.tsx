"use client";

import {
  ActionIcon,
  Alert,
  Button,
  Group,
  Modal,
  Pagination,
  SimpleGrid,
  Stack,
  Text,
  ThemeIcon,
} from "@mantine/core";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { IconFlag, IconTrash } from "@tabler/icons-react";
import { LabCard } from "@/src/features/course/components/labs/LabCard";
import { deleteChallenge } from "@/src/features/course/actions/labs";

interface ChallengeGridProps {
  labs: Array<{
    id: string;
    title: string;
    status: string;
    difficulty: string;
    maxScore: number;
    courseCount: number;
    updatedAt: string;
  }>;
  totalPages: number;
  currentPage: number;
}

function formatDateTime(iso: string): string {
  if (!iso) return "";
  return new Date(iso).toLocaleString("de-CH", {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function LabGrid({ labs, totalPages, currentPage }: ChallengeGridProps) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [removeOpened, setRemoveOpened] = useState(false);
  const [removeError, setRemoveError] = useState<string | null>(null);
  const [removing, setRemoving] = useState(false);
  const [selectedLabId, setSelectedLabId] = useState<string | null>(null);
  const [selectedLabTitle, setSelectedLabTitle] = useState<string>("");

  function handlePageChange(page: number) {
    const params = new URLSearchParams(searchParams.toString());
    params.set("page", String(page));
    router.push(`?${params.toString()}`);
  }

  function openChallenge(id: string) {
    router.push(`/dashboard/instructor/labs/${id}`);
  }

  function openRemove(lab: ChallengeGridProps["labs"][number]) {
    setSelectedLabId(lab.id);
    setSelectedLabTitle(lab.title || "this lab");
    setRemoveError(null);
    setRemoveOpened(true);
  }

  async function confirmRemove() {
    if (!selectedLabId) return;
    setRemoving(true);
    setRemoveError(null);
    const result = await deleteChallenge(selectedLabId);
    setRemoving(false);
    if (!result.success) {
      setRemoveError(result.error);
      return;
    }
    setRemoveOpened(false);
    setSelectedLabId(null);
    router.refresh();
  }

  if (labs.length === 0) {
    return (
      <div className="ds-empty-state">
        <ThemeIcon size={56} radius="xl" variant="light" color="indigo">
          <IconFlag size={26} />
        </ThemeIcon>
        <Stack gap={6} align="center">
          <Text fw={600} style={{ color: "#e2e8f0" }}>
            No labs found
          </Text>
          <Text size="sm" c="dimmed">
            Create your first lab to get started.
          </Text>
        </Stack>
      </div>
    );
  }

  return (
    <Stack gap="lg">
      <Modal opened={removeOpened} onClose={() => setRemoveOpened(false)} title="Remove Lab" centered>
        <Stack gap="md">
          <Text size="sm">
            Remove <strong>{selectedLabTitle}</strong> from instructor dashboards? Students and instructors will no longer see it in active lab lists.
          </Text>
          <Text size="sm" c="dimmed">
            Soft-deleted labs are hidden from active instructor and student lists.
          </Text>
          {removeError ? (
            <Alert color="red" title="Could not remove lab" variant="light">
              {removeError}
            </Alert>
          ) : null}
          <Group justify="flex-end">
            <Button variant="default" onClick={() => setRemoveOpened(false)} disabled={removing}>
              Cancel
            </Button>
            <Button color="red" onClick={() => void confirmRemove()} loading={removing} disabled={removing}>
              Remove
            </Button>
          </Group>
        </Stack>
      </Modal>
      <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="md">
        {labs.map((lab) => (
          <div key={lab.id} style={{ position: "relative" }}>
            <ActionIcon
              variant="filled"
              color="red"
              radius="xl"
              size="md"
              aria-label="Remove lab"
              style={{ position: "absolute", top: 10, right: 10, zIndex: 5 }}
              onClick={(e) => {
                e.preventDefault();
                e.stopPropagation();
                openRemove(lab);
              }}
            >
              <IconTrash size={16} />
            </ActionIcon>
            <LabCard
              {...lab}
              updatedAt={formatDateTime(lab.updatedAt)}
              onClick={openChallenge}
            />
          </div>
        ))}
      </SimpleGrid>

      {totalPages > 1 && (
        <Group justify="center">
          <Pagination
            total={totalPages}
            value={currentPage}
            onChange={handlePageChange}
            size="sm"
            radius="md"
          />
        </Group>
      )}
    </Stack>
  );
}
