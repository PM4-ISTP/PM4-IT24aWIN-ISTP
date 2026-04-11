import { Badge, Box, Group, Progress, Stack, Text, ThemeIcon } from "@mantine/core";

export default function CourseCard({
  title,
  topic,
  progress,
  icon,
}: {
  title: string;
  topic: string;
  progress: number;
  icon: React.ReactNode;
}) {
  return (
    <Box
      className="dashboard-course-card"
      style={{
        background: "rgba(255,255,255,0.04)",
        border: "1px solid rgba(255,255,255,0.08)",
        borderRadius: 14,
        padding: "1.25rem",
        boxShadow: "0 4px 16px rgba(0,0,0,0.2)",
      }}
    >
      <Stack gap="sm">
        <Group align="flex-start" justify="space-between" wrap="nowrap">
          <Stack gap={4} style={{ flex: 1 }}>
            <Text
              fw={600}
              lineClamp={2}
              style={{
                color: "#e2e8f0",
                fontSize: "0.9rem",
                lineHeight: 1.45,
                fontFamily: "var(--font-space-grotesk), sans-serif",
              }}
            >
              {title}
            </Text>
            <Badge
              size="sm"
              variant="outline"
              style={{
                color: "#60a5fa",
                borderColor: "rgba(96,165,250,0.25)",
                background: "rgba(96,165,250,0.06)",
                fontSize: "0.68rem",
                fontFamily: "var(--font-space-grotesk), sans-serif",
              }}
            >
              {topic}
            </Badge>
          </Stack>
          <ThemeIcon
            size="md"
            radius="md"
            style={{
              background: "rgba(96,165,250,0.1)",
              color: "#60a5fa",
              flexShrink: 0,
            }}
          >
            {icon}
          </ThemeIcon>
        </Group>
        <Box>
          <Group justify="space-between" mb={6}>
            <Text
              size="xs"
              style={{
                color: "rgba(255,255,255,0.35)",
                fontFamily: "var(--font-space-grotesk), sans-serif",
                textTransform: "uppercase",
                letterSpacing: "0.08em",
                fontWeight: 600,
              }}
            >
              Progress
            </Text>
            <Text
              size="xs"
              fw={600}
              style={{ color: "#60a5fa", fontFamily: "var(--font-space-grotesk), sans-serif" }}
            >
              {progress}%
            </Text>
          </Group>
          <Progress
            value={progress}
            size="xs"
            radius="xl"
            styles={{
              root: { background: "rgba(255,255,255,0.08)" },
              section: { background: "linear-gradient(90deg, #2563eb, #4f46e5)" },
            }}
          />
        </Box>
      </Stack>
    </Box>
  );
}
