package app.adc.taskvilla;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;

/** Manual local-first synchronization. Network work runs off the UI thread. */
public final class SyncEngine {
    private SyncEngine() {}
    public static synchronized JSONObject synchronize(Context context,LocalStore store,TaskVillaApi api)throws Exception{
        store.beginAccountSync();try{
        if(api.session()==null)throw new IllegalStateException("Sign in with email to sync and share lists.");
        store.storeUser(api.session().optString("userId"));
        // A verified account immediately claims all shares addressed to its email.
        withRetry(()->api.sharing(new JSONObject().put("action","list")));
        store.ensureOutbox();JSONObject device=store.deviceAndCursor();int pushed=0,received=0,pages=0,conflicts=0;
        while(true){
            JSONObject batch=store.takeBatch();JSONArray changes=batch.getJSONArray("changes");if(changes.length()==0)break;
            JSONObject result=withRetry(()->api.push(batch));store.applyPushResult(result);store.ensureOutbox();pushed+=result.optJSONArray("accepted")==null?0:result.getJSONArray("accepted").length();conflicts+=result.optJSONArray("conflicts")==null?0:result.getJSONArray("conflicts").length();
        }
        // Sharing changes are durable in each owner's local invitees array; no emails or accept step.
        JSONObject local=new JSONObject(store.load());JSONArray localLists=local.optJSONArray("lists");
        if(localLists!=null)for(int i=0;i<localLists.length();i++){
            JSONObject list=localLists.getJSONObject(i);if(list.optBoolean("joined"))continue;String sharingVersion=store.sharingVersion(list.getString("id"));if(sharingVersion==null)continue;
            String listServer=store.serverIdFor("list",list.getString("id"));JSONArray wanted=list.optJSONArray("invitees");java.util.HashSet<String> desired=new java.util.HashSet<>();if(wanted!=null)for(int j=0;j<wanted.length();j++)desired.add(wanted.getString(j).toLowerCase(java.util.Locale.ROOT));
              JSONObject members;
              try{members=withRetry(()->api.sharing(new JSONObject().put("action","list").put("listId",listServer)));}
              catch(TaskVillaApi.ApiException e){if(e.status==403||e.status==404){store.hideUnavailableList(list.getString("id"));continue;}throw e;}
              JSONArray invitations=members.optJSONArray("shares"),accepted=members.optJSONArray("members");java.util.HashSet<String> current=new java.util.HashSet<>();
            if(accepted!=null)for(int j=0;j<accepted.length();j++){JSONObject member=accepted.getJSONObject(j);String email=member.optString("email","").toLowerCase(java.util.Locale.ROOT);if(email.isEmpty()||"owner".equals(member.optString("permission")))continue;current.add(email);if(!desired.contains(email))withRetry(()->api.sharing(new JSONObject().put("action","revoke").put("listId",listServer).put("memberUserId",member.getString("userId"))));}
            if(invitations!=null)for(int j=0;j<invitations.length();j++){JSONObject invitation=invitations.getJSONObject(j);String email=invitation.optString("email","").toLowerCase(java.util.Locale.ROOT);if(email.isEmpty())continue;current.add(email);if(!desired.contains(email))withRetry(()->api.sharing(new JSONObject().put("action","revoke").put("listId",listServer).put("inviteToken",invitation.getString("shareId"))));}
            for(String email:desired)if(!current.contains(email))withRetry(()->api.sharing(new JSONObject().put("action","share").put("listId",listServer).put("email",email).put("permission","editor")));
            store.sharingSaved(list.getString("id"),sharingVersion);
        }
        if(store.needsListCreationBackfill())store.clearCursor();
        boolean reset=false;
        for(int i=0;;i++){
            JSONObject current=store.deviceAndCursor();JSONObject request=new JSONObject().put("deviceId",current.getString("deviceId")).put("cursor",current.isNull("cursor")?JSONObject.NULL:current.getString("cursor")).put("limit",500);
            JSONObject page;
            try{page=withRetry(()->api.pull(request));}
            catch(TaskVillaApi.ApiException e){if(e.status==410&&!reset){store.clearCursor();reset=true;i--;continue;}throw e;}
            received+=store.applyPull(page);pages++;if(!page.optBoolean("hasMore"))break;
        }
        JSONArray refreshedLists=new JSONObject(store.load()).optJSONArray("lists");if(refreshedLists!=null)for(int i=0;i<refreshedLists.length();i++){JSONObject l=refreshedLists.getJSONObject(i);if(l.optBoolean("joined"))continue;String localId=l.getString("id"),serverId=store.serverIdFor("list",localId);try{JSONObject people=withRetry(()->api.sharing(new JSONObject().put("action","list").put("listId",serverId)));store.cacheListMembers(localId,people);}catch(TaskVillaApi.ApiException e){if(e.status!=403&&e.status!=404)throw e;}}
        try{JSONArray sent=store.pendingFavourites();JSONObject personal=api.favourites(new JSONObject().put("changes",sent));store.applyFavourites(api.session().optString("userId"),sent,personal);}catch(Exception ignored){/* Personal changes remain pending for the next explicit sync. */}
        // Attachment operations are best-effort and never turn a successful local/task sync into a failure.
        int cloudUploaded=0,cloudReceived=0;String cloudWarning="";
        try{
            JSONObject status=api.cloudOperation("connection-status",new JSONObject());boolean hasDefault=false;JSONArray providers=status.optJSONArray("providers");
            if(providers!=null)for(int i=0;i<providers.length();i++){JSONObject p=providers.optJSONObject(i);String provider=p==null?"":p.optString("provider");if(("google_drive".equals(provider)||"dropbox".equals(provider))&&p.optBoolean("configured")&&p.optJSONObject("connection")!=null&&"connected".equals(p.getJSONObject("connection").optString("status")))hasDefault=true;}
            if(hasDefault){JSONArray pending=store.pendingCloudAttachments();for(int i=0;i<pending.length();i++){JSONObject item=pending.getJSONObject(i),f=item.getJSONObject("file");try{File content=AttachmentStore.content(context,f.getString("id"));JSONObject uploaded=api.uploadCloudAttachment(item.getString("taskServerId"),f.getString("id"),content,f);store.markCloudUploaded(item.getString("taskLocalId"),f.getString("id"),uploaded);cloudUploaded++;}catch(Exception e){cloudWarning="Some attachments are still waiting to upload.";}}}
            JSONArray cloudTasks=store.cloudTasks();for(int start=0;start<cloudTasks.length();start+=100){JSONArray batchIds=new JSONArray();int end=Math.min(start+100,cloudTasks.length());for(int i=start;i<end;i++)batchIds.put(cloudTasks.getJSONObject(i).getString("serverId"));try{JSONObject listed=api.cloudOperation("list",new JSONObject().put("taskIds",batchIds));JSONArray groups=listed.optJSONArray("attachmentsByTask");java.util.HashMap<String,JSONArray> byTask=new java.util.HashMap<>();if(groups!=null)for(int i=0;i<groups.length();i++){JSONObject group=groups.getJSONObject(i);byTask.put(group.getString("taskId"),group.optJSONArray("attachments")==null?new JSONArray():group.getJSONArray("attachments"));}for(int i=start;i<end;i++){JSONObject item=cloudTasks.getJSONObject(i);JSONArray refs=byTask.getOrDefault(item.getString("serverId"),new JSONArray());store.mergeCloudAttachments(item.getString("localId"),refs);cloudReceived+=refs.length();}}catch(TaskVillaApi.ApiException e){if(e.status!=403&&e.status!=404)cloudWarning="Some cloud attachments could not be refreshed.";}catch(Exception e){cloudWarning="Some cloud attachments could not be refreshed.";}}
            try{api.cloudOperation("cleanup",new JSONObject());}catch(Exception ignored){}
        }catch(Exception e){cloudWarning="Cloud attachments could not be refreshed; your local files are safe.";}
        JSONObject summary=new JSONObject().put("ok",true).put("pushed",pushed).put("received",received).put("pages",pages).put("conflicts",conflicts).put("cloudUploaded",cloudUploaded).put("cloudReceived",cloudReceived).put("cloudWarning",cloudWarning);
        return summary;
        }finally{store.endAccountSync();}
    }
    public static JSONObject sharing(Context context,LocalStore store,TaskVillaApi api,JSONObject request)throws Exception{
        if(api.session()==null)throw new IllegalStateException("Sign in with email to manage shared lists.");
        JSONObject result=api.sharing(request);
        if("shared".equals(result.optString("status"))||"revoked".equals(result.optString("status")))synchronize(context,store,api);
        return result;
    }
    private interface Request { JSONObject run()throws Exception; }
    private static JSONObject withRetry(Request request)throws Exception{
        Exception last=null;
        for(int attempt=0;attempt<3;attempt++){
            try{return request.run();}catch(Exception e){last=e;boolean retry=e instanceof java.io.IOException||e instanceof java.net.SocketTimeoutException;
                if(e instanceof TaskVillaApi.ApiException){int s=((TaskVillaApi.ApiException)e).status;retry=s==408||s==429||s==500||s==502||s==503||s==504;}
                if(!retry||attempt==2)throw e;long wait=Math.min(1000L<<attempt,3000L);Thread.sleep(wait);}
        }throw last;
    }
}
