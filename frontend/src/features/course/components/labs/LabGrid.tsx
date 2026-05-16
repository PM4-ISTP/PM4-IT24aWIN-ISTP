"use client";

import {
  Group,
  Pagination,
  SimpleGrid,
  Stack,
  Text,
  ThemeIcon,
} from "@mantine/core";
import { useRouter, useSearchParams } from "next/navigation";
import { IconFlag } from "@tabler/icons-react";
import { LabCard } from "@/src/features/course/components/labs/LabCard";

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

  function handlePageChange(page: number) {
    const params = new URLSearchParams(searchParams.toString());
    params.set("page", String(page));
    router.push(`?${params.toString()}`);
  }

  function openChallenge(id: string) {
    router.push(`/dashboard/instructor/labs/${id}`);
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
      <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="md">
        {labs.map((lab) => (
          <LabCard
            key={lab.id}
            {...lab}
            updatedAt={formatDateTime(lab.updatedAt)}
            onClick={openChallenge}
          />
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
