package com.korion.chong.leader;

public record PartnerSearchCriteria(
        String keyword,
        String status,
        String region,
        int page,
        int size
) {
    public int offset() {
        return page * size;
    }
}
