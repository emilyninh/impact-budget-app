// Registers jest-dom matchers (toBeInTheDocument, etc.) on Vitest's expect, and unmounts
// React trees between tests (RTL's auto-cleanup only runs when Vitest globals are enabled).
import "@testing-library/jest-dom/vitest";
import { afterEach } from "vitest";
import { cleanup } from "@testing-library/react";

afterEach(() => cleanup());
