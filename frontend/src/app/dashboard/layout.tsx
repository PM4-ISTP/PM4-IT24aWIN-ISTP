import { getServerSession } from "next-auth";
import { authOptions } from "@/src/lib/auth";
import { AppShell, AppShellHeader, AppShellNavbar, AppShellMain, Group } from "@mantine/core";
import UserMenu from "@/src/components/UserMenu";
import DashboardNav from "@/src/components/DashboardNav";

export default async function DashboardLayout({ children }: { children: React.ReactNode }) {
  const session = await getServerSession(authOptions);
  const name = session?.user?.name ?? "Unknown";
  const roles = (session?.roles as string[]) ?? [];

  return (
    <AppShell header={{ height: 60 }} navbar={{ width: 220, breakpoint: "sm" }} padding="md">
      <AppShellHeader style={{ background: "#F8F9FF", borderBottom: "1px solid #E5EEFF" }}>
        <Group h="100%" px="xl" justify="space-between">
          <div>
            <span
              style={{
                fontFamily: "var(--font-manrope), 'Manrope', sans-serif",
                fontWeight: 900,
                letterSpacing: "-0.03em",
                fontSize: "1.4rem",
                color: "#001E41",
              }}
            >
              ISTP
            </span>
            <p
              style={{
                fontFamily: "var(--font-space-grotesk), 'Space Grotesk', sans-serif",
                textTransform: "uppercase",
                letterSpacing: "0.18em",
                fontSize: "0.55rem",
                fontWeight: 700,
                color: "#5B606B",
                margin: 0,
                lineHeight: 1,
              }}
            >
              ZHAW
            </p>
          </div>
          <UserMenu name={name} roles={roles} />
        </Group>
      </AppShellHeader>

      <AppShellNavbar style={{ background: "#EFF4FF", borderRight: "none" }}>
        <DashboardNav roles={roles} />
      </AppShellNavbar>

      <AppShellMain style={{ background: "#FFFFFF" }}>{children}</AppShellMain>
    </AppShell>
  );
}
