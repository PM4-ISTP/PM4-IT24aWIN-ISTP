import { getServerSession } from "next-auth";
import { authOptions } from "@/src/shared/lib/auth";
import { AppShell, AppShellHeader, AppShellNavbar, AppShellMain, Group } from "@mantine/core";
import UserMenu from "@/src/features/user/components/UserMenu";
import DashboardNav from "@/src/shared/components/DashboardNav";
import JoinCourseButton from "@/src/features/course/components/enrollment/JoinCourseButton";
import { ROLES } from "@/src/shared/lib/roles";
import SessionErrorHandler from "@/src/features/user/components/SessionErrorHandler";

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
          background: "#0a1220",
          borderBottom: "1px solid rgba(255,255,255,0.06)",
        }}
      >
        <Group h="100%" px="xl" justify="space-between">
          <div>
            <span
              style={{
                fontFamily: "var(--font-space-grotesk), sans-serif",
                fontWeight: 700,
                letterSpacing: "-0.01em",
                fontSize: "1.1rem",
                color: "#e2e8f0",
              }}
            >
              ISTP
            </span>
            <p
              style={{
                fontFamily: "var(--font-space-grotesk), sans-serif",
                textTransform: "uppercase",
                letterSpacing: "0.18em",
                fontSize: "0.55rem",
                fontWeight: 700,
                color: "rgba(255,255,255,0.35)",
                margin: 0,
                lineHeight: 1,
              }}
            >
              ZHAW
            </p>
          </div>
          <Group gap="sm">
            {roles.includes(ROLES.STUDENT) && <JoinCourseButton />}
            <UserMenu name={name} roles={roles} image={image} accountUrl={accountUrl} />
          </Group>
        </Group>
      </AppShellHeader>

      <AppShellNavbar
        style={{
          background: "#0a1220",
          borderRight: "1px solid rgba(255,255,255,0.06)",
        }}
      >
        <DashboardNav roles={roles} />
      </AppShellNavbar>

      <AppShellMain
        style={{
          background: "linear-gradient(160deg, #0b1120 0%, #0e1a2e 50%, #0b1624 100%)",
          minHeight: "calc(100vh - 60px)",
          WebkitFontSmoothing: "antialiased",
          MozOsxFontSmoothing: "grayscale",
        }}
      >
        <SessionErrorHandler />
        {children}
      </AppShellMain>
    </AppShell>
  );
}
