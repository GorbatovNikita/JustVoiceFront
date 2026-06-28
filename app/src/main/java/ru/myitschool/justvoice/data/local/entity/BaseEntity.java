package ru.myitschool.justvoice.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Ignore;

public abstract class BaseEntity {

    @NonNull
    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    @NonNull
    @ColumnInfo(name = "created_at")
    private long createdAt;

    @NonNull
    @ColumnInfo(name = "sync_status")
    private SyncStatus syncStatus;

    @ColumnInfo(name = "server_id")
    private String serverId;

    public enum SyncStatus {
        SYNCED(0),
        UNSYNCED(1),
        PENDING_DELETE(2),
        PENDING_UPDATE(3);

        private final int value;

        SyncStatus(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static SyncStatus fromValue(int value) {
            for (SyncStatus status : SyncStatus.values()) {
                if (status.value == value) {
                    return status;
                }
            }
            return UNSYNCED;
        }
    }

    public BaseEntity() {
        long currentTime = System.currentTimeMillis();
        this.createdAt = currentTime;
        this.updatedAt = currentTime;
        this.syncStatus = SyncStatus.UNSYNCED;
    }

    @Ignore
    public BaseEntity(long updatedAt, long createdAt, SyncStatus syncStatus, String serverId) {
        this.updatedAt = updatedAt;
        this.createdAt = createdAt;
        this.syncStatus = syncStatus;
        this.serverId = serverId;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public void markAsUpdated() {
        this.updatedAt = System.currentTimeMillis();
        if (this.syncStatus == SyncStatus.SYNCED) {
            this.syncStatus = SyncStatus.PENDING_UPDATE;
        }
    }

    public void markForDeletion() {
        this.syncStatus = SyncStatus.PENDING_DELETE;
        this.updatedAt = System.currentTimeMillis();
    }

    public void markAsSynced(String serverId) {
        this.syncStatus = SyncStatus.SYNCED;
        if (serverId != null) {
            this.serverId = serverId;
        }
        this.updatedAt = System.currentTimeMillis();
    }
}