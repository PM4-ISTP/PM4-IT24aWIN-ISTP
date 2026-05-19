"use client";

import { useState } from "react";
import type { ButtonProps } from "@mantine/core";
import AppButton from "@/src/shared/components/AppButton";
import JoinCourseModal from "@/src/features/course/components/enrollment/JoinCourseModal";

export default function JoinCourseButton({
  size = "xs",
  fullWidth = false,
}: {
  size?: ButtonProps["size"];
  fullWidth?: boolean;
}) {
  const [opened, setOpened] = useState(false);

  return (
    <>
      <AppButton
        tone="ghost"
        size={size}
        fullWidth={fullWidth}
        onClick={() => setOpened(true)}
        leftSection={
          <span
            className="material-symbols-outlined"
            style={{
              fontSize: "1rem",
              lineHeight: 1,
              fontVariationSettings: "'FILL' 0, 'wght' 300, 'GRAD' 0, 'opsz' 24",
            }}
          >
            add
          </span>
        }
      >
        Join course
      </AppButton>

      <JoinCourseModal opened={opened} onClose={() => setOpened(false)} />
    </>
  );
}
