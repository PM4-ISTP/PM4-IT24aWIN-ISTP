import { getServerSession } from "next-auth";
import { getToken } from "next-auth/jwt";
import { authOptions } from "@/src/shared/lib/auth";
import { cookies, headers } from "next/headers";
import type { GetTokenParams } from "next-auth/jwt";
import {
  Alert,
  Badge,
  Box,
  Button,
  Grid,
  GridCol,
  Group,
  Stack,
  Text,
  ThemeIcon,
} from "@mantine/core";
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
import { CourseGrid } from "@/src/features/course/components/course/CourseGrid";
import { fetchEnrolledCoursesOfLoggedInUser } from "@/src/features/course/actions/courses";
import Link from "next/link";

const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

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

async function getAccessToken(): Promise<string | null> {
  // Trigger NextAuth callbacks (incl. refresh logic) before reading the JWT.
  // The access token is intentionally not exposed on the Session object.
  await getServerSession(authOptions);

  const req_ = {
    headers: Object.fromEntries((await headers()).entries()),
    cookies: Object.fromEntries((await cookies()).getAll().map((c) => [c.name, c.value])),
  };

  const token = await getToken({
    req: req_ as GetTokenParams["req"],
    secret: process.env.NEXTAUTH_SECRET,
  });

  return typeof token?.accessToken === "string" && token.accessToken.length > 0
    ? token.accessToken
    : null;
}

async function fetchCompletedLabsCount(): Promise<number | null> {
  try {
    const accessToken = await getAccessToken();
    if (!accessToken) return null;

    const res = await fetch(`${BACKEND_URL}/api/v1/labs/my-completed-count`, {
      cache: "no-store",
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    if (!res.ok) return null;
    const json = (await res.json()) as { count?: number };
    return typeof json.count === "number" ? json.count : null;
  } catch {
    return null;
  }
}

async function fetchMyDeadlines(): Promise<DeadlineItem[]> {
  try {
    const accessToken = await getAccessToken();
    if (!accessToken) return [];

    const res = await fetch(`${BACKEND_URL}/api/v1/courses/my-deadlines`, {
      cache: "no-store",
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    if (!res.ok) return [];
    const json = (await res.json()) as Array<{
      courseId?: string;
      courseTitle?: string;
      labId?: string;
      labTitle?: string;
      dueAt?: string;
    }>;
    return (json ?? [])
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
    const accessToken = await getAccessToken();
    if (!accessToken) return [];

    const res = await fetch(`${BACKEND_URL}/api/v1/lab-pods`, {
      cache: "no-store",
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    if (!res.ok) return [];
    const json = (await res.json()) as RunningPod[];
    return Array.isArray(json) ? json : [];
  } catch {
    return [];
  }
}

const sectionLabelStyle: React.CSSProperties = {
  fontFamily: "var(--font-space-grotesk), sans-serif",
  textTransform: "uppercase",
  letterSpacing: "0.1em",
  fontSize: "0.72rem",
  fontWeight: 700,
  color: "rgba(255,255,255,0.45)",
};

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
      <div className="ds-empty-state" style={{ padding: "2rem", width: "100%" }}>
        <ThemeIcon size={44} radius="xl" variant="light" color="gray">
          <IconBolt size={22} />
        </ThemeIcon>
        <Stack gap={4} align="center">
          <Text fw={600} style={{ color: "#e2e8f0" }}>
            No active labs
          </Text>
          <Text size="sm" c="dimmed">
            Enroll in a course to start working on labs.
          </Text>
        </Stack>
      </div>
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
            <Box
              style={{
                background: "rgba(255,255,255,0.04)",
                border: "1px solid rgba(255,255,255,0.08)",
                borderRadius: 10,
                padding: "1rem",
                height: "100%",
              }}
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
            </Box>
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
            {/* Upcoming deadlines */}
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
                <Group justify="space-between" align="center">
                  <Text style={{ ...sectionLabelStyle, alignSelf: "flex-start" }}>
                    Upcoming Deadlines
                  </Text>
                  <IconClock size={16} color="rgba(255,255,255,0.35)" />
                </Group>

                <DeadlineWidget deadlines={deadlines} userId={userId} />
              </Stack>
            </Box>
          </Stack>
        </GridCol>
      </Grid>

      <Stack gap="sm" align="flex-start">
        <Text style={{ ...sectionLabelStyle, alignSelf: "flex-start" }}>Active Labs</Text>
        <RunningLabs pods={runningPods} />
      </Stack>
    </Stack>
  );
}
