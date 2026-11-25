package br.com.pabelis.quickclock.application.finance.port;

public interface FinanceSourceGuard {
    // Call inside the source write transaction, before changing or deleting an existing source.
    void lockAndAssertUnreceived(String sourceType, Long sourceId, Long companyId);
}
