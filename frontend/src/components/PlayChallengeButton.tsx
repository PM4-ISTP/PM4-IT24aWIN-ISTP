"use client";

import { Box, Button, Text } from "@mantine/core";

export default function PlayChallengeButton({ condition }: { condition: number }) {
  // TODO: Currently, this is just a placeholder. Once the play challenge flow is implemented, this component needs to be finished.
  if (condition == 0) {
    return (
      <Button color="green">Start</Button>
    );
  } else if (condition == 1) {
    return (
      <Box>
        <Button>Restart</Button>
        <Text size="xs">You have already completed this challenge. You can restart it.</Text>
      </Box>
    );
  } else if (condition == 1) {
    return (
      <Box>
        <Button color="blue">Continue</Button>
        <Text size="xs">Continue playing. A pod is already running.</Text>
      </Box>
    );
  } else {
    return (
      <Box>
        <Button color="blue">Continue</Button>
        <Text size="xs">Continue playing. This starts a new pod.</Text>
      </Box>
    );
  }
}
