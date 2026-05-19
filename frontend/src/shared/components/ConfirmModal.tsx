"use client";

import { Group, Modal, Stack, Text } from "@mantine/core";
import type { ReactNode } from "react";
import AppButton from "@/src/shared/components/AppButton";

export interface ConfirmModalProps {
  opened: boolean;
  onClose: () => void;
  onConfirm: () => void | Promise<void>;
  title: ReactNode;
  message?: ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  loading?: boolean;
  /** Renders the confirm button in the destructive (red) style. */
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
  loading = false,
  danger = false,
}: ConfirmModalProps) {
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
          <AppButton tone="ghost" onClick={onClose} disabled={loading}>
            {cancelLabel}
          </AppButton>
          <AppButton tone={danger ? "danger" : "primary"} onClick={handleConfirm} loading={loading}>
            {confirmLabel}
          </AppButton>
        </Group>
      </Stack>
    </Modal>
  );
}
