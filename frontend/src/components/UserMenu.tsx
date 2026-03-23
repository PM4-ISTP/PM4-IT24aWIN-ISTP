"use client";

import { signOut } from "next-auth/react";
import { Avatar, Group, Menu, Text, UnstyledButton } from "@mantine/core";
import { ROLES } from "@/src/lib/roles";

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
}

export default function UserMenu({ name, roles }: UserMenuProps) {
  const { label: roleLabel, color: roleColor } = getRoleConfig(roles);
  const initials = getInitials(name);

  return (
    <Menu shadow="md" width={220} position="bottom-end">
      <Menu.Target>
        <UnstyledButton>
          <Group gap="sm">
            <Avatar radius="xl" color={roleColor}>
              {initials}
            </Avatar>
            <div style={{ lineHeight: 1.2 }}>
              <Text size="sm" fw={600}>
                {name}
              </Text>
              <Text size="xs" c={roleColor}>
                {roleLabel}
              </Text>
            </div>
          </Group>
        </UnstyledButton>
      </Menu.Target>

      <Menu.Dropdown>
        <Menu.Label>
          <Text size="sm" fw={600}>
            {name}
          </Text>
          <Text size="xs" c={roleColor}>
            {roleLabel}
          </Text>
        </Menu.Label>

        <Menu.Divider />

        <Menu.Item color="red" leftSection={<span>â†©</span>} onClick={() => void signOut()}>
          Log out
        </Menu.Item>
      </Menu.Dropdown>
    </Menu>
  );
}
