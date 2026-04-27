"use client";

import { useEffect, useRef, useState } from "react";
import {
  ActionIcon,
  Alert,
  Badge,
  Box,
  Button,
  Collapse,
  Group,
  Input,
  Stack,
  Text,
  TextInput,
  Title,
  Tooltip,
} from "@mantine/core";
import {
  IconArrowDown,
  IconArrowUp,
  IconChevronDown,
  IconChevronRight,
  IconPlus,
  IconTrash,
} from "@tabler/icons-react";
import MyEditor from "@/src/shared/components/MyEditor";

export interface SubTaskFormValues {
  id?: string;
  title: string;
  description: string;
  flag: string;
  orderIndex: number;
}

export interface SubTaskManagerProps {
  subTasks: SubTaskFormValues[];
  onChange: (subTasks: SubTaskFormValues[]) => void;
  errors?: Array<Partial<Record<"title" | "description" | "flag", string>>>;
  defaultExpandedIndex?: number | null;
}

const FLAG_INNER_PATTERN = /^ISTP\{(.+)\}$/;
const FLAG_FORBIDDEN_CHARS = /[^A-Za-z0-9_]/g;
// Mantine's default Collapse transition is 200 ms; give the close animation a bit of headroom.
const COLLAPSE_UNMOUNT_DELAY_MS = 250;

