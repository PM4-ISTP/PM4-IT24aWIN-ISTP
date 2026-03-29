import AdminConfigForm from "@/src/components/AdminConfigForm";
import { Stack, Title, Text, Button, Paper } from "@mantine/core";

// Role guard is handled by middleware (proxy.ts) - no manual check needed here.

export default function AdminDashboard() {
  return (
    <Stack p="xl" gap="xl" maw={600}>
      <div>
        <Title order={1}>Admin Dashboard</Title>
        <Text c="dimmed" mt={4}>
          Manage your platform settings and users.
        </Text>
      </div>

      <Paper withBorder radius="md" p="xl">
        <Stack gap="md">
          <div>
            <Text fw={600} size="lg">
              Keycloak Admin Console
            </Text>
            <Text c="dimmed" size="sm" mt={4}>
              Manage users and roles directly via the Keycloak Admin Console.
            </Text>
          </div>
          <Button
            component="a"
            href={process.env.KEYCLOAK_ADMIN_URL}
            target="_blank"
            rel="noopener noreferrer"
            variant="filled"
            radius="md"
          >
            Manage Users &amp; Roles with Keycloak
          </Button>
        </Stack>
      </Paper>
      <Paper withBorder radius="md" p="xl">
        <Stack gap="md">
          <AdminConfigForm />
        </Stack>
      </Paper>
    </Stack>
  );
}
