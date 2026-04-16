export default function DashboardStyles() {
  return (
    <style>{`
      @keyframes cursorBlink {
        0%, 100% { opacity: 1; }
        50%       { opacity: 0; }
      }
      @keyframes fadeSlideUp {
        from { opacity: 0; transform: translateY(14px); }
        to   { opacity: 1; transform: translateY(0); }
      }
      .dashboard-hero-content {
        animation: fadeSlideUp 0.5s cubic-bezier(0.22, 1, 0.36, 1) both;
      }
      .dashboard-course-card {
        transition: transform 0.18s ease, box-shadow 0.18s ease;
        cursor: pointer;
      }
      .dashboard-course-card:hover {
        transform: translateY(-3px);
        box-shadow: 0 8px 24px rgba(0,0,0,0.35);
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
