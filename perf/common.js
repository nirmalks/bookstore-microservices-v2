import http from 'k6/http';
import { check, fail } from 'k6';
import encoding from 'k6/encoding';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN_URL = __ENV.TOKEN_URL || `${BASE_URL}/api/v1/oauth2/token`;
const CLIENT_ID = __ENV.CLIENT_ID || 'local-client';
const CLIENT_SECRET = __ENV.CLIENT_SECRET || 'secret';
const USER_ID = __ENV.USER_ID || '1';
const BOOK_ID = __ENV.BOOK_ID || '1';
const BOOK_SEARCH = __ENV.BOOK_SEARCH || 'java';

export const config = {
  baseUrl: BASE_URL,
  tokenUrl: TOKEN_URL,
  clientId: CLIENT_ID,
  clientSecret: CLIENT_SECRET,
  userId: USER_ID,
  bookId: BOOK_ID,
  bookSearch: BOOK_SEARCH,
};

export function getAccessToken() {
  const payload = {
    grant_type: 'password',
    client_id: config.clientId,
    client_secret: config.clientSecret,
    username: 'admin',
    password: 'admin123',
    scope: 'read',
  };

  const credentials = `${config.clientId}:${config.clientSecret}`;
  const encodedCredentials = encoding.b64encode(credentials);

  const response = http.post(config.tokenUrl, payload, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Authorization': `Basic ${encodedCredentials}`
    },

    tags: { name: 'auth_token' },
  });

  const ok = check(response, {
    'auth token returned 200': (r) => r.status === 200,
    'auth token has access_token': (r) => !!r.json('access_token'),
  });

  if (!ok) {
    fail(`Token request failed: status=${response.status} body=${response.body}`);
  }

  return response.json('access_token');
}

export function authHeaders(token) {
  return {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json',
  };
}

export function directOrderPayload() {
  return JSON.stringify({
    userId: Number(config.userId),
    items: [
      {
        bookId: Number(config.bookId),
        quantity: 1,
      },
    ],
    address: {
      city: 'Bengaluru',
      state: 'KA',
      country: 'India',
      pinCode: '560001',
      default: false,
      address: '1 Performance Street',
    },
  });
}
