"use client";

import { usePathname } from "next/navigation";
import Link from "next/link";
import { ROLES } from "@/src/lib/roles";

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
  fontSize: "0.6rem",
  color: "var(--istp-label-color)",
  letterSpacing: "0.18em",
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
        style={{
          display: "flex",
          alignItems: "center",
          gap: "0.75rem",
          paddingLeft: "2rem",
          paddingTop: "0.75rem",
          paddingBottom: "0.75rem",
          paddingRight: "1rem",
          textDecoration: "none",
          transition: "color 0.15s",
          color: active ? "var(--istp-accent)" : "var(--istp-nav-inactive)",
          fontWeight: active ? 700 : 400,
        }}
      >
        <span
          className="material-symbols-outlined"
          style={{
            fontSize: "1.25rem",
            lineHeight: 1,
            fontVariationSettings: "'FILL' 0, 'wght' 300, 'GRAD' 0, 'opsz' 24",
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
            background: "var(--istp-accent)",
            borderRadius: "4px 0 0 4px",
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
          <p style={{ ...sectionLabelStyle, padding: "1.5rem 2rem 0.5rem" }}>Course Management</p>
          <NavItem
            href="/dashboard/instructor"
            label="Dashboard"
            icon="dashboard"
            active={pathname.startsWith("/dashboard/instructor")}
          />
        </>
      )}

      {isAdmin && (
        <>
          <p style={{ ...sectionLabelStyle, padding: "1.5rem 2rem 0.5rem" }}>Admin</p>
          <NavItem
            href="/dashboard/admin"
            label="Dashboard"
            icon="space_dashboard"
            active={pathname.startsWith("/dashboard/admin")}
          />
        </>
      )}
    </nav>
  );
}
