"use client";

import { useEffect, useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import {
  ActionIcon,
  Alert,
  Avatar,
  Button,
  Box,
  Container,
  Drawer,
  Group,
  Loader,
  Menu,
  NumberInput,
  Select,
  SimpleGrid,
  Stack,
  Text,
  TextInput,
  Title,
} from "@mantine/core";
import {
  IconArrowLeft,
  IconCheck,
  IconClock,
  IconDotsVertical,
  IconDownload,
  IconFilter,
  IconSearch,
  IconTrendingUp,
  IconUsers,
  IconPlayerPlay,
} from "@tabler/icons-react";
import { fetchCourse } from "@/src/features/course/actions/courses";
import {
  type CourseChallengeSubmissionsResponseDto,
  type CourseChallengeSubmissionEntryDto,
  type CourseParticipantDto,
  type CourseChallengeResponseDto,
  type CourseChallengeSubmissionStatusEnum as SubmissionStatus,
  courseChallengeSubmissionStatusEnumValues,
} from "@/src/shared/types/course";

// helpers

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

function overallStatus(statuses: SubmissionStatus[]): SubmissionStatus {
  if (statuses.includes("LATE")) return "LATE";
  if (statuses.includes("ON_TIME")) return "ON_TIME";
  if (statuses.includes("IN_PROGRESS")) return "IN_PROGRESS";
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
    borderRadius: 20,
    padding: "3px 12px",
    fontSize: "0.72rem",
    fontWeight: 600,
    letterSpacing: "0.02em",
    whiteSpace: "nowrap" as const,
    display: "inline-block",
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

function formatDate(value?: string | null): string {
  if (!value) return "—";
  try {
    return new Date(value).toLocaleDateString("en-GB", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    });
  } catch {
    return value;
  }
}

// types

interface StudentRow {
  participant: CourseParticipantDto;
  submissions: CourseChallengeSubmissionEntryDto[];
  completedLabs: number;
  totalLabs: number;
  solvedSubTasks: number;
  totalSubTasks: number;
  completionPct: number;
  status: SubmissionStatus;
  awardedPoints: number;
  maxPoints: number;
}

interface LabRow {
  row: StudentRow;
  sub?: CourseChallengeSubmissionEntryDto;
  status: SubmissionStatus;
  solved: number;
  total: number;
  pct: number;
  awardedPoints: number;
  maxPoints: number;
}

// stat card

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
      <Text
        size="xs"
        tt="uppercase"
        fw={600}
        style={{ color: "#64748b", letterSpacing: "0.08em", marginBottom: 6 }}
      >
        {label}
      </Text>
      <Stack gap={4}>
        <Text fw={700} style={{ color: "#f1f5f9", fontSize: "1.6rem", lineHeight: 1 }}>
          {value}
        </Text>
        {sub && (
          <Group gap={4} align="center">
            {icon}
            <Text size="xs" style={{ color: subColor ?? "#64748b" }}>
              {sub}
            </Text>
          </Group>
        )}
      </Stack>
      {progress !== undefined && (
        <Box
          style={{
            marginTop: 10,
            height: 4,
            borderRadius: 4,
            background: "rgba(255,255,255,0.08)",
            overflow: "hidden",
          }}
        >
          <Box
            style={{
              height: "100%",
              width: `${progress}%`,
              background: "linear-gradient(90deg,#2dd4bf,#22d3ee)",
              borderRadius: 4,
            }}
          />
        </Box>
      )}
    </Box>
  );
}

// page

