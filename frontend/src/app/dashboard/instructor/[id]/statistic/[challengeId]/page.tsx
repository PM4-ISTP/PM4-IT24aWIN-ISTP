import { ActionIcon, Group, Stack, Text, Title } from "@mantine/core";
import Link from "next/link";

import { IconArrowLeft } from "@tabler/icons-react";

import {
  fetchCourse,
  fetchChallengeProgressesForCourse,
} from "@/src/features/course/actions/courses";
import ToastOnMount from "@/src/shared/components/ToastOnMount";
import { toUserFriendlyBackendError } from "@/src/shared/lib/userFriendlyBackendError";

import { ChallengeProgressList } from "@/src/features/course/components/course/ChallengeProgressList";

export const dynamic = "force-dynamic";

async function getPageTitle(courseId: string) {
  let pageTitle = "Challenge Progress";

  try {
    const courseResult = await fetchCourse(courseId);
    const courseTitle = courseResult.success ? courseResult.data.title : undefined;
    if (courseTitle !== undefined) {
      pageTitle = pageTitle + ": " + courseTitle;
    }
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
  } catch (error) {
    // Do nothing. The title is not important. It is ok, to not show it
  }

  return pageTitle;
}

export default async function ChallengeProgressPage(props: {
  params: Promise<{ id: string; challengeId: string }>;
  searchParams: Promise<{ page?: string; query?: string }>;
}) {
  const { id, challengeId } = await props.params;
  const searchParams = await props.searchParams;
  const parsedPage = Number.parseInt(searchParams.page ?? "1", 10);
  const currentPage = Number.isNaN(parsedPage) ? 1 : Math.max(1, parsedPage);
  const query = searchParams.query ?? "";
  const backHref = "/dashboard/instructor/" + id + "/statistic";

  const [progressResult, pageTitle] = await Promise.all([
    fetchChallengeProgressesForCourse(challengeId, id, query, currentPage - 1, 12),
    getPageTitle(id),
  ]);

  return (
    <Stack p="xl" gap="lg">
      <Group justify="space-between" align="flex-end">
        <Group>
          <Link href={backHref}>
            <ActionIcon variant="subtle" size="lg" aria-label="Back to course statistics">
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
              Overview of which participants solved which subtasks for this challenge.
            </Text>
          </Stack>
        </Group>
      </Group>

      {progressResult.success ? (
        <ChallengeProgressList
          progresses={progressResult.data.content ?? []}
          query={query}
          currentPage={currentPage}
          totalPages={progressResult.data.totalPages ?? 1}
        />
      ) : (
        <ToastOnMount
          color="red"
          title="Failed to load challenge progress"
          message={toUserFriendlyBackendError(progressResult.error) ?? "Please try again later."}
        />
      )}
    </Stack>
  );
}
