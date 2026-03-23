import { getServerSession } from "next-auth";
import { authOptions } from "@/src/lib/auth";

export default async function Home() {
  const session = await getServerSession(authOptions);
  const name = session?.user?.name ?? "there";
  const firstName = name.split(" ")[0];

  return (
    <div style={{ minHeight: "100vh", background: "#ffffff", padding: "2rem" }}>
      <h1>Hey, {firstName}</h1>
    </div>
  );
}
