import AdminTabs from "@/src/features/admin/components/AdminTabs";
import { getApiClient } from "@/src/shared/lib/api/server";
import { Stack, Title, Text } from "@mantine/core";

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
    <Stack p="xl" gap="xl">
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

      <AdminTabs initialConfig={config} keycloakAdminUrl={process.env.KEYCLOAK_ADMIN_URL} />
    </Stack>
  );
}
