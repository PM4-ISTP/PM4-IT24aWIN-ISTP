"use client";

import { usePathname } from "next/navigation";
import Link from "next/link";
import { ROLES } from "@/src/shared/lib/roles";

interface DashboardNavProps {
  roles: string[];
}

const labelStyle: React.CSSProperties = {
  fontFamily: "var(--font-space-grotesk), 'Space Grotesk', sans-serif",
  textTransform: "uppercase",
  letterSpacing: "0.12em",
  fontSize: "0.7rem",
  fontWeight: 700,
};

const sectionLabelStyle: React.CSSProperties = {
  ...labelStyle,
  fontSize: "0.58rem",
  color: "rgba(255,255,255,0.28)",
  letterSpacing: "0.2em",
};

function NavItem({
  href,
  label,
  icon,
  active,
}: {
  href: string;
  label: string;
  icon: string;
  active: boolean;
}) {
  return (
    <div style={{ position: "relative" }}>
      <Link
        href={href}
        className="nav-link"
        style={{
          paddingLeft: "1.75rem",
          paddingTop: "0.7rem",
          paddingBottom: "0.7rem",
          paddingRight: "1.5rem",
          color: active ? "#60a5fa" : "rgba(255,255,255,0.52)",
          fontWeight: active ? 700 : 500,
          background: active ? "rgba(96,165,250,0.07)" : undefined,
        }}
      >
        <span
          className="material-symbols-outlined"
          style={{
            fontSize: "1.2rem",
            lineHeight: 1,
            flexShrink: 0,
            fontVariationSettings: active
              ? "'FILL' 1, 'wght' 400, 'GRAD' 0, 'opsz' 24"
              : "'FILL' 0, 'wght' 300, 'GRAD' 0, 'opsz' 24",
          }}
        >
          {icon}
        </span>
        <span style={{ ...labelStyle, color: "inherit" }}>{label}</span>
      </Link>
      {active && (
        <div
          style={{
            position: "absolute",
            right: 0,
            top: "50%",
            transform: "translateY(-50%)",
            width: 3,
            height: "55%",
            background: "#60a5fa",
            borderRadius: "4px 0 0 4px",
            boxShadow: "0 0 10px rgba(96,165,250,0.5)",
          }}
        />
      )}
    </div>
  );
}

export default function DashboardNav({ roles }: DashboardNavProps) {
  const pathname = usePathname();

  const isAdmin = roles.includes(ROLES.ADMINISTRATOR);
  const isInstructor = roles.includes(ROLES.INSTRUCTOR) || isAdmin;

  return (
    <nav style={{ display: "flex", flexDirection: "column", gap: 0, paddingTop: "0.5rem" }}>
      <NavItem href="/dashboard" label="Home" icon="home" active={pathname === "/dashboard"} />
      <NavItem
        href="/dashboard/courses"
        label="My Courses"
        icon="menu_book"
        active={pathname.startsWith("/dashboard/courses")}
      />
      <NavItem
        href="/dashboard/catalog"
        label="Browse / Catalog"
        icon="travel_explore"
        active={pathname.startsWith("/dashboard/catalog")}
      />

      {isInstructor && (
        <>
          <p style={{ ...sectionLabelStyle, padding: "1.5rem 1.75rem 0.4rem" }}>
            Course Management
          </p>
          <NavItem
            href="/dashboard/instructor"
            label="Dashboard"
            icon="dashboard"
            active={
              pathname === "/dashboard/instructor" ||
              (pathname.startsWith("/dashboard/instructor/") &&
                !pathname.startsWith("/dashboard/instructor/challenges"))
            }
          />
          <NavItem
            href="/dashboard/instructor/challenges"
            label="Challenges"
            icon="flag"
            active={pathname.startsWith("/dashboard/instructor/challenges")}
          />
        </>
      )}

      {isAdmin && (
        <>
          <p style={{ ...sectionLabelStyle, padding: "1.5rem 1.75rem 0.4rem" }}>Admin</p>
          <NavItem
            href="/dashboard/admin"
            label="Dashboard"
            icon="space_dashboard"
            active={pathname === "/dashboard/admin"}
          />
          <NavItem
            href="/dashboard/admin/users"
            label="Users"
            icon="group"
            active={pathname.startsWith("/dashboard/admin/users")}
          />
        </>
      )}
    </nav>
  );
}
