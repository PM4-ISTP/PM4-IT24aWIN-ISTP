import AdminUserManagement from "@/src/features/admin/components/AdminUserManagement";
import { Stack, Title, Text } from "@mantine/core";

export const dynamic = "force-dynamic";

export default function AdminUsersPage() {
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
          User Management
        </Title>
        <Text style={{ color: "#94a3b8" }} mt={4}>
          Manage users, roles, provisioning, and account status.
        </Text>
      </div>

      <AdminUserManagement keycloakAdminUrl={process.env.KEYCLOAK_ADMIN_URL} />
    </Stack>
  );
}
