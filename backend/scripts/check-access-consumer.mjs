// Isolated response-compatibility proof using YOUR_LIFE_ONE's actual, immutable MCP probe implementation.
// This never edits YOUR_LIFE_ONE or changes its probe URLs (KAN-327 owns adoption).
import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises';
import { createServer } from 'node:http';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const [consumerRepository, consumerRef] = process.argv.slice(2);
assert.ok(consumerRepository && consumerRef, 'Usage: node scripts/check-access-consumer.mjs YOUR_LIFE_ONE_PATH YOUR_LIFE_ONE_COMMIT');
const commit = execFileSync('git', ['-C', consumerRepository, 'rev-parse', `${consumerRef}^{commit}`],
  { encoding: 'utf8' }).trim();
const sourcePath = 'supabase/functions/_shared/serviceAccess.ts';
const source = execFileSync('git', ['-C', consumerRepository, 'show', `${commit}:${sourcePath}`]);
const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const temporary = await mkdtemp(join(tmpdir(), 'kan326-consumer-'));
const routes = new Map();
for (const [service, domains] of [['life', ['inventory', 'jobs']], ['finance', ['review', 'equity']]]) {
  for (const domain of domains) {
    const file = `contracts/${service}-service/${domain}-access.json`;
    const bytes = await readFile(join(root, file));
    const fixture = JSON.parse(bytes);
    assert.equal(fixture.request.method, 'GET');
    assert.equal(fixture.status, 204);
    assert.equal(fixture.body, null);
    const denied = JSON.parse(await readFile(join(root, `contracts/${service}-service/agent-not-approved.json`)));
    routes.set(fixture.request.path, { file, fixture, denied, sha256: hash(bytes), requests: 0 });
  }
}

let status = 204;
const server = createServer((request, response) => {
  const route = routes.get(request.url);
  assert.ok(route, `Unexpected request ${request.url}`);
  assert.equal(request.method, 'GET');
  assert.equal(request.headers.authorization, 'Bearer owned-validation-fixture');
  route.requests += 1;
  response.writeHead(status, { 'Cache-Control': 'no-store' });
  response.end(status === 403 ? JSON.stringify(route.denied.body) : undefined);
});

try {
  const modulePath = join(temporary, 'serviceAccess.ts');
  await writeFile(modulePath, source);
  const { probeServiceAccess } = await import(pathToFileURL(modulePath).href);
  await new Promise(resolveListening => server.listen(0, '127.0.0.1', resolveListening));
  const results = [];
  for (const [path, route] of routes) {
    const url = new URL(path, `http://127.0.0.1:${server.address().port}`);
    for (const [code, expected] of [[route.fixture.status, 'approved'], [403, 'not_approved'], [401, 'invalid_token']]) {
      status = code;
      assert.equal(await probeServiceAccess(url, 'owned-validation-fixture'), expected);
      results.push({ path, status: code, result: expected, fixtureSha256: route.sha256 });
    }
    assert.equal(route.requests, 3, 'Approval/revocation must issue a fresh request each time');
  }
  console.log(JSON.stringify({ consumerCommit: commit, sourcePath, sourceSha256: hash(source), results }, null, 2));
} finally {
  await new Promise(resolveClosed => server.close(resolveClosed));
  await rm(temporary, { recursive: true, force: true });
}

function hash(bytes) {
  return createHash('sha256').update(bytes).digest('hex');
}
