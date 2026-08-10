package com.caltalk.backend.schedule;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class UpdateScheduleRequest {

    private String title;
    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
    private String location;
    private Long version;
    private List<Integer> reminderMinutes;
    private boolean titlePresent;
    private boolean startAtPresent;
    private boolean endAtPresent;
    private boolean locationPresent;
    private boolean versionPresent;
    private boolean reminderMinutesPresent;

    @JsonSetter("title")
    public void setTitle(String title) {
        titlePresent = true;
        this.title = title == null ? null : title.trim();
    }

    @JsonSetter("startAt")
    public void setStartAt(OffsetDateTime startAt) {
        startAtPresent = true;
        this.startAt = startAt;
    }

    @JsonSetter("endAt")
    public void setEndAt(OffsetDateTime endAt) {
        endAtPresent = true;
        this.endAt = endAt;
    }

    @JsonSetter("location")
    public void setLocation(String location) {
        locationPresent = true;
        this.location = location == null || location.trim().isEmpty() ? null : location.trim();
    }

    @JsonSetter("version")
    public void setVersion(Long version) {
        versionPresent = true;
        this.version = version;
    }

    @JsonSetter("reminderMinutes")
    public void setReminderMinutes(List<Integer> reminderMinutes) {
        reminderMinutesPresent = true;
        this.reminderMinutes = reminderMinutes;
    }

    @JsonAnySetter
    public void rejectUnknownField(String field, Object value) {
        throw new IllegalArgumentException("Unsupported request field.");
    }

    public String title() { return title; }
    public OffsetDateTime startAt() { return startAt; }
    public OffsetDateTime endAt() { return endAt; }
    public String location() { return location; }
    public Long version() { return version; }
    public boolean titlePresent() { return titlePresent; }
    public boolean startAtPresent() { return startAtPresent; }
    public boolean endAtPresent() { return endAtPresent; }
    public boolean locationPresent() { return locationPresent; }
    public boolean versionPresent() { return versionPresent; }
    public List<Integer> reminderMinutes() { return reminderMinutes; }
    public boolean reminderMinutesPresent() { return reminderMinutesPresent; }
}
