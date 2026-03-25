import TestButton from "@/src/components/TestButton";
import { Stack, Title, Text } from "@mantine/core";

export default function CoursesPage() {
  return (
    <Stack p="xl" gap="md">
      <Title order={1}>Courses</Title>
      <Text c="dimmed">Challenges and courses will appear here.</Text>
      <TestButton />
    </Stack>
  );
}
