"use client";
export default function Logout() {
  return (
    <button
      onClick={() => {
        window.location.assign("/api/auth/logout");
      }}
    >
      Sign out
    </button>
  );
}
