// @vitest-environment node
import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";

const css = readFileSync(new URL("../src/styles/main.scss", import.meta.url), "utf8");
function luminance(hex: string) {
  const channels = hex.slice(1).match(/.{2}/g)!.map((value) => {
    const channel = parseInt(value, 16) / 255;
    return channel <= .04045 ? channel / 12.92 : ((channel + .055) / 1.055) ** 2.4;
  });
  return channels[0]! * .2126 + channels[1]! * .7152 + channels[2]! * .0722;
}
const pairs: [string, string, number][] = [
  ["ink", "paper", 7], ["ink", "surface", 7], ["muted", "paper", 7], ["muted", "surface", 7],
  ["muted", "subtle", 7], ["ink", "input", 7], ["muted", "input", 7],
  ["on-primary", "primary", 4.5], ["accent", "surface", 4.5],
  ["success", "success-bg", 4.5], ["warning", "warning-bg", 4.5], ["danger", "danger-bg", 4.5],
  ["control-border", "input", 3], ["focus", "surface", 3], ["focus", "paper", 3],
  ["danger", "surface", 4.5], ["danger", "input", 3], ["accent", "subtle", 4.5],
  ["control-border", "surface", 3], ["control-border", "subtle", 3],
];

describe.each(["light", "dark"])("%s 모드 가독성", (mode) => {
  it.each(pairs)("%s 글자·경계와 %s 배경의 대비가 %s:1 이상이다", (foreground, background, minimum) => {
    const selector = mode === "light" ? /:root\s*\{([^}]+)\}/ : /\[data-theme="dark"\]\s*\{([^}]+)\}/;
    const block = css.match(selector)?.[1] ?? "";
    const colors = Object.fromEntries([...block.matchAll(/--([\w-]+):\s*(#[a-fA-F0-9]{6})\s*;/g)].map((m) => [m[1], m[2]]));
    expect(colors[foreground], `테마의 ${foreground} 색상이 있어야 합니다`).toBeDefined();
    expect(colors[background], `테마의 ${background} 색상이 있어야 합니다`).toBeDefined();
    const a = luminance(colors[foreground]!); const b = luminance(colors[background]!);
    expect((Math.max(a, b) + .05) / (Math.min(a, b) + .05)).toBeGreaterThanOrEqual(minimum);
  });
});
