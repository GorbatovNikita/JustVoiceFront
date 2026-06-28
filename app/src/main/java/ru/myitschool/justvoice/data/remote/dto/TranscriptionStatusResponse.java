package ru.myitschool.justvoice.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class TranscriptionStatusResponse {

    @SerializedName("id")
    private int id;

    @SerializedName("original_filename")
    private String originalFilename;

    @SerializedName("display_name")
    private String displayName;

    @SerializedName("status")
    private String status;

    @SerializedName("language")
    private String language;

    @SerializedName("duration")
    private Double duration;

    @SerializedName("segments_count")
    private int segmentsCount;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("topic")
    private String topic;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Double getDuration() {
        return duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    public int getSegmentsCount() {
        return segmentsCount;
    }

    public void setSegmentsCount(int segmentsCount) {
        this.segmentsCount = segmentsCount;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}