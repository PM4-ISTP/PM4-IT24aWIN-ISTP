import { getServerSession } from "next-auth";
import { authOptions } from "@/src/shared/lib/auth";
import { AppShell, AppShellHeader, AppShellNavbar, AppShellMain, Group } from "@mantine/core";
import UserMenu from "@/src/features/user/components/UserMenu";
import DashboardNav from "@/src/shared/components/DashboardNav";
import JoinCourseButton from "@/src/features/course/components/enrollment/JoinCourseButton";
import { ROLES } from "@/src/shared/lib/roles";
import SessionErrorHandler from "@/src/features/user/components/SessionErrorHandler";
import TimeTracker from "@/src/shared/components/TimeTracker";

export default async function DashboardLayout({ children }: { children: React.ReactNode }) {
  const session = await getServerSession(authOptions);
  const name = session?.user?.name ?? "Unknown";
  const image = session?.user?.image ?? null;
  const roles = (session?.roles as string[]) ?? [];

  return (
    <AppShell header={{ height: 60 }} navbar={{ width: 224, breakpoint: "sm" }} padding="md">
      <AppShellHeader
        style={{
          background: "rgba(10,18,32,0.95)",
          backdropFilter: "blur(12px)",
          borderBottom: "1px solid rgba(255,255,255,0.07)",
        }}
      >
        <Group h="100%" px="xl" justify="space-between" wrap="nowrap">
          {/* Logo */}
          <div style={{ display: "flex", flexDirection: "column", gap: 1, flexShrink: 0 }}>
            <span
              style={{
                fontFamily: "var(--font-space-grotesk), sans-serif",
                fontWeight: 700,
                letterSpacing: "0.02em",
                fontSize: "1.05rem",
                color: "#e2e8f0",
                lineHeight: 1,
              }}
            >
              ISTP
            </span>
            <span
              style={{
                fontFamily: "var(--font-space-grotesk), sans-serif",
                textTransform: "uppercase",
                letterSpacing: "0.2em",
                fontSize: "0.52rem",
                fontWeight: 700,
                color: "rgba(255,255,255,0.3)",
                lineHeight: 1,
              }}
            >
              ZHAW
            </span>
          </div>

          {/* Right side */}
          <Group gap="sm" wrap="nowrap">
            {roles.includes(ROLES.STUDENT) && <JoinCourseButton />}
            <UserMenu name={name} roles={roles} image={image} />
          </Group>
        </Group>
      </AppShellHeader>

      <AppShellNavbar
        style={{
          background: "rgba(10,18,32,0.97)",
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
        <TimeTracker userId={session?.userId ?? null} />
        <SessionErrorHandler />
        {children}
      </AppShellMain>
    </AppShell>
  );
}
