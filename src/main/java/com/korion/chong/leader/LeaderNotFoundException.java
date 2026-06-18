package com.korion.chong.leader;

public class LeaderNotFoundException extends RuntimeException {
    public LeaderNotFoundException(long leaderId) {
        super("Country leader not found: " + leaderId);
    }
}
