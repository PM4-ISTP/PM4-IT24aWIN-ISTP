import AdminConfigForm from "@/src/components/AdminConfigForm";
import { getApiClient } from "@/src/lib/api/server";
import { Box, Button, Paper, Stack, Title, Text } from "@mantine/core";

// Role guard is handled by middleware (proxy.ts) - no manual check needed here.
export const dynamic = "force-dynamic";

export default async function AdminDashboard() {
  const client = await getApiClient();
  const { data } = await client.GET("/api/admin/config");
  const config = data ?? {
    kubeconfigUploaded: false,
    cpuLimit: "",
    memoryLimit: "",
    updatedAt: "",
  };

  return (
    <Stack p="xl" gap="xl" maw={600}>
      <div>
        <Title
          order={1}
          style={{
            color: "#f1f5f9",
            fontFamily: "var(--font-space-grotesk), sans-serif",
            fontWeight: 700,
          }}
        >
          Admin Dashboard
        </Title>
        <Text style={{ color: "#94a3b8" }} mt={4}>
          Manage your platform settings and users.
        </Text>
      </div>

      <Box
        style={{
          background: "rgba(255,255,255,0.04)",
          border: "1px solid rgba(255,255,255,0.08)",
          borderRadius: 14,
          padding: "2rem",
          boxShadow: "0 4px 24px rgba(0,0,0,0.25)",
        }}
      >
        <Stack gap="md">
          <div>
            <Text fw={600} size="lg" style={{ color: "#e2e8f0" }}>
              Keycloak Admin Console
            </Text>
            <Text style={{ color: "#94a3b8" }} size="sm" mt={4}>
              Manage users and roles directly via the Keycloak Admin Console.
            </Text>
          </div>
          <Button
            component="a"
            href={process.env.KEYCLOAK_ADMIN_URL}
            target="_blank"
            rel="noopener noreferrer"
            radius="md"
            style={{
              background: "linear-gradient(90deg, #2563eb, #4f46e5)",
              border: "none",
              fontFamily: "var(--font-space-grotesk), sans-serif",
              fontWeight: 600,
              boxShadow: "0 2px 12px rgba(79,70,229,0.3)",
            }}
          >
            Manage Users &amp; Roles with Keycloak
          </Button>
        </Stack>
        <Paper withBorder radius="md" p="xl">
          <Stack gap="md">
            <AdminConfigForm key={config.updatedAt ?? ""} initialConfig={config} />
          </Stack>
        </Paper>
      </Box>
    </Stack>
  );
}
