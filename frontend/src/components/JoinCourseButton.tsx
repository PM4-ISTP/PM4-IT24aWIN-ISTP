"use client";

import { useState } from "react";
import { Button } from "@mantine/core";
import JoinCourseModal from "@/src/components/JoinCourseModal";

export default function JoinCourseButton() {
  const [opened, setOpened] = useState(false);

  return (
    <>
      <Button
        variant="outline"
        size="xs"
        radius="md"
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
        style={{
          borderColor: "rgba(255,255,255,0.18)",
          color: "#e2e8f0",
          background: "rgba(255,255,255,0.06)",
          fontFamily: "var(--font-space-grotesk), sans-serif",
          fontWeight: 500,
        }}
      >
        Join course
      </Button>

      <JoinCourseModal opened={opened} onClose={() => setOpened(false)} />
    </>
  );
}
