import { Avatar, Badge, Box, Group, Image, Stack, Text, UnstyledButton } from "@mantine/core";
import { IconClock, IconUsers } from "@tabler/icons-react";
import { getCoursePreviewText } from "@/src/features/course/utils/courseText";
import { getInitials } from "@/src/shared/lib/utils";
import classes from "./CourseCard.module.css";
import type { ListCourseResponseDto } from "@/src/features/course/actions/courses";

export interface CourseCardProps {
  course: ListCourseResponseDto;
  onClick?: (id: string) => void;
}

function getStringRepresentationOfDate(date: string | undefined) {
  if (date === undefined) {
    return "No date specified";
  } else {
    return new Date(date).toLocaleDateString("de-CH", {
      day: "numeric",
      month: "short",
      year: "numeric",
    });
  }
}

export function CourseCard({ course, onClick }: CourseCardProps) {
  const previewText = getCoursePreviewText(course.shortDescription, course.description);
  const statusLabel = course.isPrivate ? "Private" : course.isPublished ? "Published" : "Draft";
  const statusColor = course.isPrivate ? "violet" : course.isPublished ? "teal" : "gray";

  const content = (
    <Stack gap={0} style={{ height: "100%", minWidth: 0 }}>
      {/* Thumbnail */}
      {course.imageUrl ? (
        <Box
          style={{
            height: 146,
            overflow: "hidden",
            borderRadius: "var(--mantine-radius-lg) var(--mantine-radius-lg) 0 0",
            flexShrink: 0,
          }}
        >
          <Image
            src={course.imageUrl}
            alt={course.title}
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
            {course.title?.charAt(0).toUpperCase()}
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
          {course.topic && (
            <Text
              size="sm"
              fw={700}
              tt="uppercase"
              style={{ color: "#60a5fa", letterSpacing: "0.07em", flexShrink: 0 }}
            >
              {course.topic}
            </Text>
          )}
        </Group>

        <Text fw={700} size="lg" lineClamp={2}>
          {course.title}
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
          {course.ownerName ? (
            <Group gap={10} wrap="nowrap" style={{ flex: 1, minWidth: 0 }}>
              <Avatar radius="xl" size={36} color="blue" src={course.ownerPicture}>
                {getInitials(course.ownerName)}
              </Avatar>
              <Stack gap={0} style={{ minWidth: 0 }}>
                <Text size="md" fw={700} lineClamp={1}>
                  {course.ownerName}
                </Text>
                <Text size="sm" c="dimmed">
                  {course.ownerTitle ?? "Instructor"}
                </Text>
              </Stack>
            </Group>
          ) : (
            <Group gap={6}>
              <IconUsers size={15} stroke={1.5} />
              <Text size="sm" c="dimmed">
                {course.instructorCount} instructor{course.instructorCount !== 1 ? "s" : ""}
              </Text>
            </Group>
          )}
          <Group gap={4} style={{ flexShrink: 0 }}>
            <IconClock size={14} stroke={1.5} />
            <Text size="sm" c="dimmed">
              {getStringRepresentationOfDate(course.updatedAt)}
            </Text>
          </Group>
        </Box>
      </Stack>
    </Stack>
  );

  if (!onClick || course.id === undefined) {
    return (
      <Box className={`${classes.card} ${classes.staticCard}`} style={{ padding: 0 }}>
        {content}
      </Box>
    );
  }

  return (
    <UnstyledButton
      className={classes.card}
      style={{ padding: 0 }}
      onClick={() => onClick(course.id!)} // already checked, that ID is not undefined
    >
      {content}
    </UnstyledButton>
  );
}
