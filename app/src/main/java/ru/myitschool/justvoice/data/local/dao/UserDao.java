package ru.myitschool.justvoice.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import ru.myitschool.justvoice.data.local.entity.User;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(User user);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<User> users);

    @Update
    void update(User user);

    @Delete
    void delete(User user);

    @Query("DELETE FROM users WHERE id = :userId")
    void deleteById(long userId);

    @Query("SELECT * FROM users WHERE id = :userId")
    LiveData<User> getUserById(long userId);

    @Query("SELECT * FROM users WHERE id = :userId")
    User getUserByIdSync(long userId);

    @Query("SELECT * FROM users WHERE email = :email")
    User getUserByEmail(String email);

    @Query("SELECT * FROM users ORDER BY updated_at DESC")
    LiveData<List<User>> getAllUsers();

    @Query("SELECT * FROM users WHERE sync_status != :syncStatus")
    LiveData<List<User>> getUnsyncedUsers(int syncStatus);

    @Query("SELECT * FROM users WHERE sync_status != :syncStatus")
    List<User> getUnsyncedUsersSync(int syncStatus);

    @Query("UPDATE users SET sync_status = :syncStatus, server_id = :serverId, " +
            "updated_at = :updatedAt WHERE id = :userId")
    void updateSyncStatus(long userId, int syncStatus, String serverId, long updatedAt);

    @Query("SELECT COUNT(*) FROM users WHERE sync_status != :syncStatus")
    int getUnsyncedCountSync(int syncStatus);

    @Query("SELECT COUNT(*) FROM users WHERE sync_status != :syncStatus")
    LiveData<Integer> getUnsyncedCount(int syncStatus);

    @Query("DELETE FROM users")
    void deleteAll();
}