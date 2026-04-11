"use client";

import { Tabs, Box, Stack, Text, Button } from "@mantine/core";
import AdminConfigForm from "@/src/components/AdminConfigForm";

interface AdminConfig {
  kubeconfigUploaded: boolean;
  cpuLimit: string;
  memoryLimit: string;
  updatedAt: string;
}

interface AdminTabsProps {
  initialConfig: AdminConfig;
  keycloakAdminUrl: string | undefined;
}

const cardStyle = {
  background: "rgba(255,255,255,0.04)",
  border: "1px solid rgba(255,255,255,0.08)",
  borderRadius: 14,
  padding: "2rem",
  boxShadow: "0 4px 24px rgba(0,0,0,0.25)",
} as const;

export default function AdminTabs({ initialConfig, keycloakAdminUrl }: AdminTabsProps) {
  return (
    <Tabs
      defaultValue="config"
      style={{ maxWidth: 680 }}
      styles={{
        tab: {
          color: "#94a3b8",
          fontFamily: "var(--font-space-grotesk), sans-serif",
          fontWeight: 600,
          "&[data-active]": { color: "#e2e8f0" },
        },
        tabLabel: { fontSize: "0.95rem" },
      }}
    >
      <Tabs.List mb="lg">
        <Tabs.Tab value="config">Platform Config</Tabs.Tab>
        <Tabs.Tab value="users">User Management</Tabs.Tab>
      </Tabs.List>

      <Tabs.Panel value="config">
        <Box style={cardStyle}>
          <AdminConfigForm key={initialConfig.updatedAt ?? ""} initialConfig={initialConfig} />
        </Box>
      </Tabs.Panel>

      <Tabs.Panel value="users">
        <Box style={{ ...cardStyle, maxWidth: 480 }}>
          <Stack gap="md">
            <div>
              <Text fw={600} size="lg" style={{ color: "#e2e8f0" }}>
                Keycloak Admin Console
              </Text>
              <Text style={{ color: "#94a3b8" }} size="sm" mt={4}>
                Manage users and roles directly via the Keycloak Admin Console.
              </Text>
            </div>
            <Button
              component="a"
              href={keycloakAdminUrl}
              target="_blank"
              rel="noopener noreferrer"
              radius="md"
              style={{
                background: "linear-gradient(90deg, #2563eb, #4f46e5)",
                border: "none",
                fontFamily: "var(--font-space-grotesk), sans-serif",
                fontWeight: 600,
                boxShadow: "0 2px 12px rgba(79,70,229,0.3)",
              }}
            >
              Manage Users & Roles with Keycloak
            </Button>
          </Stack>
        </Box>
      </Tabs.Panel>
    </Tabs>
  );
}
