import { Stack, Text, Title } from "@mantine/core";

export default function CoursesPage() {
  return (
    <Stack p="xl" gap="md">
      <Title order={1}>Courses</Title>
      <Text c="dimmed">Challenges and courses will appear here.</Text>
    </Stack>
  );
}
