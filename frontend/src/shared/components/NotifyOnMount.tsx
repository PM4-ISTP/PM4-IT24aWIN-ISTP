"use client";

import { useEffect, useRef } from "react";
import { notifications } from "@mantine/notifications";
import type { MantineColor } from "@mantine/core";

export interface NotifyOnMountProps {
  color: MantineColor;
  title: string;
  message: string;
  /** Stable Mantine notification id. When the same component remounts (e.g. user
   *  navigates away and back to a still-failing page), reusing the id refreshes
   *  the existing toast instead of stacking duplicates. */
  id?: string;
}

export default function NotifyOnMount({ color, title, message, id }: NotifyOnMountProps) {
  const didFireRef = useRef(false);

  useEffect(() => {
    if (didFireRef.current) return;
    didFireRef.current = true;
    notifications.show({ id, color, title, message });
  }, [id, color, title, message]);

  return null;
}
