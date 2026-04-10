import type { Metadata } from "next";
import { ColorSchemeScript, mantineHtmlProps, MantineProvider } from "@mantine/core";
import { Geist, Geist_Mono, Manrope, Orbitron, Space_Grotesk } from "next/font/google";
import NextAuthSessionProvider from "@/src/components/SessionProvider";
import "@mantine/core/styles.css";
import "@mantine/tiptap/styles.css";
import "./globals.css";

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
        <ColorSchemeScript forceColorScheme="light" />
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
        <MantineProvider forceColorScheme="light">
          <NextAuthSessionProvider>{children}</NextAuthSessionProvider>
        </MantineProvider>
      </body>
    </html>
  );
}
