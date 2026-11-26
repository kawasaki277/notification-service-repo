package com.clinicbooking.notification.domain;

public record ChannelStats(long total, long sent, long failed, long retries) {
}
