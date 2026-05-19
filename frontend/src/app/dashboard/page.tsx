import { getServerSession } from "next-auth";
import { authOptions } from "@/src/shared/lib/auth";
import { getApiClient } from "@/src/shared/lib/api/server";
import { Alert, Badge, Button, Grid, GridCol, Group, Stack, Text, ThemeIcon } from "@mantine/core";
import {
  IconArrowRight,
  IconBolt,
  IconClock,
  IconExternalLink,
  IconPlayerPlay,
} from "@tabler/icons-react";
import DashboardStyles from "@/src/shared/components/DashboardStyles";
import DashboardHero from "@/src/shared/components/DashboardHero";
import { DeadlineWidget } from "@/src/shared/components/DeadlineWidget";
import SectionLabel from "@/src/shared/components/SectionLabel";
import { SurfaceCard } from "@/src/shared/components/SurfaceCard";
import { EmptyState } from "@/src/shared/components/EmptyState";
import { CourseGrid } from "@/src/features/course/components/course/CourseGrid";
import { fetchEnrolledCoursesOfLoggedInUser } from "@/src/features/course/actions/courses";
import Link from "next/link";

type DeadlineItem = {
  courseId: string;
  courseTitle: string;
  labId: string;
  labTitle: string;
  dueAt: string;
};

type PodStatusEnum = "NOT_FOUND" | "PROVISIONING" | "RUNNING" | "FAILED" | "TERMINATING";

type RunningPod = {
  labId: string;
  labTitle?: string | null;
  courseId?: string | null;
  courseTitle?: string | null;
  pod: {
    status: PodStatusEnum;
    podName?: string | null;
    appUrl?: string | null;
    createdAt?: string | null;
    expiresAt?: string | null;
  };
};

async function fetchCompletedLabsCount(): Promise<number | null> {
  try {
    const client = await getApiClient();
    const { data } = await client.GET("/api/v1/labs/my-completed-count");
    const count = data?.count;
    return typeof count === "number" ? count : null;
  } catch {
    return null;
  }
}

async function fetchMyDeadlines(): Promise<DeadlineItem[]> {
  try {
    const client = await getApiClient();
    const { data } = await client.GET("/api/v1/courses/my-deadlines");
    return (data ?? [])
      .filter((d) => d.courseId && d.labId && d.dueAt)
      .map((d) => ({
        courseId: String(d.courseId),
        courseTitle: String(d.courseTitle ?? ""),
        labId: String(d.labId),
        labTitle: String(d.labTitle ?? ""),
        dueAt: String(d.dueAt),
      }))
      .filter((d) => !Number.isNaN(new Date(d.dueAt).getTime()))
      .slice(0, 8);
  } catch {
    return [];
  }
}

async function fetchMyRunningPods(): Promise<RunningPod[]> {
  try {
    const client = await getApiClient();
    const { data } = await client.GET("/api/v1/lab-pods");
    return (data ?? []) as RunningPod[];
  } catch {
    return [];
  }
}

function podStatusColor(status: PodStatusEnum): string {
  if (status === "RUNNING") return "teal";
  if (status === "PROVISIONING") return "blue";
  if (status === "FAILED") return "red";
  if (status === "TERMINATING") return "orange";
  return "gray";
}

