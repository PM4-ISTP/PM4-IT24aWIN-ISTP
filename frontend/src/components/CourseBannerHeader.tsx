import { Badge, Box, Container, Group, Stack, Text, Title } from "@mantine/core";
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
    <Container size="lg" pt="lg" pb="md">
      <Box
        style={{
          background: "rgba(255,255,255,0.03)",
          border: "1px solid rgba(255,255,255,0.08)",
          borderRadius: 14,
          padding: "2rem 2.5rem",
          position: "relative",
          overflow: "hidden",
          boxShadow: "0 4px 24px rgba(0,0,0,0.3)",
        }}
      >
        {/* Subtle blue accent glow — matches DashboardHero */}
        <Box
          style={{
            position: "absolute",
            top: -60,
            right: -30,
            width: 220,
            height: 220,
            borderRadius: "50%",
            background: "radial-gradient(circle, rgba(96,165,250,0.10) 0%, transparent 70%)",
            pointerEvents: "none",
          }}
        />

        <Stack gap="lg" style={{ position: "relative" }}>
          {/* Back link */}
          <Link href={backHref} style={{ textDecoration: "none" }}>
            <Group gap={6} style={{ color: "rgba(255,255,255,0.45)", fontSize: 14 }}>
              <IconArrowLeft size={16} />
              <span>Back to Catalog</span>
            </Group>
          </Link>

          {/* Topic badge */}
          {topic && (
            <Group gap="sm">
              <Badge
                size="sm"
                variant="outline"
                style={{
                  color: "#60a5fa",
                  borderColor: "rgba(96,165,250,0.25)",
                  background: "rgba(96,165,250,0.06)",
                  textTransform: "uppercase",
                  letterSpacing: "0.08em",
                  fontFamily: "var(--font-space-grotesk), sans-serif",
                }}
              >
                {topic}
              </Badge>
            </Group>
          )}

          {/* Title row */}
          <div style={{ display: "flex", alignItems: "flex-start", gap: 24, flexWrap: "wrap" }}>
            <div style={{ flex: "1 1 400px" }}>
              <Text
                size="xs"
                tt="uppercase"
                fw={700}
                style={{
                  color: "rgba(255,255,255,0.4)",
                  letterSpacing: "0.1em",
                  fontFamily: "var(--font-space-grotesk), sans-serif",
                  marginBottom: 8,
                }}
              >
                Course
              </Text>
              <Title
                order={1}
                style={{
                  color: "#e2e8f0",
                  fontFamily: "var(--font-space-grotesk), sans-serif",
                  fontWeight: 700,
                  fontSize: "clamp(1.6rem, 3vw, 2.4rem)",
                  lineHeight: 1.2,
                  letterSpacing: "-0.02em",
                  marginBottom: 12,
                }}
              >
                {title}
              </Title>
              {previewText && (
                <Text
                  style={{
                    color: "#94a3b8",
                    fontSize: "1rem",
                    lineHeight: 1.65,
                    maxWidth: 760,
                    overflowWrap: "anywhere",
                    fontFamily: "var(--font-space-grotesk), sans-serif",
                  }}
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
      </Box>
    </Container>
  );
}
