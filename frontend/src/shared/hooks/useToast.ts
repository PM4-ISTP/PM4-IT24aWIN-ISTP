import { useCallback, useEffect, useMemo, useRef, useState } from "react";

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

  const hide = useCallback(() => {
    clearToastTimeout();
    setVisible(false);
  }, []);

  const show = useCallback(() => {
    clearToastTimeout();
    setVisible(true);
    timeoutRef.current = setTimeout(() => {
      setVisible(false);
      timeoutRef.current = null;
    }, durationMs);
  }, [durationMs]);

  return useMemo(() => ({ visible, show, hide }), [visible, show, hide]);
}
