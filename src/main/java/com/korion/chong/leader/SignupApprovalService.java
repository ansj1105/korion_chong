package com.korion.chong.leader;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SignupApprovalService {
    private static final Set<String> DECISIONABLE_STATUSES = Set.of("REQUESTED", "REVIEWING", "NEED_MORE_INFO", "HOLD");

    private final SignupApprovalRepository repository;
    private final LeaderDashboardRepository leaderRepository;
    private final Clock clock;

    public SignupApprovalService(SignupApprovalRepository repository, LeaderDashboardRepository leaderRepository, Clock clock) {
        this.repository = repository;
        this.leaderRepository = leaderRepository;
        this.clock = clock;
    }

    @Transactional
    public SignupApprovalResponse approve(AuthContext authContext, long applicationId, SignupApprovalDecisionRequest request) {
        SignupApplicationApprovalRecord application = loadDecisionableApplication(applicationId);
        LeaderProfile leader = authorizeLeader(authContext, application.country());
        Instant now = Instant.now(clock);
        long userId = repository.createUser(application);

        Long partnerId = null;
        Long merchantId = null;
        Long contractId;
        Long walletAddressId;
        if ("PARTNER".equals(application.applicantType())) {
            Long parentPartnerId = parentLeaderId(application);
            if (!currentLeaderIdEquals(authContext.leaderId(), parentPartnerId)) {
                throw new ForbiddenCountryScopeException(application.country());
            }
            partnerId = repository.createPartner(application, userId, leader.userId(), parentPartnerId, now);
            contractId = repository.createContract(application, leader.userId(), parentPartnerId, partnerId, null, now);
            walletAddressId = repository.createWalletAddress(application, userId, partnerId, null, now);
        } else if ("MERCHANT".equals(application.applicantType())) {
            ParentScope parentScope = merchantParentScope(application, authContext.leaderId());
            merchantId = repository.createMerchant(application, userId, leader.userId(), parentScope.leaderPartnerId(), parentScope.salesPartnerId(), now);
            contractId = repository.createContract(application, leader.userId(), parentScope.leaderPartnerId(), parentScope.salesPartnerId(), merchantId, now);
            walletAddressId = repository.createWalletAddress(application, userId, null, merchantId, now);
        } else {
            throw new IllegalArgumentException("Unsupported applicantType: " + application.applicantType());
        }

        repository.approveApplication(applicationId, now);
        repository.recordActivity("LEADER", "SIGNUP_APPLICATION_APPROVED", "SUCCESS", "distributor_signup_applications", applicationId, request == null ? null : request.requestId());
        return new SignupApprovalResponse(
                applicationId,
                application.applicantType(),
                "APPROVED",
                userId,
                partnerId,
                merchantId,
                contractId,
                walletAddressId,
                "SIGNUP_APPLICATION_APPROVED",
                "approval.signup.approved"
        );
    }

    @Transactional
    public SignupRejectionResponse reject(AuthContext authContext, long applicationId, SignupApprovalDecisionRequest request) {
        SignupApplicationApprovalRecord application = loadDecisionableApplication(applicationId);
        authorizeLeader(authContext, application.country());
        if ("PARTNER".equals(application.applicantType()) && !currentLeaderIdEquals(authContext.leaderId(), parentLeaderId(application))) {
            throw new ForbiddenCountryScopeException(application.country());
        }
        if ("MERCHANT".equals(application.applicantType())) {
            merchantParentScope(application, authContext.leaderId());
        }
        String reason = request == null ? null : request.reason();
        Instant now = Instant.now(clock);
        repository.rejectApplication(applicationId, reason, now);
        repository.recordActivity("LEADER", "SIGNUP_APPLICATION_REJECTED", "SUCCESS", "distributor_signup_applications", applicationId, request == null ? null : request.requestId());
        return new SignupRejectionResponse(
                applicationId,
                application.applicantType(),
                "REJECTED",
                "SIGNUP_APPLICATION_REJECTED",
                "approval.signup.rejected"
        );
    }

    private SignupApplicationApprovalRecord loadDecisionableApplication(long applicationId) {
        SignupApplicationApprovalRecord application = repository.findApplicationForUpdate(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("signup application was not found: " + applicationId));
        if (!DECISIONABLE_STATUSES.contains(application.status())) {
            throw new IllegalArgumentException("signup application is not decisionable: " + application.status());
        }
        return application;
    }

    private LeaderProfile authorizeLeader(AuthContext authContext, String countryScope) {
        LeaderProfile profile = leaderRepository.findLeaderProfile(authContext.leaderId())
                .orElseThrow(() -> new LeaderNotFoundException(authContext.leaderId()));
        if (countryScope == null || !authContext.canAccess(countryScope) || !profile.countryScopes().contains(countryScope)) {
            throw new ForbiddenCountryScopeException(countryScope);
        }
        return profile;
    }

    private Long parentLeaderId(SignupApplicationApprovalRecord application) {
        if ("COUNTRY_LEADER".equals(application.ownerPartnerType())) {
            return application.ownerPartnerId();
        }
        return application.ownerLeaderPartnerId();
    }

    private ParentScope merchantParentScope(SignupApplicationApprovalRecord application, long currentLeaderId) {
        if ("COUNTRY_LEADER".equals(application.ownerPartnerType())) {
            if (!currentLeaderIdEquals(currentLeaderId, application.ownerPartnerId())) {
                throw new ForbiddenCountryScopeException(application.country());
            }
            return new ParentScope(application.ownerPartnerId(), null);
        }
        if ("SALES_PARTNER".equals(application.ownerPartnerType())) {
            if (!currentLeaderIdEquals(currentLeaderId, application.ownerLeaderPartnerId())) {
                throw new ForbiddenCountryScopeException(application.country());
            }
            return new ParentScope(application.ownerLeaderPartnerId(), application.ownerPartnerId());
        }
        throw new IllegalArgumentException("merchant approval requires a country leader or sales partner referral owner");
    }

    private boolean currentLeaderIdEquals(long currentLeaderId, Long ownerLeaderId) {
        return ownerLeaderId != null && ownerLeaderId == currentLeaderId;
    }

    private record ParentScope(Long leaderPartnerId, Long salesPartnerId) {
    }
}