export default function CourseResultsPage() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const courseId = params.id;

  const [courseTitle, setCourseTitle] = useState<string>("");
  const [data, setData] = useState<CourseChallengeSubmissionsResponseDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [selected, setSelected] = useState<StudentRow | null>(null);
  const [labFilter, setLabFilter] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<string | null>(null);
  const [scoreDrafts, setScoreDrafts] = useState<Record<string, number>>({});
  const [savingScoreKey, setSavingScoreKey] = useState<string | null>(null);

  const submissionStatuses = courseChallengeSubmissionStatusEnumValues.map((status) => {
    return { value: status, label: statusLabel(status) };
  });

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        setLoading(true);
        const [courseResult, subRes] = await Promise.all([
          fetchCourse(courseId),
          fetch(`/api/backend/api/v1/courses/${encodeURIComponent(courseId)}/submissions`, {
            cache: "no-store",
          }),
        ]);
        if (courseResult.success) setCourseTitle(courseResult.data.title);
        if (!subRes.ok) throw new Error((await subRes.text()) || subRes.statusText);
        const json = (await subRes.json()) as CourseChallengeSubmissionsResponseDto;
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

  const challenges = useMemo(
    () => [...(data?.challenges ?? [])].sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0)),
    [data]
  );

  const rows = useMemo<StudentRow[]>(() => {
    if (!data) return [];
    return (data.participants ?? []).map((p) => {
      const subs = (data.submissions ?? []).filter((s) => s.participantId === p.id);
      const completedLabs = subs.filter(
        (s) => s.status === "ON_TIME" || s.status === "LATE"
      ).length;
      const solvedSubTasks = subs.reduce((acc, s) => acc + (s.solvedSubTaskCount ?? 0), 0);
      const totalSubTasks = subs.reduce((acc, s) => acc + (s.totalSubTaskCount ?? 0), 0);
      const awardedPoints = subs.reduce((acc, s) => acc + (s.awardedPoints ?? 0), 0);
      const maxPoints = subs.reduce((acc, s) => acc + (s.maxPoints ?? 0), 0);
      const completionPct =
        totalSubTasks > 0 ? Math.round((solvedSubTasks / totalSubTasks) * 100) : 0;
      const status = overallStatus(subs.map((s) => s.status as SubmissionStatus));
      return {
        participant: p,
        submissions: subs,
        completedLabs,
        totalLabs: challenges.length,
        solvedSubTasks,
        totalSubTasks,
        completionPct,
        status,
        awardedPoints,
        maxPoints,
      };
    });
  }, [data, challenges]);

  const activeLab = useMemo(
    () => challenges.find((c) => c.challengeId === labFilter) ?? null,
    [challenges, labFilter]
  );

  const labRows = useMemo<LabRow[]>(() => {
    if (!activeLab) return [];
    return rows.map((row) => {
      const sub = row.submissions.find((s) => s.challengeId === activeLab.challengeId);
      const status = (sub?.status ?? "NOT_SUBMITTED") as SubmissionStatus;
      const solved = sub?.solvedSubTaskCount ?? 0;
      const total = sub?.totalSubTaskCount ?? 0;
      const pct = total > 0 ? Math.round((solved / total) * 100) : 0;
      return {
        row,
        sub,
        status,
        solved,
        total,
        pct,
        awardedPoints: sub?.awardedPoints ?? 0,
        maxPoints: sub?.maxPoints ?? 0,
      };
    });
  }, [rows, activeLab]);

  const filteredRows = useMemo(
    () => rows.filter((r) => r.participant.name.toLowerCase().includes(search.toLowerCase())),
    [rows, search]
  );
  const filteredLabRows = useMemo(
    () =>
      labRows.filter((lr) => lr.row.participant.name.toLowerCase().includes(search.toLowerCase())),
    [labRows, search]
  );

  const totalParticipants = rows.length;
  const statsOnTime = activeLab
    ? labRows.filter((lr) => lr.status === "ON_TIME").length
    : rows.filter((r) => r.status === "ON_TIME").length;
  const statsLate = activeLab
    ? labRows.filter((lr) => lr.status === "LATE").length
    : rows.filter((r) => r.status === "LATE").length;
  const statsInProg = activeLab
    ? labRows.filter((lr) => lr.status === "IN_PROGRESS").length
    : rows.filter((r) => r.status === "IN_PROGRESS").length;
  const avgPct =
    totalParticipants > 0
      ? Math.round(
          (activeLab ? labRows.map((lr) => lr.pct) : rows.map((r) => r.completionPct)).reduce(
            (a, b) => a + b,
            0
          ) / totalParticipants
        )
      : 0;

  const labIdx = activeLab ? challenges.indexOf(activeLab) + 1 : null;
  const tableTitle = activeLab
    ? `Participant Enrollment (Lab ${String(labIdx).padStart(2, "0")})`
    : "Participant Enrollment (All Labs)";

  const labSelectData = challenges.map((c, idx) => ({
    value: c.challengeId,
    label: `Lab ${String(idx + 1).padStart(2, "0")}: ${c.challengeTitle}`,
  }));

  const displayRows = activeLab ? filteredLabRows : filteredRows;

  function scoreKey(participantId: string, challengeId: string): string {
    return `${participantId}:${challengeId}`;
  }

  async function saveManualScore(
    participantId: string,
    challengeId: string,
    points: number,
    maxPoints: number
  ) {
    if (points < 0 || points > maxPoints) {
      return;
    }
    const key = scoreKey(participantId, challengeId);
    setSavingScoreKey(key);
    try {
      const res = await fetch(
        `/api/backend/api/v1/courses/${encodeURIComponent(courseId)}/submissions/${encodeURIComponent(participantId)}/${encodeURIComponent(challengeId)}/score`,
        {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ points }),
        }
      );
      if (!res.ok) {
        throw new Error((await res.text()) || "Failed to save score");
      }
      const updatedEntry = (await res.json()) as CourseChallengeSubmissionEntryDto;
      setData((prev) => {
        if (!prev) return prev;
        return {
          ...prev,
          submissions: prev.submissions.map((s) =>
            s.participantId === participantId && s.challengeId === challengeId ? updatedEntry : s
          ),
        };
      });
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setSavingScoreKey(null);
    }
  }

  function exportToCSV() {
    const labLabel = activeLab
      ? `Lab_${String(labIdx).padStart(2, "0")}_${activeLab.challengeTitle.replace(/\s+/g, "_")}`
      : "All_Labs";
    const filename = `${courseTitle.replace(/\s+/g, "_")}_${labLabel}_Results.csv`;
    const esc = (v: string | number) => `"${String(v).replace(/"/g, '""')}"`;

    function splitName(fullName: string): [string, string] {
      const parts = fullName.trim().split(/\s+/);
      const first = parts[0] ?? "";
      const last = parts.slice(1).join(" ");
      return [first, last];
    }

    let csvRows: string[];
    if (activeLab) {
      csvRows = [
        [
          "First Name",
          "Last Name",
          "Email",
          "Status",
          "Points",
          "Max Points",
          "Tasks Solved",
          "Tasks Total",
          "Score %",
          "Submitted",
        ].join(","),
        ...filteredLabRows.map((lr) => {
          const [first, last] = splitName(lr.row.participant.name);
          return [
            esc(first),
            esc(last),
            esc(lr.row.participant.email ?? ""),
            esc(statusLabel(lr.status)),
            lr.awardedPoints,
            lr.maxPoints,
            lr.solved,
            lr.total,
            lr.pct,
            esc(lr.sub?.completedAt ? formatDate(lr.sub.completedAt) : "—"),
          ].join(",");
        }),
      ];
    } else {
      csvRows = [
        [
          "First Name",
          "Last Name",
          "Email",
          "Overall Status",
          "Points",
          "Max Points",
          "Labs Completed",
          "Total Labs",
          "Score %",
        ].join(","),
        ...filteredRows.map((r) => {
          const [first, last] = splitName(r.participant.name);
          return [
            esc(first),
            esc(last),
            esc(r.participant.email ?? ""),
            esc(statusLabel(r.status)),
            r.awardedPoints,
            r.maxPoints,
            r.completedLabs,
            r.totalLabs,
            r.completionPct,
          ].join(",");
        }),
      ];
    }

    const blob = new Blob([csvRows.join("\n")], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }

  return (
    <Container size="xl" py="xl">
      {/* Header */}
      <Group mb="xl" justify="space-between" align="center">
        <Group align="center" gap="sm">
          <ActionIcon
            variant="subtle"
            size="lg"
            onClick={() => router.push(`/dashboard/instructor/${courseId}`)}
            aria-label="Back"
          >
            <IconArrowLeft size={20} />
          </ActionIcon>
          <Stack gap={2}>
            <Title order={2} style={{ color: "#f1f5f9", fontWeight: 700, fontSize: "1.4rem" }}>
              Results Overview
            </Title>
            <Text size="sm" style={{ color: "#64748b" }}>
              {activeLab
                ? `Analysis of Lab ${String(labIdx).padStart(2, "0")}: ${activeLab.challengeTitle}`
                : courseTitle}
            </Text>
          </Stack>
        </Group>
        <Select
          placeholder="All Labs"
          leftSection={<IconFilter size={14} />}
          data={labSelectData}
          value={labFilter}
          onChange={setLabFilter}
          clearable
          w={260}
          styles={{
            input: {
              background: "rgba(255,255,255,0.06)",
              border: "1px solid rgba(255,255,255,0.12)",
              color: "#f1f5f9",
            },
            dropdown: { background: "#1e293b", border: "1px solid rgba(255,255,255,0.12)" },
          }}
        />
        <Select
          placeholder="Lab Status"
          leftSection={<IconFilter size={14} />}
          data={submissionStatuses}
          value={statusFilter}
          onChange={setStatusFilter}
          clearable
          w={260}
          styles={{
            input: {
              background: "rgba(255,255,255,0.06)",
              border: "1px solid rgba(255,255,255,0.12)",
              color: "#f1f5f9",
            },
            dropdown: { background: "#1e293b", border: "1px solid rgba(255,255,255,0.12)" },
          }}
        />
      </Group>

      {error && (
        <Alert color="red" title="Failed to load" mb="lg">
          {error}
        </Alert>
      )}

      {loading ? (
        <Group justify="center" py="xl">
          <Loader />
        </Group>
      ) : (
        <>
          {/* Stat cards */}
          <SimpleGrid cols={{ base: 2, sm: 4 }} mb="xl" spacing="md">
            <StatCard
              label="Average Completion"
              value={`${avgPct}%`}
              sub={`+${statsOnTime} on time`}
              subColor="#2dd4bf"
              icon={<IconTrendingUp size={12} color="#2dd4bf" />}
              progress={avgPct}
            />
            <StatCard
              label="On-Time Submissions"
              value={`${statsOnTime} / ${totalParticipants}`}
              sub="submitted on time"
              icon={<IconCheck size={12} color="#64748b" />}
              progress={
                totalParticipants > 0 ? Math.round((statsOnTime / totalParticipants) * 100) : 0
              }
            />
            <StatCard
              label="In Progress"
              value={statsInProg}
              sub={`${statsLate} submitted late`}
              subColor={statsLate > 0 ? "#f87171" : "#64748b"}
              icon={<IconPlayerPlay size={12} color={statsLate > 0 ? "#f87171" : "#64748b"} />}
            />
            <StatCard
              label="Total Participants"
              value={totalParticipants}
              sub="enrolled"
              icon={<IconUsers size={12} color="#64748b" />}
            />
          </SimpleGrid>

          {/* Table card */}
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
              <Text fw={600} style={{ color: "#f1f5f9", fontSize: "1rem" }}>
                {tableTitle}
              </Text>
              <Group gap="sm">
                <TextInput
                  placeholder="Search participants…"
                  leftSection={<IconSearch size={14} />}
                  value={search}
                  onChange={(e) => setSearch(e.currentTarget.value)}
                  size="xs"
                  w={200}
                  styles={{
                    input: {
                      background: "rgba(255,255,255,0.05)",
                      border: "1px solid rgba(255,255,255,0.1)",
                      color: "#f1f5f9",
                      fontSize: "0.8rem",
                    },
                  }}
                />
                <Group gap={6} style={{ cursor: "pointer" }} onClick={exportToCSV}>
                  <IconDownload size={14} color="#60a5fa" />
                  <Text size="xs" fw={600} c="blue">
                    Export Report
                  </Text>
                </Group>
              </Group>
            </Group>

            {/* Column headers */}
            <Box
              style={{
                display: "grid",
                gridTemplateColumns: "2.5fr 1.2fr 1.4fr 1.8fr 60px",
                padding: "0.6rem 1.5rem",
                background: "rgba(255,255,255,0.02)",
                borderBottom: "1px solid rgba(255,255,255,0.06)",
              }}
            >
              {(activeLab
                ? ["Participant", "Status", "Tasks", "Completion", "Actions"]
                : ["Participant", "Status", "Points", "Completion", "Actions"]
              ).map((h) => (
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

            {/* Rows */}
            {displayRows.length === 0 ? (
              <Text size="sm" c="dimmed" p="xl" ta="center">
                No participants found.
              </Text>
            ) : (
              displayRows.map((item) => {
                const row = "row" in item ? item.row : item;
                const labItem = activeLab && "row" in item ? item : null;
                const status = labItem ? labItem.status : row.status;
                const pointsLabel = labItem
                  ? `${labItem.awardedPoints} / ${labItem.maxPoints} Punkte`
                  : `${row.awardedPoints} / ${row.maxPoints} Punkte`;
                const pct = labItem ? labItem.pct : row.completionPct;
                const submittedAt = labItem?.sub?.completedAt ?? null;

                return (
                  <Box
                    key={row.participant.id}
                    style={{
                      display: "grid",
                      gridTemplateColumns: "2.5fr 1.2fr 1.4fr 1.8fr 60px",
                      padding: "0.85rem 1.5rem",
                      borderBottom: "1px solid rgba(255,255,255,0.04)",
                      alignItems: "center",
                      cursor: "pointer",
                      transition: "background 0.12s",
                    }}
                    onMouseEnter={(e) =>
                      ((e.currentTarget as HTMLElement).style.background = "rgba(255,255,255,0.03)")
                    }
                    onMouseLeave={(e) =>
                      ((e.currentTarget as HTMLElement).style.background = "transparent")
                    }
                    onClick={() => setSelected(row)}
                  >
                    <Group gap="sm">
                      <Avatar
                        color={avatarColor(row.participant.name)}
                        radius="md"
                        size={36}
                        style={{ fontWeight: 700, fontSize: "0.8rem" }}
                      >
                        {initials(row.participant.name)}
                      </Avatar>
                      <Stack gap={1}>
                        <Text size="sm" fw={600} style={{ color: "#f1f5f9", lineHeight: 1.2 }}>
                          {row.participant.name}
                        </Text>
                        <Text size="xs" style={{ color: "#475569" }}>
                          {submittedAt
                            ? `Submitted: ${formatDate(submittedAt)}`
                            : activeLab
                              ? "Not submitted"
                              : `${row.totalLabs} labs assigned`}
                        </Text>
                      </Stack>
                    </Group>

                    <span style={statusBadgeStyle(status)}>{statusLabel(status)}</span>

                    <Stack gap={1}>
                      <Text size="sm" fw={600} style={{ color: "#f1f5f9" }}>
                        {pointsLabel}
                      </Text>
                      <Text size="xs" style={{ color: "#475569" }}>
                        {activeLab ? "lab points" : "total points"}
                      </Text>
                    </Stack>

                    <Stack gap={4}>
                      <Text size="xs" style={{ color: "#94a3b8" }}>
                        {pct}%
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
                            width: `${pct}%`,
                            background: progressColor(status),
                            borderRadius: 4,
                            transition: "width 0.3s",
                          }}
                        />
                      </Box>
                    </Stack>

                    <Menu shadow="md" width={160} position="bottom-end">
                      <Menu.Target>
                        <ActionIcon
                          variant="subtle"
                          color="gray"
                          onClick={(e) => e.stopPropagation()}
                        >
                          <IconDotsVertical size={16} />
                        </ActionIcon>
                      </Menu.Target>
                      <Menu.Dropdown
                        style={{ background: "#1e293b", border: "1px solid rgba(255,255,255,0.1)" }}
                      >
                        <Menu.Item
                          style={{ color: "#cbd5e1" }}
                          leftSection={<IconUsers size={14} />}
                          onClick={(e) => {
                            e.stopPropagation();
                            setSelected(row);
                          }}
                        >
                          View Details
                        </Menu.Item>
                      </Menu.Dropdown>
                    </Menu>
                  </Box>
                );
              })
            )}

            {/* Footer */}
            <Box
              style={{
                padding: "0.65rem 1.5rem",
                borderTop: "1px solid rgba(255,255,255,0.06)",
                background: "rgba(255,255,255,0.01)",
              }}
            >
              <Text size="xs" style={{ color: "#475569" }}>
                Showing {displayRows.length} of {totalParticipants} participants
                {activeLab ? ` · ${activeLab.challengeTitle}` : ""}
              </Text>
            </Box>
          </Box>
        </>
      )}

      {/* Detail Drawer */}
      <Drawer
        opened={!!selected}
        onClose={() => setSelected(null)}
        title={
          <Group gap="sm">
            {selected && (
              <Avatar color={avatarColor(selected.participant.name)} radius="md" size="sm">
                {initials(selected.participant.name)}
              </Avatar>
            )}
            <Stack gap={0}>
              <Text fw={700} size="sm" style={{ color: "#f1f5f9" }}>
                {selected?.participant.name}
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
          content: { background: "#0f172a", border: "1px solid rgba(255,255,255,0.08)" },
          header: { background: "#0f172a", borderBottom: "1px solid rgba(255,255,255,0.08)" },
        }}
      >
        {selected && (
          <Stack gap="md" pt="sm">
            <SimpleGrid cols={2} spacing="sm">
              <Box
                style={{ background: "rgba(255,255,255,0.04)", borderRadius: 10, padding: "1rem" }}
              >
                <Text
                  size="xs"
                  tt="uppercase"
                  style={{ color: "#64748b", letterSpacing: "0.08em" }}
                  mb={4}
                >
                  Overall
                </Text>
                <Text size="xl" fw={700} style={{ color: "#f1f5f9" }}>
                  {selected.completionPct}%
                </Text>
                <Box
                  style={{
                    height: 4,
                    borderRadius: 4,
                    background: "rgba(255,255,255,0.07)",
                    overflow: "hidden",
                    marginTop: 8,
                  }}
                >
                  <Box
                    style={{
                      height: "100%",
                      width: `${selected.completionPct}%`,
                      background: progressColor(selected.status),
                      borderRadius: 4,
                    }}
                  />
                </Box>
              </Box>
              <Box
                style={{ background: "rgba(255,255,255,0.04)", borderRadius: 10, padding: "1rem" }}
              >
                <Text
                  size="xs"
                  tt="uppercase"
                  style={{ color: "#64748b", letterSpacing: "0.08em" }}
                  mb={6}
                >
                  Status
                </Text>
                <span style={statusBadgeStyle(selected.status)}>
                  {statusLabel(selected.status)}
                </span>
                <Text size="xs" style={{ color: "#64748b", marginTop: 8 }}>
                  {selected.awardedPoints} / {selected.maxPoints} Punkte (gesamt)
                </Text>
              </Box>
            </SimpleGrid>

            <Text
              size="sm"
              fw={600}
              style={{
                color: "#94a3b8",
                letterSpacing: "0.05em",
                textTransform: "uppercase",
                fontSize: "0.7rem",
              }}
            >
              Lab Breakdown
            </Text>

            <Stack gap="xs">
              {challenges.length === 0 && (
                <Text size="sm" c="dimmed">
                  No labs assigned.
                </Text>
              )}
              {challenges.map((c: CourseChallengeResponseDto, idx) => {
                const sub = selected.submissions.find((s) => s.challengeId === c.challengeId);
                const st = (sub?.status ?? "NOT_SUBMITTED") as SubmissionStatus;
                const solved = sub?.solvedSubTaskCount ?? 0;
                const total = sub?.totalSubTaskCount ?? 0;
                const pct = total > 0 ? Math.round((solved / total) * 100) : 0;
                const currentPoints = sub?.awardedPoints ?? 0;
                const maxPoints = sub?.maxPoints ?? c.maxScore ?? 0;
                const key = scoreKey(selected.participant.id, c.challengeId);
                const draftValue = scoreDrafts[key] ?? currentPoints;
                return (
                  <Box
                    key={c.challengeId}
                    style={{
                      background:
                        activeLab?.challengeId === c.challengeId
                          ? "rgba(96,165,250,0.07)"
                          : "rgba(255,255,255,0.03)",
                      borderRadius: 10,
                      padding: "0.85rem 1rem",
                      border:
                        activeLab?.challengeId === c.challengeId
                          ? "1px solid rgba(96,165,250,0.2)"
                          : "1px solid rgba(255,255,255,0.05)",
                    }}
                  >
                    <Group justify="space-between" mb={6}>
                      <Text size="sm" fw={500} style={{ color: "#e2e8f0" }}>
                        Lab {String(idx + 1).padStart(2, "0")}: {c.challengeTitle}
                      </Text>
                      <span style={statusBadgeStyle(st)}>{statusLabel(st)}</span>
                    </Group>
                    {/* Score row */}
                    <Group justify="space-between" align="center" mb={6}>
                      <Group gap={6} align="baseline">
                        <Text
                          fw={700}
                          style={{ color: "#f1f5f9", fontSize: "1.1rem", lineHeight: 1 }}
                        >
                          {currentPoints}
                        </Text>
                        <Text size="xs" style={{ color: "#64748b" }}>
                          / {maxPoints} Punkte
                        </Text>
                        {total > 0 && (
                          <Text size="xs" style={{ color: progressColor(st), fontWeight: 600 }}>
                            {pct}%
                          </Text>
                        )}
                      </Group>
                    </Group>
                    {/* Progress bar */}
                    <Box
                      style={{
                        height: 4,
                        borderRadius: 4,
                        background: "rgba(255,255,255,0.07)",
                        overflow: "hidden",
                        marginBottom: 8,
                      }}
                    >
                      <Box
                        style={{
                          height: "100%",
                          width: `${pct}%`,
                          background: progressColor(st),
                          borderRadius: 4,
                        }}
                      />
                    </Box>
                    {/* Deadline + submission info */}
                    {c.dueAt && (
                      <Group gap={4} mb={2}>
                        <IconClock size={11} color="#475569" />
                        <Text size="xs" style={{ color: "#475569" }}>
                          Deadline: {formatDate(c.dueAt)}
                        </Text>
                      </Group>
                    )}
                    {st === "LATE" && sub?.completedAt && (
                      <Group gap={4}>
                        <IconClock size={11} color="#f87171" />
                        <Text size="xs" style={{ color: "#f87171", fontWeight: 600 }}>
                          Submitted late: {formatDate(sub.completedAt)} (kein automatischer Abzug)
                        </Text>
                      </Group>
                    )}
                    {st === "ON_TIME" && sub?.completedAt && (
                      <Group gap={4}>
                        <IconClock size={11} color="#2dd4bf" />
                        <Text size="xs" style={{ color: "#2dd4bf" }}>
                          Submitted: {formatDate(sub.completedAt)}
                        </Text>
                      </Group>
                    )}
                    {sub && (
                      <Group gap="xs" mt={10} justify="space-between" align="flex-end">
                        <Button
                          variant="light"
                          size="xs"
                          onClick={() =>
                            router.push(
                              `/dashboard/instructor/${courseId}/statistic/${c.challengeId}?query=${encodeURIComponent(selected.participant.name)}`
                            )
                          }
                        >
                          Abgabe ansehen
                        </Button>
                        <Group gap={6} align="flex-end">
                          <NumberInput
                            size="xs"
                            min={0}
                            max={maxPoints}
                            value={draftValue}
                            onChange={(value) => {
                              const next = typeof value === "number" ? value : 0;
                              setScoreDrafts((prev) => ({ ...prev, [key]: next }));
                            }}
                            allowDecimal={false}
                            clampBehavior="strict"
                            styles={{ input: { width: 92 } }}
                          />
                          <Button
                            size="xs"
                            loading={savingScoreKey === key}
                            onClick={() => {
                              void saveManualScore(
                                selected.participant.id,
                                c.challengeId,
                                draftValue,
                                maxPoints
                              );
                            }}
                          >
                            Punkte speichern
                          </Button>
                        </Group>
                      </Group>
                    )}
                  </Box>
                );
              })}
            </Stack>
          </Stack>
        )}
      </Drawer>
    </Container>
  );
}
