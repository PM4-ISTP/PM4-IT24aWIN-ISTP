"use client";

import { Button, Flex, Text } from "@mantine/core";

function ButtonWithInfoText({ buttonText, infoText, buttonColor }: { buttonText: string; infoText: string, buttonColor: string }) {
  return (
    <Flex direction="column" justify="flex-end">
      <Button mb={12} color={buttonColor} style={{ marginLeft: "auto" }}>
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
      <ButtonWithInfoText
        buttonText="Start"
        infoText="You have not started this challenge yet."
        buttonColor="darkgreen"
      />
    );
  } else if (condition == 1) {
    return (
      <ButtonWithInfoText
        buttonText="Restart"
        infoText="You have already completed this challenge. You can restart it."
        buttonColor="darkgreen"
      />
    );
  } else if (condition == 2) {
    return (
      <ButtonWithInfoText
        buttonText="Continue"
        infoText="Continue playing. A pod is already running."
        buttonColor="blue"
      />
    );
  } else {
    return (
      <ButtonWithInfoText
        buttonText="Continue"
        infoText="Continue playing. This starts a new pod."
        buttonColor="blue"
      />
    );
  }
}
