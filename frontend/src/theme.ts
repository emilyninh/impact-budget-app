// Two accessible, clearly-distinct hues for the two impact dimensions.
export const COLORS = {
  local: "#1f9d8b", // teal — "local / independent"
  sustainability: "#d08b2c", // amber — "sustainable"
  muted: "#8a94a6",
  budgetOk: "#1f9d8b", // on track — within limit
  budgetWarn: "#d08b2c", // at risk — pace projects over
  budgetOver: "#d9534f", // over budget
};

export function formatUsd(value: number): string {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
  }).format(value);
}
