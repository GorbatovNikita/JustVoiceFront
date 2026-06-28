package ru.myitschool.justvoice.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import ru.myitschool.justvoice.data.local.dao.UserDao;
import ru.myitschool.justvoice.data.local.entity.TranscriptionTask;
import ru.myitschool.justvoice.data.local.entity.User;
import ru.myitschool.justvoice.data.remote.TranscriptionTaskDao;

@Database(entities = {User.class, TranscriptionTask.class}, version = 6, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract TranscriptionTaskDao transcriptionTaskDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "app_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}