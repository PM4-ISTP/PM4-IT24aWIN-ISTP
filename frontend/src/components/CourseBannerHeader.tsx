import { Badge, Container, Group, Stack, Text, Title } from "@mantine/core";
import { IconArrowLeft } from "@tabler/icons-react";
import Link from "next/link";
import { CourseEnrollmentButton } from "@/src/components/CourseEnrollmentButton";
import { getCoursePreviewText } from "@/src/lib/courseText";

interface CourseBannerHeaderProps {
  title: string;
  topic?: string | null;
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
    <div style={{ background: "var(--mantine-color-gray-0)" }}>
      <Container size="lg" pt="lg" pb="md">
        <Stack gap="lg">
          {/* Back link */}
          <Link href={backHref} style={{ textDecoration: "none" }}>
            <Group gap={6} style={{ color: "var(--mantine-color-dimmed)", fontSize: 14 }}>
              <IconArrowLeft size={16} />
              <span>Back to Catalog</span>
            </Group>
          </Link>

          {/* Topic badge */}
          {topic && (
            <Group gap="sm">
              <Badge
                size="sm"
                variant="light"
                color="blue"
                style={{ textTransform: "uppercase", letterSpacing: "0.08em" }}
              >
                {topic}
              </Badge>
            </Group>
          )}

          {/* Title row */}
          <div style={{ display: "flex", alignItems: "flex-start", gap: 24, flexWrap: "wrap" }}>
            <div style={{ flex: "1 1 400px" }}>
              <Title
                order={1}
                style={{
                  color: "var(--mantine-color-text)",
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
                  c="dimmed"
                  style={{ lineHeight: 1.6, maxWidth: 760, overflowWrap: "anywhere" }}
                >
                  {previewText}
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
