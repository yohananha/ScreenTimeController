/** @type {import('jest').Config} */
module.exports = {
  preset: "ts-jest",
  testEnvironment: "node",
  testMatch: [
    "<rootDir>/src/__tests__/**/*.test.ts",
    "<rootDir>/test/**/*.test.ts",
  ],
  collectCoverageFrom: [
    "src/**/*.ts",
    "!src/**/*.d.ts",
    "!src/__tests__/**",
  ],
  coverageDirectory: "coverage",
  coverageReporters: ["text-summary", "lcov", "html"],
  coverageThreshold: {
    global: {
      lines: 80,
      branches: 75,
      functions: 80,
      statements: 80,
    },
  },
  testTimeout: 30000,
  // No setupFiles — Jest globals (afterEach etc.) aren't defined in
  // setupFiles scope. The per-test cleanup lives in src/__tests__/helpers.ts
  // and is registered when each test imports it.
};
