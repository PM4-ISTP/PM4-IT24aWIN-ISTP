import { Avatar, Badge, Box, Group, Image, Stack, Text, UnstyledButton } from "@mantine/core";
import { IconClock, IconUsers } from "@tabler/icons-react";
import { getCoursePreviewText } from "@/src/features/course/utils/courseText";
import { getInitials } from "@/src/shared/lib/utils";
import classes from "./CourseCard.module.css";

export interface CourseCardProps {
  id: string;
  title: string;
  description: string | null;
  shortDescription: string | null;
  isPublished: boolean;
  isPrivate: boolean;
  instructorCount: number;
  updatedAt: string;
  imageUrl?: string | null;
  topic?: string | null;
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
  isPrivate,
  instructorCount,
  updatedAt,
  imageUrl,
  topic,
  ownerName,
  ownerPicture,
  ownerTitle,
  onClick,
}: CourseCardProps) {
  const previewText = getCoursePreviewText(shortDescription, description);
  const statusLabel = isPrivate ? "Private" : isPublished ? "Published" : "Draft";
  const statusColor = isPrivate ? "violet" : isPublished ? "teal" : "gray";

  const content = (
    <Stack gap={0} style={{ height: "100%", minWidth: 0 }}>
      {/* Thumbnail */}
      {imageUrl ? (
        <Box
          style={{
            height: 146,
            overflow: "hidden",
            borderRadius: "var(--mantine-radius-lg) var(--mantine-radius-lg) 0 0",
            flexShrink: 0,
          }}
        >
          <Image
            src={imageUrl}
            alt={title}
            h={146}
            fit="cover"
            style={{ display: "block", width: "100%" }}
          />
        </Box>
      ) : (
        <Box
          style={{
            height: 146,
            flexShrink: 0,
            overflow: "hidden",
            borderRadius: "var(--mantine-radius-lg) var(--mantine-radius-lg) 0 0",
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
              fontSize: 58,
              lineHeight: 1,
            }}
          >
            {title.charAt(0).toUpperCase()}
          </Text>
        </Box>
      )}

      {/* Body */}
      <Stack gap="md" p="lg" style={{ flex: 1 }}>
        {/* Topic + visibility */}
        <Group gap={6} wrap="nowrap">
          <Badge size="xs" variant="light" color={statusColor} style={{ flexShrink: 0 }}>
            {statusLabel}
          </Badge>
          {topic && (
            <Text
              size="sm"
              fw={700}
              tt="uppercase"
              style={{ color: "#60a5fa", letterSpacing: "0.07em", flexShrink: 0 }}
            >
              {topic}
            </Text>
          )}
        </Group>

        <Text fw={700} size="lg" lineClamp={2}>
          {title}
        </Text>

        <Text
          size="sm"
          c="dimmed"
          lineClamp={4}
          style={{ flex: 1, overflowWrap: "break-word", wordBreak: "break-word" }}
          title={previewText ?? undefined}
        >
          {previewText || "No short description provided."}
        </Text>

        <Box className={classes.footer}>
          {ownerName ? (
            <Group gap={10} wrap="nowrap" style={{ flex: 1, minWidth: 0 }}>
              <Avatar radius="xl" size={36} color="blue" src={ownerPicture ?? undefined}>
                {getInitials(ownerName)}
              </Avatar>
              <Stack gap={0} style={{ minWidth: 0 }}>
                <Text size="md" fw={700} lineClamp={1}>
                  {ownerName}
                </Text>
                <Text size="sm" c="dimmed">
                  {ownerTitle ?? "Instructor"}
                </Text>
              </Stack>
            </Group>
          ) : (
            <Group gap={6}>
              <IconUsers size={15} stroke={1.5} />
              <Text size="sm" c="dimmed">
                {instructorCount} instructor{instructorCount !== 1 ? "s" : ""}
              </Text>
            </Group>
          )}
          <Group gap={4} style={{ flexShrink: 0 }}>
            <IconClock size={14} stroke={1.5} />
            <Text size="sm" c="dimmed">
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
