"use client";

import { Button, Flex, Text } from "@mantine/core";

function ButtonWithInfoText({ buttonText, infoText }: { buttonText: string; infoText: string }) {
  return (
    <Flex direction="column" justify="flex-end">
      <Button mb={12} style={{ marginLeft: "auto" }}>
        {buttonText}
      </Button>
      <Text size="xs">{infoText}</Text>
    </Flex>
  );
}

export default function PlayChallengeButton({ condition }: { condition: number }) {
  // TODO: Currently, this is just a placeholder. Once the play challenge flow is implemented, this component needs to be finished.
  if (condition == 0) {
    return (
      <Button color="green" style={{ marginLeft: "auto" }}>
        Start
      </Button>
    );
  } else if (condition == 1) {
    return (
      <ButtonWithInfoText
        buttonText="Restart"
        infoText="You have already completed this challenge. You can restart it."
      />
    );
  } else if (condition == 2) {
    return (
      <ButtonWithInfoText
        buttonText="Continue"
        infoText="Continue playing. A pod is already running."
      />
    );
  } else {
    return (
      <ButtonWithInfoText
        buttonText="Continue"
        infoText="Continue playing. This starts a new pod."
      />
    );
  }
}
