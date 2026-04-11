import { Avatar, Box, Group, Stack, Text } from "@mantine/core";
import { getInitials } from "@/src/lib/utils";
import type { CourseUserSummary } from "@/src/types/course";

interface CourseInstructorCardProps {
  instructor: CourseUserSummary;
}

const sectionLabelStyle: React.CSSProperties = {
  fontFamily: "var(--font-space-grotesk), sans-serif",
  textTransform: "uppercase",
  letterSpacing: "0.1em",
  fontSize: "0.7rem",
  fontWeight: 700,
  color: "rgba(255,255,255,0.45)",
};

export function CourseInstructorCard({ instructor }: CourseInstructorCardProps) {
  return (
    <Box
      style={{
        background: "rgba(255,255,255,0.04)",
        border: "1px solid rgba(255,255,255,0.08)",
        borderRadius: 14,
        padding: "1.25rem 1.5rem",
        boxShadow: "0 4px 24px rgba(0,0,0,0.25)",
      }}
    >
      <Stack gap={12}>
        <Text style={sectionLabelStyle}>Instructor</Text>
        <Group gap="md" align="center" wrap="nowrap">
          <Avatar
            radius="xl"
            size={52}
            color="blue"
            src={instructor.picture ?? undefined}
            style={{
              border: "2px solid rgba(255,255,255,0.1)",
              flexShrink: 0,
            }}
          >
            {getInitials(instructor.name)}
          </Avatar>
          <Stack gap={2}>
            <Text fw={600} size="sm" style={{ color: "#e2e8f0", lineHeight: 1.2 }}>
              {instructor.name}
            </Text>
            {instructor.title && (
              <Text size="xs" style={{ color: "#94a3b8", lineHeight: 1.3 }}>
                {instructor.title}
              </Text>
            )}
          </Stack>
        </Group>
      </Stack>
    </Box>
  );
}
