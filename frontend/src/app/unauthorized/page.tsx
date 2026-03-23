import { Center, Stack, Title, Text, Button } from "@mantine/core";
import Link from "next/link";

export default function UnauthorizedPage() {
  return (
    <Center h="100vh">
      <Stack align="center" gap="md">
        <Title order={1} c="red">
          403 – Unauthorized
        </Title>
        <Text c="dimmed" ta="center">
          You do not have permission to access this page.
        </Text>
        <Button component={Link} href="/dashboard" variant="light">
          Go to Dashboard
        </Button>
      </Stack>
    </Center>
  );
}
