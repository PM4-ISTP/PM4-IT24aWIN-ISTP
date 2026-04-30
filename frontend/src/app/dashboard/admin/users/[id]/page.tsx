import AdminUserProfile from "@/src/features/admin/components/AdminUserProfile";
import { Stack, Title, Text } from "@mantine/core";

export const dynamic = "force-dynamic";

export default async function AdminUserProfilePage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
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
          User
        </Title>
        <Text style={{ color: "#94a3b8" }} mt={4}>
          View and edit user profile data (email is read-only).
        </Text>
      </div>

      <AdminUserProfile userId={id} />
    </Stack>
  );
}
