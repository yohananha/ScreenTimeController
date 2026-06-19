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
  // setupFilesAfterEach doesn't exist in Jest — use setupFiles which DOES
  // give the module access to globals like afterEach (registered globally).
  setupFiles: ["<rootDir>/src/__tests__/setup.ts"],
};
