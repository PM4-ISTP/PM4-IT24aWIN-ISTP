import { Anchor, Box, Container, Group, SimpleGrid, Stack, Text } from "@mantine/core";
import BrandLockup from "./parts/BrandLockup";
import Kicker from "./parts/Kicker";
import { FONT_MONO, INK_DIM, LINE, MUTED, ROSE } from "../theme";

const sections = [
  {
    title: "Project",
    links: ["GitHub", "Docs", "Issues", "Roadmap"],
  },
  {
    title: "Community",
    links: ["Contribute", "Code of Conduct"],
  },
  {
    title: "For universities",
    links: ["Self-hosting", "Authoring", "Contact"],
  },
];

const heartSvg =
  "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 20 20'><path d='M10 17 C 4 12 1 9 3 5 C 5 2 9 3 10 6 C 11 3 15 2 17 5 C 19 9 16 12 10 17 Z' fill='black'/></svg>";

export default function LandingFooter() {
  return (
    <Box
      component="footer"
      style={{
        borderTop: `1px solid ${LINE}`,
        padding: "56px 0 28px",
      }}
    >
      <Container size="xl" px={32}>
        <SimpleGrid cols={{ base: 2, md: 4 }} spacing={40}>
          <Stack gap={14}>
            <BrandLockup />
            <Text
              style={{
                color: MUTED,
                fontSize: 13.5,
                lineHeight: 1.6,
                maxWidth: 280,
              }}
            >
              Self-hosted, Kubernetes-based CTF training for universities. Built at ZHAW for
              IT.PM4, FS2026.
            </Text>
          </Stack>

          {sections.map((section) => (
            <Stack key={section.title} gap={16}>
              <Kicker size={10}>{section.title}</Kicker>
              <Stack gap={10}>
                {section.links.map((link) => (
                  <Anchor
                    key={link}
                    href="#"
                    underline="never"
                    style={{ fontSize: 14, color: INK_DIM }}
                  >
                    {link}
                  </Anchor>
                ))}
              </Stack>
            </Stack>
          ))}
        </SimpleGrid>

        <Group
          justify="space-between"
          align="center"
          mt={48}
          pt={20}
          style={{ borderTop: `1px solid ${LINE}` }}
        >
          <Text style={{ fontFamily: FONT_MONO, fontSize: 12, color: MUTED }}>
            © 2026 ISTP · ZHAW IT.PM4 · Open-source
          </Text>
          <Group gap={4} align="center" wrap="nowrap">
            <Text style={{ fontFamily: FONT_MONO, fontSize: 12, color: MUTED }}>
              Developed in Switzerland with
            </Text>
            <Box
              component="span"
              w={12}
              h={12}
              style={{
                display: "inline-block",
                background: ROSE,
                WebkitMaskImage: `url("${heartSvg}")`,
                maskImage: `url("${heartSvg}")`,
                WebkitMaskSize: "contain",
                maskSize: "contain",
                WebkitMaskRepeat: "no-repeat",
                maskRepeat: "no-repeat",
                WebkitMaskPosition: "center",
                maskPosition: "center",
                verticalAlign: "-1px",
              }}
            />
            <Text
              style={{
                fontFamily: FONT_MONO,
                fontSize: 12,
                color: ROSE,
                fontWeight: 600,
                letterSpacing: "0.06em",
              }}
            >
              CH
            </Text>
          </Group>
        </Group>
      </Container>
    </Box>
  );
}
