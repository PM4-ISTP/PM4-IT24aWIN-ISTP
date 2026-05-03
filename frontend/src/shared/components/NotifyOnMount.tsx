"use client";

import { useEffect, useRef } from "react";
import { notifications } from "@mantine/notifications";
import type { MantineColor } from "@mantine/core";

export interface NotifyOnMountProps {
  color: MantineColor;
  title: string;
  message: string;
}

export default function NotifyOnMount({ color, title, message }: NotifyOnMountProps) {
  const didFireRef = useRef(false);

  useEffect(() => {
    if (didFireRef.current) return;
    didFireRef.current = true;
    notifications.show({ color, title, message });
  }, [color, title, message]);

  return null;
}
