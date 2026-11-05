import { execFileSync } from "node:child_process";
import { describe, expect, test } from "@jest/globals";

function dockerReady() {
  try {
    execFileSync("docker", ["info"], { stdio: "ignore", timeout: 4000 });
    return true;
  } catch {
    return false;
  }
}

const run = dockerReady() ? describe : describe.skip;

run("pdf-service integration", () => {
  test("placeholder until a docker-backed PDF flow is wired", () => {
    expect(dockerReady()).toBe(true);
  });
});
