"use client";

import { ActionIcon, useComputedColorScheme, useMantineColorScheme } from "@mantine/core";
import { IconMoon, IconSun } from "@tabler/icons-react";

export default function ColorSchemeToggle() {
  const { toggleColorScheme } = useMantineColorScheme();
  const computed = useComputedColorScheme("light", { getInitialValueInEffect: true });

  return (
    <ActionIcon
      variant="subtle"
      size="lg"
      radius="md"
      onClick={toggleColorScheme}
      aria-label="Toggle color scheme"
    >
      {computed === "dark" ? <IconSun size={18} /> : <IconMoon size={18} />}
    </ActionIcon>
  );
}
