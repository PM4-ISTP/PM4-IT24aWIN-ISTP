import { Badge, Container, Group, Stack, Text, Title } from "@mantine/core";
import { IconArrowLeft } from "@tabler/icons-react";
import Link from "next/link";
import { CourseEnrollmentButton } from "@/src/components/CourseEnrollmentButton";
import { getCoursePreviewText } from "@/src/lib/courseText";
import { difficultyColor, difficultyLabel } from "@/src/lib/courseUtils";
import type { CourseDifficulty } from "@/src/types/course";

const BANNER_PREVIEW_MAX_CHARS = 200;

interface CourseBannerHeaderProps {
  title: string;
  topic?: string | null;
  difficulty?: CourseDifficulty | null;
  shortDescription?: string | null;
  description?: string | null;
  courseId: string;
  isEnrolled: boolean;
  participantCount: number;
  isInstructor: boolean;
  isPublished: boolean;
  backHref: string;
}

export function CourseBannerHeader({
  title,
  topic,
  difficulty,
  shortDescription,
  description,
  courseId,
  isEnrolled,
  participantCount,
  isInstructor,
  isPublished,
  backHref,
}: CourseBannerHeaderProps) {
  const previewText = getCoursePreviewText(shortDescription, description);

  return (
    <div
      style={{
        background: "linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%)",
        padding: "48px 0 40px",
        position: "relative",
        overflow: "hidden",
      }}
    >
      {/* Radial glow overlay */}
      <div
        style={{
          position: "absolute",
          inset: 0,
          backgroundImage:
            "radial-gradient(circle at 20% 50%, rgba(59,130,246,0.15) 0%, transparent 50%)," +
            "radial-gradient(circle at 80% 20%, rgba(99,102,241,0.1) 0%, transparent 40%)",
          pointerEvents: "none",
        }}
      />

      <Container size="lg" style={{ position: "relative" }}>
        <Stack gap="lg">
          {/* Back link */}
          <Link href={backHref} style={{ textDecoration: "none" }}>
            <Group gap={6} style={{ color: "rgba(255,255,255,0.55)", fontSize: 14 }}>
              <IconArrowLeft size={16} />
              <span>Back to Catalog</span>
            </Group>
          </Link>

          {/* Topic / Difficulty badges */}
          <Group gap="sm">
            {topic && (
              <Badge
                size="sm"
                variant="light"
                color="blue"
                style={{ textTransform: "uppercase", letterSpacing: "0.08em" }}
              >
                {topic}
              </Badge>
            )}
            {difficulty && (
              <Badge
                size="sm"
                variant="light"
                color={difficultyColor(difficulty)}
                style={{ textTransform: "uppercase", letterSpacing: "0.08em" }}
              >
                {difficultyLabel(difficulty)}
              </Badge>
            )}
          </Group>

          {/* Title row */}
          <div style={{ display: "flex", alignItems: "flex-start", gap: 24, flexWrap: "wrap" }}>
            <div style={{ flex: "1 1 400px" }}>
              <Title
                order={1}
                style={{
                  color: "white",
                  fontSize: "clamp(1.6rem, 3vw, 2.4rem)",
                  lineHeight: 1.2,
                  marginBottom: 12,
                }}
              >
                {title}
              </Title>
              {previewText && (
                <Text
                  size="md"
                  lineClamp={3}
                  style={{ color: "rgba(255,255,255,0.7)", lineHeight: 1.6, maxWidth: 600 }}
                >
                  {previewText.length > BANNER_PREVIEW_MAX_CHARS
                    ? `${previewText.slice(0, BANNER_PREVIEW_MAX_CHARS)}…`
                    : previewText}
                </Text>
              )}
            </div>
            <div style={{ flexShrink: 0, paddingTop: 4 }}>
              <CourseEnrollmentButton
                courseId={courseId}
                isEnrolled={isEnrolled}
                participantCount={participantCount}
                isInstructor={isInstructor}
                isPublished={isPublished}
              />
            </div>
          </div>
        </Stack>
      </Container>
    </div>
  );
}
