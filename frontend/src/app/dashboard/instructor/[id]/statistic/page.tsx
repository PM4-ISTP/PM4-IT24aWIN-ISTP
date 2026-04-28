import { ActionIcon, Alert, Group, Stack, Text, Title } from "@mantine/core";
import Link from "next/link";

import {
  fetchChallengeStatisticsOfCourse,
  fetchCourse,
} from "@/src/features/course/actions/courses";
import { CourseChallengeStatisticsList } from "@/src/features/course/components/management/CourseChallengeStatisticsList";
import { IconArrowLeft } from "@tabler/icons-react";

export const dynamic = "force-dynamic";

async function getPageTitle(courseId: string) {
  let pageTitle = "Course Statistics";
  try {
    const courseResult = await fetchCourse(courseId);
    const courseTitle = courseResult.success ? courseResult.data.title : undefined;
    if (courseTitle !== undefined) {
      pageTitle = pageTitle + ": " + courseTitle;
    }
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  } catch (error) {
    // do nothing. The title is not important. It is ok, to not show it
  }
  return pageTitle;
}

export default async function CourseStatistic({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const backHref = "/dashboard/instructor/" + id;
  const challengesResult = await fetchChallengeStatisticsOfCourse(id);
  const pageTitle = await getPageTitle(id);

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
              {pageTitle}
            </Title>
            <Text size="sm" style={{ color: "#94a3b8" }}>
              Overview of how often each challenge has been solved.
            </Text>
          </Stack>
        </Group>
      </Group>

      {challengesResult.success ? (
        <CourseChallengeStatisticsList
          title="All challenges"
          statistics={challengesResult.data.statistics ?? []}
        />
      ) : (
        <Alert color="red" title="Failed to load challenge statistics">
          {challengesResult.error}
        </Alert>
      )}
    </Stack>
  );
}
