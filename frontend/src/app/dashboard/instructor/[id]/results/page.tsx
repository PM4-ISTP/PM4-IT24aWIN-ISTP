"use client";

import { useEffect, useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import {
  ActionIcon,
  Alert,
  Avatar,
  Box,
  Container,
  Group,
  Loader,
  Modal,
  Select,
  SimpleGrid,
  Stack,
  Text,
  TextInput,
  Title,
  Tooltip,
} from "@mantine/core";
import {
  IconArrowLeft,
  IconCheck,
  IconPlayerPlay,
  IconSearch,
  IconTrendingUp,
  IconUsers,
} from "@tabler/icons-react";
import { fetchCourse } from "@/src/features/course/actions/courses";
import {
  type CourseChallengeSubmissionEntryDto,
  type CourseLabChallengeSubmissionDetailDto,
  type CourseLabResponseDto,
  type CourseLabSubmissionDetailDto,
  type CourseLabSubmissionsResponseDto,
  type CourseLabSubmissionStatusEnum as SubmissionStatus,
  type CourseParticipantDto,
} from "@/src/shared/types/course";

const statusValues: SubmissionStatus[] = ["NOT_SUBMITTED", "IN_PROGRESS", "ON_TIME", "LATE"];

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
  if (statuses.includes("LATE")) return "LATE";
  if (statuses.includes("IN_PROGRESS")) return "IN_PROGRESS";
  if (statuses.includes("ON_TIME")) return "ON_TIME";
  return "NOT_SUBMITTED";
}

function statusLabel(s: SubmissionStatus): string {
  switch (s) {
    case "ON_TIME":
      return "On Time";
    case "LATE":
      return "Late";
    case "IN_PROGRESS":
      return "In Progress";
    default:
      return "Not Started";
  }
}