export function SubTaskManager({
  subTasks,
  onChange,
  errors,
  defaultExpandedIndex = null,
}: SubTaskManagerProps) {
  const [expandedIndex, setExpandedIndex] = useState<number | null>(() => {
    if (defaultExpandedIndex === null) return null;
    return defaultExpandedIndex >= 0 && defaultExpandedIndex < subTasks.length
      ? defaultExpandedIndex
      : null;
  });
  // Index of a sub-task whose body is still rendered while its close animation runs.
  const [closingIndex, setClosingIndex] = useState<number | null>(null);
  const closeTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    return () => {
      if (closeTimerRef.current) clearTimeout(closeTimerRef.current);
    };
  }, []);

  function changeExpanded(next: number | null) {
    setExpandedIndex((prev) => {
      if (prev !== null && prev !== next) {
        if (closeTimerRef.current) clearTimeout(closeTimerRef.current);
        setClosingIndex(prev);
        closeTimerRef.current = setTimeout(() => {
          setClosingIndex(null);
          closeTimerRef.current = null;
        }, COLLAPSE_UNMOUNT_DELAY_MS);
      }
      return next;
    });
  }

  function clearClosing() {
    if (closeTimerRef.current) {
      clearTimeout(closeTimerRef.current);
      closeTimerRef.current = null;
    }
    setClosingIndex(null);
  }

  function updateAt(index: number, patch: Partial<SubTaskFormValues>) {
    const next = subTasks.map((st, i) => (i === index ? { ...st, ...patch } : st));
    onChange(next);
  }

  function handleAdd() {
    const newIndex = subTasks.length;
    onChange([...subTasks, { title: "", description: "", flag: "", orderIndex: newIndex }]);
    changeExpanded(newIndex);
  }

  function handleRemove(index: number) {
    const next = subTasks.filter((_, i) => i !== index).map((st, i) => ({ ...st, orderIndex: i }));
    onChange(next);
    clearClosing();
    if (expandedIndex === index) {
      setExpandedIndex(null);
    } else if (expandedIndex !== null && expandedIndex > index) {
      setExpandedIndex(expandedIndex - 1);
    }
  }

  function handleMoveUp(index: number) {
    if (index === 0) return;
    const next = [...subTasks];
    [next[index - 1], next[index]] = [next[index], next[index - 1]];
    onChange(next.map((st, i) => ({ ...st, orderIndex: i })));
    clearClosing();
    if (expandedIndex === index) setExpandedIndex(index - 1);
    else if (expandedIndex === index - 1) setExpandedIndex(index);
  }

  function handleMoveDown(index: number) {
    if (index === subTasks.length - 1) return;
    const next = [...subTasks];
    [next[index], next[index + 1]] = [next[index + 1], next[index]];
    onChange(next.map((st, i) => ({ ...st, orderIndex: i })));
    clearClosing();
    if (expandedIndex === index) setExpandedIndex(index + 1);
    else if (expandedIndex === index + 1) setExpandedIndex(index);
  }

  function handleToggleExpand(index: number) {
    changeExpanded(expandedIndex === index ? null : index);
  }

  function handleFlagInnerChange(index: number, raw: string) {
    // If the user pastes a full ISTP{...} value, unwrap it first.
    const wrappedMatch = raw.match(FLAG_INNER_PATTERN);
    const inner = wrappedMatch ? wrappedMatch[1] : raw;
    // Strip everything that isn't part of the backend-allowed alphabet, then
    // upper-case so all stored flags share a consistent shape.
    const sanitized = inner.replace(FLAG_FORBIDDEN_CHARS, "").toUpperCase();
    updateAt(index, { flag: sanitized });
  }

  return (
    <Stack gap="md">
      <Group justify="space-between" align="center">
        <Stack gap={2}>
          <Title order={4}>Sub Tasks</Title>
          <Text size="sm" c="dimmed">
            Each sub task is worth one point. A sub task without a flag is just a description.
          </Text>
        </Stack>
        <Badge size="lg" variant="light">
          {subTasks.length} point{subTasks.length === 1 ? "" : "s"}
        </Badge>
      </Group>

      {subTasks.length === 0 && <Alert color="orange">At least one sub task is required.</Alert>}

      {subTasks.length > 0 && (
        <Stack gap="sm">
          {subTasks.map((st, index) => {
            const err = errors?.[index] ?? {};
            const isExpanded = expandedIndex === index;
            const shouldRenderBody = isExpanded || closingIndex === index;
            const hasError = Boolean(err.title || err.description || err.flag);
            const displayTitle = st.title.trim() || `Sub Task ${index + 1}`;
            const hasFlag = st.flag.trim().length > 0;

            return (
              <Box
                key={st.id ?? `local-${index}`}
                style={{
                  border: `1px solid ${
                    hasError ? "var(--mantine-color-red-6)" : "var(--mantine-color-default-border)"
                  }`,
                  borderRadius: "var(--mantine-radius-md)",
                  overflow: "hidden",
                }}
              >
                {/* Collapsed header — always visible */}
                <Box p="md" style={{ cursor: "pointer" }} onClick={() => handleToggleExpand(index)}>
                  <Group justify="space-between" align="center" wrap="nowrap">
                    <Group gap="sm" wrap="nowrap" align="center" style={{ flex: 1, minWidth: 0 }}>
                      <ActionIcon variant="transparent" size="sm" tabIndex={-1}>
                        {isExpanded ? (
                          <IconChevronDown size={16} />
                        ) : (
                          <IconChevronRight size={16} />
                        )}
                      </ActionIcon>
                      <Text size="sm" c="dimmed" fw={600} style={{ flexShrink: 0 }}>
                        #{index + 1}
                      </Text>
                      <Text size="sm" fw={600} truncate style={{ flex: 1, minWidth: 0 }}>
                        {displayTitle}
                      </Text>
                    </Group>

                    <Group
                      gap="xs"
                      wrap="nowrap"
                      align="center"
                      onClick={(e) => e.stopPropagation()}
                    >
                      {hasFlag && (
                        <Badge size="xs" variant="light" color="grape">
                          Flag
                        </Badge>
                      )}
                      <Tooltip label="Move up">
                        <ActionIcon
                          variant="subtle"
                          size="sm"
                          disabled={index === 0}
                          onClick={() => handleMoveUp(index)}
                          aria-label="Move up"
                        >
                          <IconArrowUp size={14} />
                        </ActionIcon>
                      </Tooltip>
                      <Tooltip label="Move down">
                        <ActionIcon
                          variant="subtle"
                          size="sm"
                          disabled={index === subTasks.length - 1}
                          onClick={() => handleMoveDown(index)}
                          aria-label="Move down"
                        >
                          <IconArrowDown size={14} />
                        </ActionIcon>
                      </Tooltip>
                      <Tooltip label="Remove">
                        <ActionIcon
                          variant="subtle"
                          size="sm"
                          color="red"
                          onClick={() => handleRemove(index)}
                          aria-label="Remove sub task"
                        >
                          <IconTrash size={14} />
                        </ActionIcon>
                      </Tooltip>
                    </Group>
                  </Group>
                </Box>

                {/* Expanded body — MyEditor is only mounted while open (or during the
                    close animation) to avoid keeping a TipTap instance per subtask in memory. */}
                <Collapse expanded={isExpanded}>
                  {shouldRenderBody && (
                    <Box
                      p="md"
                      style={{
                        borderTop: "1px solid rgba(255,255,255,0.08)",
                        background: "rgba(255,255,255,0.02)",
                      }}
                    >
                      <Stack gap="md">
                        <TextInput
                          label="Title"
                          placeholder="Task title"
                          value={st.title}
                          onChange={(e) => updateAt(index, { title: e.currentTarget.value })}
                          error={err.title}
                          required
                        />

                        <Input.Wrapper label="Description" required error={err.description}>
                          <MyEditor
                            description={st.description}
                            setDescription={(value) => updateAt(index, { description: value })}
                          />
                        </Input.Wrapper>

                        <TextInput
                          label="Flag"
                          description="Leave empty if this sub task has no flag to submit."
                          placeholder="flag_content_here"
                          leftSection={<Text size="sm">ISTP&#123;</Text>}
                          leftSectionWidth={52}
                          rightSection={<Text size="sm">&#125;</Text>}
                          value={st.flag}
                          onChange={(e) => handleFlagInnerChange(index, e.currentTarget.value)}
                          error={err.flag}
                        />
                      </Stack>
                    </Box>
                  )}
                </Collapse>
              </Box>
            );
          })}
        </Stack>
      )}

      <Button
        variant="light"
        leftSection={<IconPlus size={14} />}
        onClick={handleAdd}
        aria-label="Add sub task"
      >
        Add Sub Task
      </Button>
    </Stack>
  );
}
