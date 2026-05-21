"use client";
import { Avatar, Box, Group, Menu, Text, UnstyledButton } from "@mantine/core";
import Link from "next/link";
import { ROLES } from "@/src/shared/lib/roles";

const ROLE_CONFIG: Record<string, { label: string; color: string }> = {
  [ROLES.ADMINISTRATOR]: { label: "Admin", color: "red" },
  [ROLES.INSTRUCTOR]: { label: "Instructor", color: "blue" },
  [ROLES.STUDENT]: { label: "Student", color: "gray" },
};

function getRoleConfig(roles: string[]): { label: string; color: string } {
  if (roles.includes(ROLES.ADMINISTRATOR)) return ROLE_CONFIG[ROLES.ADMINISTRATOR];
  if (roles.includes(ROLES.INSTRUCTOR)) return ROLE_CONFIG[ROLES.INSTRUCTOR];
  if (roles.includes(ROLES.STUDENT)) return ROLE_CONFIG[ROLES.STUDENT];
  return { label: "User", color: "gray" };
}

function getInitials(name: string): string {
  return name
    .split(" ")
    .map((n) => n[0])
    .join("")
    .toUpperCase()
    .slice(0, 2);
}

interface UserMenuProps {
  name: string;
  roles: string[];
  image?: string | null;
}

export default function UserMenu({ name, roles, image }: UserMenuProps) {
  const { label: roleLabel, color: roleColor } = getRoleConfig(roles);
  const initials = getInitials(name);

  return (
    <Menu shadow="md" width={220} position="bottom-end">
      <Menu.Target>
        <UnstyledButton aria-label="Open user menu" data-testid="user-menu-trigger">
          <Group gap="sm" wrap="nowrap">
            <Avatar radius="xl" color={roleColor} src={image ?? undefined}>
              {initials}
            </Avatar>
            <Box visibleFrom="sm" style={{ lineHeight: 1.2 }}>
              <Text size="sm" fw={600} style={{ color: "#e2e8f0" }}>
                {name}
              </Text>
              <Text size="xs" c={roleColor}>
                {roleLabel}
              </Text>
            </Box>
          </Group>
        </UnstyledButton>
      </Menu.Target>

      <Menu.Dropdown>
        <Menu.Label>
          <Text size="sm" fw={600} truncate>
            {name}
          </Text>
          <Text size="xs" c={roleColor}>
            {roleLabel}
          </Text>
        </Menu.Label>

        <Menu.Divider />
        <Menu.Item
          component={Link}
          href="/dashboard/profile"
          prefetch={false}
          data-testid="edit-profile-link"
          leftSection={
            <span
              className="material-symbols-outlined"
              style={{
                fontSize: "1.1rem",
                lineHeight: 1,
                fontVariationSettings: "'FILL' 0, 'wght' 300, 'GRAD' 0, 'opsz' 24",
              }}
            >
              manage_accounts
            </span>
          }
        >
          Edit profile
        </Menu.Item>

        <Menu.Divider />

        <Menu.Item
          color="red"
          component={Link}
          href="/logout"
          prefetch={false}
          data-testid="logout-link"
          leftSection={
            <span
              className="material-symbols-outlined"
              style={{
                fontSize: "1.1rem",
                lineHeight: 1,
                fontVariationSettings: "'FILL' 0, 'wght' 300, 'GRAD' 0, 'opsz' 24",
              }}
            >
              logout
            </span>
          }
        >
          Log out
        </Menu.Item>
      </Menu.Dropdown>
    </Menu>
  );
}
