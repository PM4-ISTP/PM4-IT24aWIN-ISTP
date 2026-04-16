import { Alert, Button, Group, Stack, Text, Title } from "@mantine/core";
import { IconPlus } from "@tabler/icons-react";
import Link from "next/link";
import { fetchInstructorChallenges } from "@/src/features/course/actions/challenges";
import { ChallengeGrid } from "@/src/features/course/components/ChallengeGrid";

export default async function InstructorChallenges(props: {
  searchParams: Promise<{ page?: string }>;
}) {
  const searchParams = await props.searchParams;
  const currentPage = Math.max(1, parseInt(searchParams.page ?? "1"));
  const result = await fetchInstructorChallenges(currentPage - 1, 12);

  return (
    <Stack p="xl" gap="lg">
      <Group justify="space-between" align="flex-end">
        <Stack gap={4}>
          <Title order={1} size="h2">
            Challenges
          </Title>
          <Text size="sm" c="dimmed">
            Manage or create your challenges here.
          </Text>
        </Stack>
        <Link href="/dashboard/instructor/challenges/create">
          <Button leftSection={<IconPlus size={16} />}>New Challenge</Button>
        </Link>
      </Group>

      {result.success ? (
        <ChallengeGrid
          challenges={(result.data.content ?? []).map((c) => ({
            id: c.id ?? "",
            title: c.title ?? "",
            shortDescription: c.shortDescription ?? undefined,
            status: c.status ?? "DRAFT",
            difficulty: c.difficulty ?? "MEDIUM",
            maxScore: c.maxScore ?? 0,
            courseCount: c.courseCount ?? 0,
            updatedAt: c.updatedAt ?? "",
          }))}
          totalPages={result.data.totalPages ?? 0}
          currentPage={currentPage}
        />
      ) : (
        <Alert color="red" title="Failed to load challenges">
          {result.error}
        </Alert>
      )}
    </Stack>
  );
}
