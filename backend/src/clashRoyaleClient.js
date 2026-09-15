'use strict';

const API_BASE_URL = 'https://api.clashroyale.com/v1';

/**
 * Thin wrapper around the official Clash Royale API. The API key it uses is locked to this
 * server's IP address by Supercell (see developer.clashroyale.com), which is the whole reason
 * this proxy exists: a mobile app has no fixed IP, so the key can never live on-device.
 */
class ClashRoyaleClient {
  constructor(apiKey) {
    if (!apiKey) {
      throw new Error('CLASH_ROYALE_API_KEY is not set.');
    }
    this.apiKey = apiKey;
  }

  /** Clash Royale tags are written with a leading '#', which must be percent-encoded as %23 in the URL. */
  static encodeTag(rawTag) {
    const tag = rawTag.startsWith('#') ? rawTag : `#${rawTag}`;
    return encodeURIComponent(tag);
  }

  async getPlayer(rawTag) {
    return this._get(`/players/${ClashRoyaleClient.encodeTag(rawTag)}`);
  }

  async getBattleLog(rawTag) {
    return this._get(`/players/${ClashRoyaleClient.encodeTag(rawTag)}/battlelog`);
  }

  /** The full card list (name, id, elixir cost, max level...) — used to resolve deck-link card IDs to names. */
  async getCards() {
    return this._get('/cards');
  }

  async _get(path) {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      headers: {
        Authorization: `Bearer ${this.apiKey}`,
        Accept: 'application/json',
      },
    });

    const body = await response.json().catch(() => null);
    if (!response.ok) {
      const error = new Error(body?.message || `Clash Royale API error (${response.status})`);
      error.status = response.status;
      throw error;
    }
    return body;
  }
}

module.exports = { ClashRoyaleClient };
