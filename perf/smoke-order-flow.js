import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

import { authHeaders, config, directOrderPayload, getAccessToken } from './common.js';

export const options = {
  vus: 1,
  iterations: 5,
  thresholds: {
    http_req_failed: ['rate<0.20'],
    smoke_checks: ['rate>0.90'],
  },
};

export const smokeChecks = new Rate('smoke_checks');

let token;

export function setup() {
  token = getAccessToken();
  return { token };
}

export default function (data) {
  const booksResponse = http.get(`${config.baseUrl}/api/v1/books?page=0&size=5`, {
    tags: { name: 'smoke_books_list' },
  });
  const orderResponse = http.post(`${config.baseUrl}/api/v1/orders/direct`, directOrderPayload(), {
    headers: authHeaders(data.token),
    tags: { name: 'smoke_orders_direct' },
  });

  smokeChecks.add(
    check(booksResponse, {
      'smoke books returned 200': (r) => r.status === 200,
    }) &&
      check(orderResponse, {
        'smoke order returned success status': (r) => r.status === 200 || r.status === 201,
      })
  );

  sleep(1);
}
