package br.com.pabelis.quickclock.adapters.in.web.workday;

final class WorkDayRequests {

    private WorkDayRequests() {
    }

    record SaveWorkDayRequest(
        boolean workedBeforeLunch,
        boolean workedAfterLunch
    ) {
    }
}
