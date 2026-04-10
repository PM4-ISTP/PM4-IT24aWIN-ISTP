import { Group, Paper, Stack, Text, ThemeIcon } from "@mantine/core";
import { labelStyle } from "./_shared";

const accentColors: Record<string, string> = {
  blue: "#0071e3",
  teal: "#34c759",
  orange: "#ff9500",
  grape: "#bf5af2",
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
  const accent = accentColors[color] ?? "#0071e3";
  return (
    <Paper
      withBorder
      radius="lg"
      p="lg"
      className="dashboard-stat-card"
      style={{ borderColor: "var(--istp-card-border)", borderTop: `3px solid ${accent}` }}
    >
      <Group align="flex-start" justify="space-between" wrap="nowrap">
        <Stack gap={4}>
          <Text style={labelStyle}>{label}</Text>
          <Text fw={700} size="xl" style={{ color: "var(--istp-heading-color)", lineHeight: 1.2 }}>
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
