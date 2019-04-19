package net.xzos.upgraderall;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class DatabaseHelper {
    RepoDatabaseHelper repoDatabaseHelper(Context context, int version) {
        String name = "Repo";
        SQLiteDatabase.CursorFactory factory = null;
        return new RepoDatabaseHelper(context, name, factory, version);
    }
}

class RepoDatabaseHelper extends SQLiteOpenHelper {
    private static final String CREATE_REPO = "create table Repo ("
            + "id integer primary key autoincrement, "
            + "api text, "
            + "name text, "
            + "owner text, "
            + "repo text, "
            + "tag_name text, "
            + "latest_release text, "
            + "installed_version text)";

    RepoDatabaseHelper(Context context, String name,
                       SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_REPO);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
