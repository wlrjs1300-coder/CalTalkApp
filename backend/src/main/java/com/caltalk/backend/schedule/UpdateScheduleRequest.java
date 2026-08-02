package com.caltalk.backend.schedule;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class UpdateScheduleRequest {

    private String title;
    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
    private String location;
    private Long version;
    private boolean titlePresent;
    private boolean startAtPresent;
    private boolean endAtPresent;
    private boolean locationPresent;
    private boolean versionPresent;

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
}
