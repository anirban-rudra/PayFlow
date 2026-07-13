import { request } from "./http";
import type { JwtResponse, LoginRequest, MessageResponse, SignupRequest } from "./types";

export function login(payload: LoginRequest) {
  return request<JwtResponse>("/auth/login", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function signup(payload: SignupRequest) {
  return request<MessageResponse>("/auth/signup", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}
