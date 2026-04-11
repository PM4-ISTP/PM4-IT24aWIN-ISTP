import { Box, Stack, Title, Text, Button } from "@mantine/core";

export default function UnauthorizedPage() {
  return (
    <Box
      style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        fontFamily: "var(--font-space-grotesk), sans-serif",
      }}
    >
      <Stack gap="md" align="center" ta="center" maw={400}>
        <Title order={2} style={{ color: "#f1f5f9", letterSpacing: "-0.01em" }}>
          Access Denied
        </Title>
        <Text style={{ color: "#94a3b8" }}>
          You do not have permission to access this page.
        </Text>
        <Button
          component="a"
          href="/dashboard"
          variant="outline"
          radius="md"
          style={{
            borderColor: "rgba(255,255,255,0.12)",
            color: "#e2e8f0",
            background: "rgba(255,255,255,0.04)",
            fontFamily: "var(--font-space-grotesk), sans-serif",
          }}
        >
          Back to Dashboard
        </Button>
      </Stack>
    </Box>
  );
}
