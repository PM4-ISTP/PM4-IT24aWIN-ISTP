export default function DashboardStyles() {
  return (
    <style>{`
      @keyframes cursorBlink {
        0%, 100% { opacity: 1; }
        50%       { opacity: 0; }
      }
      @keyframes fadeSlideUp {
        from { opacity: 0; transform: translateY(18px); }
        to   { opacity: 1; transform: translateY(0); }
      }
      @keyframes pulseGlow {
        0%, 100% { opacity: 0.7; }
        50%       { opacity: 1; }
      }
      .dashboard-hero-content {
        animation: fadeSlideUp 0.55s cubic-bezier(0.22, 1, 0.36, 1) both;
      }
      .dashboard-glow-tr {
        animation: pulseGlow 5s ease-in-out infinite;
      }
      .dashboard-glow-bl {
        animation: pulseGlow 5s ease-in-out 2.5s infinite;
      }
      .dashboard-stat-card {
        transition: transform 0.18s ease, box-shadow 0.18s ease;
        cursor: default;
      }
      .dashboard-stat-card:hover {
        transform: translateY(-4px);
        box-shadow: 0 10px 28px rgba(59, 130, 246, 0.13);
      }
      .dashboard-course-card {
        transition: transform 0.18s ease, box-shadow 0.18s ease;
        cursor: pointer;
      }
      .dashboard-course-card:hover {
        transform: translateY(-4px);
        box-shadow: 0 10px 28px rgba(59, 130, 246, 0.10);
      }
      .dashboard-activity-item {
        transition: background 0.15s ease;
        border-radius: 6px;
        padding: 4px 6px;
        margin: 0 -6px;
        cursor: default;
      }
      .dashboard-activity-item:hover {
        background: rgba(59, 130, 246, 0.06);
      }
      .dashboard-hero-stats {
        display: flex;
      }
      @media (max-width: 700px) {
        .dashboard-hero-stats { display: none; }
      }
    `}</style>
  );
}
