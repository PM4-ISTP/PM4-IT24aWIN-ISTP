"use client";

import { useCallback, useEffect, useState } from "react";
import {
  Badge,
  Box,
  Group,
  Loader,
  Modal,
  Pagination,
  Stack,
  Text,
  TextInput,
  UnstyledButton,
} from "@mantine/core";
import { useDebouncedCallback } from "@mantine/hooks";
import { IconCheck, IconSearch } from "@tabler/icons-react";
import {
  searchChallenges,
  type ListLabResponseDto,
} from "@/src/features/course/actions/labs";
import {
  getDifficultyColor,
  getStatusColor,
} from "@/src/features/course/constants/challengeConstants";
import classes from "./LabPickerModal.module.css";

const PAGE_SIZE = 8;

interface ChallengePickerModalProps {
  opened: boolean;
  onClose: () => void;
  addedIds: Set<string>;
  onSelect: (lab: ListLabResponseDto) => void;
}

export function LabPickerModal({
  opened,
  onClose,
  addedIds,
  onSelect,
}: ChallengePickerModalProps) {
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [labs, setLabs] = useState<ListLabResponseDto[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchPage = useCallback(async (q: string, p: number) => {
    setLoading(true);
    setError(null);
    try {
      const result = await searchChallenges(q, p, PAGE_SIZE);
      if (!result.success) {
        setError(result.error);
        return;
      }
      setLabs(result.data.content ?? []);
      setTotalPages(result.data.totalPages ?? 0);
    } catch {
      setError("Failed to load labs");
    } finally {
      setLoading(false);
    }
  }, []);

  // Load initial page when modal opens
  useEffect(() => {
    if (opened) {
      setQuery("");
      setPage(0);
      void fetchPage("", 0);
    }
  }, [opened, fetchPage]);

  const debouncedSearch = useDebouncedCallback((q: string) => {
    setPage(0);
    void fetchPage(q, 0);
  }, 300);

  function handleSearchChange(value: string) {
    setQuery(value);
    debouncedSearch(value);
  }

  function handlePageChange(newPage: number) {
    const zeroIndexed = newPage - 1;
    setPage(zeroIndexed);
    void fetchPage(query, zeroIndexed);
  }

  return (
    <Modal opened={opened} onClose={onClose} title="Add Lab to Course" size="lg" centered>
      <Stack gap="md">
        <TextInput
          placeholder="Search labs..."
          leftSection={<IconSearch size={16} />}
          value={query}
          onChange={(e) => handleSearchChange(e.currentTarget.value)}
          autoFocus
        />

        {loading ? (
          <Group justify="center" p="xl">
            <Loader size="sm" />
          </Group>
        ) : error ? (
          <Text c="red" size="sm" ta="center" p="md">
            {error}
          </Text>
        ) : labs.length === 0 ? (
          <Text c="dimmed" size="sm" ta="center" p="md">
            No labs found.
          </Text>
        ) : (
          <Stack gap="xs">
            {labs.map((lab) => {
              const isAdded = addedIds.has(lab.id ?? "");

              return (
                <UnstyledButton
                  key={lab.id}
                  onClick={() => onSelect(lab)}
                  disabled={isAdded}
                  style={{
                    opacity: isAdded ? 0.6 : 1,
                    cursor: isAdded ? "default" : "pointer",
                  }}
                >
                  <Box className={classes.pickerItem} data-selectable={!isAdded}>
                    <Stack gap={4}>
                      <Group gap="xs" wrap="nowrap">
                        <Text size="sm" fw={500} truncate>
                          {lab.title}
                        </Text>
                        {isAdded && <IconCheck size={16} color="var(--mantine-color-teal-5)" />}
                      </Group>

                      <Group gap="xs">
                        <Badge
                          size="xs"
                          variant="light"
                          color={getStatusColor(lab.status ?? "")}
                        >
                          {lab.status}
                        </Badge>
                        <Badge
                          size="xs"
                          variant="light"
                          color={getDifficultyColor(lab.difficulty ?? "")}
                        >
                          {lab.difficulty}
                        </Badge>
                        <Text size="xs" c="dimmed">
                          by {lab.creatorName}
                        </Text>
                        <Text size="xs" c="dimmed">
                          Score: {lab.maxScore ?? 0}
                        </Text>
                      </Group>
                    </Stack>
                  </Box>
                </UnstyledButton>
              );
            })}
          </Stack>
        )}

        {totalPages > 1 && (
          <Group justify="center">
            <Pagination total={totalPages} value={page + 1} onChange={handlePageChange} size="sm" />
          </Group>
        )}
      </Stack>
    </Modal>
  );
}
