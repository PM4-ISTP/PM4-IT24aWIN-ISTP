import {
  Badge,
  Button,
  Paper,
  SimpleGrid,
  Stack,
  Table,
  Text,
  Title,
} from "@mantine/core";
import {
  type KeycloakUser,
  getKeycloakActiveSessionCount,
  getKeycloakUserCount,
  getKeycloakUsers,
} from "@/src/app/actions";

// Role guard is handled by middleware (proxy.ts) - no manual check needed here.

/** Fetches a stat value and returns a fallback string on error. */
async function fetchStat<T>(fn: () => Promise<T>, fallback: T): Promise<T> {
  try {
    return await fn();
  } catch {
    return fallback;
  }
}

export default async function AdminDashboard() {
  const [userCount, sessionCount, users] = await Promise.all([
    fetchStat(getKeycloakUserCount, null as number | null),
    fetchStat(getKeycloakActiveSessionCount, null as number | null),
    fetchStat(getKeycloakUsers, [] as KeycloakUser[]),
  ]);

  return (
    <Stack p="xl" gap="xl" maw={900}>
      <div>
        <Title order={1}>Admin Dashboard</Title>
        <Text c="dimmed" mt={4}>
          Manage your platform settings and users.
        </Text>
      </div>

      {/* ── Keycloak stats ─────────────────────────────────────────────── */}
      <SimpleGrid cols={{ base: 1, sm: 2 }}>
        <Paper withBorder radius="md" p="xl">
          <Text c="dimmed" size="sm" tt="uppercase" fw={600}>
            Total Users
          </Text>
          <Title order={2} mt={4}>
            {userCount !== null ? userCount : "—"}
          </Title>
          <Text c="dimmed" size="xs" mt={4}>
            Registered accounts in Keycloak
          </Text>
        </Paper>

        <Paper withBorder radius="md" p="xl">
          <Text c="dimmed" size="sm" tt="uppercase" fw={600}>
            Active Sessions
          </Text>
          <Title order={2} mt={4}>
            {sessionCount !== null ? sessionCount : "—"}
          </Title>
          <Text c="dimmed" size="xs" mt={4}>
            Active client sessions across the realm
          </Text>
        </Paper>
      </SimpleGrid>

      {/* ── User list ──────────────────────────────────────────────────── */}
      {users.length > 0 && (
        <Paper withBorder radius="md" p="xl">
          <Text fw={600} size="lg" mb="md">
            Users
          </Text>
          <Table striped highlightOnHover withTableBorder>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Username</Table.Th>
                <Table.Th>Name</Table.Th>
                <Table.Th>Email</Table.Th>
                <Table.Th>Status</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {users.map((u) => (
                <Table.Tr key={u.id}>
                  <Table.Td>{u.username}</Table.Td>
                  <Table.Td>
                    {[u.firstName, u.lastName].filter(Boolean).join(" ") || "—"}
                  </Table.Td>
                  <Table.Td>{u.email ?? "—"}</Table.Td>
                  <Table.Td>
                    <Badge color={u.enabled ? "green" : "red"} variant="light">
                      {u.enabled ? "Enabled" : "Disabled"}
                    </Badge>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Paper>
      )}

      {/* ── Keycloak Admin Console ─────────────────────────────────────── */}
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
    </Stack>
  );
}
