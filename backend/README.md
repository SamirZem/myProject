# Clash Analyzer — backend proxy

A tiny Express server that sits between the Android app and the official Clash Royale API.
It exists for one reason: Supercell's API keys are locked to a single, fixed IP address, and a
phone doesn't have one — so the key has to live on a server, not in the APK.

The app calls **this** server; this server calls Supercell using the key from
`CLASH_ROYALE_API_KEY`.

## Endpoints

- `GET /api/player/:tag` → proxies `GET /v1/players/{tag}`
- `GET /api/player/:tag/battlelog` → proxies `GET /v1/players/{tag}/battlelog`
- `GET /health` → `{ "status": "ok" }`

`:tag` can be passed with or without the leading `#` (e.g. `/api/player/%23ABC123` or
`/api/player/ABC123`); it's normalized and percent-encoded before being sent to Supercell.

## Setup

1. Deploy this anywhere with a fixed outbound IP (a small VPS, Render, Fly.io, Railway...).
2. Note that server's public IP address.
3. Go to https://developer.clashroyale.com, create an API key, and restrict it to that IP.
4. Set `CLASH_ROYALE_API_KEY` as an environment variable (see `.env.example`).
5. `npm install && npm start` (or `docker build -t clash-analyzer-backend . && docker run -p 3000:3000 -e CLASH_ROYALE_API_KEY=... clash-analyzer-backend`).
6. Put this server's URL into the Android app's Settings screen.

## Tests

```
npm test
```

Runs against a mocked `fetch`, so it needs no real API key or network access.
