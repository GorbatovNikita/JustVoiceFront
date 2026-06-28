package ru.myitschool.justvoice.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "transcription_tasks",
        indices = {@Index("user_id"), @Index("server_id")})
public class TranscriptionTask extends BaseEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    @ColumnInfo(name = "original_filename")
    private String originalFilename;

    @ColumnInfo(name = "display_name")
    private String displayName;

    @NonNull
    @ColumnInfo(name = "status")
    private String status;

    @ColumnInfo(name = "language")
    private String language;

    @ColumnInfo(name = "duration")
    private Double duration;

    @ColumnInfo(name = "segments_count")
    private int segmentsCount;

    @ColumnInfo(name = "file_path")
    private String filePath;

    @ColumnInfo(name = "user_id")
    private long userId;

    @ColumnInfo(name = "topic")
    private String topic;

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public TranscriptionTask(@NonNull String originalFilename, @NonNull String status,
                             long userId) {
        super();
        this.originalFilename = originalFilename;
        this.status = status;
        this.userId = userId;
    }

    @Ignore
    public TranscriptionTask(long id, @NonNull String originalFilename,
                             String displayName, @NonNull String status,
                             String language, Double duration,
                             int segmentsCount, String filePath, long userId,
                             long updatedAt, long createdAt,
                             SyncStatus syncStatus, String serverId) {
        super(updatedAt, createdAt, syncStatus, serverId);
        this.id = id;
        this.originalFilename = originalFilename;
        this.displayName = displayName;
        this.status = status;
        this.language = language;
        this.duration = duration;
        this.segmentsCount = segmentsCount;
        this.filePath = filePath;
        this.userId = userId;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    @NonNull
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(@NonNull String originalFilename) {
        this.originalFilename = originalFilename;
        markAsUpdated();
    }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        markAsUpdated();
    }

    @NonNull
    public String getStatus() { return status; }
    public void setStatus(@NonNull String status) {
        this.status = status;
        markAsUpdated();
    }

    public String getLanguage() { return language; }
    public void setLanguage(String language) {
        this.language = language;
        markAsUpdated();
    }

    public Double getDuration() { return duration; }
    public void setDuration(Double duration) {
        this.duration = duration;
        markAsUpdated();
    }

    public int getSegmentsCount() { return segmentsCount; }
    public void setSegmentsCount(int segmentsCount) {
        this.segmentsCount = segmentsCount;
        markAsUpdated();
    }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) {
        this.filePath = filePath;
        markAsUpdated();
    }

    public long getUserId() { return userId; }
    public void setUserId(long userId) {
        this.userId = userId;
        markAsUpdated();
    }

    public String getEffectiveDisplayName() {
        if (displayName != null && !displayName.isEmpty()) {
            return displayName;
        }
        if (originalFilename != null && (originalFilename.startsWith("recording_") ||
                originalFilename.startsWith("audio_"))) {
            return "Recording #" + getId();
        }
        return originalFilename != null ? originalFilename : "Unknown";
    }
}