import { Group, Stack, Text, ThemeIcon } from "@mantine/core";
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
    <div
      style={{
        background: "rgba(255,255,255,0.04)",
        border: "1px solid rgba(255,255,255,0.08)",
        borderTop: `3px solid ${accent}`,
        borderRadius: 12,
        padding: "1.25rem",
        boxShadow: "0 2px 8px rgba(0,0,0,0.2)",
        transition: "border-color 150ms ease, box-shadow 150ms ease",
      }}
    >
      <Group align="flex-start" justify="space-between" wrap="nowrap">
        <Stack gap={4}>
          <Text style={labelStyle}>{label}</Text>
          <Text fw={700} size="xl" style={{ color: "#e2e8f0", lineHeight: 1.2 }}>
            {value}
          </Text>
          {sub && (
            <Text size="xs" c="dimmed" mt={2}>
              {sub}
            </Text>
          )}
        </Stack>
        <ThemeIcon
          size={42}
          radius="md"
          color={color}
          variant="light"
          style={{ flexShrink: 0 }}
        >
          {icon}
        </ThemeIcon>
      </Group>
    </div>
  );
}