function podStatusLabel(status: PodStatusEnum): string {
  return status
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function formatDateTime(value?: string | null): string | null {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return date.toLocaleString("de-CH", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function RunningLabs({ pods }: { pods: RunningPod[] }) {
  if (pods.length === 0) {
    return (
      <SurfaceCard variant="strong" radius="sm" padding={0} style={{ width: "100%" }}>
        <EmptyState
          icon={
            <ThemeIcon size={44} radius="xl" variant="light" color="gray">
              <IconBolt size={22} />
            </ThemeIcon>
          }
          title="No active labs"
          message="Enroll in a course to start working on labs."
        />
      </SurfaceCard>
    );
  }

  return (
    <Grid style={{ width: "100%" }}>
      {pods.map((item) => {
        const expiresAt = formatDateTime(item.pod.expiresAt);
        const playHref =
          item.courseId && item.labId
            ? `/dashboard/courses/${item.courseId}/labs/${item.labId}/play`
            : null;

        return (
          <GridCol key={item.pod.podName ?? item.labId} span={{ base: 12, md: 6, lg: 4 }}>
            <SurfaceCard
              variant="strong"
              elevation="sm"
              radius="sm"
              padding="1rem"
              style={{ height: "100%" }}
            >
              <Stack gap="sm" h="100%">
                <Group justify="space-between" align="flex-start" wrap="nowrap">
                  <Stack gap={2} style={{ minWidth: 0 }}>
                    <Text fw={700} truncate style={{ color: "#e2e8f0" }}>
                      {item.labTitle ?? "Running lab"}
                    </Text>
                    {item.courseTitle ? (
                      <Text size="sm" c="dimmed" truncate>
                        {item.courseTitle}
                      </Text>
                    ) : null}
                  </Stack>
                  <Badge color={podStatusColor(item.pod.status)} variant="light">
                    {podStatusLabel(item.pod.status)}
                  </Badge>
                </Group>

                {expiresAt ? (
                  <Text size="sm" c="dimmed">
                    Expires {expiresAt}
                  </Text>
                ) : null}

                <Group gap="xs" mt="auto">
                  {playHref ? (
                    <Button
                      component="a"
                      href={playHref}
                      size="xs"
                      variant="light"
                      leftSection={<IconPlayerPlay size={14} />}
                    >
                      Open lab
                    </Button>
                  ) : null}
                  {item.pod.appUrl ? (
                    <Button
                      component="a"
                      href={item.pod.appUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      size="xs"
                      variant="subtle"
                      leftSection={<IconExternalLink size={14} />}
                    >
                      Open pod
                    </Button>
                  ) : null}
                </Group>
              </Stack>
            </SurfaceCard>
          </GridCol>
        );
      })}
    </Grid>
  );
}

export default async function Home() {
  const session = await getServerSession(authOptions);
  const name = session?.user?.name ?? "there";
  const firstName = name.split(" ")[0];
  const userId = session?.userId ?? undefined;
  const result = await fetchEnrolledCoursesOfLoggedInUser(0, 3);
  const [deadlines, completedLabsCount, runningPods] = await Promise.all([
    fetchMyDeadlines(),
    fetchCompletedLabsCount(),
    fetchMyRunningPods(),
  ]);

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

      <DashboardHero
        firstName={firstName}
        dateStr={dateStr}
        enrolledCoursesCount={result.success ? (result.data.totalElements ?? 0) : null}
        completedLabsCount={completedLabsCount}
        userId={userId ?? null}
      />

      {/* Main content row */}
      <Grid gap="md">
        {/* Continue learning */}
        <GridCol span={{ base: 12, md: 8 }}>
          <Stack gap="sm">
            <Group justify="space-between" align="center">
              <SectionLabel>Continue Learning</SectionLabel>
              <Link
                href="/dashboard/courses"
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: 4,
                  color: "#5d6ef0",
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
            {/* Upcoming deadlines */}
            <SurfaceCard variant="strong" elevation="md">
              <Stack gap="sm">
                <Group justify="space-between" align="center">
                  <SectionLabel style={{ alignSelf: "flex-start" }}>
                    Upcoming Deadlines
                  </SectionLabel>
                  <IconClock size={16} color="rgba(255,255,255,0.35)" />
                </Group>

                <DeadlineWidget deadlines={deadlines} userId={userId} />
              </Stack>
            </SurfaceCard>
          </Stack>
        </GridCol>
      </Grid>

      <Stack gap="sm" align="flex-start">
        <SectionLabel style={{ alignSelf: "flex-start" }}>Active Labs</SectionLabel>
        <RunningLabs pods={runningPods} />
      </Stack>
    </Stack>
  );
}
