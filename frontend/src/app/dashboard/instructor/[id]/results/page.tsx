"use client";

import { useEffect, useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import {
  ActionIcon,
  Alert,
  Avatar,
  Box,
  Container,
  Flex,
  Group,
  Loader,
  Drawer,
  Modal,
  ScrollArea,
  Select,
  SimpleGrid,
  Stack,
  Table,
  Text,
  TextInput,
  Title,
  Tooltip,
} from "@mantine/core";
import {
  IconArrowLeft,
  IconCheck,
  IconClock,
  IconPlayerPlay,
  IconSearch,
  IconTrendingUp,
  IconUsers,
} from "@tabler/icons-react";
import { fetchCourse } from "@/src/features/course/actions/courses";
import { useApiClient } from "@/src/shared/lib/api/client";
import { apiErrorText } from "@/src/shared/lib/api";
import {
  type CourseLabSubmissionEntryDto,
  type CourseLabChallengeSubmissionDetailDto,
  type CourseLabResponseDto,
  type CourseLabSubmissionDetailDto,
  type CourseLabSubmissionsResponseDto,
  type CourseLabSubmissionStatusEnum as SubmissionStatus,
  type CourseParticipantDto,
} from "@/src/shared/types/course";

const statusValues: SubmissionStatus[] = ["NOT_STARTED", "IN_PROGRESS", "SUBMITTED"];

function initials(name: string): string {
  return name
    .split(" ")
    .slice(0, 2)
    .map((w) => w[0]?.toUpperCase() ?? "")
    .join("");
}

function avatarColor(name: string): string {
  const colors = ["blue", "indigo", "violet", "teal", "cyan", "green", "orange", "red"];
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
  return colors[Math.abs(hash) % colors.length];
}

function worstStatus(statuses: SubmissionStatus[]): SubmissionStatus {
  if (statuses.includes("IN_PROGRESS")) return "IN_PROGRESS";
  if (statuses.includes("SUBMITTED")) return "SUBMITTED";
  return "NOT_STARTED";
}

function statusLabel(s: SubmissionStatus): string {
  switch (s) {
    case "SUBMITTED":
      return "Submitted";
    case "IN_PROGRESS":
      return "In Progress";
    default:
      return "Not Started";
  }
}

function statusBadgeStyle(s: SubmissionStatus): React.CSSProperties {
  const map: Record<SubmissionStatus, { bg: string; color: string; border: string }> = {
    SUBMITTED: { bg: "rgba(20,184,166,0.15)", color: "#2dd4bf", border: "rgba(20,184,166,0.3)" },
    IN_PROGRESS: { bg: "rgba(96,165,250,0.15)", color: "#60a5fa", border: "rgba(96,165,250,0.3)" },
    NOT_STARTED: {
      bg: "rgba(148,163,184,0.1)",
      color: "#94a3b8",
      border: "rgba(148,163,184,0.2)",
    },
  };
  const t = map[s];
  return {
    background: t.bg,
    color: t.color,
    border: `1px solid ${t.border}`,
    borderRadius: 999,
    padding: "3px 10px",
    fontSize: "0.72rem",
    fontWeight: 600,
    letterSpacing: "0.02em",
    whiteSpace: "nowrap",
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    justifySelf: "start",
  };
}

function progressColor(s: SubmissionStatus): string {
  switch (s) {
    case "SUBMITTED":
      return "#2dd4bf";
    case "IN_PROGRESS":
      return "#60a5fa";
    default:
      return "#475569";
  }
}

function formatDateTime(value?: string | null): string {
  if (!value) return "-";
  try {
    return new Date(value).toLocaleString("de-CH", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return value;
  }
}

function StatCard({
  label,
  value,
  sub,
  subColor,
  icon,
  progress,
}: {
  label: string;
  value: string | number;
  sub?: string;
  subColor?: string;
  icon: React.ReactNode;
  progress?: number;
}) {
  return (
    <Box
      style={{
        background: "rgba(255,255,255,0.04)",
        border: "1px solid rgba(255,255,255,0.08)",
        borderRadius: 12,
        padding: "1.1rem 1.25rem",
      }}
    >
      <Group justify="space-between" align="center" mb={8}>
        <Text
          size="xs"
          tt="uppercase"
          fw={600}
          style={{ color: "#64748b", letterSpacing: "0.08em" }}
        >
          {label}
        </Text>
        <Box style={{ opacity: 0.9 }}>{icon}</Box>
      </Group>
      <Text fw={800} style={{ color: "#f1f5f9", fontSize: "1.6rem", lineHeight: 1.1 }}>
        {value}
      </Text>
      {sub ? (
        <Text size="xs" style={{ color: subColor ?? "#64748b", marginTop: 4 }}>
          {sub}
        </Text>
      ) : null}
      {typeof progress === "number" ? (
        <Box
          style={{
            height: 6,
            borderRadius: 999,
            background: "rgba(255,255,255,0.07)",
            marginTop: 10,
            overflow: "hidden",
          }}
        >
          <Box
            style={{
              height: "100%",
              width: `${Math.max(0, Math.min(100, progress))}%`,
              background: "#2dd4bf",
              borderRadius: 999,
              transition: "width 0.3s",
            }}
          />
        </Box>
      ) : null}
    </Box>
  );
}

interface ParticipantRow {
  participant: CourseParticipantDto;
  byLabId: Map<string, CourseLabSubmissionEntryDto>;
  solvedChallenges: number;
  totalChallenges: number;
  awardedPoints: number;
  maxPoints: number;
  completionPct: number;
  overallStatus: SubmissionStatus;
  currentLabId: string | null;
  currentLabTitle: string | null;
  currentLabStatus: SubmissionStatus;
  latestStatus: SubmissionStatus;
  latestLabTitle: string | null;
  latestCompletedAt: string | null;
}

export default function CourseResultsPage() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const courseId = params.id;
  const client = useApiClient();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [courseTitle, setCourseTitle] = useState("Course Results");
  const [data, setData] = useState<CourseLabSubmissionsResponseDto | null>(null);
  const [search, setSearch] = useState("");
  const [labFilter, setLabFilter] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<SubmissionStatus | null>(null);
  const [drawerOpened, setDrawerOpened] = useState(false);
  const [activeParticipant, setActiveParticipant] = useState<ParticipantRow | null>(null);
  const [detailsOpened, setDetailsOpened] = useState(false);
  const [activeLabId, setActiveLabId] = useState<string | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);
  const [detail, setDetail] = useState<CourseLabSubmissionDetailDto | null>(null);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        setLoading(true);
        setError(null);
        const [courseResult, subResult] = await Promise.all([
          fetchCourse(courseId),
          client.GET("/api/v1/courses/{id}/submissions", {
            params: { path: { id: courseId } },
          }),
        ]);
        if (courseResult.success) setCourseTitle(courseResult.data.title);
        if (subResult.error || !subResult.data) {
          throw new Error(apiErrorText(subResult.error) ?? "Could not load submissions");
        }
        if (!cancelled) setData(subResult.data as CourseLabSubmissionsResponseDto);
      } catch (e) {
        if (!cancelled) setError((e as Error).message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [client, courseId]);

  const labs = useMemo<CourseLabResponseDto[]>(() => {
    if (!data) return [];
    return [...(data.labs ?? [])].sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0));
  }, [data]);

  const submissionsByParticipant = useMemo(() => {
    const map = new Map<string, CourseLabSubmissionEntryDto[]>();
    if (!data) return map;
    for (const s of data.submissions ?? []) {
      const key = s.participantId;
      const next = map.get(key) ?? [];
      next.push(s);
      map.set(key, next);
    }
    return map;
  }, [data]);

  const labTitleById = useMemo(() => {
    const map = new Map<string, string>();
    for (const l of labs) map.set(l.labId, l.labTitle);
    return map;
  }, [labs]);

  const rows = useMemo<ParticipantRow[]>(() => {
    if (!data) return [];
    return (data.participants ?? []).map((p) => {
      const subs = submissionsByParticipant.get(p.id) ?? [];
      const byLabId = new Map<string, CourseLabSubmissionEntryDto>();
      for (const s of subs) byLabId.set(s.labId, s);

      const effectiveSubs = labFilter
        ? byLabId.get(labFilter)
          ? [byLabId.get(labFilter)!]
          : []
        : subs;
      const solvedChallenges = effectiveSubs.reduce(
        (acc, s) => acc + (s.solvedChallengeCount ?? 0),
        0
      );
      const totalChallenges = effectiveSubs.reduce(
        (acc, s) => acc + (s.totalChallengeCount ?? 0),
        0
      );
      const awardedPoints = effectiveSubs.reduce((acc, s) => acc + (s.awardedPoints ?? 0), 0);
      const maxPoints = effectiveSubs.reduce((acc, s) => acc + (s.maxPoints ?? 0), 0);
      const completionPct =
        totalChallenges > 0 ? Math.round((solvedChallenges / totalChallenges) * 100) : 0;

      const overallStatus = worstStatus(effectiveSubs.map((s) => s.status));
      let currentLabId: string | null = null;
      let currentLabStatus: SubmissionStatus = "NOT_STARTED";
      if (!labFilter) {
        const orderedLabIds = labs.map((l) => l.labId);
        for (const lid of orderedLabIds) {
          const s = byLabId.get(lid);
          const started =
            !!s &&
            ((s.solvedChallengeCount ?? 0) > 0 ||
              (s.awardedPoints ?? 0) > 0 ||
              Boolean(s.completedAt));
          if (started) {
            currentLabId = lid;
            currentLabStatus = s.status;
          }
        }
      } else {
        currentLabId = labFilter;
        currentLabStatus = byLabId.get(labFilter)?.status ?? "NOT_STARTED";
      }

      const latest = [...subs]
        .filter((s) => Boolean(s.completedAt))
        .sort((a, b) => new Date(b.completedAt!).getTime() - new Date(a.completedAt!).getTime())[0];

      return {
        participant: p,
        byLabId,
        solvedChallenges,
        totalChallenges,
        awardedPoints,
        maxPoints,
        completionPct,
        overallStatus,
        currentLabId,
        currentLabTitle: currentLabId ? (labTitleById.get(currentLabId) ?? null) : null,
        currentLabStatus,
        latestStatus: latest?.status ?? "NOT_STARTED",
        latestLabTitle: latest ? (labTitleById.get(latest.labId) ?? null) : null,
        latestCompletedAt: latest?.completedAt ?? null,
      };
    });
  }, [data, submissionsByParticipant, labFilter, labTitleById, labs]);

  const filteredRows = useMemo(() => {
    const q = search.trim().toLowerCase();
    return rows.filter((r) => {
      const matchesSearch =
        q.length === 0 ||
        r.participant.name.toLowerCase().includes(q) ||
        (r.participant.email ?? "").toLowerCase().includes(q);
      const matchesStatus = !statusFilter || r.currentLabStatus === statusFilter;
      return matchesSearch && matchesStatus;
    });
  }, [rows, search, statusFilter]);

  const totalParticipants = rows.length;
  const statsSubmitted = rows.filter((r) => r.overallStatus === "SUBMITTED").length;
  const statsInProg = rows.filter((r) => r.overallStatus === "IN_PROGRESS").length;
  const statsNotStarted = rows.filter((r) => r.overallStatus === "NOT_STARTED").length;
  const avgPct =
    totalParticipants > 0
      ? Math.round(rows.map((r) => r.completionPct).reduce((a, b) => a + b, 0) / totalParticipants)
      : 0;

  const labSelectData = labs.map((l, idx) => ({
    value: l.labId,
    label: `Lab ${String(idx + 1).padStart(2, "0")}: ${l.labTitle}`,
  }));

  async function loadDetail(participantId: string, labId: string) {
    try {
      setDetailLoading(true);
      setDetailError(null);
      const { data, error } = await client.GET(
        "/api/v1/courses/{id}/submissions/{participantId}/{labId}",
        { params: { path: { id: courseId, participantId, labId } } }
      );
      if (error || !data) {
        throw new Error(apiErrorText(error) ?? "Could not load submission details");
      }
      setDetail(data as CourseLabSubmissionDetailDto);
    } catch (e) {
      setDetailError((e as Error).message);
      setDetail(null);
    } finally {
      setDetailLoading(false);
    }
  }

  function openDrawer(row: ParticipantRow) {
    setActiveParticipant(row);
    setDrawerOpened(true);
  }

  async function openDetails(labId: string) {
    if (!activeParticipant) return;
    setActiveLabId(labId);
    setDetailsOpened(true);
    await loadDetail(activeParticipant.participant.id, labId);
  }

  return (
    <Container size="lg" py="xl">
      <Group justify="space-between" align="center" mb="lg">
        <Group gap="sm">
          <ActionIcon variant="subtle" color="gray" onClick={() => router.back()}>
            <IconArrowLeft size={18} />
          </ActionIcon>
          <Stack gap={0}>
            <Title order={2} style={{ color: "#f1f5f9" }}>
              Results Overview
            </Title>
            <Text size="sm" style={{ color: "#64748b" }}>
              {courseTitle}
            </Text>
          </Stack>
        </Group>
        {loading ? <Loader size="sm" /> : null}
      </Group>

      {error ? (
        <Alert color="red" title="Failed to load results">
          {error}
        </Alert>
      ) : null}

      <SimpleGrid cols={{ base: 2, sm: 4 }} mb="xl" spacing="md">
        <StatCard
          label="Average Completion"
          value={`${avgPct}%`}
          sub={labFilter ? "This lab only" : "All labs"}
          icon={<IconTrendingUp size={12} color="#2dd4bf" />}
          progress={avgPct}
        />
        <StatCard
          label="Submitted"
          value={`${statsSubmitted} / ${totalParticipants}`}
          sub="overall submitted"
          icon={<IconCheck size={12} color="#64748b" />}
          progress={
            totalParticipants > 0 ? Math.round((statsSubmitted / totalParticipants) * 100) : 0
          }
        />
        <StatCard
          label="In Progress"
          value={statsInProg}
          sub={`${statsNotStarted} not started`}
          icon={<IconPlayerPlay size={12} color="#64748b" />}
        />
        <StatCard
          label="Participants"
          value={totalParticipants}
          sub="enrolled"
          icon={<IconUsers size={12} color="#64748b" />}
        />
      </SimpleGrid>

      <Box
        style={{
          background: "rgba(255,255,255,0.03)",
          border: "1px solid rgba(255,255,255,0.08)",
          borderRadius: 14,
          overflow: "hidden",
        }}
      >
        {/* Toolbar */}
        <Flex
          direction={{ base: "column", sm: "row" }}
          justify="space-between"
          align={{ base: "stretch", sm: "center" }}
          gap="sm"
          px={{ base: "md", sm: "xl" }}
          py="md"
          style={{ borderBottom: "1px solid rgba(255,255,255,0.07)" }}
        >
          <Group gap="sm" wrap="wrap" style={{ minWidth: 0 }}>
            <Text fw={600} style={{ color: "#f1f5f9", fontSize: "1rem", whiteSpace: "nowrap" }}>
              Participants
            </Text>
            <Select
              placeholder="All labs"
              data={labSelectData}
              value={labFilter}
              onChange={setLabFilter}
              clearable
              size="xs"
              w={{ base: "100%", sm: 260 }}
              styles={{
                input: {
                  background: "rgba(255,255,255,0.05)",
                  border: "1px solid rgba(255,255,255,0.1)",
                  color: "#f1f5f9",
                  fontSize: "0.8rem",
                },
                dropdown: { background: "#0b1220", border: "1px solid rgba(255,255,255,0.1)" },
                option: { color: "#e2e8f0" },
              }}
            />
            <Select
              placeholder="All statuses"
              data={statusValues.map((s) => ({ value: s, label: statusLabel(s) }))}
              value={statusFilter}
              onChange={(v) => setStatusFilter((v as SubmissionStatus) ?? null)}
              clearable
              size="xs"
              w={{ base: "100%", sm: 170 }}
              styles={{
                input: {
                  background: "rgba(255,255,255,0.05)",
                  border: "1px solid rgba(255,255,255,0.1)",
                  color: "#f1f5f9",
                  fontSize: "0.8rem",
                },
                dropdown: { background: "#0b1220", border: "1px solid rgba(255,255,255,0.1)" },
                option: { color: "#e2e8f0" },
              }}
            />
          </Group>
          <TextInput
            placeholder="Search participants..."
            leftSection={<IconSearch size={14} />}
            value={search}
            onChange={(e) => setSearch(e.currentTarget.value)}
            size="xs"
            w={{ base: "100%", sm: 230 }}
            styles={{
              input: {
                background: "rgba(255,255,255,0.05)",
                border: "1px solid rgba(255,255,255,0.1)",
                color: "#f1f5f9",
                fontSize: "0.8rem",
              },
            }}
          />
        </Flex>

        <ScrollArea>
          <Table miw={760} highlightOnHover verticalSpacing="sm" horizontalSpacing="xl">
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Participant</Table.Th>
                <Table.Th>Status</Table.Th>
                <Table.Th>Points / Challenges</Table.Th>
                <Table.Th>Completion</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {loading ? (
                <Table.Tr>
                  <Table.Td colSpan={4}>
                    <Group justify="center" py="xl">
                      <Loader size="sm" />
                    </Group>
                  </Table.Td>
                </Table.Tr>
              ) : filteredRows.length === 0 ? (
                <Table.Tr>
                  <Table.Td colSpan={4}>
                    <Text size="sm" c="dimmed" py="xl" ta="center">
                      No participants found.
                    </Text>
                  </Table.Td>
                </Table.Tr>
              ) : (
                filteredRows.map((r) => {
                  const status = r.currentLabStatus;
                  const currentIndex =
                    r.currentLabId != null ? labs.findIndex((l) => l.labId === r.currentLabId) : -1;
                  const labMeta =
                    r.currentLabId && currentIndex >= 0
                      ? `Lab ${String(currentIndex + 1).padStart(2, "0")}`
                      : null;
                  const currentMeta = labMeta
                    ? `${labMeta}${r.currentLabTitle ? `: ${r.currentLabTitle}` : ""}`
                    : null;
                  return (
                    <Table.Tr
                      key={r.participant.id}
                      style={{ cursor: "pointer" }}
                      onClick={() => openDrawer(r)}
                    >
                      <Table.Td>
                        <Group gap="sm" wrap="nowrap">
                          <Avatar
                            color={avatarColor(r.participant.name)}
                            radius="md"
                            size={36}
                            style={{ fontWeight: 700, fontSize: "0.8rem" }}
                          >
                            {initials(r.participant.name)}
                          </Avatar>
                          <Stack gap={1}>
                            <Text size="sm" fw={600} style={{ color: "#f1f5f9", lineHeight: 1.2 }}>
                              {r.participant.name}
                            </Text>
                            <Text size="xs" style={{ color: "#475569" }}>
                              {r.participant.email ?? "-"}
                            </Text>
                          </Stack>
                        </Group>
                      </Table.Td>

                      <Table.Td>
                        <Tooltip
                          withArrow
                          position="top"
                          label={
                            currentMeta
                              ? `Current: ${statusLabel(status)} · ${currentMeta}`
                              : statusLabel(status)
                          }
                        >
                          <Group gap={10} wrap="nowrap">
                            <span style={{ ...statusBadgeStyle(status), cursor: "help" }}>
                              {statusLabel(status)}
                            </span>
                            {labMeta ? (
                              <Text size="xs" style={{ color: "#475569" }}>
                                {labMeta}
                              </Text>
                            ) : null}
                          </Group>
                        </Tooltip>
                      </Table.Td>

                      <Table.Td>
                        <Stack gap={2}>
                          <Text size="sm" fw={700} style={{ color: "#f1f5f9" }}>
                            {r.awardedPoints}/{r.maxPoints} pts
                          </Text>
                          <Text size="xs" style={{ color: "#475569" }}>
                            {r.solvedChallenges}/{r.totalChallenges} challenges
                          </Text>
                        </Stack>
                      </Table.Td>

                      <Table.Td>
                        <Stack gap={4} miw={140}>
                          <Text size="xs" style={{ color: "#94a3b8" }}>
                            {r.completionPct}%
                          </Text>
                          <Box
                            style={{
                              height: 5,
                              borderRadius: 4,
                              background: "rgba(255,255,255,0.07)",
                              overflow: "hidden",
                            }}
                          >
                            <Box
                              style={{
                                height: "100%",
                                width: `${r.completionPct}%`,
                                background: progressColor(status),
                                borderRadius: 4,
                                transition: "width 0.3s",
                              }}
                            />
                          </Box>
                        </Stack>
                      </Table.Td>
                    </Table.Tr>
                  );
                })
              )}
            </Table.Tbody>
          </Table>
        </ScrollArea>
      </Box>

      <Drawer
        opened={drawerOpened && !!activeParticipant}
        onClose={() => {
          setDrawerOpened(false);
          setDetailsOpened(false);
          setActiveParticipant(null);
          setActiveLabId(null);
          setDetail(null);
          setDetailError(null);
        }}
        title={
          <Group gap="sm">
            {activeParticipant ? (
              <Avatar
                color={avatarColor(activeParticipant.participant.name)}
                radius="md"
                size="sm"
                style={{ fontWeight: 700, fontSize: "0.78rem" }}
              >
                {initials(activeParticipant.participant.name)}
              </Avatar>
            ) : null}
            <Stack gap={0} style={{ minWidth: 0 }}>
              <Text fw={700} size="sm" style={{ color: "#f1f5f9" }}>
                {activeParticipant?.participant.name ?? "Participant"}
              </Text>
              <Text size="xs" style={{ color: "#64748b" }}>
                Lab breakdown
              </Text>
            </Stack>
          </Group>
        }
        position="right"
        size="md"
        styles={{
          content: { background: "#0b1220", border: "1px solid rgba(255,255,255,0.08)" },
          header: { background: "#0b1220", borderBottom: "1px solid rgba(255,255,255,0.08)" },
          body: { paddingTop: 10 },
        }}
      >
        {!activeParticipant ? null : (
          <Stack gap="md">
            <SimpleGrid cols={2} spacing="sm">
              <Box
                style={{
                  background: "rgba(255,255,255,0.04)",
                  border: "1px solid rgba(255,255,255,0.08)",
                  borderRadius: 12,
                  padding: "0.95rem 1rem",
                }}
              >
                <Text size="xs" tt="uppercase" fw={700} style={{ color: "#64748b" }}>
                  Points
                </Text>
                <Text fw={800} style={{ color: "#f1f5f9", fontSize: "1.4rem", marginTop: 6 }}>
                  {activeParticipant.awardedPoints}/{activeParticipant.maxPoints}
                </Text>
              </Box>
              <Box
                style={{
                  background: "rgba(255,255,255,0.04)",
                  border: "1px solid rgba(255,255,255,0.08)",
                  borderRadius: 12,
                  padding: "0.95rem 1rem",
                }}
              >
                <Text size="xs" tt="uppercase" fw={700} style={{ color: "#64748b" }}>
                  Challenges
                </Text>
                <Text fw={800} style={{ color: "#f1f5f9", fontSize: "1.4rem", marginTop: 6 }}>
                  {activeParticipant.solvedChallenges}/{activeParticipant.totalChallenges}
                </Text>
              </Box>
            </SimpleGrid>

            {labs.length === 0 ? (
              <Alert color="gray" title="No labs assigned">
                This course currently has no labs assigned, so there is nothing to grade yet.
              </Alert>
            ) : (
              <Stack gap="sm">
                {labs.map((l, idx) => {
                  const sub = activeParticipant.byLabId.get(l.labId);
                  const status = sub?.status ?? "NOT_STARTED";

                  // Deadline logic
                  const now = new Date();
                  const dueDate = l.dueAt ? new Date(l.dueAt) : null;
                  const deadlinePassed = dueDate ? now > dueDate : false;
                  const hasDeadline = dueDate !== null;

                  let deadlineColor = "#64748b";
                  let deadlineLabel = "";
                  let deadlineBg = "transparent";
                  let deadlineBorder = "transparent";

                  if (hasDeadline) {
                    if (!deadlinePassed) {
                      deadlineColor = "#2dd4bf";
                      deadlineLabel = `Due: ${formatDateTime(l.dueAt)} — still open`;
                      deadlineBg = "rgba(20,184,166,0.07)";
                      deadlineBorder = "rgba(20,184,166,0.2)";
                    } else if (status === "SUBMITTED") {
                      deadlineColor = "#2dd4bf";
                      deadlineLabel = `Due: ${formatDateTime(l.dueAt)} — submitted on time`;
                      deadlineBg = "rgba(20,184,166,0.07)";
                      deadlineBorder = "rgba(20,184,166,0.2)";
                    } else {
                      deadlineColor = "#f87171";
                      deadlineLabel = `Due: ${formatDateTime(l.dueAt)} — expired`;
                      deadlineBg = "rgba(239,68,68,0.08)";
                      deadlineBorder = "rgba(239,68,68,0.25)";
                    }
                  }

                  return (
                    <Box
                      key={l.labId}
                      style={{
                        background: "rgba(255,255,255,0.03)",
                        border: "1px solid rgba(255,255,255,0.08)",
                        borderRadius: 14,
                        padding: "0.95rem 1rem",
                      }}
                    >
                      <Group justify="space-between" align="flex-start" wrap="nowrap">
                        <Stack gap={2} style={{ minWidth: 0 }}>
                          <Text fw={750} style={{ color: "#f1f5f9" }} lineClamp={1}>
                            Lab {String(idx + 1).padStart(2, "0")}: {l.labTitle}
                          </Text>
                          <Group gap={8}>
                            <span style={statusBadgeStyle(status)}>{statusLabel(status)}</span>
                            {sub?.completedAt ? (
                              <Text size="xs" style={{ color: "#475569" }}>
                                {formatDateTime(sub.completedAt)}
                              </Text>
                            ) : null}
                          </Group>
                          <Text size="sm" style={{ color: "#94a3b8", marginTop: 6 }}>
                            {sub?.awardedPoints ?? 0}/{sub?.maxPoints ?? 0} pts ·{" "}
                            {sub?.solvedChallengeCount ?? 0}/{sub?.totalChallengeCount ?? 0}{" "}
                            challenges
                          </Text>

                          {hasDeadline && (
                            <Box
                              style={{
                                marginTop: 8,
                                background: deadlineBg,
                                border: `1px solid ${deadlineBorder}`,
                                borderRadius: 8,
                                padding: "5px 10px",
                                display: "inline-flex",
                                alignItems: "center",
                                gap: 6,
                              }}
                            >
                              <IconClock
                                size={13}
                                color={deadlineColor}
                                style={{ flexShrink: 0 }}
                              />
                              <Text size="xs" fw={600} style={{ color: deadlineColor }}>
                                {deadlineLabel}
                              </Text>
                            </Box>
                          )}
                        </Stack>

                        <ActionIcon
                          variant="subtle"
                          color="gray"
                          onClick={(e) => {
                            e.stopPropagation();
                            void openDetails(l.labId);
                          }}
                          style={{ marginTop: 2 }}
                        >
                          <IconSearch size={16} />
                        </ActionIcon>
                      </Group>
                    </Box>
                  );
                })}
              </Stack>
            )}
          </Stack>
        )}
      </Drawer>

      <Modal
        opened={detailsOpened}
        onClose={() => {
          setDetailsOpened(false);
          setDetail(null);
          setDetailError(null);
        }}
        title={
          <Text fw={700} style={{ color: "#f1f5f9" }}>
            Submission details{activeParticipant ? ` — ${activeParticipant.participant.name}` : ""}
          </Text>
        }
        centered
        size="lg"
        styles={{
          content: { background: "#0b1220", border: "1px solid rgba(255,255,255,0.08)" },
          header: { background: "#0b1220", borderBottom: "1px solid rgba(255,255,255,0.08)" },
          body: { paddingTop: 10 },
        }}
      >
        {!activeParticipant ? null : (
          <Stack gap="md">
            <Group justify="space-between" align="center">
              <Select
                placeholder="Select lab"
                data={labSelectData}
                value={activeLabId}
                onChange={(v) => {
                  const next = (v as string) ?? null;
                  setActiveLabId(next);
                  if (next) void loadDetail(activeParticipant.participant.id, next);
                }}
                w={360}
                styles={{
                  input: {
                    background: "rgba(255,255,255,0.05)",
                    border: "1px solid rgba(255,255,255,0.1)",
                    color: "#f1f5f9",
                  },
                  dropdown: { background: "#0b1220", border: "1px solid rgba(255,255,255,0.1)" },
                  option: { color: "#e2e8f0" },
                }}
              />
              <Text size="sm" style={{ color: "#94a3b8" }}>
                {detail ? `${detail.awardedPoints}/${detail.maxPoints} pts` : ""}
              </Text>
            </Group>

            {detailLoading ? (
              <Group justify="center" py="md">
                <Loader size="sm" />
              </Group>
            ) : detailError ? (
              <Alert color="red" title="Failed to load submission details">
                {detailError}
              </Alert>
            ) : !detail ? (
              <Text size="sm" c="dimmed">
                No lab selected.
              </Text>
            ) : (
              <Stack gap="sm">
                {detail.challenges.map((c: CourseLabChallengeSubmissionDetailDto, idx: number) => {
                  const attempted =
                    Boolean(c.completed) ||
                    Boolean(c.submittedFlag && c.submittedFlag.trim().length > 0) ||
                    Boolean(c.selectedOptionText && c.selectedOptionText.trim().length > 0);

                  const correctLabel = !attempted
                    ? "not attempted"
                    : c.correct === null
                      ? c.completed
                        ? "completed"
                        : "attempted"
                      : c.correct
                        ? "correct"
                        : "wrong";
                  const correctColor = !attempted
                    ? "#94a3b8"
                    : c.correct === null
                      ? "#94a3b8"
                      : c.correct
                        ? "#2dd4bf"
                        : "#f87171";

                  return (
                    <Box
                      key={c.challengeId}
                      style={{
                        background: "rgba(255,255,255,0.03)",
                        border: "1px solid rgba(255,255,255,0.08)",
                        borderRadius: 14,
                        padding: "1rem 1.1rem",
                      }}
                    >
                      <Group justify="space-between" align="flex-start" wrap="nowrap">
                        <Stack gap={2} style={{ minWidth: 0 }}>
                          <Text fw={700} style={{ color: "#f1f5f9" }}>
                            {idx + 1}. {c.title}
                          </Text>
                          <Text size="xs" style={{ color: "#64748b" }}>
                            {c.type} • <span style={{ color: correctColor }}>{correctLabel}</span>
                          </Text>
                          {c.submittedFlag ? (
                            <Text size="sm" style={{ color: "#e2e8f0", marginTop: 6 }}>
                              Submitted flag:{" "}
                              <span style={{ color: "#94a3b8" }}>{c.submittedFlag}</span>
                            </Text>
                          ) : null}
                          {c.selectedOptionText ? (
                            <Text size="sm" style={{ color: "#e2e8f0", marginTop: 6 }}>
                              Selected:{" "}
                              <span style={{ color: "#94a3b8" }}>{c.selectedOptionText}</span>
                            </Text>
                          ) : null}
                        </Stack>

                        <Stack gap={6} align="flex-end" style={{ flexShrink: 0 }}>
                          <Text size="xs" style={{ color: "#64748b" }}>
                            Points
                          </Text>
                          <Text fw={800} style={{ color: "#f1f5f9" }}>
                            {c.awardedPoints ?? 0}/{c.maxPoints ?? 0}
                          </Text>
                        </Stack>
                      </Group>
                    </Box>
                  );
                })}
              </Stack>
            )}
          </Stack>
        )}
      </Modal>
    </Container>
  );
}
