import { describe, it, expect } from "vitest";
import { formatUsd } from "./theme";

describe("formatUsd", () => {
  it("formats numbers as USD currency", () => {
    expect(formatUsd(1234.5)).toBe("$1,234.50");
    expect(formatUsd(0)).toBe("$0.00");
    expect(formatUsd(9.9)).toBe("$9.90");
  });
});
