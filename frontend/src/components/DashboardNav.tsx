"use client";

import { NavLink, Stack, Text } from "@mantine/core";
import { usePathname } from "next/navigation";
import Link from "next/link";
import { ROLES } from "@/src/lib/roles";

interface DashboardNavProps {
  roles: string[];
}

export default function DashboardNav({ roles }: DashboardNavProps) {
  const pathname = usePathname();

  const isAdmin = roles.includes(ROLES.ADMINISTRATOR);
  const isInstructor = roles.includes(ROLES.INSTRUCTOR) || isAdmin;

  return (
    <Stack gap={4} p="sm">
      <Text size="xs" fw={600} c="dimmed" px="sm" mb={4} tt="uppercase">
        Menu
      </Text>

      <NavLink
        component={Link}
        href="/dashboard"
        label="Home"
        active={pathname === "/dashboard"}
        leftSection={<span>🏠</span>}
      />

      <NavLink
        component={Link}
        href="/dashboard/courses"
        label="Courses"
        active={pathname.startsWith("/dashboard/courses")}
        leftSection={<span>📚</span>}
      />

      {isInstructor && (
        <>
          <Text size="xs" fw={600} c="dimmed" px="sm" mt="md" mb={4} tt="uppercase">
            Instructor
          </Text>
          <NavLink
            component={Link}
            href="/dashboard/instructor"
            label="Dashboard"
            active={pathname.startsWith("/dashboard/instructor")}
            leftSection={<span>🎓</span>}
          />
        </>
      )}

      {isAdmin && (
        <>
          <Text size="xs" fw={600} c="dimmed" px="sm" mt="md" mb={4} tt="uppercase">
            Admin
          </Text>
          <NavLink
            component={Link}
            href="/dashboard/admin"
            label="Dashboard"
            active={pathname.startsWith("/dashboard/admin")}
            leftSection={<span>⚙️</span>}
          />
        </>
      )}
    </Stack>
  );
}
