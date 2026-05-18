"use client";

import { Alert, Group, Modal, Stack, Text } from "@mantine/core";
import AppButton from "@/src/shared/components/AppButton";

export function CourseDeleteModal({
  opened,
  title,
  isDeleting,
  deleteError,
  onClose,
  onConfirm,
}: {
  opened: boolean;
  title: string;
  isDeleting: boolean;
  deleteError: string | null;
  onClose: () => void;
  onConfirm: () => void;
}) {
  return (
    <Modal opened={opened} onClose={onClose} title="Delete Course" centered>
      <Stack gap="md">
        <Text size="sm">
          Are you sure you want to delete <strong>{title}</strong>? This action cannot be undone.
        </Text>
        {deleteError && (
          <Alert color="red" title="Could not delete course" variant="light">
            Something went wrong. Please try again.
          </Alert>
        )}
        <Group justify="flex-end" gap="sm">
          <AppButton tone="ghost" onClick={onClose} disabled={isDeleting}>
            Cancel
          </AppButton>
          <AppButton tone="danger" loading={isDeleting} disabled={isDeleting} onClick={onConfirm}>
            Delete Course
          </AppButton>
        </Group>
      </Stack>
    </Modal>
  );
}
