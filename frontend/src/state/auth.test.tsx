import { useEffect } from "react";
import { waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { request, setAuthToken } from "../api/http";
import { createTestToken } from "../test/jwt";
import { renderWithProviders } from "../test/render";

function RequestOnMount() {
  useEffect(() => {
    void request("/api/protected");
  }, []);

  return <div>Request mounted</div>;
}

describe("AuthProvider", () => {
  beforeEach(() => {
    setAuthToken(null);
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        text: () => Promise.resolve("{}")
      })
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    setAuthToken(null);
  });

  it("attaches a restored session token before child requests run", async () => {
    const token = createTestToken({ userId: 1 });
    sessionStorage.setItem("payflow.session", token);

    renderWithProviders(<RequestOnMount />);

    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(1));
    const [, init] = vi.mocked(fetch).mock.calls[0];
    expect((init?.headers as Headers).get("Authorization")).toBe(`Bearer ${token}`);
  });
});
