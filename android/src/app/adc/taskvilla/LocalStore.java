package app.adc.taskvilla;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.UUID;

/** Local-only repository. Author: Aniruddha DuttaChowdhury, ADChowdhury. */
public final class LocalStore extends SQLiteOpenHelper {
    private long accountEpoch=0;
    private final ThreadLocal<Long> syncEpoch=new ThreadLocal<>();
    public synchronized void beginAccountSync(){syncEpoch.set(accountEpoch);}
    public void endAccountSync(){syncEpoch.remove();}
    private void assertAccountSync(){Long token=syncEpoch.get();if(token!=null&&token.longValue()!=accountEpoch)throw new IllegalStateException("Sync cancelled after sign-out.");}
    private static long createdMillis(String value,long fallback){try{long t=java.time.Instant.parse(value).toEpochMilli();return t>946684800000L?t:fallback;}catch(Exception e){return fallback;}}
    private static long documentRevision(SQLiteDatabase db){String v=meta(db,"document_revision");return v==null?0:Long.parseLong(v);}
    private static void bumpDocument(SQLiteDatabase db){putMeta(db,"document_revision",Long.toString(documentRevision(db)+1));}
    public LocalStore(Context context) { super(context, "taskvilla.db", null, 9); }
    @Override public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE lists (id TEXT PRIMARY KEY, name TEXT NOT NULL, icon TEXT NOT NULL, shared INTEGER NOT NULL, colour TEXT, built_in INTEGER NOT NULL, joined INTEGER NOT NULL, invitees TEXT NOT NULL DEFAULT '[]', latest_on_top INTEGER NOT NULL DEFAULT 0, shared_user_id TEXT, created_at INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE tasks (id INTEGER PRIMARY KEY, list_id TEXT NOT NULL REFERENCES lists(id) ON DELETE CASCADE, title TEXT NOT NULL, done INTEGER NOT NULL, starred INTEGER NOT NULL, due TEXT, reminder TEXT, tags TEXT, note TEXT, assignee TEXT, subtasks TEXT NOT NULL, attachments TEXT NOT NULL, sort_order INTEGER NOT NULL DEFAULT 0, comments TEXT, owner_user_id TEXT)");
        db.execSQL("CREATE INDEX tasks_by_list ON tasks(list_id)");
        db.execSQL("CREATE TABLE preferences (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
        createSyncTables(db);createPersonalFavourites(db);
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion<2)db.execSQL("ALTER TABLE lists ADD COLUMN invitees TEXT NOT NULL DEFAULT '[]'");
        if(oldVersion<3){db.execSQL("ALTER TABLE tasks ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0");db.execSQL("UPDATE tasks SET sort_order=rowid");}
        if(oldVersion<4){db.execSQL("ALTER TABLE lists ADD COLUMN latest_on_top INTEGER NOT NULL DEFAULT 0");db.execSQL("ALTER TABLE tasks ADD COLUMN comments TEXT");}
        if(oldVersion<5)createSyncTables(db);
        if(oldVersion<6){db.execSQL("ALTER TABLE lists ADD COLUMN shared_user_id TEXT");db.execSQL("UPDATE lists SET shared_user_id=(SELECT value FROM sync_meta WHERE key='user_id') WHERE joined=1");}
        if(oldVersion<7)db.execSQL("ALTER TABLE tasks ADD COLUMN owner_user_id TEXT");
        if(oldVersion<8){createPersonalFavourites(db);String u=meta(db,"user_id");if(u!=null)putMeta(db,"favourite_user",u);}
        if(oldVersion<9){db.execSQL("ALTER TABLE lists ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0");db.execSQL("UPDATE lists SET created_at=rowid");}
    }
    private static void createPersonalFavourites(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS personal_favourites (user_id TEXT NOT NULL,local_id TEXT NOT NULL,starred INTEGER NOT NULL,dirty INTEGER NOT NULL DEFAULT 1,PRIMARY KEY(user_id,local_id))");
        db.execSQL("INSERT OR IGNORE INTO personal_favourites(user_id,local_id,starred) SELECT coalesce((SELECT value FROM sync_meta WHERE key='user_id'),'local'),CAST(t.id AS TEXT),1 FROM tasks t JOIN lists l ON l.id=t.list_id WHERE t.starred=1 AND l.joined=0");
    }
    private static String favouriteUser(SQLiteDatabase db){String user=meta(db,"favourite_user");return user==null?"local":user;}
    private static boolean rawStar(SQLiteDatabase db,String id){try(Cursor c=db.rawQuery("SELECT starred FROM tasks WHERE id=?",new String[]{id})){return c.moveToFirst()&&c.getInt(0)!=0;}}
    private static boolean personalStar(SQLiteDatabase db,String id){try(Cursor c=db.rawQuery("SELECT starred FROM personal_favourites WHERE user_id=? AND local_id=?",new String[]{favouriteUser(db),id})){return c.moveToFirst()&&c.getInt(0)!=0;}}
    private static void savePersonalStar(SQLiteDatabase db,String id,boolean star){if(personalStar(db,id)==star)return;ContentValues v=new ContentValues();v.put("user_id",favouriteUser(db));v.put("local_id",id);v.put("starred",star?1:0);v.put("dirty",1);db.insertWithOnConflict("personal_favourites",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    public synchronized void signedOut(){assertAccountSync();putMeta(getWritableDatabase(),"favourite_user","local");}
    public synchronized JSONArray pendingFavourites()throws Exception{assertAccountSync();JSONArray out=new JSONArray();try(Cursor c=getReadableDatabase().rawQuery("SELECT s.server_id,f.starred FROM personal_favourites f JOIN sync_records s ON s.entity='task' AND s.local_id=f.local_id JOIN tasks t ON CAST(t.id AS TEXT)=f.local_id WHERE f.user_id=? AND f.dirty=1 AND s.revision IS NOT NULL LIMIT 500",new String[]{favouriteUser(getReadableDatabase())})){while(c.moveToNext())out.put(new JSONObject().put("taskId",c.getString(0)).put("starred",c.getInt(1)!=0));}return out;}
    public synchronized void applyFavourites(String user,JSONArray sent,JSONObject response)throws Exception{assertAccountSync();SQLiteDatabase db=getWritableDatabase();if(!user.equals(favouriteUser(db)))return;db.beginTransaction();try{
        for(int i=0;i<sent.length();i++){JSONObject item=sent.getJSONObject(i);db.execSQL("UPDATE personal_favourites SET dirty=0 WHERE user_id=? AND local_id=(SELECT local_id FROM sync_records WHERE entity='task' AND server_id=?) AND starred=?",new Object[]{user,item.getString("taskId"),item.optBoolean("starred")?1:0});}
        JSONArray entries=response.optJSONArray("favourites");if(entries!=null)for(int i=0;i<entries.length();i++){JSONObject item=entries.getJSONObject(i);String local=null;try(Cursor c=db.rawQuery("SELECT local_id FROM sync_records WHERE entity='task' AND server_id=?",new String[]{item.getString("taskId")})){if(c.moveToFirst())local=c.getString(0);}if(local==null)continue;try(Cursor c=db.rawQuery("SELECT dirty FROM personal_favourites WHERE user_id=? AND local_id=?",new String[]{user,local})){if(c.moveToFirst()&&c.getInt(0)!=0)continue;}ContentValues v=new ContentValues();v.put("user_id",user);v.put("local_id",local);v.put("starred",item.optBoolean("starred")?1:0);v.put("dirty",0);db.insertWithOnConflict("personal_favourites",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
        bumpDocument(db);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    private static void createSyncTables(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS sync_meta (key TEXT PRIMARY KEY,value TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS sync_records (entity TEXT NOT NULL,local_id TEXT NOT NULL,server_id TEXT NOT NULL,revision INTEGER,PRIMARY KEY(entity,local_id),UNIQUE(entity,server_id))");
        db.execSQL("CREATE TABLE IF NOT EXISTS sync_outbox (change_id TEXT PRIMARY KEY,entity TEXT NOT NULL,local_id TEXT NOT NULL,entity_id TEXT NOT NULL,operation TEXT NOT NULL,base_revision INTEGER,payload TEXT,client_modified_at TEXT NOT NULL,batch_id TEXT,status TEXT NOT NULL DEFAULT 'pending',conflict TEXT)");
        db.execSQL("CREATE INDEX IF NOT EXISTS sync_outbox_pending_idx ON sync_outbox(status,batch_id)");
    }
    private static int flag(JSONObject value, String key) { return value.optBoolean(key) ? 1 : 0; }
    public synchronized String load() {assertAccountSync();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor prefs = db.rawQuery("SELECT value FROM preferences WHERE key='state'", null)) {
            if (!prefs.moveToFirst()) return "null";
            JSONObject result = new JSONObject(); result.put("state", new JSONObject(prefs.getString(0)));
            JSONArray lists = new JSONArray();
            try (Cursor c = db.rawQuery("SELECT * FROM lists ORDER BY created_at,id", null)) {
                while(c.moveToNext()) { JSONObject l=new JSONObject(); l.put("id",c.getString(0)); l.put("name",c.getString(1)); l.put("icon",c.getString(2)); l.put("shared",c.getInt(3)!=0); l.put("colour",c.getString(4)); l.put("builtIn",c.getInt(5)!=0); l.put("joined",c.getInt(6)!=0); l.put("invitees",new JSONArray(c.getString(7))); l.put("latestTaskOnTop",c.getInt(8)!=0); l.put("sharedUserId",c.isNull(9)?JSONObject.NULL:c.getString(9));l.put("createdAt",java.time.Instant.ofEpochMilli(c.getLong(10)).toString()); lists.put(l); }
            }
            JSONArray tasks = new JSONArray();
            try (Cursor c = db.rawQuery("SELECT * FROM tasks ORDER BY sort_order,rowid", null)) {
                while(c.moveToNext()) { JSONObject t=new JSONObject(); t.put("id",c.getLong(0)); t.put("list",c.getString(1)); t.put("title",c.getString(2)); t.put("done",c.getInt(3)!=0); t.put("star",c.getInt(4)!=0); t.put("due",c.getString(5)); t.put("reminder",c.getString(6)); t.put("tags",c.getString(7)); t.put("note",c.getString(8)); t.put("who",c.getString(9)); t.put("subtasks",new JSONArray(c.getString(10))); t.put("files",new JSONArray(c.getString(11))); t.put("sortOrder",c.getInt(12)); if(!c.isNull(13))t.put("comments",new JSONArray(c.getString(13))); if(!c.isNull(14))t.put("ownerUserId",c.getString(14)); tasks.put(t); }
            }
            for(int i=0;i<tasks.length();i++){JSONObject t=tasks.getJSONObject(i);t.put("star",personalStar(db,t.getString("id")));}
            result.put("lists", lists); result.put("tasks", tasks);
            try(Cursor extra=db.rawQuery("SELECT value FROM preferences WHERE key='tracker'",null)){result.put("tracker",extra.moveToFirst()?new JSONObject(extra.getString(0)):new JSONObject().put("activities",new JSONArray()).put("customTypes",new JSONArray()));}
            result.put("documentRevision",documentRevision(db));return result.toString();
        } catch (Exception e) { throw new IllegalStateException("Could not read local data", e); }
    }
    public synchronized long save(String json) throws Exception {assertAccountSync();
        JSONObject data=new JSONObject(json); JSONArray lists=data.getJSONArray("lists"), tasks=data.getJSONArray("tasks");
        SQLiteDatabase db=getWritableDatabase(); db.beginTransaction();
        try {
            if(data.optLong("documentRevision",-1)!=documentRevision(db))throw new IllegalStateException("STALE_DOCUMENT");
            JSONObject previous=readDocument(db);
            for(int i=0;i<tasks.length();i++){JSONObject t=tasks.getJSONObject(i);String id=t.getString("id");savePersonalStar(db,id,t.optBoolean("star"));t.put("star",rawStar(db,id));}
            java.util.LinkedHashMap<String,JSONObject> oldLists=map(previous.optJSONArray("lists"),"id");
            queueLocalDiff(db,previous,data);
            db.delete("tasks",null,null); db.delete("lists",null,null);
            for(int i=0;i<lists.length();i++) { JSONObject l=lists.getJSONObject(i); ContentValues v=new ContentValues(); v.put("id",l.getString("id")); JSONObject previousList=oldLists.get(l.getString("id"));long previousCreated=previousList==null?System.currentTimeMillis():java.time.Instant.parse(previousList.getString("createdAt")).toEpochMilli();v.put("created_at",createdMillis(l.optString("createdAt"),previousCreated)); v.put("name",l.getString("name")); v.put("icon",l.optString("icon","list")); v.put("shared",flag(l,"shared")); v.put("colour",l.optString("colour","saffron")); v.put("built_in",flag(l,"builtIn")); v.put("joined",flag(l,"joined")); v.put("invitees",l.optJSONArray("invitees")==null?"[]":l.getJSONArray("invitees").toString()); v.put("latest_on_top",flag(l,"latestTaskOnTop"));if(l.isNull("sharedUserId"))v.putNull("shared_user_id");else v.put("shared_user_id",l.optString("sharedUserId",null)); db.insertOrThrow("lists",null,v); }
            for(int i=0;i<tasks.length();i++) { JSONObject t=tasks.getJSONObject(i); ContentValues v=new ContentValues(); v.put("id",t.getLong("id")); v.put("list_id",t.getString("list")); v.put("title",t.getString("title")); v.put("done",flag(t,"done")); v.put("starred",flag(t,"star")); v.put("due",t.optString("due","")); v.put("reminder",t.optString("reminder","No reminder")); v.put("tags",t.optString("tags","")); v.put("note",t.optString("note","")); v.put("assignee",t.optString("who","")); v.put("subtasks",t.optJSONArray("subtasks")==null?"[]":t.getJSONArray("subtasks").toString()); v.put("attachments",t.optJSONArray("files")==null?"[]":t.getJSONArray("files").toString()); v.put("sort_order",i); if(t.optJSONArray("comments")!=null)v.put("comments",t.getJSONArray("comments").toString());if(!t.isNull("ownerUserId"))v.put("owner_user_id",t.optString("ownerUserId")); db.insertOrThrow("tasks",null,v); }
            ContentValues p=new ContentValues(); p.put("key","state"); p.put("value",data.getJSONObject("state").toString()); db.insertWithOnConflict("preferences",null,p,SQLiteDatabase.CONFLICT_REPLACE); ContentValues extra=new ContentValues();extra.put("key","tracker");extra.put("value",data.optJSONObject("tracker")==null?"{\"activities\":[],\"customTypes\":[]}":data.getJSONObject("tracker").toString());db.insertWithOnConflict("preferences",null,extra,SQLiteDatabase.CONFLICT_REPLACE);bumpDocument(db);db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        return documentRevision(db);
    }

    private JSONObject readDocument(SQLiteDatabase db)throws Exception{
        JSONObject result=new JSONObject();JSONArray lists=new JSONArray(),tasks=new JSONArray();
        try(Cursor c=db.rawQuery("SELECT * FROM lists ORDER BY created_at,id",null)){while(c.moveToNext()){JSONObject l=new JSONObject();l.put("id",c.getString(0));l.put("name",c.getString(1));l.put("icon",c.getString(2));l.put("shared",c.getInt(3)!=0);l.put("colour",c.getString(4));l.put("builtIn",c.getInt(5)!=0);l.put("joined",c.getInt(6)!=0);l.put("invitees",new JSONArray(c.getString(7)));l.put("latestTaskOnTop",c.getInt(8)!=0);l.put("sharedUserId",c.isNull(9)?JSONObject.NULL:c.getString(9));l.put("createdAt",java.time.Instant.ofEpochMilli(c.getLong(10)).toString());lists.put(l);}}
        try(Cursor c=db.rawQuery("SELECT * FROM tasks ORDER BY sort_order,rowid",null)){while(c.moveToNext()){JSONObject t=new JSONObject();t.put("id",c.getLong(0));t.put("list",c.getString(1));t.put("title",c.getString(2));t.put("done",c.getInt(3)!=0);t.put("star",c.getInt(4)!=0);t.put("due",c.getString(5));t.put("reminder",c.getString(6));t.put("tags",c.getString(7));t.put("note",c.getString(8));t.put("who",c.getString(9));t.put("subtasks",new JSONArray(c.getString(10)));t.put("files",new JSONArray(c.getString(11)));t.put("sortOrder",c.getInt(12));if(!c.isNull(13))t.put("comments",new JSONArray(c.getString(13)));if(!c.isNull(14))t.put("ownerUserId",c.getString(14));tasks.put(t);}}
        try(Cursor c=db.rawQuery("SELECT value FROM preferences WHERE key='state'",null)){if(c.moveToFirst())result.put("state",new JSONObject(c.getString(0)));else result.put("state",new JSONObject());}
        try(Cursor c=db.rawQuery("SELECT value FROM preferences WHERE key='tracker'",null)){result.put("tracker",c.moveToFirst()?new JSONObject(c.getString(0)):new JSONObject().put("activities",new JSONArray()).put("customTypes",new JSONArray()));}
        return result.put("lists",lists).put("tasks",tasks);
    }
    private static String stamp(){return java.time.Instant.now().toString();}
    private static String serverId(SQLiteDatabase db,String entity,String localId)throws Exception{
        try(Cursor c=db.rawQuery("SELECT server_id FROM sync_records WHERE entity=? AND local_id=?",new String[]{entity,localId})){if(c.moveToFirst())return c.getString(0);}
        String id=UUID.randomUUID().toString();ContentValues v=new ContentValues();v.put("entity",entity);v.put("local_id",localId);v.put("server_id",id);db.insertOrThrow("sync_records",null,v);return id;
    }
    private static Long revision(SQLiteDatabase db,String entity,String localId){try(Cursor c=db.rawQuery("SELECT revision FROM sync_records WHERE entity=? AND local_id=?",new String[]{entity,localId})){return c.moveToFirst()&&!c.isNull(0)?c.getLong(0):null;} }
    private static JSONObject listPayload(SQLiteDatabase db,JSONObject l)throws Exception{JSONObject p=new JSONObject().put("name",l.optString("name","")).put("icon",l.optString("icon","📋")).put("colour",l.optString("colour","#6D4C41")).put("builtIn",l.optBoolean("builtIn")).put("latestTaskOnTop",l.optBoolean("latestTaskOnTop"));String user=meta(db,"user_id");if(user!=null)p.put("ownerUserId",user);if(createdMillis(l.optString("createdAt"),0)>0)p.put("createdAt",l.getString("createdAt"));return p;}
    private static JSONObject taskPayload(SQLiteDatabase db,JSONObject t,int sortOrder)throws Exception{
        String localList=t.getString("list"),listServer=serverId(db,"list",localList);
        JSONObject p=new JSONObject().put("listId",listServer).put("title",t.optString("title","")).put("done",t.optBoolean("done")).put("star",rawStar(db,t.optString("id"))).put("due",t.optString("due","")).put("reminder",t.optString("reminder","No reminder")).put("tags",t.optString("tags","")).put("note",t.optString("note","")).put("who",t.optString("who","")).put("subtasks",t.optJSONArray("subtasks")==null?new JSONArray():t.getJSONArray("subtasks")).put("comments",t.optJSONArray("comments")==null?new JSONArray():t.getJSONArray("comments")).put("sortOrder",sortOrder);if(!t.isNull("ownerUserId"))p.put("ownerUserId",t.optString("ownerUserId"));
        return p;
    }
    private static void queueOne(SQLiteDatabase db,String entity,String localId,String op,JSONObject payload)throws Exception{
        String id=serverId(db,entity,localId);Long rev=revision(db,entity,localId);
        if("delete".equals(op)&&rev==null){try(Cursor c=db.rawQuery("SELECT change_id,batch_id FROM sync_outbox WHERE entity=? AND local_id=? AND status='pending' ORDER BY rowid DESC LIMIT 1",new String[]{entity,localId})){if(c.moveToFirst()&&c.isNull(1)){db.delete("sync_outbox","change_id=?",new String[]{c.getString(0)});db.delete("sync_records","entity=? AND local_id=?",new String[]{entity,localId});return;}}}
        String existing=null;try(Cursor c=db.rawQuery("SELECT change_id,batch_id FROM sync_outbox WHERE entity=? AND local_id=? AND status='pending' ORDER BY rowid DESC LIMIT 1",new String[]{entity,localId})){if(c.moveToFirst()&&c.isNull(1))existing=c.getString(0);}
        String now=stamp();
        if(existing!=null){ContentValues v=new ContentValues();v.put("operation",op);if(rev==null)v.putNull("base_revision");else v.put("base_revision",rev);v.put("payload",payload==null?null:payload.toString());v.put("client_modified_at",now);db.update("sync_outbox",v,"change_id=?",new String[]{existing});return;}
        ContentValues v=new ContentValues();v.put("change_id",UUID.randomUUID().toString());v.put("entity",entity);v.put("local_id",localId);v.put("entity_id",id);v.put("operation",op);if(rev==null)v.putNull("base_revision");else v.put("base_revision",rev);v.put("payload",payload==null?null:payload.toString());v.put("client_modified_at",now);v.put("status","pending");db.insertOrThrow("sync_outbox",null,v);
    }
    private static JSONObject readTracker(SQLiteDatabase db)throws Exception{try(Cursor c=db.rawQuery("SELECT value FROM preferences WHERE key='tracker'",null)){return c.moveToFirst()?new JSONObject(c.getString(0)):new JSONObject().put("activities",new JSONArray()).put("customTypes",new JSONArray());}}
    private static void writeTracker(SQLiteDatabase db,JSONObject tracker){ContentValues v=new ContentValues();v.put("key","tracker");v.put("value",tracker.toString());db.insertWithOnConflict("preferences",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    private static JSONObject activityPayload(JSONObject a)throws Exception{return new JSONObject(a.toString()).put("kind","repetitiveActivity");}
    private static void queueActivityDiff(SQLiteDatabase db,JSONObject before,JSONObject after)throws Exception{
        java.util.LinkedHashMap<String,JSONObject> old=map(before==null?null:before.optJSONArray("activities"),"id"),current=map(after==null?null:after.optJSONArray("activities"),"id");
        for(String id:current.keySet())if(!old.containsKey(id)||!activityPayload(old.get(id)).toString().equals(activityPayload(current.get(id)).toString()))queueOne(db,"activity",id,"upsert",activityPayload(current.get(id)));
        for(String id:old.keySet())if(!current.containsKey(id))queueOne(db,"activity",id,"delete",null);
        JSONArray oldTypes=before==null?null:before.optJSONArray("customTypes"),types=after==null?null:after.optJSONArray("customTypes");if(types==null)types=new JSONArray();
        if(!types.toString().equals(oldTypes==null?"[]":oldTypes.toString()))queueOne(db,"activity","tracker-types","upsert",new JSONObject().put("kind","trackerTypes").put("customTypes",types));
    }
    private static void applyActivity(SQLiteDatabase db,JSONObject change,String localId)throws Exception{
        JSONObject p=change.optJSONObject("payload"),tracker=readTracker(db);String server=change.getString("entityId");boolean deleted="delete".equals(change.optString("operation"));
        if(localId==null)localId=p!=null&&"trackerTypes".equals(p.optString("kind"))?"tracker-types":p==null?server:p.optString("id",server);
        if("tracker-types".equals(localId))tracker.put("customTypes",deleted?new JSONArray():p.optJSONArray("customTypes"));
        else{JSONArray current=tracker.optJSONArray("activities"),next=new JSONArray();boolean replaced=false;if(current!=null)for(int i=0;i<current.length();i++){JSONObject a=current.getJSONObject(i);if(localId.equals(a.optString("id"))){if(!deleted){next.put(new JSONObject(p.toString()).put("id",localId));replaced=true;}}else next.put(a);}if(!deleted&&!replaced)next.put(new JSONObject(p.toString()).put("id",localId));tracker.put("activities",next);}
        writeTracker(db,tracker);ContentValues v=new ContentValues();v.put("entity","activity");v.put("local_id",localId);v.put("server_id",server);v.put("revision",change.optLong("revision"));db.insertWithOnConflict("sync_records",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
    public synchronized boolean hasUnsyncedUserData()throws Exception{assertAccountSync();ensureOutbox();SQLiteDatabase db=getReadableDatabase();try(Cursor c=db.rawQuery("SELECT 1 FROM sync_outbox WHERE status IN ('pending','conflict') LIMIT 1",null)){if(c.moveToFirst())return true;}return pendingFavourites().length()>0||pendingCloudAttachments().length()>0;}
    public synchronized void clearSignedInData()throws Exception{accountEpoch++;SQLiteDatabase db=getWritableDatabase();long nextDocumentRevision=documentRevision(db)+1;db.beginTransaction();try{JSONObject settings=new JSONObject();try(Cursor c=db.rawQuery("SELECT value FROM preferences WHERE key='state'",null)){if(c.moveToFirst()){JSONObject old=new JSONObject(c.getString(0));settings.put("theme",old.optString("theme","wood"));settings.put("attachmentOrderVersion",1);}}db.delete("tasks",null,null);db.delete("lists",null,null);db.delete("sync_outbox",null,null);db.delete("sync_records",null,null);db.delete("personal_favourites",null,null);db.delete("sync_meta","key<>'device_id'",null);putMeta(db,"document_revision",Long.toString(nextDocumentRevision));db.delete("preferences",null,null);ContentValues v=new ContentValues();v.put("key","state");v.put("value",settings.toString());db.insertOrThrow("preferences",null,v);writeTracker(db,new JSONObject().put("activities",new JSONArray()).put("customTypes",new JSONArray()));db.setTransactionSuccessful();}finally{db.endTransaction();}}
    public synchronized String sharingVersion(String id){assertAccountSync();return meta(getReadableDatabase(),"sharing_dirty:"+id);}
    public synchronized void sharingSaved(String id,String version){assertAccountSync();getWritableDatabase().delete("sync_meta","key=? AND value=?",new String[]{"sharing_dirty:"+id,version});}
    public synchronized void cacheListMembers(String id,JSONObject people)throws Exception{assertAccountSync();SQLiteDatabase db=getWritableDatabase();if(meta(db,"sharing_dirty:"+id)!=null)return;JSONArray emails=new JSONArray(),members=people.optJSONArray("members"),shares=people.optJSONArray("shares");HashSet<String> unique=new HashSet<>();if(members!=null)for(int i=0;i<members.length();i++){JSONObject m=members.getJSONObject(i);String email=m.optString("email").toLowerCase(java.util.Locale.ROOT);if(!"owner".equals(m.optString("permission"))&&!email.isEmpty()&&unique.add(email))emails.put(email);}if(shares!=null)for(int i=0;i<shares.length();i++){String email=shares.getJSONObject(i).optString("email").toLowerCase(java.util.Locale.ROOT);if(!email.isEmpty()&&unique.add(email))emails.put(email);}ContentValues v=new ContentValues();v.put("invitees",emails.toString());v.put("shared",emails.length()>0?1:0);db.update("lists",v,"id=? AND joined=0",new String[]{id});bumpDocument(db);}
    private static void queueLocalDiff(SQLiteDatabase db,JSONObject old,JSONObject current)throws Exception{
        queueActivityDiff(db,old.optJSONObject("tracker"),current.optJSONObject("tracker"));
        java.util.LinkedHashMap<String,JSONObject> oldLists=map(old.optJSONArray("lists"),"id"),newLists=map(current.optJSONArray("lists"),"id");
        java.util.LinkedHashMap<String,JSONObject> oldTasks=map(old.optJSONArray("tasks"),"id"),newTasks=map(current.optJSONArray("tasks"),"id");
        for(String id:newLists.keySet()){JSONObject currentList=newLists.get(id),oldList=oldLists.get(id);String nowEmails=currentList.optJSONArray("invitees")==null?"[]":currentList.getJSONArray("invitees").toString(),oldEmails=oldList==null||oldList.optJSONArray("invitees")==null?"[]":oldList.getJSONArray("invitees").toString();if(!currentList.optBoolean("joined")&&!nowEmails.equals(oldEmails))putMeta(db,"sharing_dirty:"+id,UUID.randomUUID().toString());JSONObject p=listPayload(db,newLists.get(id)),prior=oldLists.get(id);if(prior==null||!listPayload(db,prior).toString().equals(p.toString()))queueOne(db,"list",id,"upsert",p);}
        for(String id:oldLists.keySet())if(!newLists.containsKey(id))queueOne(db,"list",id,"delete",null);
        java.util.HashMap<String,Integer> nextOrder=nextTaskOrder(old.optJSONArray("tasks"),current.optJSONArray("tasks"),newLists,oldTasks);
        for(String id:newTasks.keySet()){JSONObject p=taskPayload(db,newTasks.get(id),nextOrder.getOrDefault(id,0)),prior=oldTasks.get(id);if(prior==null||!taskPayload(db,prior,prior.optInt("sortOrder",0)).toString().equals(p.toString()))queueOne(db,"task",id,"upsert",p);}
        for(String id:oldTasks.keySet())if(!newTasks.containsKey(id)){JSONObject prior=oldTasks.get(id);if(newLists.containsKey(prior.optString("list")))queueOne(db,"task",id,"delete",null);}
    }
    private static java.util.HashMap<String,Integer> nextTaskOrder(JSONArray oldTasks,JSONArray newTasks,java.util.Map<String,JSONObject> newLists,java.util.Map<String,JSONObject> oldById)throws Exception{
        java.util.HashMap<String,Integer> order=new java.util.HashMap<>();java.util.LinkedHashMap<String,java.util.ArrayList<String>> before=groupTaskIds(oldTasks),after=groupTaskIds(newTasks);java.util.LinkedHashMap<String,JSONObject> newById=map(newTasks,"id");
        for(String listId:after.keySet()){java.util.ArrayList<String> oldIds=before.getOrDefault(listId,new java.util.ArrayList<>()),newIds=after.get(listId);boolean preserved=true;int at=0;for(String oldId:oldIds){while(at<newIds.size()&&!newIds.get(at).equals(oldId))at++;if(at==newIds.size()){preserved=false;break;}at++;}
            if(!preserved){for(int i=0;i<newIds.size();i++)order.put(newIds.get(i),i);continue;}
            int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE,newCount=0;for(String id:newIds){JSONObject prior=oldById.get(id);if(prior!=null){int value=prior.optInt("sortOrder",0);order.put(id,value);min=Math.min(min,value);max=Math.max(max,value);}else newCount++;}
            if(newCount==0)continue;JSONObject list=newLists.get(listId);boolean latest=list!=null&&list.optBoolean("latestTaskOnTop");int offset=0;for(String id:newIds)if(!oldById.containsKey(id)){int value=min==Integer.MAX_VALUE?offset:(latest?min-newCount+offset:max+1+offset);order.put(id,value);offset++;}
        }
        return order;
    }
    private static java.util.LinkedHashMap<String,java.util.ArrayList<String>> groupTaskIds(JSONArray tasks)throws Exception{java.util.LinkedHashMap<String,java.util.ArrayList<String>> grouped=new java.util.LinkedHashMap<>();if(tasks!=null)for(int i=0;i<tasks.length();i++){JSONObject t=tasks.getJSONObject(i);String list=t.getString("list");grouped.computeIfAbsent(list,key->new java.util.ArrayList<>()).add(String.valueOf(t.get("id")));}return grouped;}
    private static java.util.LinkedHashMap<String,JSONObject> map(JSONArray a,String key)throws Exception{java.util.LinkedHashMap<String,JSONObject> m=new java.util.LinkedHashMap<>();if(a!=null)for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);m.put(o.get(key).toString(),o);}return m;}

    public synchronized JSONObject deviceAndCursor()throws Exception{assertAccountSync();SQLiteDatabase db=getWritableDatabase();String device=meta(db,"device_id");if(device==null){device=UUID.randomUUID().toString();putMeta(db,"device_id",device);}return new JSONObject().put("deviceId",device).put("cursor",meta(db,"cursor"));}
    public synchronized JSONObject takeBatch()throws Exception{assertAccountSync();SQLiteDatabase db=getWritableDatabase();String batch=null;try(Cursor c=db.rawQuery("SELECT batch_id FROM sync_outbox WHERE status='pending' AND batch_id IS NOT NULL ORDER BY rowid LIMIT 1",null)){if(c.moveToFirst())batch=c.getString(0);}if(batch==null){batch=UUID.randomUUID().toString();ContentValues v=new ContentValues();v.put("batch_id",batch);db.update("sync_outbox",v,"status='pending' AND batch_id IS NULL AND change_id IN (SELECT change_id FROM sync_outbox WHERE status='pending' AND batch_id IS NULL ORDER BY rowid LIMIT 200)",null);}JSONArray changes=new JSONArray();try(Cursor c=db.rawQuery("SELECT change_id,entity,entity_id,operation,base_revision,payload,client_modified_at FROM sync_outbox WHERE status='pending' AND batch_id=? ORDER BY CASE entity WHEN 'list' THEN 0 ELSE 1 END,rowid LIMIT 200",new String[]{batch})){while(c.moveToNext()){JSONObject ch=new JSONObject().put("changeId",c.getString(0)).put("entity",c.getString(1)).put("entityId",c.getString(2)).put("operation",c.getString(3)).put("baseRevision",c.isNull(4)?JSONObject.NULL:c.getLong(4)).put("clientModifiedAt",c.getString(6));if(!c.isNull(5))ch.put("payload",new JSONObject(c.getString(5)));changes.put(ch);}}return new JSONObject().put("deviceId",meta(db,"device_id")).put("batchId",batch).put("changes",changes);}
    public synchronized void applyPushResult(JSONObject response)throws Exception{assertAccountSync();SQLiteDatabase db=getWritableDatabase();JSONArray accepted=response.optJSONArray("accepted");if(accepted!=null)for(int i=0;i<accepted.length();i++){String changeId=accepted.getString(i);try(Cursor c=db.rawQuery("SELECT entity,local_id,base_revision,operation FROM sync_outbox WHERE change_id=?",new String[]{changeId})){if(!c.moveToFirst())continue;String entity=c.getString(0),localId=c.getString(1),op=c.getString(3);long revision=c.isNull(2)?1:c.getLong(2)+1;ContentValues m=new ContentValues();m.put("revision",revision);db.update("sync_records",m,"entity=? AND local_id=?",new String[]{entity,localId});ContentValues next=new ContentValues();next.put("base_revision",revision);db.update("sync_outbox",next,"entity=? AND local_id=? AND status='pending' AND change_id<>?",new String[]{entity,localId,changeId});}db.delete("sync_outbox","change_id=?",new String[]{changeId});}
        JSONArray conflicts=response.optJSONArray("conflicts");if(conflicts!=null)for(int i=0;i<conflicts.length();i++){JSONObject c=conflicts.getJSONObject(i);ContentValues v=new ContentValues();v.put("status","conflict");v.put("conflict",c.toString());db.update("sync_outbox",v,"change_id=?",new String[]{c.optString("changeId")});}}
    public synchronized int applyPull(JSONObject result)throws Exception{assertAccountSync();SQLiteDatabase db=getWritableDatabase();JSONArray changes=result.getJSONArray("changes");int received=changes.length();db.beginTransaction();try{for(int i=0;i<received;i++)applyRemote(db,changes.getJSONObject(i));putMeta(db,"cursor",result.getString("nextCursor"));if(received>0)bumpDocument(db);db.setTransactionSuccessful();}finally{db.endTransaction();}return received;}
    private static void applyRemote(SQLiteDatabase db,JSONObject change)throws Exception{
        String entity=change.getString("entity"),serverId=change.getString("entityId"),op=change.getString("operation");String localId=null;try(Cursor c=db.rawQuery("SELECT local_id FROM sync_records WHERE entity=? AND server_id=?",new String[]{entity,serverId})){if(c.moveToFirst())localId=c.getString(0);}
        boolean revokedSharedCopy=false;
        if(localId!=null&&"list".equals(entity)){try(Cursor c=db.rawQuery("SELECT joined FROM lists WHERE id=?",new String[]{localId})){revokedSharedCopy=c.moveToFirst()&&c.getInt(0)!=0;}}
        if(localId!=null&&!revokedSharedCopy){try(Cursor c=db.rawQuery("SELECT change_id FROM sync_outbox WHERE entity=? AND local_id=? AND status='pending' LIMIT 1",new String[]{entity,localId})){if(c.moveToFirst()){ContentValues conflict=new ContentValues();conflict.put("status","conflict");conflict.put("conflict",change.toString());db.update("sync_outbox",conflict,"entity=? AND local_id=? AND status='pending'",new String[]{entity,localId});return;}}}
        if("activity".equals(entity)){applyActivity(db,change,localId);return;}
        if("delete".equals(op)){if(localId!=null){if("list".equals(entity)){if(revokedSharedCopy){db.delete("sync_outbox","local_id IN (SELECT CAST(id AS TEXT) FROM tasks WHERE list_id=?) OR (entity='list' AND local_id=?)",new String[]{localId,localId});db.delete("sync_records","entity='task' AND local_id IN (SELECT CAST(id AS TEXT) FROM tasks WHERE list_id=?)",new String[]{localId});db.delete("sync_outbox","entity='task' AND local_id IN (SELECT CAST(id AS TEXT) FROM tasks WHERE list_id=?)",new String[]{localId});db.delete("sync_records","entity='list' AND local_id=?",new String[]{localId});}db.delete("tasks","list_id=?",new String[]{localId});db.delete("lists","id=?",new String[]{localId});}else if("task".equals(entity))db.delete("tasks","id=?",new String[]{localId});ContentValues m=new ContentValues();m.put("revision",change.optLong("revision"));db.update("sync_records",m,"entity=? AND local_id=?",new String[]{entity,localId});}return;}
        JSONObject p=change.optJSONObject("payload");if(p==null)return;
        if("list".equals(entity)){
            if(localId==null){localId="remote"+serverId.replace("-","");ContentValues m=new ContentValues();m.put("entity",entity);m.put("local_id",localId);m.put("server_id",serverId);m.put("revision",change.optLong("revision"));db.insertOrThrow("sync_records",null,m);}
            String currentUser=meta(db,"user_id");int joined=currentUser!=null&&currentUser.equals(p.optString("ownerUserId"))?0:1;
            String invitees="[]";try(Cursor c=db.rawQuery("SELECT invitees FROM lists WHERE id=?",new String[]{localId})){if(c.moveToFirst())invitees=c.getString(0);}
            long created=createdMillis(change.optString("serverModifiedAt"),System.currentTimeMillis());try(Cursor c=db.rawQuery("SELECT created_at FROM lists WHERE id=?",new String[]{localId})){if(c.moveToFirst()&&c.getLong(0)>946684800000L)created=c.getLong(0);}created=createdMillis(p.optString("createdAt"),created);String sharedUser=joined==1?meta(db,"user_id"):null;ContentValues v=new ContentValues();v.put("created_at",created);v.put("id",localId);v.put("name",p.optString("name","Shared list"));v.put("icon",p.optString("icon","📋"));v.put("shared",joined==1||new JSONArray(invitees).length()>0?1:0);v.put("colour",p.optString("colour",p.optString("color","#6D4C41")));v.put("built_in",p.optBoolean("builtIn")?1:0);v.put("joined",joined);v.put("invitees",invitees);v.put("latest_on_top",p.optBoolean("latestTaskOnTop")?1:0);if(sharedUser==null)v.putNull("shared_user_id");else v.put("shared_user_id",sharedUser);if(db.update("lists",v,"id=?",new String[]{localId})==0)db.insertOrThrow("lists",null,v);ContentValues m=new ContentValues();m.put("revision",change.optLong("revision"));db.update("sync_records",m,"entity=? AND local_id=?",new String[]{entity,localId});
        }else if("task".equals(entity)){
            String remoteList=p.optString("listId","");String localList=null;try(Cursor c=db.rawQuery("SELECT local_id FROM sync_records WHERE entity='list' AND server_id=?",new String[]{remoteList})){if(c.moveToFirst())localList=c.getString(0);}if(localList==null)throw new IllegalStateException("The shared list snapshot is incomplete");
            if(localId==null){localId=allocateRemoteTaskId(db);ContentValues m=new ContentValues();m.put("entity",entity);m.put("local_id",localId);m.put("server_id",serverId);m.put("revision",change.optLong("revision"));db.insertOrThrow("sync_records",null,m);}
            String attachments="[]";try(Cursor c=db.rawQuery("SELECT attachments FROM tasks WHERE id=?",new String[]{localId})){if(c.moveToFirst())attachments=c.getString(0);}
            ContentValues v=new ContentValues();v.put("id",Long.parseLong(localId));v.put("list_id",localList);v.put("title",p.optString("title",""));v.put("done",p.optBoolean("done")?1:0);v.put("starred",p.optBoolean("star")?1:0);v.put("due",p.optString("due",""));v.put("reminder",p.optString("reminder","No reminder"));v.put("tags",p.optString("tags",""));v.put("note",p.optString("note",""));v.put("assignee",p.optString("who",""));v.put("subtasks",p.optJSONArray("subtasks")==null?"[]":p.getJSONArray("subtasks").toString());v.put("attachments",attachments);v.put("sort_order",p.optInt("sortOrder",0));v.put("comments",p.optJSONArray("comments")==null?"[]":p.getJSONArray("comments").toString());if(p.has("ownerUserId")&&!p.isNull("ownerUserId"))v.put("owner_user_id",p.optString("ownerUserId"));db.insertWithOnConflict("tasks",null,v,SQLiteDatabase.CONFLICT_REPLACE);ContentValues m=new ContentValues();m.put("revision",change.optLong("revision"));db.update("sync_records",m,"entity=? AND local_id=?",new String[]{entity,localId});
        }
    }
    private static String allocateRemoteTaskId(SQLiteDatabase db){
        long candidate=1;try(Cursor c=db.rawQuery("SELECT COALESCE(MAX(id),0)+1 FROM tasks",null)){if(c.moveToFirst())candidate=c.getLong(0);}
        while(true){boolean reserved=false;try(Cursor c=db.rawQuery("SELECT 1 FROM sync_records WHERE entity='task' AND local_id=? LIMIT 1",new String[]{Long.toString(candidate)})){reserved=c.moveToFirst();}if(!reserved)return Long.toString(candidate);candidate++;}
    }
    private static String meta(SQLiteDatabase db,String key){try(Cursor c=db.rawQuery("SELECT value FROM sync_meta WHERE key=?",new String[]{key})){return c.moveToFirst()?c.getString(0):null;}}
    private static void putMeta(SQLiteDatabase db,String key,String value){if(value==null)return;ContentValues v=new ContentValues();v.put("key",key);v.put("value",value);db.insertWithOnConflict("sync_meta",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    public synchronized void storeUser(String userId)throws Exception{assertAccountSync();SQLiteDatabase db=getWritableDatabase();String previous=meta(db,"user_id");if(previous!=null&&!previous.equals(userId))db.delete("sync_meta","key='cursor'",null);putMeta(db,"user_id",userId);if(!userId.equals(meta(db,"favourite_user")))bumpDocument(db);putMeta(db,"favourite_user",userId);db.execSQL("INSERT OR IGNORE INTO personal_favourites(user_id,local_id,starred,dirty) SELECT ?,local_id,starred,1 FROM personal_favourites WHERE user_id='local'",new Object[]{userId});try(Cursor c=db.rawQuery("SELECT change_id,payload FROM sync_outbox WHERE entity='list' AND operation='upsert' AND status='pending' AND batch_id IS NULL",null)){while(c.moveToNext()){JSONObject payload=new JSONObject(c.getString(1)).put("ownerUserId",userId);ContentValues v=new ContentValues();v.put("payload",payload.toString());db.update("sync_outbox",v,"change_id=?",new String[]{c.getString(0)});}}}
    /** Hides a stale local mapping whose server list is not accessible to this account. Local data is retained. */
    public synchronized void hideUnavailableList(String localId){assertAccountSync();SQLiteDatabase db=getWritableDatabase();ContentValues v=new ContentValues();v.put("joined",1);v.put("shared",0);v.putNull("shared_user_id");db.update("lists",v,"id=?",new String[]{localId});bumpDocument(db);}
    public synchronized void repairListCreationTimes(){assertAccountSync();
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{
            long fallback=System.currentTimeMillis();try(Cursor c=db.rawQuery("SELECT MIN(created_at) FROM lists WHERE created_at>946684800000",null)){if(c.moveToFirst()&&!c.isNull(0))fallback=c.getLong(0);}
            java.util.ArrayList<String> ids=new java.util.ArrayList<>();try(Cursor c=db.rawQuery("SELECT id FROM lists WHERE created_at<=946684800000 ORDER BY created_at,rowid",null)){while(c.moveToNext())ids.add(c.getString(0));}
            long start=Math.max(946684800001L,fallback-ids.size());for(int i=0;i<ids.size();i++){ContentValues v=new ContentValues();v.put("created_at",start+i);db.update("lists",v,"id=?",new String[]{ids.get(i)});}
            if(!ids.isEmpty())bumpDocument(db);db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    public synchronized void clearCursor(){assertAccountSync();SQLiteDatabase db=getWritableDatabase();db.delete("sync_meta","key='cursor'",null);}
    public synchronized String serverIdFor(String entity,String localId)throws Exception{assertAccountSync();try(Cursor c=getReadableDatabase().rawQuery("SELECT server_id FROM sync_records WHERE entity=? AND local_id=?",new String[]{entity,localId})){if(c.moveToFirst())return c.getString(0);}throw new IllegalStateException("Record is not available on this device.");}
    public synchronized JSONArray pendingCloudAttachments()throws Exception{assertAccountSync();
        JSONArray pending=new JSONArray();SQLiteDatabase db=getReadableDatabase();
        try(Cursor c=db.rawQuery("SELECT t.id,t.attachments,s.server_id FROM tasks t JOIN sync_records s ON s.entity='task' AND s.local_id=CAST(t.id AS TEXT) ORDER BY t.sort_order",null)){
            while(c.moveToNext()){JSONArray files=new JSONArray(c.getString(1));for(int i=0;i<files.length();i++){JSONObject f=files.optJSONObject(i);if(f==null||f.has("remoteAttachmentId")||f.optBoolean("browserOnly"))continue;String local=f.optString("id");if(!local.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))continue;pending.put(new JSONObject().put("taskLocalId",c.getString(0)).put("taskServerId",c.getString(2)).put("file",f));}}
        }return pending;
    }
    public synchronized void markCloudUploaded(String taskLocalId,String fileId,JSONObject remote)throws Exception{assertAccountSync();
        SQLiteDatabase db=getWritableDatabase();JSONObject a=remote.optJSONObject("attachment");if(a==null)return;try(Cursor c=db.rawQuery("SELECT attachments FROM tasks WHERE id=?",new String[]{taskLocalId})){if(!c.moveToFirst())return;JSONArray files=new JSONArray(c.getString(0));for(int i=0;i<files.length();i++){JSONObject f=files.optJSONObject(i);if(f!=null&&fileId.equals(f.optString("id"))){f.put("remoteAttachmentId",a.optString("attachmentId"));f.put("cloud",true).put("cloudCached",true).put("uploadState","complete").put("provider",a.optString("provider")).put("uploaderId",a.optString("uploaderId"));break;}}ContentValues v=new ContentValues();v.put("attachments",files.toString());db.update("tasks",v,"id=?",new String[]{taskLocalId});bumpDocument(db);}
    }
    public synchronized JSONArray cloudTasks()throws Exception{assertAccountSync();
        JSONArray out=new JSONArray();try(Cursor c=getReadableDatabase().rawQuery("SELECT t.id,s.server_id FROM tasks t JOIN sync_records s ON s.entity='task' AND s.local_id=CAST(t.id AS TEXT)",null)){while(c.moveToNext())out.put(new JSONObject().put("localId",c.getString(0)).put("serverId",c.getString(1)));}return out;
    }
    public synchronized void mergeCloudAttachments(String taskLocalId,JSONArray remote)throws Exception{assertAccountSync();
        SQLiteDatabase db=getWritableDatabase();try(Cursor c=db.rawQuery("SELECT attachments FROM tasks WHERE id=?",new String[]{taskLocalId})){if(!c.moveToFirst())return;JSONArray existing=new JSONArray(c.getString(0)),current=new JSONArray();HashSet<String> active=new HashSet<>();for(int i=0;i<remote.length();i++)active.add(remote.getJSONObject(i).optString("attachmentId"));HashSet<String> seen=new HashSet<>();for(int i=0;i<existing.length();i++){Object item=existing.opt(i);if(!(item instanceof JSONObject)){current.put(item);continue;}JSONObject f=(JSONObject)item;String remoteId=f.optString("remoteAttachmentId","");if(remoteId.isEmpty()||active.contains(remoteId)){current.put(f);if(!remoteId.isEmpty())seen.add(remoteId);}}for(int i=0;i<remote.length();i++){JSONObject a=remote.getJSONObject(i);String id=a.optString("attachmentId");if(id.isEmpty()||seen.contains(id))continue;current.put(new JSONObject().put("id",id).put("remoteAttachmentId",id).put("name",a.optString("fileName","Attachment")).put("size",a.optLong("sizeBytes",0)).put("mime",a.optString("mimeType","application/octet-stream")).put("cloud",true).put("cached",false).put("accessible",a.optBoolean("accessible")).put("uploaderId",a.optString("uploaderId")).put("provider",a.optString("provider")));seen.add(id);}ContentValues v=new ContentValues();v.put("attachments",current.toString());db.update("tasks",v,"id=?",new String[]{taskLocalId});bumpDocument(db);}
    }
    public synchronized void cacheCloudAttachment(String taskLocalId,String remoteId,JSONObject downloaded)throws Exception{assertAccountSync();SQLiteDatabase db=getWritableDatabase();try(Cursor c=db.rawQuery("SELECT attachments FROM tasks WHERE id=?",new String[]{taskLocalId})){if(!c.moveToFirst())return;JSONArray files=new JSONArray(c.getString(0));for(int i=0;i<files.length();i++){JSONObject f=files.optJSONObject(i);if(f!=null&&remoteId.equals(f.optString("remoteAttachmentId"))){f.put("id",downloaded.optString("id")).put("cloudCached",true).put("cached",true);break;}}ContentValues v=new ContentValues();v.put("attachments",files.toString());db.update("tasks",v,"id=?",new String[]{taskLocalId});bumpDocument(db);}}
    public synchronized void ensureOutbox()throws Exception{assertAccountSync();
        JSONObject now=new JSONObject(load());SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{
            JSONObject tracker=now.optJSONObject("tracker");JSONArray activities=tracker==null?null:tracker.optJSONArray("activities");if(activities!=null)for(int i=0;i<activities.length();i++){JSONObject activity=activities.getJSONObject(i);String id=activity.getString("id");if(revision(db,"activity",id)==null&&!hasOutbox(db,"activity",id,"pending")&&!hasOutbox(db,"activity",id,"conflict"))queueOne(db,"activity",id,"upsert",activityPayload(activity));}
            JSONArray types=tracker==null?null:tracker.optJSONArray("customTypes");if(types!=null&&types.length()>0&&revision(db,"activity","tracker-types")==null&&!hasOutbox(db,"activity","tracker-types","pending")&&!hasOutbox(db,"activity","tracker-types","conflict"))queueOne(db,"activity","tracker-types","upsert",new JSONObject().put("kind","trackerTypes").put("customTypes",types));
            JSONArray lists=now.optJSONArray("lists"),tasks=now.optJSONArray("tasks");
            if(lists!=null)for(int i=0;i<lists.length();i++){
                JSONObject l=lists.getJSONObject(i);String id=l.getString("id");serverId(db,"list",id);
                if(revision(db,"list",id)==null&&!hasOutbox(db,"list",id,"pending")&&!hasOutbox(db,"list",id,"conflict"))queueOne(db,"list",id,"upsert",listPayload(db,l));
            }
            if(tasks!=null)for(int i=0;i<tasks.length();i++){
                JSONObject t=tasks.getJSONObject(i);String id=String.valueOf(t.get("id")),listId=t.getString("list");serverId(db,"task",id);
                if(revision(db,"task",id)!=null||hasOutbox(db,"task",id,"pending"))continue;
                boolean joined=false;try(Cursor c=db.rawQuery("SELECT joined FROM lists WHERE id=?",new String[]{listId})){joined=c.moveToFirst()&&c.getInt(0)!=0;}if(joined){String user=meta(db,"user_id");if(user==null||!user.equals(t.optString("ownerUserId","")))continue;}
                if(!hasOutbox(db,"task",id,"conflict")){queueOne(db,"task",id,"upsert",taskPayload(db,t,t.optInt("sortOrder",0)));continue;}
                Long listRevision=revision(db,"list",listId);if(listRevision==null)continue;
                int priorRetries=0;try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM sync_outbox WHERE entity='task' AND local_id=? AND status='superseded'",new String[]{id})){if(c.moveToFirst())priorRetries=c.getInt(0);}
                if(priorRetries>0)continue;
                try(Cursor c=db.rawQuery("SELECT change_id,conflict FROM sync_outbox WHERE entity='task' AND local_id=? AND status='conflict' ORDER BY rowid DESC LIMIT 1",new String[]{id})){
                    if(c.moveToFirst()){
                        JSONObject reason=new JSONObject(c.getString(1));if(!"permission_denied".equals(reason.optString("reason")))continue;
                        JSONObject marked=reason.put("dependencyRetryQueued",true);ContentValues old=new ContentValues();old.put("status","superseded");old.put("conflict",marked.toString());db.update("sync_outbox",old,"change_id=?",new String[]{c.getString(0)});
                        queueOne(db,"task",id,"upsert",taskPayload(db,t,t.optInt("sortOrder",0)));
                    }
                }
            }
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    private static boolean hasOutbox(SQLiteDatabase db,String entity,String localId,String status){try(Cursor c=db.rawQuery("SELECT 1 FROM sync_outbox WHERE entity=? AND local_id=? AND status=? LIMIT 1",new String[]{entity,localId,status})){return c.moveToFirst();}}
}
