"use client";

import { Group, Pagination, SimpleGrid, Stack, Text, ThemeIcon } from "@mantine/core";
import { IconFlag } from "@tabler/icons-react";
import { useRouter, useSearchParams } from "next/navigation";
import { ChallengeCard } from "@/src/features/course/components/challenges/ChallengeCard";

interface ChallengeGridProps {
  challenges: Array<{
    id: string;
    title: string;
    shortDescription?: string;
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

export function ChallengeGrid({ challenges, totalPages, currentPage }: ChallengeGridProps) {
  const router = useRouter();
  const searchParams = useSearchParams();

  function handlePageChange(page: number) {
    const params = new URLSearchParams(searchParams.toString());
    params.set("page", String(page));
    router.push(`?${params.toString()}`);
  }

  function openChallenge(id: string) {
    router.push(`/dashboard/instructor/challenges/${id}`);
  }

  if (challenges.length === 0) {
    return (
      <div className="ds-empty-state">
        <ThemeIcon size={56} radius="xl" variant="light" color="indigo">
          <IconFlag size={26} />
        </ThemeIcon>
        <Stack gap={6} align="center">
          <Text fw={600} style={{ color: "#e2e8f0" }}>
            No challenges found
          </Text>
          <Text size="sm" c="dimmed">
            Create your first challenge to get started.
          </Text>
        </Stack>
      </div>
    );
  }

  return (
    <Stack gap="lg">
      <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="md">
        {challenges.map((challenge) => (
          <ChallengeCard
            key={challenge.id}
            {...challenge}
            updatedAt={formatDateTime(challenge.updatedAt)}
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
