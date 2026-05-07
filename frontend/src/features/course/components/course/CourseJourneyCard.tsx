import {
  Avatar,
  Box,
  Divider,
  Group,
  Progress,
  Stack,
  Text,
  ThemeIcon,
  Tooltip,
} from "@mantine/core";
import { IconClock, IconFlame, IconListCheck, IconLock } from "@tabler/icons-react";
import { getInitials } from "@/src/shared/lib/utils";
import type { CourseDetailInstructorResponseDto } from "@/src/features/course/actions/courses";

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface LessonsProgress {
  /** Number of lessons the user has completed */
  finished: number;
  /** Total number of lessons in the course */
  total: number;
}

interface SimpleProgress {
  completed: number;
  total: number;
}

export interface CourseJourneyCardProps {
  /**
   * Lesson progress data.
   * TODO: Wire up to real lesson-completion API once lessons are trackable.
   */
  lessons?: LessonsProgress;

  /** How many labs (top-level) the student has fully solved. */
  labs?: SimpleProgress;

  /** How many individual challenges across all labs the student has solved. */
  challenges?: SimpleProgress;

  /**
   * Deprecated. The primary course CTA lives in the banner header.
   */
  nextChallengeHref?: string;

  /**
   * When provided, renders an "Instructor" section to the right of the
   * journey content, separated by a vertical divider — no separate card needed.
   */
  instructor?: CourseDetailInstructorResponseDto;
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function calcPercent(done: number, total: number): number {
  if (total === 0) return 0;
  return Math.round((done / total) * 100);
}

// ---------------------------------------------------------------------------
// Sub-components
// ---------------------------------------------------------------------------

interface ProgressSectionProps {
  label: string;
  percent: number;
  color: string;
  statLeft: React.ReactNode;
  statRight: React.ReactNode;
}

function ProgressSection({ label, percent, color, statLeft, statRight }: ProgressSectionProps) {
  return (
    <Stack gap={8}>
      <Group justify="space-between">
        <Text size="sm" fw={600}>
          {label}
        </Text>
        <Text size="sm" fw={700} c={color}>
          {percent}% Complete
        </Text>
      </Group>
      <Progress value={percent} color={color} radius="xl" size="md" />
      <Group gap="lg">
        {statLeft}
        {statRight}
      </Group>
    </Stack>
  );
}

interface StatChipProps {
  icon: React.ReactNode;
  label: string;
  dimmed?: boolean;
}

function StatChip({ icon, label, dimmed }: StatChipProps) {
  return (
    <Group gap={6}>
      {icon}
      <Text size="xs" c={dimmed ? "dimmed" : undefined}>
        {label}
      </Text>
    </Group>
  );
}

// ---------------------------------------------------------------------------
// Placeholder — shown when labs have not been implemented yet
// ---------------------------------------------------------------------------

/**
 * PLACEHOLDER: Rendered in place of the labs progress bar until the
 * labs feature is available. Remove this component and wire up the real
 * ChallengesProgress prop instead.
 */
function ChallengesPlaceholder() {
  return (
    <Stack gap={8}>
      <Group justify="space-between" align="center">
        <Group gap={6}>
          <Text size="sm" fw={600} c="dimmed">
            Challenges
          </Text>
          <Tooltip label="Challenges are coming soon!" withArrow>
            <ThemeIcon size="xs" variant="transparent" color="dimmed" style={{ cursor: "default" }}>
              <IconLock size={12} />
            </ThemeIcon>
          </Tooltip>
        </Group>
        <Text size="xs" c="dimmed" fs="italic">
          Coming soon
        </Text>
      </Group>
      {/* Visual placeholder bar — replace with real Progress once backend supports labs */}
      <Progress
        value={0}
        color="orange"
        radius="xl"
        size="md"
        style={{ opacity: 0.35 }}
        aria-label="Challenges progress — not yet available"
      />
      <Group gap="lg">
        <StatChip
          icon={<IconFlame size={13} color="var(--mantine-color-dimmed)" />}
          label="0 Challenges Completed"
          dimmed
        />
      </Group>
    </Stack>
  );
}

// ---------------------------------------------------------------------------
// Main component
// ---------------------------------------------------------------------------

export function CourseJourneyCard({ labs, challenges, instructor }: CourseJourneyCardProps) {
  const labPercent = labs ? calcPercent(labs.completed, labs.total) : 0;
  const challengePercent = challenges ? calcPercent(challenges.completed, challenges.total) : 0;

  return (
    <Box
      style={{
        background: "rgba(255,255,255,0.04)",
        border: "1px solid rgba(255,255,255,0.08)",
        borderRadius: 14,
        boxShadow: "0 4px 24px rgba(0,0,0,0.25)",
        overflow: "hidden",
      }}
    >
      <div style={{ display: "flex", alignItems: "stretch" }}>
        {/* ── Journey section ── */}
        <Stack gap="md" style={{ flex: 1, minWidth: 0, padding: "2rem" }}>
          {/* Header */}
          <Group justify="space-between" align="center">
            <Text size="xs" tt="uppercase" fw={700} c="dimmed" style={{ letterSpacing: "0.08em" }}>
              Course Journey
            </Text>
          </Group>

          <Divider />

          {/* ── Labs progress ── */}
          {labs ? (
            <ProgressSection
              label="Labs"
              percent={labPercent}
              color="orange"
              statLeft={
                <StatChip
                  icon={<IconFlame size={13} color="var(--mantine-color-orange-5)" />}
                  label={`${labs.completed} Lab${labs.completed !== 1 ? "s" : ""} Solved`}
                />
              }
              statRight={
                <StatChip
                  icon={<IconClock size={13} color="var(--mantine-color-dimmed)" />}
                  label={`${Math.max(labs.total - labs.completed, 0)} Remaining`}
                  dimmed
                />
              }
            />
          ) : (
            <Stack gap={8}>
              <Group justify="space-between">
                <Text size="sm" fw={600} c="dimmed">Labs</Text>
                <Text size="xs" c="dimmed" fs="italic">Coming soon</Text>
              </Group>
              <Progress value={0} color="orange" radius="xl" size="md" style={{ opacity: 0.35 }} />
            </Stack>
          )}

          {/* ── Challenges progress ── */}
          {challenges ? (
            <ProgressSection
              label="Challenges"
              percent={challengePercent}
              color="blue"
              statLeft={
                <StatChip
                  icon={<IconListCheck size={13} color="var(--mantine-color-blue-5)" />}
                  label={`${challenges.completed} Challenge${challenges.completed !== 1 ? "s" : ""} Solved`}
                />
              }
              statRight={
                <StatChip
                  icon={<IconClock size={13} color="var(--mantine-color-dimmed)" />}
                  label={`${Math.max(challenges.total - challenges.completed, 0)} Remaining`}
                  dimmed
                />
              }
            />
          ) : (
            <ChallengesPlaceholder />
          )}
        </Stack>

        {/* ── Instructor section (optional) ── */}
        {instructor && (
          <>
            <div
              style={{
                width: 1,
                background: "rgba(255,255,255,0.08)",
                flexShrink: 0,
              }}
            />
            <Stack
              gap={12}
              justify="center"
              style={{
                flex: "0 0 220px",
                padding: "2rem 1.75rem",
              }}
            >
              <Text
                size="xs"
                tt="uppercase"
                fw={700}
                c="dimmed"
                style={{ letterSpacing: "0.08em" }}
              >
                Instructor
              </Text>
              <Group gap="md" align="center" wrap="nowrap">
                <Avatar
                  radius="xl"
                  size={52}
                  color="blue"
                  src={instructor.instructor?.picture}
                  style={{ border: "2px solid rgba(255,255,255,0.1)", flexShrink: 0 }}
                >
                  {getInitials(instructor.instructor?.name ?? "")}
                </Avatar>
                <Stack gap={2}>
                  <Text fw={600} size="sm" style={{ color: "#e2e8f0", lineHeight: 1.2 }}>
                    {instructor.instructor?.name}
                  </Text>
                  {instructor.instructor?.title && (
                    <Text size="xs" style={{ color: "#94a3b8", lineHeight: 1.3 }}>
                      {instructor.instructor?.title}
                    </Text>
                  )}
                </Stack>
              </Group>
            </Stack>
          </>
        )}
      </div>
    </Box>
  );
}
