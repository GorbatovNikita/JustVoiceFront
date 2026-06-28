package ru.myitschool.justvoice.data.local;

import androidx.room.TypeConverter;

import ru.myitschool.justvoice.data.local.entity.BaseEntity;

public class Converters {

    @TypeConverter
    public static int fromSyncStatus(BaseEntity.SyncStatus status) {
        return status == null ? BaseEntity.SyncStatus.UNSYNCED.getValue() : status.getValue();
    }

    @TypeConverter
    public static BaseEntity.SyncStatus toSyncStatus(int value) {
        return BaseEntity.SyncStatus.fromValue(value);
    }
}
