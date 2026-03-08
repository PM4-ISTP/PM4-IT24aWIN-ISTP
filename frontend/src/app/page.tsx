// src/app/page.tsx
import { getServerSession } from "next-auth";
import { authOptions } from "@/src/lib/auth";
import Login from "../components/Login";
import Logout from "../components/Logout";
import TestButton from "../components/TestButton";

export default async function Home() {
  const session = await getServerSession(authOptions);
  if (session) {
    return (
      <div>
        <div>Your name is {session.user?.name + " " + session.user?.email}</div>
        <div>
          <Logout />
        </div>
        <TestButton />
      </div>
    );
  }
  return (
    <div>
      <Login />
      <TestButton />
    </div>
  );
}
