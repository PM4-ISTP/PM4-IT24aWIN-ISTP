import { getServerSession } from "next-auth";
import { authOptions } from "@/src/shared/lib/auth";
import { Grid, GridCol, Group, RingProgress, Stack, Text, Box, Alert } from "@mantine/core";
import { IconArrowRight } from "@tabler/icons-react";
import DashboardStyles from "@/src/shared/components/DashboardStyles";
import DashboardHero from "@/src/shared/components/DashboardHero";
import { CourseGrid } from "@/src/features/course/components/course/CourseGrid";
import {
  fetchEnrolledCoursesOfLoggedInUser,
  fetchPublicCourse,
} from "@/src/features/course/actions/courses";
import type {
  PageListCourseResponseDto,
  PublicCourseDetailResponseDto,
} from "@/src/features/course/actions/courses";
import Link from "next/link";
import { CourseChallengeDetailsList } from "@/src/features/course/components/management/CourseChallengeDetailsList";
import type { ActionResult } from "@/src/shared/lib/api/actionResult";

const sectionLabelStyle: React.CSSProperties = {
  fontFamily: "var(--font-space-grotesk), sans-serif",
  textTransform: "uppercase",
  letterSpacing: "0.1em",
  fontSize: "0.72rem",
  fontWeight: 700,
  color: "rgba(255,255,255,0.45)",
};

/**
 * This function is a helper function used to load placeholder data.
 */
async function getFirstCourse(fetchCourseResult: ActionResult<PageListCourseResponseDto>) {
  // TODO: delete this function
  let firstCourse: ActionResult<PublicCourseDetailResponseDto> | undefined = undefined;
  if (
    fetchCourseResult.success &&
    fetchCourseResult.data !== undefined &&
    fetchCourseResult.data.content !== undefined
  ) {
    const firstEnrolledCourse = fetchCourseResult.data.content[0];
    if (firstEnrolledCourse !== undefined && firstEnrolledCourse.id !== undefined) {
      firstCourse = await fetchPublicCourse(firstEnrolledCourse.id);
    }
  }
  return firstCourse;
}

function RunningChallenges({
  fetchCourseResult,
}: {
  fetchCourseResult: ActionResult<PublicCourseDetailResponseDto> | undefined;
}) {
  if (fetchCourseResult === undefined) {
    return <Text>No currently running challenges</Text>;
  } else {
    return (
      <>
        {fetchCourseResult.success ? (
          <CourseChallengeDetailsList
            challenges={fetchCourseResult.data.courseChallenges ?? []}
            title=""
            showIndex={false}
            courseId={fetchCourseResult.data.id}
          />
        ) : (
          <Alert color="red" title="Failed to load challenges">
            {fetchCourseResult.error}
          </Alert>
        )}
      </>
    );
  }
}

export default async function Home() {
  const session = await getServerSession(authOptions);
  const name = session?.user?.name ?? "there";
  const firstName = name.split(" ")[0];
  const result = await fetchEnrolledCoursesOfLoggedInUser(0, 3);

  // TODO: delete when using real data
  const firstCourse = await getFirstCourse(result);

  const today = new Date();
  const dateStr = today.toLocaleDateString("en-GB", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  });

  return (
    <Stack gap="xl">
      <DashboardStyles />

      <DashboardHero firstName={firstName} dateStr={dateStr} />

      {/* Main content row */}
      <Grid gap="md">
        {/* Continue learning */}
        <GridCol span={{ base: 12, md: 8 }}>
          <Stack gap="sm">
            <Group justify="space-between" align="center">
              <Text style={sectionLabelStyle}>Continue Learning</Text>
              <Group gap={4} style={{ cursor: "pointer" }}>
                <Link
                  href="/dashboard/courses"
                  style={{
                    color: "#60a5fa",
                    fontFamily: "var(--font-space-grotesk), sans-serif",
                  }}
                >
                  View all
                </Link>
                <IconArrowRight size={15} color="#60a5fa" />
              </Group>
            </Group>
            {result.success ? (
              <CourseGrid
                courses={result.data.content ?? []}
                totalPages={1}
                currentPage={1}
                coursePathPrefix="/dashboard/courses"
              />
            ) : (
              <Alert color="red" title="Failed to load courses">
                {result.error}
              </Alert>
            )}
          </Stack>
        </GridCol>

        {/* Right column */}
        <GridCol span={{ base: 12, md: 4 }}>
          <Stack gap="md">
            {/* Overall progress */}
            <Box
              style={{
                background: "rgba(255,255,255,0.04)",
                border: "1px solid rgba(255,255,255,0.08)",
                borderRadius: 14,
                padding: "1.5rem",
                boxShadow: "0 4px 24px rgba(0,0,0,0.25)",
              }}
            >
              <Stack gap="sm" align="center">
                <Text style={{ ...sectionLabelStyle, alignSelf: "flex-start" }}>
                  Overall Progress
                </Text>
                <RingProgress
                  size={130}
                  thickness={13}
                  sections={[{ value: 33, color: "#2563eb" }]}
                  label={
                    <Text size="md" fw={700} ta="center" style={{ color: "#60a5fa" }}>
                      33%
                    </Text>
                  }
                />
                <Text size="sm" ta="center" style={{ color: "#64748b" }}>
                  Placeholder — 2 of 6 courses completed
                </Text>
              </Stack>
            </Box>
          </Stack>
        </GridCol>
      </Grid>
      <Stack gap="sm" align="flex-start">
        <Text style={{ ...sectionLabelStyle, alignSelf: "flex-start" }}>
          Currently running Challenges
        </Text>
        <RunningChallenges fetchCourseResult={firstCourse} />
      </Stack>
    </Stack>
  );
}
