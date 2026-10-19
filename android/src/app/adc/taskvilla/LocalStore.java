package app.adc.taskvilla;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;

/** Local-only repository. Author: Aniruddha DuttaChowdhury, ADChowdhury. */
public final class LocalStore extends SQLiteOpenHelper {
    public LocalStore(Context context) { super(context, "taskvilla.db", null, 4); }
    @Override public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE lists (id TEXT PRIMARY KEY, name TEXT NOT NULL, icon TEXT NOT NULL, shared INTEGER NOT NULL, colour TEXT, built_in INTEGER NOT NULL, joined INTEGER NOT NULL, invitees TEXT NOT NULL DEFAULT '[]', latest_on_top INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE tasks (id INTEGER PRIMARY KEY, list_id TEXT NOT NULL REFERENCES lists(id) ON DELETE CASCADE, title TEXT NOT NULL, done INTEGER NOT NULL, starred INTEGER NOT NULL, due TEXT, reminder TEXT, tags TEXT, note TEXT, assignee TEXT, subtasks TEXT NOT NULL, attachments TEXT NOT NULL, sort_order INTEGER NOT NULL DEFAULT 0, comments TEXT)");
        db.execSQL("CREATE INDEX tasks_by_list ON tasks(list_id)");
        db.execSQL("CREATE TABLE preferences (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion<2)db.execSQL("ALTER TABLE lists ADD COLUMN invitees TEXT NOT NULL DEFAULT '[]'");
        if(oldVersion<3){db.execSQL("ALTER TABLE tasks ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0");db.execSQL("UPDATE tasks SET sort_order=rowid");}
        if(oldVersion<4){db.execSQL("ALTER TABLE lists ADD COLUMN latest_on_top INTEGER NOT NULL DEFAULT 0");db.execSQL("ALTER TABLE tasks ADD COLUMN comments TEXT");}
    }
    private static int flag(JSONObject value, String key) { return value.optBoolean(key) ? 1 : 0; }
    public synchronized String load() {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor prefs = db.rawQuery("SELECT value FROM preferences WHERE key='state'", null)) {
            if (!prefs.moveToFirst()) return "null";
            JSONObject result = new JSONObject(); result.put("state", new JSONObject(prefs.getString(0)));
            JSONArray lists = new JSONArray();
            try (Cursor c = db.rawQuery("SELECT * FROM lists ORDER BY rowid", null)) {
                while(c.moveToNext()) { JSONObject l=new JSONObject(); l.put("id",c.getString(0)); l.put("name",c.getString(1)); l.put("icon",c.getString(2)); l.put("shared",c.getInt(3)!=0); l.put("colour",c.getString(4)); l.put("builtIn",c.getInt(5)!=0); l.put("joined",c.getInt(6)!=0); l.put("invitees",new JSONArray(c.getString(7))); l.put("latestTaskOnTop",c.getInt(8)!=0); lists.put(l); }
            }
            JSONArray tasks = new JSONArray();
            try (Cursor c = db.rawQuery("SELECT * FROM tasks ORDER BY sort_order,rowid", null)) {
                while(c.moveToNext()) { JSONObject t=new JSONObject(); t.put("id",c.getLong(0)); t.put("list",c.getString(1)); t.put("title",c.getString(2)); t.put("done",c.getInt(3)!=0); t.put("star",c.getInt(4)!=0); t.put("due",c.getString(5)); t.put("reminder",c.getString(6)); t.put("tags",c.getString(7)); t.put("note",c.getString(8)); t.put("who",c.getString(9)); t.put("subtasks",new JSONArray(c.getString(10))); t.put("files",new JSONArray(c.getString(11))); t.put("sortOrder",c.getInt(12)); if(!c.isNull(13))t.put("comments",new JSONArray(c.getString(13))); tasks.put(t); }
            }
            result.put("lists", lists); result.put("tasks", tasks);
            try(Cursor extra=db.rawQuery("SELECT value FROM preferences WHERE key='tracker'",null)){result.put("tracker",extra.moveToFirst()?new JSONObject(extra.getString(0)):new JSONObject().put("activities",new JSONArray()).put("customTypes",new JSONArray()));}
            return result.toString();
        } catch (Exception e) { throw new IllegalStateException("Could not read local data", e); }
    }
    public synchronized void save(String json) throws Exception {
        JSONObject data=new JSONObject(json); JSONArray lists=data.getJSONArray("lists"), tasks=data.getJSONArray("tasks");
        SQLiteDatabase db=getWritableDatabase(); db.beginTransaction();
        try {
            db.delete("tasks",null,null); db.delete("lists",null,null);
            for(int i=0;i<lists.length();i++) { JSONObject l=lists.getJSONObject(i); ContentValues v=new ContentValues(); v.put("id",l.getString("id")); v.put("name",l.getString("name")); v.put("icon",l.optString("icon","list")); v.put("shared",flag(l,"shared")); v.put("colour",l.optString("colour","saffron")); v.put("built_in",flag(l,"builtIn")); v.put("joined",flag(l,"joined")); v.put("invitees",l.optJSONArray("invitees")==null?"[]":l.getJSONArray("invitees").toString()); v.put("latest_on_top",flag(l,"latestTaskOnTop")); db.insertOrThrow("lists",null,v); }
            for(int i=0;i<tasks.length();i++) { JSONObject t=tasks.getJSONObject(i); ContentValues v=new ContentValues(); v.put("id",t.getLong("id")); v.put("list_id",t.getString("list")); v.put("title",t.getString("title")); v.put("done",flag(t,"done")); v.put("starred",flag(t,"star")); v.put("due",t.optString("due","")); v.put("reminder",t.optString("reminder","No reminder")); v.put("tags",t.optString("tags","")); v.put("note",t.optString("note","")); v.put("assignee",t.optString("who","")); v.put("subtasks",t.optJSONArray("subtasks")==null?"[]":t.getJSONArray("subtasks").toString()); v.put("attachments",t.optJSONArray("files")==null?"[]":t.getJSONArray("files").toString()); v.put("sort_order",i); if(t.optJSONArray("comments")!=null)v.put("comments",t.getJSONArray("comments").toString()); db.insertOrThrow("tasks",null,v); }
            ContentValues p=new ContentValues(); p.put("key","state"); p.put("value",data.getJSONObject("state").toString()); db.insertWithOnConflict("preferences",null,p,SQLiteDatabase.CONFLICT_REPLACE); ContentValues extra=new ContentValues();extra.put("key","tracker");extra.put("value",data.optJSONObject("tracker")==null?"{\"activities\":[],\"customTypes\":[]}":data.getJSONObject("tracker").toString());db.insertWithOnConflict("preferences",null,extra,SQLiteDatabase.CONFLICT_REPLACE);db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
}
