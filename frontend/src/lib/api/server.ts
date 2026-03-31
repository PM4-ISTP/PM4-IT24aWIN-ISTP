import { getServerSession } from "next-auth";
import { createApiClient } from ".";
import { authOptions } from "../auth";

const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

export async function getApiClient() {
  // getServerSession triggers the JWT callback (including token refresh).
  const session = await getServerSession(authOptions);

  if (!session?.accessToken) {
    throw new Error("Not authenticated");
  }

  return createApiClient(BACKEND_URL, session.accessToken);
}

