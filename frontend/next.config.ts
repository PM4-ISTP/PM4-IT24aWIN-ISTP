import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  rewrites() {
    return Promise.resolve([
      {
        source: "/scalar",
        destination: "/api/backend/scalar",
      },
      {
        source: "/scalar/:path*",
        destination: "/api/backend/scalar/:path*",
      },
      {
        source: "/v3/api-docs",
        destination: "/api/backend/v3/api-docs",
      },
      {
        source: "/v3/api-docs/:path*",
        destination: "/api/backend/v3/api-docs/:path*",
      },
      {
        source: "/v3/api-docs.yaml",
        destination: "/api/backend/v3/api-docs.yaml",
      },
    ]);
  },
  turbopack: {
    root: process.cwd(),
  },
};

export default nextConfig;
