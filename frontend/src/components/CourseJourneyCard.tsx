import { Box, Divider, Group, Progress, Stack, Text, ThemeIcon, Tooltip } from "@mantine/core";
import { IconCheck, IconClock, IconFlame, IconLock } from "@tabler/icons-react";

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface LessonsProgress {
  /** Number of lessons the user has completed */
  finished: number;
  /** Total number of lessons in the course */
  total: number;
}

/**
 * Props for challenge progress.
 *
 * TODO: Replace with real challenge data once the challenges feature is
 * implemented in the backend. Until then, leave this prop undefined to render
 * the placeholder state.
 */
interface ChallengesProgress {
  /** Number of challenges the user has completed */
  completed: number;
  /** Total number of challenges in the course */
  total: number;
}

export interface CourseJourneyCardProps {
  /**
   * Lesson progress data.
   * TODO: Wire up to real lesson-completion API once lessons are trackable.
   */
  lessons?: LessonsProgress;

  /**
   * Challenge progress data.
   * PLACEHOLDER — challenges are not yet implemented in the backend.
   * When challenges are ready, pass this prop and the placeholder state
   * will automatically be replaced with the real progress bar.
   */
  challenges?: ChallengesProgress;
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
// Placeholder — shown when challenges have not been implemented yet
// ---------------------------------------------------------------------------

/**
 * PLACEHOLDER: Rendered in place of the challenges progress bar until the
 * challenges feature is available. Remove this component and wire up the real
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
      {/* Visual placeholder bar — replace with real Progress once backend supports challenges */}
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

export function CourseJourneyCard({ lessons, challenges }: CourseJourneyCardProps) {
  const lessonPercent = lessons ? calcPercent(lessons.finished, lessons.total) : 0;
  const challengePercent = challenges ? calcPercent(challenges.completed, challenges.total) : 0;

  return (
    <Box
      style={{
        background: "rgba(255,255,255,0.04)",
        border: "1px solid rgba(255,255,255,0.08)",
        borderRadius: 14,
        padding: "2rem",
        boxShadow: "0 4px 24px rgba(0,0,0,0.25)",
      }}
    >      <Stack gap="md">
        {/* Header */}
        <Group justify="space-between" align="center">
          <Text size="xs" tt="uppercase" fw={700} c="dimmed" style={{ letterSpacing: "0.08em" }}>
            Course Journey
          </Text>
        </Group>

        <Divider />

        {/* ── Lessons progress ── */}
        {lessons ? (
          <ProgressSection
            label="Lessons"
            percent={lessonPercent}
            color="blue"
            statLeft={
              <StatChip
                icon={<IconCheck size={13} color="var(--mantine-color-blue-5)" />}
                label={`${lessons.finished} Lesson${lessons.finished !== 1 ? "s" : ""} Finished`}
              />
            }
            statRight={
              <StatChip
                icon={<IconClock size={13} color="var(--mantine-color-dimmed)" />}
                label={`${lessons.total - lessons.finished} Remaining`}
                dimmed
              />
            }
          />
        ) : (
          // TODO: Remove this once lesson tracking is implemented
          <Stack gap={8}>
            <Group justify="space-between">
              <Text size="sm" fw={600} c="dimmed">
                Lessons
              </Text>
              <Text size="xs" c="dimmed" fs="italic">
                Coming soon
              </Text>
            </Group>
            <Progress value={0} color="blue" radius="xl" size="md" style={{ opacity: 0.35 }} />
          </Stack>
        )}

        {/* ── Challenges progress (or placeholder) ── */}
        {challenges ? (
          <ProgressSection
            label="Challenges"
            percent={challengePercent}
            color="orange"
            statLeft={
              <StatChip
                icon={<IconFlame size={13} color="var(--mantine-color-orange-5)" />}
                label={`${challenges.completed} Challenge${challenges.completed !== 1 ? "s" : ""} Completed`}
              />
            }
            statRight={
              <StatChip
                icon={<IconClock size={13} color="var(--mantine-color-dimmed)" />}
                label={`${challenges.total - challenges.completed} Remaining`}
                dimmed
              />
            }
          />
        ) : (
          // PLACEHOLDER: Replace with real challenges data when backend is ready
          <ChallengesPlaceholder />
        )}
      </Stack>
    </Box>
  );
}
