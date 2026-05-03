"use client";

import { useCallback, useState } from "react";
import { notifications } from "@mantine/notifications";

export interface AsyncActionMessages<T> {
  successTitle?: string;
  successMessage?: string | ((result: T) => string);
  errorTitle?: string;
  errorMessage?: string | ((error: Error) => string);
}

export interface AsyncActionOptions<T> extends AsyncActionMessages<T> {
  onSuccess?: (result: T) => void | Promise<void>;
  onError?: (error: Error) => void | Promise<void>;
  silentSuccess?: boolean;
  silentError?: boolean;
}

export interface AsyncActionState<T, Args extends unknown[]> {
  run: (...args: Args) => Promise<T | undefined>;
  loading: boolean;
  error: Error | null;
  reset: () => void;
}

function resolve<T>(value: string | ((arg: T) => string) | undefined, arg: T): string | undefined {
  if (typeof value === "function") return value(arg);
  return value;
}

export function useAsyncAction<T, Args extends unknown[]>(
  action: (...args: Args) => Promise<T>,
  options: AsyncActionOptions<T> = {}
): AsyncActionState<T, Args> {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const run = useCallback(
    async (...args: Args): Promise<T | undefined> => {
      setLoading(true);
      setError(null);
      try {
        const result = await action(...args);

        if (!options.silentSuccess) {
          const message = resolve(options.successMessage, result);
          if (options.successTitle || message) {
            notifications.show({
              title: options.successTitle ?? "Success",
              message: message ?? "",
              color: "green",
            });
          }
        }

        if (options.onSuccess) await options.onSuccess(result);
        return result;
      } catch (e) {
        const err = e instanceof Error ? e : new Error(String(e));
        setError(err);

        if (!options.silentError) {
          notifications.show({
            title: options.errorTitle ?? "Error",
            message: resolve(options.errorMessage, err) ?? err.message,
            color: "red",
          });
        }

        if (options.onError) await options.onError(err);
        return undefined;
      } finally {
        setLoading(false);
      }
    },
    [action, options]
  );

  const reset = useCallback(() => {
    setError(null);
    setLoading(false);
  }, []);

  return { run, loading, error, reset };
}
