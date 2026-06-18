package com.korion.chong.settlement;

import com.korion.chong.leader.AuthContext;
import com.korion.chong.leader.ForbiddenCountryScopeException;
import com.korion.chong.leader.LeaderDashboardRepository;
import com.korion.chong.leader.LeaderNotFoundException;
import com.korion.chong.partner.PartnerAuthContext;
import com.korion.chong.partner.PartnerDashboardRepository;
import com.korion.chong.partner.PartnerNotFoundException;
import com.korion.chong.partner.PartnerProfile;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementService {
    private final SettlementRepository repository;
    private final LeaderDashboardRepository leaderRepository;
    private final PartnerDashboardRepository partnerRepository;
    private final Clock clock;

    public SettlementService(
            SettlementRepository repository,
            LeaderDashboardRepository leaderRepository,
            PartnerDashboardRepository partnerRepository,
            Clock clock
    ) {
        this.repository = repository;
        this.leaderRepository = leaderRepository;
        this.partnerRepository = partnerRepository;
        this.clock = clock;
    }

    @Transactional
    public SettlementActionResponse createLeaderRequest(AuthContext authContext, SettlementCreateRequest request) {
        long leaderUserId = authorizeLeader(authContext).userId();
        validatePeriod(request);
        SettlementActionResponse response = createPartnerBackedRequest(
                "LEADER",
                authContext.leaderId(),
                leaderUserId,
                request
        );
        repository.recordActivity("LEADER", "SETTLEMENT_REQUEST_CREATED", "SUCCESS", "distributor_settlement_requests", response.settlementRequestId(), request.requestId());
        return response;
    }

    @Transactional
    public SettlementActionResponse createPartnerRequest(PartnerAuthContext authContext, SettlementCreateRequest request) {
        PartnerProfile profile = authorizePartner(authContext);
        validatePeriod(request);
        SettlementActionResponse response = createPartnerBackedRequest(
                "PARTNER",
                authContext.partnerId(),
                profile.userId(),
                request
        );
        repository.recordActivity("PARTNER", "SETTLEMENT_REQUEST_CREATED", "SUCCESS", "distributor_settlement_requests", response.settlementRequestId(), request.requestId());
        return response;
    }

    @Transactional
    public SettlementActionResponse approve(AuthContext authContext, long settlementRequestId, SettlementActionRequest request) {
        long reviewerUserId = authorizeLeader(authContext).userId();
        SettlementRequestRecord current = loadManageable(authContext, settlementRequestId, "REQUESTED");
        BigDecimal approvedAmount = request.amount() == null ? current.requestedAmount() : request.amount();
        if (approvedAmount.compareTo(current.requestedAmount()) > 0) {
            throw new IllegalArgumentException("approved amount cannot exceed requested amount");
        }
        SettlementRequestRecord updated = repository.approve(
                settlementRequestId,
                reviewerUserId,
                approvedAmount,
                current.requestedAmount().subtract(approvedAmount),
                request.reason(),
                Instant.now(clock)
        );
        repository.recordActivity("LEADER", "SETTLEMENT_REQUEST_APPROVED", "SUCCESS", "distributor_settlement_requests", settlementRequestId, request.requestId());
        return response(updated, "SETTLEMENT_REQUEST_APPROVED", "settlement.request.approved");
    }

    @Transactional
    public SettlementActionResponse reject(AuthContext authContext, long settlementRequestId, SettlementActionRequest request) {
        long reviewerUserId = authorizeLeader(authContext).userId();
        loadManageable(authContext, settlementRequestId, "REQUESTED");
        SettlementRequestRecord updated = repository.reject(settlementRequestId, reviewerUserId, request.reason(), Instant.now(clock));
        repository.recordActivity("LEADER", "SETTLEMENT_REQUEST_REJECTED", "SUCCESS", "distributor_settlement_requests", settlementRequestId, request.requestId());
        return response(updated, "SETTLEMENT_REQUEST_REJECTED", "settlement.request.rejected");
    }

    @Transactional
    public SettlementActionResponse markPaid(AuthContext authContext, long settlementRequestId, SettlementActionRequest request) {
        long reviewerUserId = authorizeLeader(authContext).userId();
        loadManageable(authContext, settlementRequestId, "APPROVED");
        SettlementRequestRecord updated = repository.markPaid(settlementRequestId, reviewerUserId, Instant.now(clock));
        repository.recordActivity("LEADER", "SETTLEMENT_REQUEST_PAID", "SUCCESS", "distributor_settlement_requests", settlementRequestId, request == null ? null : request.requestId());
        return response(updated, "SETTLEMENT_REQUEST_PAID", "settlement.request.paid");
    }

    private SettlementActionResponse createPartnerBackedRequest(String recipientType, long partnerId, long requesterUserId, SettlementCreateRequest request) {
        BigDecimal available = repository.availablePartnerAmount(partnerId, request.periodStart(), request.periodEnd());
        if (request.requestedAmount().compareTo(available) > 0) {
            throw new IllegalArgumentException("requested amount cannot exceed available settlement amount");
        }
        long requestId = repository.createPartnerRequest(
                requesterUserId,
                recipientType,
                partnerId,
                request.walletAddressId(),
                request.periodStart(),
                request.periodEnd(),
                request.requestedAmount(),
                request.memo(),
                Instant.now(clock)
        );
        repository.attachPartnerCommissions(requestId, partnerId, request.periodStart(), request.periodEnd());
        SettlementRequestRecord created = repository.findForUpdate(requestId)
                .orElseThrow(() -> new IllegalArgumentException("settlement request was not created: " + requestId));
        return response(created, "SETTLEMENT_REQUEST_CREATED", "settlement.request.created");
    }

    private com.korion.chong.leader.LeaderProfile authorizeLeader(AuthContext authContext) {
        return leaderRepository.findLeaderProfile(authContext.leaderId())
                .orElseThrow(() -> new LeaderNotFoundException(authContext.leaderId()));
    }

    private PartnerProfile authorizePartner(PartnerAuthContext authContext) {
        PartnerProfile profile = partnerRepository.findPartnerProfile(authContext.partnerId())
                .orElseThrow(() -> new PartnerNotFoundException(authContext.partnerId()));
        if (!authContext.canAccess(profile.countryScope())) {
            throw new ForbiddenCountryScopeException(profile.countryScope());
        }
        return profile;
    }

    private SettlementRequestRecord loadManageable(AuthContext authContext, long settlementRequestId, String requiredStatus) {
        authorizeLeader(authContext);
        SettlementRequestRecord request = repository.findForUpdate(settlementRequestId)
                .orElseThrow(() -> new IllegalArgumentException("settlement request was not found: " + settlementRequestId));
        if (!requiredStatus.equals(request.status())) {
            throw new IllegalArgumentException("settlement request status must be " + requiredStatus + ": " + request.status());
        }
        if (!repository.canLeaderManage(authContext.leaderId(), request)) {
            throw new ForbiddenCountryScopeException("settlementRequest:" + settlementRequestId);
        }
        return request;
    }

    private void validatePeriod(SettlementCreateRequest request) {
        if (request.periodEnd().isBefore(request.periodStart())) {
            throw new IllegalArgumentException("periodEnd must be on or after periodStart");
        }
    }

    private SettlementActionResponse response(SettlementRequestRecord record, String resultCode, String messageKey) {
        return new SettlementActionResponse(
                record.id(),
                record.requestNo(),
                record.recipientType(),
                record.recipientPartnerId(),
                record.recipientMerchantId(),
                record.requestedAmount(),
                record.approvedAmount(),
                record.heldAmount(),
                record.status(),
                resultCode,
                messageKey
        );
    }
}
