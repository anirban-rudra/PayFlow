import Decimal from "decimal.js";
import type { Reward } from "../api/types";

export function sumRewardPoints(rewards: Reward[] = []) {
  return rewards.reduce((total, reward) => total.plus(reward.points), new Decimal(0));
}
