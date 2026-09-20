package com.example.modernized;

public final class Carddemo {
    public Decision review(CardAccount account) {
        int riskScore = 42;
        if ("A".equals(account.status()) && riskScore < 70) {
            return new Decision("APPROVED", riskScore);
        }
        return new Decision("REVIEW", riskScore);
    }

    public record CardAccount(String cardId, String customerId, String status) {}

    public record Decision(String status, int riskScore) {}
}
