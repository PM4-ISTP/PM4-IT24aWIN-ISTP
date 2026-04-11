import { Avatar, Stack, Text } from "@mantine/core";
import { getInitials } from "@/src/lib/utils";
import type { CourseUserSummary } from "@/src/types/course";

interface CourseInstructorCardProps {
  instructor: CourseUserSummary;
}

export function CourseInstructorCard({ instructor }: CourseInstructorCardProps) {
  return (
    <div
      style={{
        background: "rgba(255,255,255,0.04)",
        border: "1px solid rgba(255,255,255,0.08)",
        borderRadius: 14,
        overflow: "hidden",
        height: "100%",
        boxShadow: "0 4px 16px rgba(0,0,0,0.2)",
      }}
    >
      <div
        style={{
          height: 78,
          background: "linear-gradient(90deg, rgba(79,70,229,0.6) 0%, rgba(37,99,235,0.6) 100%)",
        }}
      />

      <Stack gap={8} align="center" p="lg" style={{ marginTop: -38, paddingTop: 0 }}>
        <Avatar
          radius="lg"
          size={74}
          color="blue"
          src={instructor.picture ?? undefined}
          style={{
            border: "3px solid rgba(255,255,255,0.12)",
            boxShadow: "0 8px 20px rgba(0,0,0,0.4)",
          }}
        >
          {getInitials(instructor.name)}
        </Avatar>

        <Stack gap={2} align="center" ta="center" style={{ width: "100%" }}>
          <Text fw={700} size="md" style={{ color: "#e2e8f0", lineHeight: 1.1 }}>
            {instructor.name}
          </Text>
          {instructor.title && (
            <Text
              size="xs"
              fw={700}
              style={{ color: "#60a5fa", letterSpacing: "0.08em", lineHeight: 1.2 }}
              tt="uppercase"
            >
              {instructor.title}
            </Text>
          )}
        </Stack>
      </Stack>
    </div>
  );
}
