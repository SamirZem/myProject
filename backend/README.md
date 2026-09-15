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

This needs a host with a genuinely fixed **outbound** IP address — the one Supercell's servers
see when this backend calls their API, not just a stable public URL. A plain VPS guarantees that
with zero extra configuration; most PaaS free tiers (Render, Railway, Fly.io...) don't — check
their current docs for a "static outbound IP" feature (often a paid add-on) before assuming one
works.

### Walkthrough with any VPS (DigitalOcean, Hetzner, OVH, Oracle Cloud Free Tier...)

1. Create the smallest/cheapest VM with Docker support (or install Docker yourself after
   creation — most providers offer a "Docker" image preset). Note its public IPv4 address; that's
   your fixed IP.
2. SSH in, then clone this repo (or just copy the `backend/` folder) and build the image:
   ```
   git clone https://github.com/SamirZem/myProject.git
   cd myProject/backend
   docker build -t clash-analyzer-backend .
   ```
3. Go to https://developer.clashroyale.com, create an API key, and restrict it to the VM's IP
   from step 1.
4. Run the container, passing that key in (`-p 80:3000` serves it on the standard HTTP port so
   you don't need to include a port number in the app's Settings URL):
   ```
   docker run -d --restart unless-stopped --name clash-backend \
     -p 80:3000 -e CLASH_ROYALE_API_KEY=your-key-here \
     clash-analyzer-backend
   ```
5. Check it's alive: `curl http://<your-vm-ip>/health` should return `{"status":"ok"}`.
6. In the Android app's Settings screen, set the backend URL to `http://<your-vm-ip>/`.

The app allows plain HTTP specifically for this self-hosted backend (see the comment on
`android:usesCleartextTraffic` in `AndroidManifest.xml`) since setting up a TLS certificate for a
personal single-user tool is unnecessary friction. If you'd rather have HTTPS anyway (e.g. you're
sharing this with others), put a reverse proxy like [Caddy](https://caddyserver.com/) in front —
it provisions and renews Let's Encrypt certificates automatically given a real domain name
pointed at the VM.

### Without Docker

`npm install && npm start` works directly too (set `CLASH_ROYALE_API_KEY` in the environment
first, e.g. via `.env` — see `.env.example` — with a process manager like `pm2` to keep it running
and restart it on reboot).

## Tests

```
npm test
```

Runs against a mocked `fetch`, so it needs no real API key or network access.
