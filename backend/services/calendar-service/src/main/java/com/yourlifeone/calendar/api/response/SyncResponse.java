package com.yourlifeone.calendar.api.response;

import java.util.List;

public record SyncResponse(List<AccountSyncResponse> accounts) {
}
