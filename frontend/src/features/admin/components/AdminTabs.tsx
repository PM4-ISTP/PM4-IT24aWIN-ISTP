"use client";

import { Tabs, Box } from "@mantine/core";
import AdminConfigForm from "@/src/features/admin/components/AdminConfigForm";
import AdminCourseManagement from "@/src/features/admin/components/AdminCourseManagement";
import AdminChallengeManagement from "@/src/features/admin/components/AdminChallengeManagement";
import AdminTopicManagement from "@/src/features/admin/components/AdminTopicManagement";
import type { components } from "@/src/shared/lib/api/schema";

type AdminConfigResponse = components["schemas"]["AdminConfigResponse"];

interface AdminTabsProps {
  initialConfig: AdminConfigResponse;
}

const cardStyle = {
  background: "rgba(255,255,255,0.04)",
  border: "1px solid rgba(255,255,255,0.08)",
  borderRadius: 14,
  padding: "2rem",
  boxShadow: "0 4px 24px rgba(0,0,0,0.25)",
} as const;

export default function AdminTabs({ initialConfig }: AdminTabsProps) {
  return (
    <Tabs
      defaultValue="config"
      style={{ maxWidth: 1100, width: "100%" }}
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
        <Tabs.Tab value="courses">Courses</Tabs.Tab>
        <Tabs.Tab value="labs">Labs</Tabs.Tab>
        <Tabs.Tab value="topics">Topics</Tabs.Tab>
      </Tabs.List>

      <Tabs.Panel value="config">
        <Box style={cardStyle}>
          <AdminConfigForm key={initialConfig.updatedAt ?? ""} initialConfig={initialConfig} />
        </Box>
      </Tabs.Panel>

      <Tabs.Panel value="courses">
        <Box style={cardStyle}>
          <AdminCourseManagement />
        </Box>
      </Tabs.Panel>

      <Tabs.Panel value="labs">
        <Box style={cardStyle}>
          <AdminChallengeManagement />
        </Box>
      </Tabs.Panel>

      <Tabs.Panel value="topics">
        <Box style={{ ...cardStyle, maxWidth: 720 }}>
          <AdminTopicManagement />
        </Box>
      </Tabs.Panel>
    </Tabs>
  );
}
