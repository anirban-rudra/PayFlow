import http from "k6/http";
import { check, sleep } from "k6";
import encoding from "k6/encoding";

export const options = {
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<800"]
  },
  scenarios: {
    wallet_transfer_smoke: {
      executor: "ramping-vus",
      stages: [
        { duration: "30s", target: 5 },
        { duration: "1m", target: 20 },
        { duration: "30s", target: 0 }
      ]
    }
  }
};

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const password = __ENV.TEST_PASSWORD || "LoadTest@12345";

function jsonHeaders(token, idempotencyKey) {
  const headers = {
    "Content-Type": "application/json",
    "X-Correlation-Id": `k6-${__VU}-${__ITER}`
  };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  if (idempotencyKey) {
    headers["Idempotency-Key"] = idempotencyKey;
  }
  return { headers };
}

function signup(name, email, payTag) {
  return http.post(`${baseUrl}/auth/signup`, JSON.stringify({
    name,
    email,
    payTag,
    password
  }), jsonHeaders());
}

function login(email) {
  const response = http.post(`${baseUrl}/auth/login`, JSON.stringify({ email, password }), jsonHeaders());
  check(response, {
    "login succeeded": (res) => res.status === 200,
    "login returned token": (res) => Boolean(res.json("token"))
  });
  return response.json("token");
}

function userIdFromJwt(token) {
  const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
  return JSON.parse(encoding.b64decode(payload, "s")).userId;
}

export default function () {
  const suffix = `${Date.now()}-${__VU}-${__ITER}`;
  const senderEmail = `sender-${suffix}@payflow.local`;
  const receiverEmail = `receiver-${suffix}@payflow.local`;
  const senderPayTag = `@sender${__VU}${__ITER}${Date.now()}`;
  const receiverPayTag = `@receiver${__VU}${__ITER}${Date.now()}`;

  check(signup("Load Sender", senderEmail, senderPayTag), {
    "sender signup accepted": (res) => [200, 201, 409].includes(res.status)
  });
  check(signup("Load Receiver", receiverEmail, receiverPayTag), {
    "receiver signup accepted": (res) => [200, 201, 409].includes(res.status)
  });

  const senderToken = login(senderEmail);
  const senderId = userIdFromJwt(senderToken);

  const topUp = http.post(`${baseUrl}/api/v1/wallets/${senderId}/top-ups`, JSON.stringify({
    amount: "1000.00",
    currency: "INR"
  }), jsonHeaders(senderToken, `topup-${suffix}`));
  check(topUp, {
    "top up succeeded": (res) => res.status === 200
  });

  const transfer = http.post(`${baseUrl}/api/transactions/create`, JSON.stringify({
    receiverPayTag,
    amount: "10.00"
  }), jsonHeaders(senderToken, `transfer-${suffix}`));
  check(transfer, {
    "transfer succeeded": (res) => res.status === 200,
    "transfer has public reference": (res) => Boolean(res.json("publicReference"))
  });

  const history = http.get(`${baseUrl}/api/transactions/user/${senderId}`, jsonHeaders(senderToken));
  check(history, {
    "history succeeded": (res) => res.status === 200,
    "history includes records": (res) => Array.isArray(res.json()) && res.json().length > 0
  });

  sleep(1);
}
