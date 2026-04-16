"use client";

import { Group, Pagination, SimpleGrid, Stack, Text } from "@mantine/core";
import { useRouter, useSearchParams } from "next/navigation";
import { ChallengeCard } from "@/src/features/course/components/ChallengeCard";

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
    return <Text c="dimmed">No challenges found.</Text>;
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
          />
        </Group>
      )}
    </Stack>
  );
}
