import { Box, Group, Text } from "@mantine/core";

export default function ActivityItem({
  label,
  time,
  color,
}: {
  label: string;
  time: string;
  color: string;
}) {
  return (
    <Group
      justify="space-between"
      wrap="nowrap"
      style={{
        padding: "0.6rem 0",
        borderBottom: "1px solid rgba(255,255,255,0.04)",
      }}
    >
      <Group gap="sm" wrap="nowrap" style={{ minWidth: 0 }}>
        <Box
          style={{
            width: 7,
            height: 7,
            borderRadius: "50%",
            background: color,
            flexShrink: 0,
            boxShadow: `0 0 6px ${color}66`,
          }}
        />
        <Text size="sm" c="dimmed" truncate>
          {label}
        </Text>
      </Group>
      <Text size="xs" c="dimmed" style={{ flexShrink: 0, paddingLeft: "0.5rem" }}>
        {time}
      </Text>
    </Group>
  );
}
