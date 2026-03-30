import checkoutOrderFlow, { setup as baseSetup } from './checkout-order-flow.js';

export const options = {
  scenarios: {
    checkout_orders_stress: {
      executor: 'ramping-vus',
      startVUs: 2,
      stages: [
        { duration: '1m', target: 10 },
        { duration: '3m', target: 20 },
        { duration: '1m', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.10'],
    http_req_duration: ['p(95)<2000'],
    order_checks: ['rate>0.90'],
  },
};

export function setup() {
  return baseSetup();
}

export default function (data) {
  checkoutOrderFlow(data);
}
