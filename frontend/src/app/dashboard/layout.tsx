import { getServerSession } from "next-auth";
import { authOptions } from "@/src/lib/auth";
import { AppShell, AppShellHeader, AppShellNavbar, AppShellMain, Group } from "@mantine/core";
import UserMenu from "@/src/components/UserMenu";
import DashboardNav from "@/src/components/DashboardNav";
import ColorSchemeToggle from "@/src/components/ColorSchemeToggle";

export default async function DashboardLayout({ children }: { children: React.ReactNode }) {
  const session = await getServerSession(authOptions);
  const name = session?.user?.name ?? "Unknown";
  const image = session?.user?.image ?? null;
  const roles = (session?.roles as string[]) ?? [];
  const keycloakIssuer = process.env.AUTH_KEYCLOAK_ISSUER;
  const accountUrl = keycloakIssuer ? `${keycloakIssuer.replace(/\/$/, "")}/account` : undefined;

  return (
    <AppShell header={{ height: 60 }} navbar={{ width: 220, breakpoint: "sm" }} padding="md">
      <AppShellHeader
        style={{
          background: "var(--istp-header-bg)",
          borderBottom: "1px solid var(--istp-header-border)",
        }}
      >
        <Group h="100%" px="xl" justify="space-between">
          <div>
            <span
              style={{
                fontFamily: "var(--font-manrope), 'Manrope', sans-serif",
                fontWeight: 900,
                letterSpacing: "-0.03em",
                fontSize: "1.4rem",
                color: "var(--istp-heading-color)",
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
                color: "var(--istp-label-color)",
                margin: 0,
                lineHeight: 1,
              }}
            >
              ZHAW
            </p>
          </div>
          <Group gap="sm">
            <ColorSchemeToggle />
            <UserMenu name={name} roles={roles} image={image} accountUrl={accountUrl} />
          </Group>
        </Group>
      </AppShellHeader>

      <AppShellNavbar style={{ background: "var(--istp-nav-bg)", borderRight: "none" }}>
        <DashboardNav roles={roles} />
      </AppShellNavbar>

      <AppShellMain style={{ background: "var(--istp-main-bg)" }}>{children}</AppShellMain>
    </AppShell>
  );
}
