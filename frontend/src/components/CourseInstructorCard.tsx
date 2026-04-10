import { Avatar, Paper, Stack, Text } from "@mantine/core";
import { getInitials } from "@/src/lib/utils";
import type { CourseUserSummary } from "@/src/types/course";

interface CourseInstructorCardProps {
  instructor: CourseUserSummary;
}

export function CourseInstructorCard({ instructor }: CourseInstructorCardProps) {
  return (
    <Paper
      withBorder
      radius="lg"
      shadow="xs"
      style={{ overflow: "hidden", background: "var(--mantine-color-white)", height: "100%" }}
    >
      <div
        style={{
          height: 78,
          background: "linear-gradient(90deg, #6b21ff 0%, #2363ff 100%)",
        }}
      />

      <Stack gap={8} align="center" p="lg" style={{ marginTop: -38, paddingTop: 0 }}>
        <Avatar
          radius="lg"
          size={74}
          color="blue"
          src={instructor.picture ?? undefined}
          style={{
            border: "3px solid var(--mantine-color-white)",
            boxShadow: "0 10px 24px rgba(0,0,0,0.28)",
          }}
        >
          {getInitials(instructor.name)}
        </Avatar>

        <Stack gap={2} align="center" ta="center" style={{ width: "100%" }}>
          <Text fw={700} size="md" c="dark" style={{ lineHeight: 1.1 }}>
            {instructor.name}
          </Text>
          {/* Title is set in Keycloak (user attribute "title") — same principle as picture */}
          {instructor.title && (
            <Text
              size="xs"
              fw={700}
              c="blue.6"
              tt="uppercase"
              style={{ letterSpacing: "0.08em", lineHeight: 1.2 }}
            >
              {instructor.title}
            </Text>
          )}
        </Stack>
      </Stack>
    </Paper>
  );
}
