"use client";

import { Button } from "@mantine/core";
import { IconCheck, IconPlayerPlay } from "@tabler/icons-react";
import Link from "next/link";

/**
 * Action button that navigates to the play view for a challenge. Renders the
 * appropriate label/colour based on solved state.
 */
export default function PlayChallengeButton({
  href,
  solved,
  inProgress,
}: {
  href: string;
  solved: boolean;
  inProgress: boolean;
}) {
  if (solved) {
    return (
      <Button
        component={Link}
        href={href}
        variant="light"
        color="teal"
        leftSection={<IconCheck size={16} />}
        size="sm"
      >
        Replay
      </Button>
    );
  }
  if (inProgress) {
    return (
      <Button
        component={Link}
        href={href}
        color="blue"
        leftSection={<IconPlayerPlay size={16} />}
        size="sm"
      >
        Continue
      </Button>
    );
  }
  return (
    <Button
      component={Link}
      href={href}
      color="blue"
      leftSection={<IconPlayerPlay size={16} />}
      size="sm"
    >
      Start
    </Button>
  );
}
