"use client";

import { Button, Group, Modal, Stack, Text } from "@mantine/core";
import type { MantineColor } from "@mantine/core";
import type { ReactNode } from "react";

export interface ConfirmModalProps {
  opened: boolean;
  onClose: () => void;
  onConfirm: () => void | Promise<void>;
  title: ReactNode;
  message?: ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  confirmColor?: MantineColor;
  loading?: boolean;
  danger?: boolean;
}

export function ConfirmModal({
  opened,
  onClose,
  onConfirm,
  title,
  message,
  confirmLabel = "Confirm",
  cancelLabel = "Cancel",
  confirmColor,
  loading = false,
  danger = false,
}: ConfirmModalProps) {
  const resolvedColor = confirmColor ?? (danger ? "red" : "blue");

  const handleConfirm = () => {
    void onConfirm();
  };

  return (
    <Modal opened={opened} onClose={onClose} title={title} centered>
      <Stack gap="md">
        {message ? (
          <Text size="sm" c="dimmed">
            {message}
          </Text>
        ) : null}
        <Group justify="flex-end" gap="sm">
          <Button variant="default" onClick={onClose} disabled={loading}>
            {cancelLabel}
          </Button>
          <Button color={resolvedColor} onClick={handleConfirm} loading={loading}>
            {confirmLabel}
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
