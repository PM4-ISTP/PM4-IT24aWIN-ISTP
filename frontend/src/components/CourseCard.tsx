import { Badge, Box, Group, Stack, Text, UnstyledButton } from "@mantine/core";
import { IconClock, IconUsers } from "@tabler/icons-react";
import classes from "./CourseCard.module.css";

export interface CourseCardProps {
  id: string;
  title: string;
  description: string | null;
  isPublished: boolean;
  instructorCount: number;
  updatedAt: string;
  onClick?: (id: string) => void;
}

export function CourseCard({
  id,
  title,
  description,
  isPublished,
  instructorCount,
  updatedAt,
  onClick,
}: CourseCardProps) {
  const content = (
    <Stack gap="sm" h="100%">
      <Group justify="space-between" align="flex-start" wrap="nowrap">
        <Text fw={500} size="sm" lineClamp={2} style={{ flex: 1 }}>
          {title}
        </Text>
        <Badge
          size="xs"
          variant="light"
          color={isPublished ? "teal" : "gray"}
          style={{ flexShrink: 0 }}
        >
          {isPublished ? "Published" : "Draft"}
        </Badge>
      </Group>

      <Text size="xs" c="dimmed" lineClamp={2} style={{ flex: 1 }}>
        {description
          ? description
              .replace(/<\/(p|h[1-6]|li|br|div)>/gi, " ")
              .replace(/<[^>]*>/g, "")
              .trim()
          : "No description provided."}
      </Text>

      <Box className={classes.footer}>
        <Group gap="xs">
          <IconUsers size={13} stroke={1.5} />
          <Text size="xs" c="dimmed">
            {instructorCount} instructor{instructorCount !== 1 ? "s" : ""}
          </Text>
        </Group>
        <Group gap="xs">
          <IconClock size={13} stroke={1.5} />
          <Text size="xs" c="dimmed">
            {updatedAt}
          </Text>
        </Group>
      </Box>
    </Stack>
  );

  if (!onClick) {
    return <Box className={`${classes.card} ${classes.staticCard}`}>{content}</Box>;
  }

  return (
    <UnstyledButton className={classes.card} onClick={() => onClick(id)}>
      {content}
    </UnstyledButton>
  );
}
