'use strict';

const { test } = require('node:test');
const assert = require('node:assert');
const http = require('node:http');
const { createApp } = require('../src/server');

/** Minimal client using node:http directly, so it never touches the mocked global.fetch below. */
function requestJson(port, path) {
  return new Promise((resolve, reject) => {
    http.get({ host: '127.0.0.1', port, path }, (res) => {
      let data = '';
      res.on('data', (chunk) => (data += chunk));
      res.on('end', () => {
        resolve({ status: res.statusCode, body: JSON.parse(data) });
      });
    }).on('error', reject);
  });
}

async function withServer(mockFetch, fn) {
  const originalFetch = global.fetch;
  global.fetch = mockFetch;
  const app = createApp('fake-api-key');
  const server = app.listen(0);
  const port = server.address().port;
  try {
    await fn(port);
  } finally {
    global.fetch = originalFetch;
    server.close();
  }
}

test('GET /api/player/:tag proxies to the official API and percent-encodes the tag', async () => {
  let requestedUrl = null;
  await withServer(
    async (url) => {
      requestedUrl = url;
      return { ok: true, status: 200, json: async () => ({ tag: '#ABC123', name: 'Test Player' }) };
    },
    async (port) => {
      const { status, body } = await requestJson(port, '/api/player/%23ABC123');
      assert.strictEqual(status, 200);
      assert.strictEqual(body.name, 'Test Player');
      assert.ok(requestedUrl.includes('%23ABC123'), `expected encoded tag in ${requestedUrl}`);
    },
  );
});

test('GET /api/player/:tag/battlelog forwards the battle log array', async () => {
  await withServer(
    async () => ({ ok: true, status: 200, json: async () => [{ battleTime: '20260101T120000.000Z' }] }),
    async (port) => {
      const { status, body } = await requestJson(port, '/api/player/ABC123/battlelog');
      assert.strictEqual(status, 200);
      assert.strictEqual(body.length, 1);
    },
  );
});

test('an upstream error is forwarded with its status code', async () => {
  await withServer(
    async () => ({ ok: false, status: 404, json: async () => ({ message: 'unknown player' }) }),
    async (port) => {
      const { status, body } = await requestJson(port, '/api/player/DOES_NOT_EXIST');
      assert.strictEqual(status, 404);
      assert.strictEqual(body.message, 'unknown player');
    },
  );
});

test('unknown routes return 404', async () => {
  await withServer(
    async () => ({ ok: true, status: 200, json: async () => ({}) }),
    async (port) => {
      const { status } = await requestJson(port, '/not-a-real-route');
      assert.strictEqual(status, 404);
    },
  );
});
