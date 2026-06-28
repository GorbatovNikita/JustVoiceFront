package ru.myitschool.justvoice.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class TranscriptionSegment {

    @SerializedName("speaker")
    private String speaker;

    @SerializedName("start_time")
    private Double startTime;

    @SerializedName("end_time")
    private Double endTime;

    @SerializedName("text")
    private String text;

    @SerializedName("confidence")
    private Double confidence;

    public String getSpeaker() {
        return speaker;
    }

    public void setSpeaker(String speaker) {
        this.speaker = speaker;
    }

    public Double getStartTime() {
        return startTime;
    }

    public void setStartTime(Double startTime) {
        this.startTime = startTime;
    }

    public Double getEndTime() {
        return endTime;
    }

    public void setEndTime(Double endTime) {
        this.endTime = endTime;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
}