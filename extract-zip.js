#!/usr/bin/env node
/**
 * Auto-extracts any .zip file uploaded to the repository root directly into the root,
 * removes the processed .zip file, and skips .github/workflows so GITHUB_TOKEN push succeeds.
 * Zero npm dependencies required (uses built-in Node.js + system unzip).
 */

const fs = require("fs");
const path = require("path");
const os = require("os");
const { execFileSync } = require("child_process");

const ROOT = process.cwd();

// Paths we must never overwrite so GitHub Actions push never fails
const PROTECTED_TOP_LEVEL = new Set([".git", "extract-zip.js"]);

function shouldSkip(relPath) {
  const norm = relPath.split(path.sep).join("/");
  if (norm === ".github/workflows" || norm.startsWith(".github/workflows/")) {
    return true;
  }
  const top = norm.split("/")[0];
  if (PROTECTED_TOP_LEVEL.has(top)) return true;
  if (norm.includes("__MACOSX/") || norm.endsWith(".DS_Store")) return true;
  return false;
}

function copyDirRecursive(srcDir, destRoot, baseDir = srcDir) {
  let count = 0;
  for (const entry of fs.readdirSync(srcDir, { withFileTypes: true })) {
    const srcPath = path.join(srcDir, entry.name);
    const relPath = path.relative(baseDir, srcPath);

    if (shouldSkip(relPath)) {
      console.log(`[SKIP] ${relPath}`);
      continue;
    }

    const destPath = path.join(destRoot, relPath);
    if (entry.isDirectory()) {
      fs.mkdirSync(destPath, { recursive: true });
      count += copyDirRecursive(srcPath, destRoot, baseDir);
    } else if (entry.isFile()) {
      fs.mkdirSync(path.dirname(destPath), { recursive: true });
      fs.copyFileSync(srcPath, destPath);
      console.log(`[EXTRACT] ${relPath}`);
      count += 1;
    }
  }
  return count;
}

function resolveContentRoot(tmpDir) {
  const entries = fs
    .readdirSync(tmpDir, { withFileTypes: true })
    .filter((e) => e.name !== "__MACOSX" && e.name !== ".DS_Store");

  // If the zip contains a single wrapper folder (e.g., MyAvroKeyboard/...), unwrap it
  if (entries.length === 1 && entries[0].isDirectory()) {
    const candidate = path.join(tmpDir, entries[0].name);
    const inner = fs.readdirSync(candidate);
    if (inner.length > 0 && entries[0].name !== "app" && entries[0].name !== "scripts" && entries[0].name !== ".github") {
      console.log(`[INFO] Unwrapping single top-level folder: ${entries[0].name}/`);
      return candidate;
    }
  }
  return tmpDir;
}

function main() {
  const zipFiles = fs
    .readdirSync(ROOT)
    .filter((name) => name.toLowerCase().endsWith(".zip") && fs.statSync(path.join(ROOT, name)).isFile());

  if (zipFiles.length === 0) {
    console.log("No .zip files found in repository root.");
    return;
  }

  for (const zipName of zipFiles) {
    const zipPath = path.join(ROOT, zipName);
    const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), "extract-zip-"));

    try {
      console.log(`\n=== Extracting ${zipName} ===`);
      execFileSync("unzip", ["-o", "-q", zipPath, "-d", tmpDir], { stdio: "inherit" });

      const contentRoot = resolveContentRoot(tmpDir);
      const extractedCount = copyDirRecursive(contentRoot, ROOT);
      console.log(`Extracted ${extractedCount} file(s) from ${zipName} to repository root.`);

      // Delete the uploaded zip so it does not stay in root or re-trigger
      fs.unlinkSync(zipPath);
      console.log(`[CLEANUP] Removed ${zipName}`);
    } finally {
      fs.rmSync(tmpDir, { recursive: true, force: true });
    }
  }
}

main();
