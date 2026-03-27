import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

import { config } from './common.js';

const REQUEST_TIMEOUT = __ENV.REQUEST_TIMEOUT || '10s';
const LIST_SIZE = __ENV.BOOK_PAGE_SIZE || '10';

export const options = {
  scenarios: {
    browse: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '30s', target: 2 },
        { duration: '45s', target: 3 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.10'],
    http_req_duration: ['p(95)<3000'],
    browse_checks: ['rate>0.95'],
    browse_timeouts: ['rate<0.05'],
  },
};

export const browseChecks = new Rate('browse_checks');
export const browseTimeouts = new Rate('browse_timeouts');

export default function () {
  const listResponse = http.get(`${config.baseUrl}/api/v1/books?page=0&size=${LIST_SIZE}`, {
    tags: { name: 'books_list' },
    timeout: REQUEST_TIMEOUT,
  });
  const searchResponse = http.get(
    `${config.baseUrl}/api/v1/books/search?search=${encodeURIComponent(config.bookSearch)}&page=0&size=${LIST_SIZE}`,
    {
      tags: { name: 'books_search' },
      timeout: REQUEST_TIMEOUT,
    }
  );

  const listTimedOut = !!listResponse.error;
  const searchTimedOut = !!searchResponse.error;
  browseTimeouts.add(listTimedOut || searchTimedOut);

  browseChecks.add(
    check(listResponse, {
      'books list did not timeout': (r) => !r.error,
      'books list returned 200': (r) => r.status === 200,
    }) &&
    check(searchResponse, {
      'books search did not timeout': (r) => !r.error,
      'books search returned 200': (r) => r.status === 200,
    })
  );

  sleep(1);
}
