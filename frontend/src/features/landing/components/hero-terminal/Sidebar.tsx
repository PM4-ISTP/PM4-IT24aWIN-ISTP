import { Box, Group, Stack, Text } from "@mantine/core";
import BrandLockup from "../parts/BrandLockup";
import { ACCENT, INK, INK_DIM, LINE, MUTED } from "../../theme";
import { NAV_ITEMS } from "./data";

function SidebarItem({ icon, label, active }: { icon: string; label: string; active?: boolean }) {
  return (
    <Group
      gap={10}
      px={12}
      py={9}
      wrap="nowrap"
      style={{
        borderRadius: 8,
        background: active ? "rgba(93,110,240,0.12)" : "transparent",
        boxShadow: active ? `inset 2px 0 0 ${ACCENT}` : undefined,
        color: active ? INK : INK_DIM,
        fontSize: 13,
      }}
    >
      <Text style={{ color: active ? ACCENT : MUTED, fontSize: 14, lineHeight: 1 }}>{icon}</Text>
      <Text style={{ color: "inherit", fontSize: 13 }}>{label}</Text>
    </Group>
  );
}

export default function Sidebar() {
  return (
    <Stack gap={4} visibleFrom="md" p="22px 12px" style={{ borderRight: `1px solid ${LINE}` }}>
      <Box px={12} mb={14}>
        <BrandLockup size={22} labelSize={13} />
      </Box>
      {NAV_ITEMS.map((item) => (
        <SidebarItem key={item.label} icon={item.icon} label={item.label} active={item.active} />
      ))}
    </Stack>
  );
}
