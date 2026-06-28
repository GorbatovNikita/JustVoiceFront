package ru.myitschool.justvoice.data.remote;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import ru.myitschool.justvoice.data.local.entity.TranscriptionTask;

@Dao
public interface TranscriptionTaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(TranscriptionTask task);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<TranscriptionTask> tasks);

    @Update
    void update(TranscriptionTask task);

    @Delete
    void delete(TranscriptionTask task);

    @Query("DELETE FROM transcription_tasks WHERE id = :taskId")
    void deleteById(long taskId);

    @Query("SELECT * FROM transcription_tasks WHERE id = :taskId")
    LiveData<TranscriptionTask> getTaskById(long taskId);

    @Query("SELECT * FROM transcription_tasks WHERE id = :taskId")
    TranscriptionTask getTaskByIdSync(long taskId);

    @Query("SELECT * FROM transcription_tasks WHERE server_id = :serverId")
    TranscriptionTask getTaskByServerId(String serverId);

    @Query("SELECT * FROM transcription_tasks WHERE user_id = :userId ORDER BY updated_at DESC")
    LiveData<List<TranscriptionTask>> getTasksByUserId(long userId);

    @Query("SELECT * FROM transcription_tasks ORDER BY updated_at DESC")
    LiveData<List<TranscriptionTask>> getAllTasks();

    @Query("SELECT * FROM transcription_tasks WHERE sync_status != :syncStatus")
    LiveData<List<TranscriptionTask>> getUnsyncedTasks(int syncStatus);

    @Query("SELECT * FROM transcription_tasks WHERE sync_status != :syncStatus")
    List<TranscriptionTask> getUnsyncedTasksSync(int syncStatus);

    @Query("SELECT * FROM transcription_tasks WHERE status = :status AND user_id = :userId")
    LiveData<List<TranscriptionTask>> getTasksByStatus(String status, long userId);

    @Query("UPDATE transcription_tasks SET sync_status = :syncStatus, server_id = :serverId, " +
            "updated_at = :updatedAt WHERE id = :taskId")
    void updateSyncStatus(long taskId, int syncStatus, String serverId, long updatedAt);

    @Query("SELECT COUNT(*) FROM transcription_tasks WHERE sync_status != :syncStatus")
    int getUnsyncedCountSync(int syncStatus);

    @Query("SELECT COUNT(*) FROM transcription_tasks WHERE sync_status != :syncStatus")
    LiveData<Integer> getUnsyncedCount(int syncStatus);

    @Query("DELETE FROM transcription_tasks")
    void deleteAll();
}