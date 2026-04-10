import { Group, Paper, Stack, Text, ThemeIcon } from "@mantine/core";
import { labelStyle } from "./_shared";

const accentColors: Record<string, string> = {
  blue: "#3B82F6",
  teal: "#10B981",
  orange: "#F59E0B",
  grape: "#8B5CF6",
};

export default function StatCard({
  icon,
  label,
  value,
  sub,
  color,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  sub?: string;
  color: string;
}) {
  const accent = accentColors[color] ?? "#3B82F6";
  return (
    <Paper
      withBorder
      radius="lg"
      p="lg"
      className="dashboard-stat-card"
      style={{ borderColor: "#E5EEFF", borderTop: `3px solid ${accent}` }}
    >
      <Group align="flex-start" justify="space-between" wrap="nowrap">
        <Stack gap={4}>
          <Text style={labelStyle}>{label}</Text>
          <Text fw={700} size="xl" style={{ color: "#001E41", lineHeight: 1.2 }}>
            {value}
          </Text>
          {sub && (
            <Text size="xs" c="dimmed">
              {sub}
            </Text>
          )}
        </Stack>
        <ThemeIcon size="lg" radius="md" color={color} variant="light">
          {icon}
        </ThemeIcon>
      </Group>
    </Paper>
  );
}
