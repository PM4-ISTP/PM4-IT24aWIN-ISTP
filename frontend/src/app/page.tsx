import { getServerSession } from "next-auth";
import { authOptions } from "@/src/lib/auth";
import { redirect } from "next/navigation";
import { Center, Stack, Title, Text, Paper } from "@mantine/core";
import Login from "@/src/components/Login";

export default async function Home() {
  const session = await getServerSession(authOptions);

  if (session) {
    redirect("/dashboard");
  }

  return (
    <Center h="100vh">
      <Paper withBorder radius="lg" p="xl" w={380} shadow="sm">
        <Stack gap="lg" align="center">
          <Stack gap={4} align="center">
            <Title order={2}>ISTP</Title>
            <Text c="dimmed" size="sm" ta="center">
              Interactive Security Training Platform
            </Text>
          </Stack>

          <Login />
        </Stack>
      </Paper>
    </Center>
  );
}
