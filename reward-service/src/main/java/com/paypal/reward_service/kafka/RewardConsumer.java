package com.paypal.reward_service.kafka;

import com.paypal.reward_service.entity.Reward;
import com.paypal.reward_service.entity.Transaction;
import com.paypal.reward_service.repository.RewardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Component
public class RewardConsumer {

    private static final Logger log = LoggerFactory.getLogger(RewardConsumer.class);

    private final RewardRepository rewardRepository;
    private final BigDecimal pointsRate;
    private final BigDecimal minimumRewardAmount;

    public RewardConsumer(RewardRepository rewardRepository,
                          @Value("${app.rewards.points-rate}") BigDecimal pointsRate,
                          @Value("${app.rewards.minimum-amount-for-rewards}") BigDecimal minimumRewardAmount) {
        this.rewardRepository = rewardRepository;
        this.pointsRate = pointsRate;
        this.minimumRewardAmount = minimumRewardAmount;
    }

    @KafkaListener(topics = "txn-initiated", groupId = "reward-group")
    public void consumerTransaction(Transaction transaction){
        try {
            if(rewardRepository.existsByTransactionId(transaction.getId())){
                log.info("Reward already exists for transaction {}", transaction.getId());
                return;
            }
            if (transaction.getAmount().compareTo(minimumRewardAmount) < 0) {
                log.info("Transaction {} below minimum reward amount", transaction.getId());
                return;
            }

            Reward reward = new Reward();
            reward.setUserId(transaction.getSenderId());
            reward.setPoints(transaction.getAmount().multiply(pointsRate).setScale(4, RoundingMode.HALF_UP));
            reward.setSentAt(LocalDateTime.now());
            reward.setTransactionId(transaction.getId());

            rewardRepository.save(reward);
            log.info("Reward saved for transaction {}", transaction.getId());
        }catch (Exception e){
            log.error("Failed to process reward for transaction {}", transaction.getId(), e);
            throw e; // Let Spring Kafka handle the retry
        }
    }


}
