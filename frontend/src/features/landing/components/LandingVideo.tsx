import { AspectRatio, Box, Container, Stack, Title } from "@mantine/core";
import Kicker from "./parts/Kicker";
import { INK, LINE_2 } from "../theme";

const VIDEO_ID = "3nkFtJMCs1Q";
const VIDEO_START = 0;

export default function LandingVideo() {
  return (
    <Box component="section" style={{ padding: "140px 0 40px" }}>
      <Container size="xl" px={32}>
        <Stack gap={18}>
          <Stack gap={10}>
            <Kicker>$ ./watch-demo.sh — 02:14</Kicker>
            <Title
              order={2}
              style={{
                fontSize: 38,
                fontWeight: 600,
                letterSpacing: "-0.02em",
                margin: 0,
                color: INK,
              }}
            >
              A class, in two minutes.
            </Title>
          </Stack>

          <Box
            style={{
              position: "relative",
              border: `1px solid ${LINE_2}`,
              borderRadius: 16,
              overflow: "hidden",
              boxShadow: "0 30px 80px -20px rgba(0,0,0,0.6)",
            }}
          >
            <AspectRatio ratio={16 / 9}>
              <iframe
                src={`https://www.youtube-nocookie.com/embed/${VIDEO_ID}?start=${VIDEO_START}&rel=0`}
                title="ISTP demo video placeholder"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                allowFullScreen
                style={{ border: 0, width: "100%", height: "100%" }}
              />
            </AspectRatio>
          </Box>
        </Stack>
      </Container>
    </Box>
  );
}
