import type { Metadata } from "next";
import { ColorSchemeScript, createTheme, mantineHtmlProps, MantineProvider } from "@mantine/core";
import { Notifications } from "@mantine/notifications";
import { Geist, Geist_Mono, Manrope, Orbitron, Space_Grotesk } from "next/font/google";
import NextAuthSessionProvider from "@/src/features/user/components/SessionProvider";
import "@mantine/core/styles.css";
import "@mantine/tiptap/styles.css";
import "@mantine/notifications/styles.css";
import "./globals.css";

/**
 * Custom dark theme — maps Mantine's `dark` color scale to our
 * navy/slate design palette so all built-in components (Paper, Input,
 * Modal, Menu …) automatically pick up the right colours in dark mode.
 *
 * Mapping reference (Mantine dark mode defaults → our values):
 *   dark[0]  primary text on dark bg   → #e2e8f0
 *   dark[1]  secondary text            → #cbd5e1
 *   dark[2]  dimmed / placeholder      → #94a3b8
 *   dark[3]  stronger border           → #475569
 *   dark[4]  default border / divider  → #1e293b
 *   dark[5]  subtle bg tint            → #152135
 *   dark[6]  input / control bg        → #0c1929
 *   dark[7]  Paper / card bg (= body)  → #0e1a2e
 *   dark[8]  AppShell body bg          → #0a1220
 *   dark[9]  darkest shade             → #060c18
 */
const theme = createTheme({
  colors: {
    dark: [
      "#e2e8f0",
      "#cbd5e1",
      "#94a3b8",
      "#475569",
      "#1e293b",
      "#152135",
      "#0c1929",
      "#0e1a2e",
      "#0a1220",
      "#060c18",
    ],
  },
  fontFamily: "var(--font-space-grotesk), sans-serif",
  primaryColor: "blue",
  defaultRadius: "md",
});

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

const manrope = Manrope({
  variable: "--font-manrope",
  subsets: ["latin"],
  weight: ["400", "600", "700", "800"],
});

const spaceGrotesk = Space_Grotesk({
  variable: "--font-space-grotesk",
  subsets: ["latin"],
  weight: ["500", "700"],
});

const orbitron = Orbitron({
  variable: "--font-orbitron",
  subsets: ["latin"],
  weight: ["700", "800", "900"],
});

export const metadata: Metadata = {
  title: "ISTP",
  description: "Interactive Security Training Platform",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    // suppressHydrationWarning: Mantine's ColorSchemeScript changes data-mantine-color-scheme
    // before React hydrates ("auto" on server → "light" on client). This attribute is intentional
    // and safe to suppress. See https://mantine.dev/guides/next/
    <html lang="en" {...mantineHtmlProps} suppressHydrationWarning>
      <head>
        <ColorSchemeScript forceColorScheme="dark" />
        {/* Material Symbols are used by the existing dashboard navigation. */}
        {/* eslint-disable-next-line @next/next/no-page-custom-font */}
        <link
          rel="stylesheet"
          href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200&display=swap"
        />
      </head>
      <body
        className={`${geistSans.variable} ${geistMono.variable} ${manrope.variable} ${spaceGrotesk.variable} ${orbitron.variable} antialiased`}
      >
        <MantineProvider forceColorScheme="dark" theme={theme}>
          <Notifications position="top-right" />
          <NextAuthSessionProvider>{children}</NextAuthSessionProvider>
        </MantineProvider>
      </body>
    </html>
  );
}
