"use client";

import { Alert, Button, Group, Modal, Stack, Text } from "@mantine/core";

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
          <Button
            variant="outline"
            radius="md"
            onClick={onClose}
            disabled={isDeleting}
            style={{
              borderColor: "rgba(255,255,255,0.12)",
              color: "#e2e8f0",
              background: "rgba(255,255,255,0.04)",
              fontFamily: "var(--font-space-grotesk), sans-serif",
            }}
          >
            Cancel
          </Button>
          <Button color="red" loading={isDeleting} disabled={isDeleting} onClick={onConfirm}>
            Delete Course
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
