const EMAIL_PATTERN = /^\S+@\S+\.\S+$/;
const HTTP_URL_PATTERN = /^https?:\/\//i;

export function isEmail(value: string): boolean {
  return EMAIL_PATTERN.test(value.trim());
}

export function isHttpUrl(value: string): boolean {
  return HTTP_URL_PATTERN.test(value.trim());
}

export interface RequiredStringOptions {
  min?: number;
  max?: number;
  trim?: boolean;
}

export function requiredString(value: string, options: RequiredStringOptions = {}): boolean {
  const { min = 1, max = Infinity, trim = true } = options;
  const target = trim ? value.trim() : value;
  return target.length >= min && target.length <= max;
}

export function emailValidator(message = "Invalid email address") {
  return (value: string) => (isEmail(value) ? null : message);
}

export function httpUrlValidator(message = "Must start with http:// or https://") {
  return (value: string) => {
    if (!value.trim()) return null;
    return isHttpUrl(value) ? null : message;
  };
}

export function requiredStringValidator(
  message = "This field is required",
  options: RequiredStringOptions = {}
) {
  return (value: string) => (requiredString(value, options) ? null : message);
}
