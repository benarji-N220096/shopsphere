package com.shopsphere.order.dto;

public class BatchRecoveryResponse {

    private int totalFound;
    private int confirmedCount;
    private int cancelledCount;
    private int stillPendingCount;
    private int failedCount;

    public BatchRecoveryResponse() {
    }

    public BatchRecoveryResponse(int totalFound, int confirmedCount, int cancelledCount, int stillPendingCount, int failedCount) {
        this.totalFound = totalFound;
        this.confirmedCount = confirmedCount;
        this.cancelledCount = cancelledCount;
        this.stillPendingCount = stillPendingCount;
        this.failedCount = failedCount;
    }

    public int getTotalFound() {
        return totalFound;
    }

    public void setTotalFound(int totalFound) {
        this.totalFound = totalFound;
    }

    public int getConfirmedCount() {
        return confirmedCount;
    }

    public void setConfirmedCount(int confirmedCount) {
        this.confirmedCount = confirmedCount;
    }

    public int getCancelledCount() {
        return cancelledCount;
    }

    public void setCancelledCount(int cancelledCount) {
        this.cancelledCount = cancelledCount;
    }

    public int getStillPendingCount() {
        return stillPendingCount;
    }

    public void setStillPendingCount(int stillPendingCount) {
        this.stillPendingCount = stillPendingCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }
}
