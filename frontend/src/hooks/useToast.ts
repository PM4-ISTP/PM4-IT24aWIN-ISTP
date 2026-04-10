import { useEffect, useRef, useState } from "react";

export interface ToastControls {
  visible: boolean;
  show: () => void;
  hide: () => void;
}

export function useToast(durationMs = 3500): ToastControls {
  const [visible, setVisible] = useState(false);
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  function clearToastTimeout() {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
      timeoutRef.current = null;
    }
  }

  useEffect(() => clearToastTimeout, []);

  function hide() {
    clearToastTimeout();
    setVisible(false);
  }

  function show() {
    clearToastTimeout();
    setVisible(true);
    timeoutRef.current = setTimeout(() => {
      setVisible(false);
      timeoutRef.current = null;
    }, durationMs);
  }

  return { visible, show, hide };
}