function statusBadgeStyle(s: SubmissionStatus): React.CSSProperties {
  const map: Record<SubmissionStatus, { bg: string; color: string; border: string }> = {
    ON_TIME: { bg: "rgba(20,184,166,0.15)", color: "#2dd4bf", border: "rgba(20,184,166,0.3)" },
    LATE: { bg: "rgba(239,68,68,0.15)", color: "#f87171", border: "rgba(239,68,68,0.3)" },
    IN_PROGRESS: { bg: "rgba(96,165,250,0.15)", color: "#60a5fa", border: "rgba(96,165,250,0.3)" },
    NOT_SUBMITTED: {
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
    case "ON_TIME":
      return "#2dd4bf";
    case "LATE":
      return "#f87171";
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
  byLabId: Map<string, CourseChallengeSubmissionEntryDto>;
  solvedChallenges: number;
  totalChallenges: number;
  awardedPoints: number;
  maxPoints: number;
  completionPct: number;
  overallStatus: SubmissionStatus;
  latestStatus: SubmissionStatus;
  latestLabTitle: string | null;
  latestCompletedAt: string | null;
}

export default function CourseResultsPage() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const courseId = params.id;

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [courseTitle, setCourseTitle] = useState("Course Results");
  const [data, setData] = useState<CourseLabSubmissionsResponseDto | null>(null);
  const [search, setSearch] = useState("");
  const [labFilter, setLabFilter] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<SubmissionStatus | null>(null);
  const [detailsOpened, setDetailsOpened] = useState(false);
  const [activeParticipant, setActiveParticipant] = useState<ParticipantRow | null>(null);
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
        const [courseResult, subRes] = await Promise.all([
          fetchCourse(courseId),
          fetch(`/api/backend/api/v1/courses/${encodeURIComponent(courseId)}/submissions`, {
            cache: "no-store",
          }),
        ]);
        if (courseResult.success) setCourseTitle(courseResult.data.title);
        if (!subRes.ok) throw new Error((await subRes.text()) || subRes.statusText);
        const json = (await subRes.json()) as CourseLabSubmissionsResponseDto;
        if (!cancelled) setData(json);
      } catch (e) {
        if (!cancelled) setError((e as Error).message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [courseId]);

  const labs = useMemo<CourseLabResponseDto[]>(() => {
    if (!data) return [];
    return [...(data.labs ?? [])].sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0));
  }, [data]);

  const submissionsByParticipant = useMemo(() => {
    const map = new Map<string, CourseChallengeSubmissionEntryDto[]>();
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
      const byLabId = new Map<string, CourseChallengeSubmissionEntryDto>();
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

      const latest = [...effectiveSubs]
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
        latestStatus: latest?.status ?? "NOT_SUBMITTED",
        latestLabTitle: latest ? (labTitleById.get(latest.labId) ?? null) : null,
        latestCompletedAt: latest?.completedAt ?? null,
      };
    });
  }, [data, submissionsByParticipant, labFilter, labTitleById]);

  const filteredRows = useMemo(() => {
    const q = search.trim().toLowerCase();
    return rows.filter((r) => {
      const matchesSearch =
        q.length === 0 ||
        r.participant.name.toLowerCase().includes(q) ||
        (r.participant.email ?? "").toLowerCase().includes(q);
      const matchesStatus = !statusFilter || r.overallStatus === statusFilter;
      return matchesSearch && matchesStatus;
    });
  }, [rows, search, statusFilter]);

  const totalParticipants = rows.length;
  const statsOnTime = rows.filter((r) => r.overallStatus === "ON_TIME").length;
  const statsLate = rows.filter((r) => r.overallStatus === "LATE").length;
  const statsInProg = rows.filter((r) => r.overallStatus === "IN_PROGRESS").length;
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
      const res = await fetch(
        `/api/backend/api/v1/courses/${encodeURIComponent(courseId)}/submissions/${encodeURIComponent(
          participantId
        )}/${encodeURIComponent(labId)}`,
        { cache: "no-store" }
      );
      if (!res.ok) throw new Error((await res.text()) || res.statusText);
      const json = (await res.json()) as CourseLabSubmissionDetailDto;
      setDetail(json);
    } catch (e) {
      setDetailError((e as Error).message);
      setDetail(null);
    } finally {
      setDetailLoading(false);
    }
  }

  async function openDetails(row: ParticipantRow) {
    setActiveParticipant(row);
    const nextLabId = labFilter ?? labs[0]?.labId ?? null;
    setActiveLabId(nextLabId);
    setDetailsOpened(true);
    if (nextLabId) {
      await loadDetail(row.participant.id, nextLabId);
    } else {
      setDetail(null);
      setDetailError(null);
    }
  }

  async function setOverridePoints(challengeId: string, points: number) {
    if (!activeParticipant || !activeLabId) return;
    const res = await fetch(
      `/api/backend/api/v1/courses/${encodeURIComponent(courseId)}/submissions/${encodeURIComponent(
        activeParticipant.participant.id
      )}/${encodeURIComponent(challengeId)}/score`,
      {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ points }),
      }
    );
    if (!res.ok) throw new Error((await res.text()) || res.statusText);
    const updated = (await res.json()) as CourseChallengeSubmissionEntryDto;

    setData((prev) => {
      if (!prev) return prev;
      const nextSubs = (prev.submissions ?? []).map((s) => {
        if (s.participantId === updated.participantId && s.labId === updated.labId) return updated;
        return s;
      });
      return { ...prev, submissions: nextSubs };
    });

    await loadDetail(activeParticipant.participant.id, activeLabId);
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
          label="On-Time"
          value={`${statsOnTime} / ${totalParticipants}`}
          sub="overall on time"
          icon={<IconCheck size={12} color="#64748b" />}
          progress={totalParticipants > 0 ? Math.round((statsOnTime / totalParticipants) * 100) : 0}
        />
        <StatCard
          label="In Progress"
          value={statsInProg}
          sub={`${statsLate} late`}
          subColor={statsLate > 0 ? "#f87171" : "#64748b"}
          icon={<IconPlayerPlay size={12} color={statsLate > 0 ? "#f87171" : "#64748b"} />}
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
        <Group
          justify="space-between"
          align="center"
          px="xl"
          py="md"
          style={{ borderBottom: "1px solid rgba(255,255,255,0.07)" }}
        >
          <Group gap="sm" style={{ minWidth: 0 }}>
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
              w={280}
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
              w={170}
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
            w={230}
            styles={{
              input: {
                background: "rgba(255,255,255,0.05)",
                border: "1px solid rgba(255,255,255,0.1)",
                color: "#f1f5f9",
                fontSize: "0.8rem",
              },
            }}
          />
        </Group>

        {/* Column headers */}
        <Box
          style={{
            display: "grid",
            gridTemplateColumns: "2.5fr 1.2fr 1.8fr 1.8fr",
            padding: "0.6rem 1.5rem",
            background: "rgba(255,255,255,0.02)",
            borderBottom: "1px solid rgba(255,255,255,0.06)",
          }}
        >
          {["Participant", "Status", "Points / Challenges", "Completion"].map((h) => (
            <Text
              key={h}
              size="xs"
              fw={700}
              style={{ color: "#475569", letterSpacing: "0.1em", textTransform: "uppercase" }}
            >
              {h}
            </Text>
          ))}
        </Box>

        {loading ? (
          <Group justify="center" py="xl">
            <Loader size="sm" />
          </Group>
        ) : filteredRows.length === 0 ? (
          <Text size="sm" c="dimmed" p="xl" ta="center">
            No participants found.
          </Text>
        ) : (
          filteredRows.map((r) => {
            const latestMeta =
              r.latestLabTitle && r.latestCompletedAt
                ? `${r.latestLabTitle} • ${formatDateTime(r.latestCompletedAt)}`
                : r.latestLabTitle
                  ? r.latestLabTitle
                  : r.latestCompletedAt
                    ? formatDateTime(r.latestCompletedAt)
                    : null;

            const status = labFilter ? r.overallStatus : r.latestStatus;
            return (
              <Box
                key={r.participant.id}
                style={{
                  display: "grid",
                  gridTemplateColumns: "2.5fr 1.2fr 1.8fr 1.8fr",
                  padding: "0.85rem 1.5rem",
                  borderBottom: "1px solid rgba(255,255,255,0.04)",
                  alignItems: "center",
                  cursor: "pointer",
                }}
                onClick={() => {
                  void openDetails(r);
                }}
              >
                <Group gap="sm">
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

                <Tooltip
                  withArrow
                  position="top"
                  label={
                    labFilter
                      ? statusLabel(status)
                      : latestMeta
                        ? `Latest: ${statusLabel(status)} • ${latestMeta}`
                        : `Latest: ${statusLabel(status)}`
                  }
                >
                  <span style={{ ...statusBadgeStyle(status), cursor: "help" }}>
                    {statusLabel(status)}
                  </span>
                </Tooltip>

                <Stack gap={2}>
                  <Text size="sm" fw={700} style={{ color: "#f1f5f9" }}>
                    {r.awardedPoints}/{r.maxPoints} pts
                  </Text>
                  <Text size="xs" style={{ color: "#475569" }}>
                    {r.solvedChallenges}/{r.totalChallenges} challenges
                  </Text>
                </Stack>

                <Stack gap={4}>
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
              </Box>
            );
          })
        )}
      </Box>

      <Modal
        opened={detailsOpened}
        onClose={() => {
          setDetailsOpened(false);
          setActiveParticipant(null);
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
                {detail.status === "LATE" ? (
                  <Box
                    style={{
                      background: "rgba(249,115,22,0.12)",
                      border: "1px solid rgba(249,115,22,0.25)",
                      borderRadius: 12,
                      padding: "0.9rem 1rem",
                    }}
                  >
                    <Text fw={700} style={{ color: "#fdba74" }}>
                      Late submission
                    </Text>
                    <Text size="sm" style={{ color: "#94a3b8", marginTop: 4 }}>
                      Due: {formatDateTime(detail.dueAt)} • Submitted:{" "}
                      {formatDateTime(detail.completedAt)}
                    </Text>
                  </Box>
                ) : null}

                {detail.challenges.map((c: CourseLabChallengeSubmissionDetailDto, idx: number) => {
                  const isLate = detail.status === "LATE";
                  const correctLabel =
                    c.correct === null ? "unknown" : c.correct ? "correct" : "wrong";
                  const correctColor =
                    c.correct === null ? "#94a3b8" : c.correct ? "#2dd4bf" : "#f87171";

                  const max = Math.max(0, c.maxPoints ?? 0);
                  const options = Array.from({ length: max + 1 }, (_, i) => ({
                    value: String(i),
                    label: String(i),
                  }));

                  return (
                    <Box
                      key={c.challengeId}
                      style={{
                        background: "rgba(255,255,255,0.03)",
                        border: `1px solid ${
                          isLate ? "rgba(249,115,22,0.25)" : "rgba(255,255,255,0.08)"
                        }`,
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
                            {c.type} • {c.maxPoints} pts •{" "}
                            <span style={{ color: correctColor }}>{correctLabel}</span>
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
                            Award points
                          </Text>
                          <Select
                            value={String(c.overridePoints ?? c.awardedPoints ?? 0)}
                            data={options}
                            onChange={(v) => {
                              const val = Number(v ?? "0");
                              void setOverridePoints(c.challengeId, val);
                            }}
                            w={96}
                            size="xs"
                            styles={{
                              input: {
                                background: "rgba(255,255,255,0.05)",
                                border: "1px solid rgba(255,255,255,0.1)",
                                color: "#f1f5f9",
                                textAlign: "center",
                                fontWeight: 700,
                              },
                              dropdown: {
                                background: "#0b1220",
                                border: "1px solid rgba(255,255,255,0.1)",
                              },
                              option: { color: "#e2e8f0" },
                            }}
                          />
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
