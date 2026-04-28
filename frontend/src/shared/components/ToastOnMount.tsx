"use client";

import { useEffect, useRef } from "react";
import { Affix, Notification } from "@mantine/core";
import { IconAlertTriangle, IconCheck, IconX } from "@tabler/icons-react";
import { useToast } from "@/src/shared/hooks/useToast";

type ToastOnMountProps = {
  color: "green" | "red" | "orange";
  title: string;
  message: string;
};

export default function ToastOnMount({ color, title, message }: ToastOnMountProps) {
  const toast = useToast();
  const didShowRef = useRef(false);

  useEffect(() => {
    if (didShowRef.current) return;
    didShowRef.current = true;
    toast.show();
  }, [toast]);

  if (!toast.visible) return null;

  const icon =
    color === "green" ? (
      <IconCheck size={18} />
    ) : color === "orange" ? (
      <IconAlertTriangle size={18} />
    ) : (
      <IconX size={18} />
    );

  return (
    <Affix position={{ bottom: 20, right: 20 }} style={{ zIndex: 3000 }}>
      <Notification color={color} title={title} onClose={toast.hide} withCloseButton icon={icon}>
        {message}
      </Notification>
    </Affix>
  );
}
