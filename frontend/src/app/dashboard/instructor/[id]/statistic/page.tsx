import { ActionIcon, Alert, Button, Group, Stack, Text, Title } from "@mantine/core";
import Link from "next/link";

import { fetchChallengeStatisticsOfCourse } from "@/src/features/course/actions/courses";
import { CourseChallengeStatisticsList } from "@/src/features/course/components/management/CourseChallengeStatisticsList";
import { IconArrowLeft } from "@tabler/icons-react";

export const dynamic = "force-dynamic";

export default async function CourseStatistic({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const backHref = "/dashboard/instructor/" + id;
  const result = await fetchChallengeStatisticsOfCourse(id);

  return (
    <Stack p="xl" gap="lg">
      <Group justify="space-between" align="flex-end">
        <Group>
          <Link href={backHref}>
            <ActionIcon variant="subtle" size="lg" aria-label="Back to edit course page">
              <IconArrowLeft size={20} />
            </ActionIcon>
          </Link>
          <Stack gap={4}>
            <Title
              order={1}
              size="h2"
              style={{
                color: "#f1f5f9",
                fontFamily: "var(--font-space-grotesk), sans-serif",
                fontWeight: 700,
              }}
            >
              Course Statistics
            </Title>
            <Text size="sm" style={{ color: "#94a3b8" }}>
              Overview of how often each challenge has been solved.
            </Text>
          </Stack>
        </Group>
      </Group>

      {result.success ? (
        <CourseChallengeStatisticsList
          title="All challenges"
          statistics={result.data.statistics ?? []}
        />
      ) : (
        <Alert color="red" title="Failed to load challenge statistics">
          {result.error}
        </Alert>
      )}
    </Stack>
  );
}
