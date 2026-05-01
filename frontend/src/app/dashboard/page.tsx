import { getServerSession } from "next-auth";
import { authOptions } from "@/src/shared/lib/auth";
import { Grid, GridCol, Group, Stack, Text, Box, Alert, ThemeIcon } from "@mantine/core";
import { IconArrowRight, IconBolt } from "@tabler/icons-react";
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
    return (
      <div className="ds-empty-state" style={{ padding: "2rem", width: "100%" }}>
        <ThemeIcon size={44} radius="xl" variant="light" color="gray">
          <IconBolt size={22} />
        </ThemeIcon>
        <Stack gap={4} align="center">
          <Text fw={600} style={{ color: "#e2e8f0" }}>
            No active challenges
          </Text>
          <Text size="sm" c="dimmed">
            Enroll in a course to start working on challenges.
          </Text>
        </Stack>
      </div>
    );
  }
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
        <Alert color="red" title="Could not load challenges" variant="light">
          Something went wrong loading your challenges. Please refresh the page.
        </Alert>
      )}
    </>
  );
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
              <Link
                href="/dashboard/courses"
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: 4,
                  color: "#60a5fa",
                  fontFamily: "var(--font-space-grotesk), sans-serif",
                  fontSize: "0.8rem",
                  fontWeight: 600,
                  textDecoration: "none",
                }}
              >
                View all
                <IconArrowRight size={14} />
              </Link>
            </Group>
            {result.success ? (
              <CourseGrid
                courses={result.data.content ?? []}
                totalPages={1}
                currentPage={1}
                coursePathPrefix="/dashboard/courses"
              />
            ) : (
              <Alert color="red" title="Could not load your courses" variant="light">
                Something went wrong. Please try refreshing the page.
              </Alert>
            )}
          </Stack>
        </GridCol>

        {/* Right column */}
        <GridCol span={{ base: 12, md: 4 }}>
          <Stack gap="md">
            {/* Quick stats card */}
            <Box
              style={{
                background: "rgba(255,255,255,0.04)",
                border: "1px solid rgba(255,255,255,0.08)",
                borderRadius: 14,
                padding: "1.5rem",
                boxShadow: "0 4px 24px rgba(0,0,0,0.25)",
              }}
            >
              <Stack gap="sm">
                <Text style={{ ...sectionLabelStyle, alignSelf: "flex-start" }}>Quick Stats</Text>
                <Box
                  style={{
                    background: "rgba(255,255,255,0.02)",
                    border: "1px solid rgba(255,255,255,0.06)",
                    borderRadius: 10,
                    padding: "1rem",
                    textAlign: "center",
                  }}
                >
                  <Text style={{ ...sectionLabelStyle, marginBottom: "0.5rem" }}>
                    Enrolled Courses
                  </Text>
                  <Text fw={700} size="xl" style={{ color: "#e2e8f0", lineHeight: 1.2 }}>
                    {result.success ? (result.data.totalElements ?? 0) : "—"}
                  </Text>
                  <Text size="xs" c="dimmed" mt={4}>
                    courses in progress
                  </Text>
                </Box>
              </Stack>
            </Box>
          </Stack>
        </GridCol>
      </Grid>

      <Stack gap="sm" align="flex-start">
        <Text style={{ ...sectionLabelStyle, alignSelf: "flex-start" }}>Active Challenges</Text>
        <RunningChallenges fetchCourseResult={firstCourse} />
      </Stack>
    </Stack>
  );
}
