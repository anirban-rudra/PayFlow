import type { ApiProblem } from "./types";
import { config } from "../config/env";

let authToken: string | null = null;
let unauthorizedHandler: (() => void) | null = null;

export function setAuthToken(token: string | null) {
  authToken = token;
}

export function setUnauthorizedHandler(handler: (() => void) | null) {
  unauthorizedHandler = handler;
}

export class ApiError extends Error {
  status: number;

  constructor(problem: ApiProblem) {
    super(problem.message);
    this.status = problem.status ?? 0;
  }
}

async function parseBody(response: Response) {
  const text = await response.text();
  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function toProblem(status: number, body: unknown): ApiProblem {
  if (typeof body === "string") {
    return { status, message: body };
  }
  if (body && typeof body === "object" && "message" in body) {
    return { status, message: String((body as { message: unknown }).message) };
  }
  return { status, message: `Request failed with status ${status}` };
}

function createAbortSignal(signal?: AbortSignal) {
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), config.requestTimeoutMs);

  if (signal) {
    if (signal.aborted) {
      controller.abort();
    } else {
      signal.addEventListener("abort", () => controller.abort(), { once: true });
    }
  }

  return {
    signal: controller.signal,
    clear: () => window.clearTimeout(timeoutId)
  };
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");

  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  if (authToken) {
    headers.set("Authorization", `Bearer ${authToken}`);
  }

  const abort = createAbortSignal(init.signal ?? undefined);
  let response: Response;
  try {
    response = await fetch(`${config.apiBaseUrl}${path}`, {
      ...init,
      headers,
      signal: abort.signal
    });
  } catch (err) {
    if (err instanceof DOMException && err.name === "AbortError") {
      throw new ApiError({
        status: 0,
        message: "Request timed out. Check the PayFlow gateway and retry."
      });
    }
    throw new ApiError({
      status: 0,
      message: "Network unavailable. Check that the PayFlow gateway is running."
    });
  } finally {
    abort.clear();
  }

  const body = await parseBody(response);

  if (!response.ok) {
    if (response.status === 401) {
      unauthorizedHandler?.();
    }
    throw new ApiError(toProblem(response.status, body));
  }

  return body as T;
}
