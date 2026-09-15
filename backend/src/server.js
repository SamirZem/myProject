'use strict';

const express = require('express');
const { ClashRoyaleClient } = require('./clashRoyaleClient');

function createApp(apiKey = process.env.CLASH_ROYALE_API_KEY) {
  const app = express();
  const client = new ClashRoyaleClient(apiKey);

  // Minimal CORS: the app only ever calls this from the Android client, but keeping it open
  // makes local testing (curl, browser) painless. Lock this down to your own origins if you
  // expose this publicly beyond personal use.
  app.use((req, res, next) => {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
    if (req.method === 'OPTIONS') return res.sendStatus(204);
    next();
  });

  app.get('/health', (_req, res) => res.json({ status: 'ok' }));

  app.get('/api/player/:tag', async (req, res) => {
    try {
      const player = await client.getPlayer(req.params.tag);
      res.json(player);
    } catch (err) {
      forwardError(res, err);
    }
  });

  app.get('/api/player/:tag/battlelog', async (req, res) => {
    try {
      const battleLog = await client.getBattleLog(req.params.tag);
      res.json(battleLog);
    } catch (err) {
      forwardError(res, err);
    }
  });

  app.use((_req, res) => res.status(404).json({ message: 'Not found' }));

  return app;
}

function forwardError(res, err) {
  const status = err.status && err.status >= 400 && err.status < 600 ? err.status : 502;
  res.status(status).json({ message: err.message });
}

if (require.main === module) {
  const port = process.env.PORT || 3000;
  const app = createApp();
  app.listen(port, () => {
    console.log(`Clash Analyzer backend listening on port ${port}`);
  });
}

module.exports = { createApp };
