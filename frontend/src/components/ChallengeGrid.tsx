"use client";

import { Group, Pagination, SimpleGrid, Stack, Text } from "@mantine/core";
import { useRouter, useSearchParams } from "next/navigation";
import { ChallengeCard } from "@/src/components/ChallengeCard";

interface ChallengeGridProps {
  challenges: Array<{
    id: string;
    title: string;
    status: string;
    difficulty: string;
    maxScore: number;
    creatorName: string;
    courseCount: number;
    updatedAt: string;
  }>;
  totalPages: number;
  currentPage: number;
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
            updatedAt={new Date(challenge.updatedAt).toLocaleDateString("de-CH", {
              day: "numeric",
              month: "short",
              year: "numeric",
            })}
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
