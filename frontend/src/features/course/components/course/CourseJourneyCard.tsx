import {
  Avatar,
  Box,
  Divider,
  Flex,
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

interface SimpleProgress {
  completed: number;
  total: number;
}

export interface CourseJourneyCardProps {
  /** How many labs (top-level) the student has fully solved. */
  labs?: SimpleProgress;

  /** How many individual challenges across all labs the student has solved. */
  challenges?: SimpleProgress;

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
  testId?: string;
}

function ProgressSection({
  label,
  percent,
  color,
  statLeft,
  statRight,
  testId,
}: ProgressSectionProps) {
  return (
    <Stack gap={8} data-testid={testId}>
      <Group justify="space-between">
        <Text size="sm" fw={600}>
          {label}
        </Text>
        <Text size="sm" fw={700} c={color} data-testid={testId ? `${testId}-percent` : undefined}>
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
  testId?: string;
}

function StatChip({ icon, label, dimmed, testId }: StatChipProps) {
  return (
    <Group gap={6} data-testid={testId}>
      {icon}
      <Text size="xs" c={dimmed ? "dimmed" : undefined}>
        {label}
      </Text>
    </Group>
  );
}

interface UnavailableProgressSectionProps {
  label: string;
  color: string;
  hint: string;
  testId: string;
}

function UnavailableProgressSection({
  label,
  color,
  hint,
  testId,
}: UnavailableProgressSectionProps) {
  return (
    <Stack gap={8} data-testid={testId}>
      <Group justify="space-between" align="center">
        <Group gap={6}>
          <Text size="sm" fw={600} c="dimmed">
            {label}
          </Text>
          <Tooltip label={hint} withArrow>
            <ThemeIcon size="xs" variant="transparent" color="dimmed" style={{ cursor: "default" }}>
              <IconLock size={12} />
            </ThemeIcon>
          </Tooltip>
        </Group>
        <Text size="xs" c="dimmed" fs="italic">
          Not available
        </Text>
      </Group>
      <Progress value={0} color={color} radius="xl" size="md" style={{ opacity: 0.35 }} />
    </Stack>
  );
}

// ---------------------------------------------------------------------------
// Main component
// ---------------------------------------------------------------------------

export function CourseJourneyCard({ labs, challenges, instructor }: CourseJourneyCardProps) {
  const labPercent = labs ? calcPercent(labs.completed, labs.total) : 0;
  const challengePercent = challenges ? calcPercent(challenges.completed, challenges.total) : 0;
  const unavailableHint =
    !labs && !challenges
      ? "Enroll to start tracking your progress."
      : "Progress data is unavailable.";

  return (
    <Box
      data-testid="course-journey-card"
      style={{
        background: "rgba(255,255,255,0.04)",
        border: "1px solid rgba(255,255,255,0.08)",
        borderRadius: 14,
        boxShadow: "0 4px 24px rgba(0,0,0,0.25)",
        overflow: "hidden",
      }}
    >
      <Flex direction={{ base: "column", sm: "row" }} align="stretch">
        {/* —— Journey section —— */}
        <Stack gap="md" style={{ flex: 1, minWidth: 0, padding: "2rem" }}>
          <Group justify="space-between" align="center">
            <Text size="xs" tt="uppercase" fw={700} c="dimmed" style={{ letterSpacing: "0.08em" }}>
              Course Journey
            </Text>
          </Group>

          <Divider />

          {/* —— Labs progress —— */}
          {labs ? (
            <ProgressSection
              label="Labs"
              percent={labPercent}
              color="orange"
              testId="course-journey-labs"
              statLeft={
                <StatChip
                  icon={<IconFlame size={13} color="var(--mantine-color-orange-5)" />}
                  label={`${labs.completed} Lab${labs.completed !== 1 ? "s" : ""} Solved`}
                  testId="course-journey-labs-solved"
                />
              }
              statRight={
                <StatChip
                  icon={<IconClock size={13} color="var(--mantine-color-dimmed)" />}
                  label={`${Math.max(labs.total - labs.completed, 0)} Remaining`}
                  dimmed
                  testId="course-journey-labs-remaining"
                />
              }
            />
          ) : (
            <UnavailableProgressSection
              label="Labs"
              color="orange"
              hint={unavailableHint}
              testId="unavailable-labs-progress-section"
            />
          )}

          {/* —— Challenges progress —— */}
          {challenges ? (
            <ProgressSection
              label="Challenges"
              percent={challengePercent}
              color="blue"
              testId="course-journey-challenges"
              statLeft={
                <StatChip
                  icon={<IconListCheck size={13} color="var(--mantine-color-blue-5)" />}
                  label={`${
                    challenges.completed
                  } Challenge${challenges.completed !== 1 ? "s" : ""} Solved`}
                  testId="course-journey-challenges-solved"
                />
              }
              statRight={
                <StatChip
                  icon={<IconClock size={13} color="var(--mantine-color-dimmed)" />}
                  label={`${Math.max(challenges.total - challenges.completed, 0)} Remaining`}
                  dimmed
                  testId="course-journey-challenges-remaining"
                />
              }
            />
          ) : (
            <UnavailableProgressSection
              label="Challenges"
              color="blue"
              hint={unavailableHint}
              testId="unavailable-challenges-progress-section"
            />
          )}
        </Stack>

        {/* —— Instructor section (optional) —— */}
        {instructor && (
          <>
            <Box
              visibleFrom="sm"
              style={{ width: 1, background: "rgba(255,255,255,0.08)", flexShrink: 0 }}
            />
            <Box hiddenFrom="sm" style={{ height: 1, background: "rgba(255,255,255,0.08)" }} />
            <Stack
              gap={12}
              justify="center"
              w={{ base: "auto", sm: 240 }}
              style={{
                flexShrink: 0,
                padding: "2rem 1.75rem",
              }}
              data-testid="course-instructor"
            >
              <Text
                size="xs"
                tt="uppercase"
                fw={700}
                c="dimmed"
                style={{ letterSpacing: "0.08em" }}
                data-testid="course-instructor-label"
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
                  <Text
                    fw={600}
                    size="sm"
                    style={{ color: "#e2e8f0", lineHeight: 1.2 }}
                    data-testid="course-instructor-name"
                  >
                    {instructor.instructor?.name}
                  </Text>
                  {instructor.instructor?.title && (
                    <Text
                      size="xs"
                      style={{ color: "#94a3b8", lineHeight: 1.3 }}
                      data-testid="course-instructor-title"
                    >
                      {instructor.instructor?.title}
                    </Text>
                  )}
                </Stack>
              </Group>
            </Stack>
          </>
        )}
      </Flex>
    </Box>
  );
}
