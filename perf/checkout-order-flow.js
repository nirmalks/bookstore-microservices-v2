import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

import { authHeaders, config, directOrderPayload, getAccessToken } from './common.js';

export const options = {
  scenarios: {
    checkout_orders: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '30s', target: 2 },
        { duration: '1m', target: 5 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.10'],
    http_req_duration: ['p(95)<2000'],
    order_checks: ['rate>0.90'],
  },
};

export const orderChecks = new Rate('order_checks');

let token;

export function setup() {
  token = getAccessToken();
  return { token };
}

export default function (data) {
  const response = http.post(`${config.baseUrl}/api/v1/orders/direct`, directOrderPayload(), {
    headers: authHeaders(data.token),
    tags: { name: 'orders_direct' },
  });

  orderChecks.add(
    check(response, {
      'order returned success status': (r) => r.status === 200 || r.status === 201,
    })
  );

  sleep(1);
}
