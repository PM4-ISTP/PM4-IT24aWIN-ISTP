import { getServerSession } from "next-auth";
import { authOptions } from "@/src/lib/auth";
import { AppShell, AppShellHeader, AppShellMain, Group } from "@mantine/core";
import UserMenu from "@/src/components/UserMenu";
import { Anaheim } from "next/font/google";

const anaheim = Anaheim({ weight: "400", subsets: ["latin"] });

export default async function DashboardLayout({ children }: { children: React.ReactNode }) {
  const session = await getServerSession(authOptions);
  const name = session?.user?.name ?? "Unknown";
  const roles = (session?.roles as string[]) ?? [];

  return (
    <AppShell header={{ height: 60 }} padding="md">
      <AppShellHeader>
        <Group h="100%" px="xl" justify="space-between">
          <span
            className={anaheim.className}
            style={{
              letterSpacing: "0.25em",
              fontSize: "1.4rem",
              background: "linear-gradient(135deg, #c8960c 0%, #ffd700 50%, #c8960c 100%)",
              WebkitBackgroundClip: "text",
              WebkitTextFillColor: "transparent",
              backgroundClip: "text",
            }}
          >
            ISTP
          </span>
          <UserMenu name={name} roles={roles} />
        </Group>
      </AppShellHeader>

      <AppShellMain>{children}</AppShellMain>
    </AppShell>
  );
}
