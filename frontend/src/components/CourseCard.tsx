import { Avatar, Badge, Box, Group, Image, Stack, Text, UnstyledButton } from "@mantine/core";
import { IconClock, IconUsers } from "@tabler/icons-react";
import { getCoursePreviewText } from "@/src/lib/courseText";
import { difficultyColor, difficultyLabel } from "@/src/lib/courseUtils";
import { getInitials } from "@/src/lib/utils";
import classes from "./CourseCard.module.css";
import type { CourseDifficulty } from "@/src/types/course";

export interface CourseCardProps {
  id: string;
  title: string;
  description: string | null;
  shortDescription: string | null;
  isPublished: boolean;
  instructorCount: number;
  updatedAt: string;
  imageUrl?: string | null;
  topic?: string | null;
  difficulty?: CourseDifficulty | null;
  ownerName?: string | null;
  ownerPicture?: string | null;
  ownerTitle?: string | null;
  onClick?: (id: string) => void;
}

export function CourseCard({
  id,
  title,
  description,
  shortDescription,
  isPublished,
  instructorCount,
  updatedAt,
  imageUrl,
  topic,
  difficulty,
  ownerName,
  ownerPicture,
  ownerTitle,
  onClick,
}: CourseCardProps) {
  const previewText = getCoursePreviewText(shortDescription, description);

  const content = (
    <Stack gap={0} h="100%" style={{ overflow: "hidden" }}>
      {/* Thumbnail */}
      {imageUrl ? (
        <Box
          style={{
            height: 130,
            overflow: "hidden",
            borderRadius: "inherit inherit 0 0",
            flexShrink: 0,
          }}
        >
          <Image
            src={imageUrl}
            alt={title}
            h={130}
            fit="cover"
            style={{ display: "block", width: "100%" }}
          />
        </Box>
      ) : (
        <Box
          style={{
            height: 130,
            flexShrink: 0,
            background: "linear-gradient(135deg, #1e293b 0%, #0f172a 100%)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
          }}
        >
          <Text
            style={{
              color: "rgba(255,255,255,0.12)",
              fontWeight: 700,
              fontSize: 52,
              lineHeight: 1,
            }}
          >
            {title.charAt(0).toUpperCase()}
          </Text>
        </Box>
      )}

      {/* Body */}
      <Stack gap="sm" p="md" style={{ flex: 1 }}>
        {/* Topic + Difficulty */}
        <Group gap={6} wrap="nowrap">
          {topic && (
            <Text
              size="xs"
              fw={700}
              tt="uppercase"
              c="blue"
              style={{ letterSpacing: "0.07em", flexShrink: 0 }}
            >
              {topic}
            </Text>
          )}
          {topic && difficulty && (
            <Text size="xs" c="dimmed">
              •
            </Text>
          )}
          {difficulty && (
            <Badge
              size="xs"
              variant="light"
              color={difficultyColor(difficulty)}
              style={{ flexShrink: 0 }}
            >
              {difficultyLabel(difficulty)}
            </Badge>
          )}
          {!topic && !difficulty && (
            <Badge
              size="xs"
              variant="light"
              color={isPublished ? "teal" : "gray"}
              style={{ flexShrink: 0 }}
            >
              {isPublished ? "Published" : "Draft"}
            </Badge>
          )}
        </Group>

        <Text fw={600} size="sm" lineClamp={2}>
          {title}
        </Text>

        <Text size="xs" c="dimmed" lineClamp={2} style={{ flex: 1 }}>
          {previewText || "No short description provided."}
        </Text>

        <Box className={classes.footer}>
          {ownerName ? (
            <Group gap={8} wrap="nowrap" style={{ flex: 1, minWidth: 0 }}>
              <Avatar radius="xl" size={28} color="blue" src={ownerPicture ?? undefined}>
                {getInitials(ownerName)}
              </Avatar>
              <Stack gap={0} style={{ minWidth: 0 }}>
                <Text size="xs" fw={600} lineClamp={1}>
                  {ownerName}
                </Text>
                <Text size="xs" c="dimmed">
                  {ownerTitle ?? "Instructor"}
                </Text>
              </Stack>
            </Group>
          ) : (
            <Group gap={6}>
              <IconUsers size={13} stroke={1.5} />
              <Text size="xs" c="dimmed">
                {instructorCount} instructor{instructorCount !== 1 ? "s" : ""}
              </Text>
            </Group>
          )}
          <Group gap={4} style={{ flexShrink: 0 }}>
            <IconClock size={13} stroke={1.5} />
            <Text size="xs" c="dimmed">
              {updatedAt}
            </Text>
          </Group>
        </Box>
      </Stack>
    </Stack>
  );

  if (!onClick) {
    return (
      <Box className={`${classes.card} ${classes.staticCard}`} style={{ padding: 0 }}>
        {content}
      </Box>
    );
  }

  return (
    <UnstyledButton className={classes.card} style={{ padding: 0 }} onClick={() => onClick(id)}>
      {content}
    </UnstyledButton>
  );
}
