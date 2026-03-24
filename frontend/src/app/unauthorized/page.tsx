import { Center, Stack, Title, Text, Button } from "@mantine/core";

export default function UnauthorizedPage() {
  return (
    <Center h="100vh">
      <Stack gap="md" align="center" ta="center" maw={400}>
        <Title order={2}>Access Denied</Title>
        <Text c="dimmed">You do not have permission to access this page.</Text>
        <Button component="a" href="/dashboard" variant="light">
          Back to Dashboard
        </Button>
      </Stack>
    </Center>
  );
}
