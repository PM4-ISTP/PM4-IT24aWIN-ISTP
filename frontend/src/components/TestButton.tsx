"use client";

import { useState } from "react";
import { postCourse } from "@/src/app/actions";

export default function TestButton() {
  const [response, setResponse] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleClick() {
    setLoading(true);
    setError(null);
    setResponse(null);

    try {
      const result = await postCourse();
      setResponse(JSON.stringify(result, null, 2));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unknown error");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <button onClick={() => void handleClick()} disabled={loading}>
        {loading ? "Loading..." : "POST /api/v1/courses"}
      </button>

      {error && <pre style={{ color: "red", marginTop: "1rem" }}>Error: {error}</pre>}

      {response && <pre style={{ marginTop: "1rem", whiteSpace: "pre-wrap" }}>{response}</pre>}
    </div>
  );
}
