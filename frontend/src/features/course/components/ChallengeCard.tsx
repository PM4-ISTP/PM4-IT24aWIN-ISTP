import { Badge, Box, Group, Stack, Text, UnstyledButton } from "@mantine/core";
import { IconBook, IconClock } from "@tabler/icons-react";
import classes from "./ChallengeCard.module.css";
import { getDifficultyColor, getStatusColor } from "@/src/features/course/constants/challengeConstants";

export interface ChallengeCardProps {
  id: string;
  title: string;
  shortDescription?: string;
  status: string;
  difficulty: string;
  maxScore: number;
  courseCount: number;
  updatedAt: string;
  onClick: (id: string) => void;
}

export function ChallengeCard({
  id,
  title,
  shortDescription,
  status,
  difficulty,
  maxScore,
  courseCount,
  updatedAt,
  onClick,
}: ChallengeCardProps) {
  return (
    <UnstyledButton className={classes.card} onClick={() => onClick(id)}>
      <Stack gap="sm" h="100%">
        {/* Header: Title + Status badge */}
        <Group justify="space-between" align="flex-start" wrap="nowrap">
          <Text fw={600} size="sm" lineClamp={2} style={{ flex: 1, minWidth: 0 }}>
            {title}
          </Text>
          <Badge size="xs" variant="light" color={getStatusColor(status)} style={{ flexShrink: 0 }}>
            {status}
          </Badge>
        </Group>

        {/* Short description */}
        <Box style={{ flex: 1 }}>
          {shortDescription && (
            <Text size="xs" c="dimmed" lineClamp={2} style={{ lineHeight: 1.5 }}>
              {shortDescription}
            </Text>
          )}
        </Box>

        {/* Difficulty — pinned above footer */}
        <Group justify="flex-start" mb={4}>
          <Text size="xs" c="dimmed">
            Difficulty:{" "}
            <Text span size="xs" fw={600} c={getDifficultyColor(difficulty)}>
              {difficulty}
            </Text>
          </Text>
          {maxScore > 0 && (
            <>
              <Text size="xs" c="dimmed">
                |
              </Text>
              <Text size="xs" c="dimmed">
                Score: {maxScore}
              </Text>
            </>
          )}
        </Group>

        {/* Footer */}
        <Box className={classes.footer}>
          <Group gap={4}>
            <IconBook size={13} stroke={1.5} />
            <Text size="xs" c="dimmed">
              {courseCount} course{courseCount !== 1 ? "s" : ""}
            </Text>
          </Group>
          <Group gap={4}>
            <IconClock size={13} stroke={1.5} />
            <Text size="xs" c="dimmed">
              {updatedAt}
            </Text>
          </Group>
        </Box>
      </Stack>
    </UnstyledButton>
  );
}
