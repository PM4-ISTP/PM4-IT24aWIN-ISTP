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
    <Group className="dashboard-activity-item" justify="space-between" wrap="nowrap">
      <Group gap="sm" wrap="nowrap">
        <Box
          style={{
            width: 8,
            height: 8,
            borderRadius: "50%",
            background: color,
            flexShrink: 0,
          }}
        />
        <Text size="md" c="dimmed">
          {label}
        </Text>
      </Group>
      <Text size="sm" c="dimmed" style={{ flexShrink: 0 }}>
        {time}
      </Text>
    </Group>
  );
}
