package app.adc.taskvilla;

import android.content.Context;
import android.util.Xml;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Configuration-driven HTTP adapter. Supabase wire mappings stay outside the provider-neutral contract. */
public final class TaskVillaApi {
    private final Context context;
    private final SecureSessionStore sessions;
    private String baseUrl, apiKey, schema, otpRequest, otpVerify, tokenRefresh, syncPush, syncPull, sharing, favourites;

    public TaskVillaApi(Context context, SecureSessionStore sessions) throws Exception {
        this.context=context.getApplicationContext();this.sessions=sessions;readConfig();
    }
    private void readConfig() throws Exception {
        XmlPullParser p=Xml.newPullParser();p.setInput(context.getAssets().open("api-config.xml"),"UTF-8");
        for(int e=p.getEventType();e!=XmlPullParser.END_DOCUMENT;e=p.next())if(e==XmlPullParser.START_TAG){
            String tag=p.getName();
            if("backend".equals(tag)){baseUrl=req(p,"baseUrl");apiKey=req(p,"publishableKey");schema=req(p,"schema");}
            else if("operation".equals(tag)){String n=req(p,"name"),path=req(p,"path");if("otpRequest".equals(n))otpRequest=path;else if("otpVerify".equals(n))otpVerify=path;else if("tokenRefresh".equals(n))tokenRefresh=path;else if("syncPush".equals(n))syncPush=path;else if("syncPull".equals(n))syncPull=path;else if("sharing".equals(n))sharing=path;else if("favourites".equals(n))favourites=path;}
        }
    }
    private static String req(XmlPullParser p,String a)throws Exception{String v=p.getAttributeValue(null,a);if(v==null)throw new IllegalStateException("Missing API configuration: "+a);return v;}
    public JSONObject requestOtp(String email,boolean createUser)throws Exception{
        JSONObject body=new JSONObject().put("email",email).put("create_user",createUser);
        JSONObject raw=send("POST",otpRequest,body,false,false);
        return new JSONObject().put("requestId",java.util.UUID.randomUUID().toString()).put("delivery","email");
    }
    public JSONObject verifyOtp(String email,String code)throws Exception{
        JSONObject raw=send("POST",otpVerify,new JSONObject().put("type","email").put("email",email).put("token",code),false,false);
        JSONObject user=raw.optJSONObject("user");JSONObject normalized=sessionResult(raw,user==null?"":user.optString("id"),email);sessions.put(normalized.toString());return normalized;
    }
    public synchronized JSONObject refreshSession()throws Exception{
        int generation=sessions.generation();
        JSONObject saved=readSession();if(saved==null||saved.optString("refreshToken").isEmpty())throw new IllegalStateException("Sign in again to sync.");
        JSONObject raw=send("POST",tokenRefresh,new JSONObject().put("refresh_token",saved.getString("refreshToken")),false,false);
        JSONObject user=raw.optJSONObject("user");JSONObject normalized=sessionResult(raw,user==null?saved.optString("userId"):user.optString("id"),saved.optString("email"));if(!sessions.putIfGeneration(normalized.toString(),generation))throw new IllegalStateException("Signed out.");return normalized;
    }
    public JSONObject session()throws Exception{return readSession();}
    private JSONObject readSession()throws Exception{String s=sessions.get();return s==null?null:new JSONObject(s);}
    public JSONObject rpc(String path,JSONObject body)throws Exception{return send("POST",path,body,true,true);}
    public JSONObject push(JSONObject body)throws Exception{return rpc(syncPush,body);}
    public JSONObject pull(JSONObject body)throws Exception{return rpc(syncPull,body);}
    public JSONObject favourites(JSONObject body)throws Exception{return rpc(favourites,body);}
    public JSONObject sharing(JSONObject body)throws Exception{return rpc(sharing,body);}
    public JSONObject cloudOperation(String operation,JSONObject body)throws Exception{
        JSONObject s=readSession();if(s==null)throw new IllegalStateException("Sign in with email to use cloud storage.");
        for(int attempt=0;attempt<2;attempt++)try{return cloudRequest(operation,s.optString("accessToken"),body,null,null,null,null);}
        catch(ApiException e){if(e.status!=401||attempt>0)throw e;refreshSession();s=readSession();if(s==null)throw new IllegalStateException("Sign in to use cloud storage.");}
        throw new IllegalStateException("Cloud storage request failed.");
    }
    public JSONObject uploadCloudAttachment(String taskId,String localId,File file,JSONObject metadata)throws Exception{
        JSONObject s=readSession();if(s==null)throw new IllegalStateException("Sign in to upload attachments.");
        for(int attempt=0;attempt<2;attempt++)try{return cloudRequest("upload",s.optString("accessToken"),null,taskId,localId,metadata,file);}
        catch(ApiException e){if(e.status!=401||attempt>0)throw e;refreshSession();s=readSession();}
        throw new IllegalStateException("Upload failed.");
    }
    private JSONObject cloudRequest(String operation,String accessToken,JSONObject body,String taskId,String localId,JSONObject metadata,File file)throws Exception{
        HttpURLConnection c=null;try{
            URL url=new URL(baseUrl+"/functions/v1/taskvilla-attachments?op="+java.net.URLEncoder.encode(operation,"UTF-8"));c=(HttpURLConnection)url.openConnection();c.setRequestMethod("POST");c.setConnectTimeout(10000);c.setReadTimeout(file==null?15000:60000);c.setDoInput(true);c.setDoOutput(true);
            c.setRequestProperty("apikey",apiKey);c.setRequestProperty("Authorization","Bearer "+accessToken);c.setRequestProperty("Accept","application/json");
            if(file!=null){c.setRequestProperty("Content-Type","application/octet-stream");c.setRequestProperty("X-Task-Id",taskId);c.setRequestProperty("X-Client-Attachment-Id",localId);c.setRequestProperty("X-File-Name",android.net.Uri.encode(metadata.optString("name","Attachment")));c.setRequestProperty("X-File-Mime",metadata.optString("mime","application/octet-stream"));c.setRequestProperty("X-File-Size",Long.toString(file.length()));c.setFixedLengthStreamingMode(file.length());try(InputStream in=new java.io.FileInputStream(file);OutputStream out=c.getOutputStream()){byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}}
            else {c.setRequestProperty("Content-Type","application/json; charset=UTF-8");if(taskId!=null)c.setRequestProperty("X-Task-Id",taskId);byte[] bytes=(body==null?new JSONObject():body).toString().getBytes(StandardCharsets.UTF_8);try(OutputStream out=c.getOutputStream()){out.write(bytes);}}
            int status=c.getResponseCode();InputStream in=status>=400?c.getErrorStream():c.getInputStream();String response=in==null?"":read(in);if(status<200||status>=300){JSONObject parsed;try{parsed=new JSONObject(response);}catch(Exception ignored){parsed=new JSONObject().put("error",new JSONObject().put("code",String.valueOf(status)).put("message","Cloud storage request failed."));}throw new ApiException(status,parsed,retryAfter(c.getHeaderField("Retry-After")));}return response.trim().isEmpty()?new JSONObject():new JSONObject(response);
        }finally{if(c!=null)c.disconnect();}
    }
    public JSONObject downloadCloudAttachment(String attachmentId,String localId,String name,String mime,long size)throws Exception{
        JSONObject s=readSession();if(s==null)throw new IllegalStateException("Sign in to download attachments.");
        for(int attempt=0;attempt<2;attempt++){HttpURLConnection c=null;try{URL url=new URL(baseUrl+"/functions/v1/taskvilla-attachments?op=download&attachmentId="+java.net.URLEncoder.encode(attachmentId,"UTF-8"));c=(HttpURLConnection)url.openConnection();c.setConnectTimeout(15000);c.setReadTimeout(60000);c.setRequestProperty("apikey",apiKey);c.setRequestProperty("Authorization","Bearer "+s.optString("accessToken"));int status=c.getResponseCode();if(status==401&&attempt==0){refreshSession();s=readSession();continue;}if(status<200||status>=300){String response=read(c.getErrorStream());JSONObject parsed;try{parsed=new JSONObject(response);}catch(Exception ignored){parsed=new JSONObject().put("error",new JSONObject().put("code",String.valueOf(status)).put("message","Download failed."));}throw new ApiException(status,parsed,retryAfter(c.getHeaderField("Retry-After")));}if(size>AttachmentStore.MAX_BYTES)throw new java.io.IOException("Each file must be 2 MB or smaller.");try(InputStream in=c.getInputStream()){JSONObject result=AttachmentStore.importCloud(context,localId,name,mime,size,in);result.put("remoteAttachmentId",attachmentId);return result;}}
            finally{if(c!=null)c.disconnect();}}
        throw new IllegalStateException("Download failed.");
    }
    public JSONObject send(String method,String path,JSONObject body,boolean authenticated,boolean useSchema)throws Exception{
        JSONObject session=authenticated?readSession():null;if(authenticated&&session==null)throw new IllegalStateException("Sign in with email to sync and share lists.");
        try{return sendRaw(method,path,body,session==null?null:session.optString("accessToken"),useSchema);}
        catch(ApiException e){if(!authenticated||e.status!=401)throw e;refreshSession();JSONObject fresh=readSession();return sendRaw(method,path,body,fresh.optString("accessToken"),useSchema);}
    }
    private JSONObject sendRaw(String method,String path,JSONObject body,String accessToken,boolean useSchema)throws Exception{
        HttpURLConnection c=null;try{
            URL url=new URL(baseUrl+path);c=(HttpURLConnection)url.openConnection();c.setRequestMethod(method);c.setConnectTimeout(10000);c.setReadTimeout(30000);c.setDoInput(true);
            c.setRequestProperty("apikey",apiKey);c.setRequestProperty("Accept","application/json");
            c.setRequestProperty("Authorization","Bearer "+(accessToken==null||accessToken.isEmpty()?apiKey:accessToken));
            if(useSchema){c.setRequestProperty("Accept-Profile",schema);c.setRequestProperty("Content-Profile",schema);}
            if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");byte[] data=body.toString().getBytes(StandardCharsets.UTF_8);try(OutputStream out=c.getOutputStream()){out.write(data);}}
            int status=c.getResponseCode();InputStream in=status>=400?c.getErrorStream():c.getInputStream();String response=in==null?"":read(in);
            if(status<200||status>=300)throw new ApiException(status,normalizeError(response,status),retryAfter(c.getHeaderField("Retry-After")));
            if(response.trim().isEmpty())return new JSONObject();
            Object parsed=new org.json.JSONTokener(response).nextValue();if(parsed instanceof JSONObject)return (JSONObject)parsed;if(parsed instanceof JSONArray){JSONArray a=(JSONArray)parsed;return a.length()>0&&a.opt(0) instanceof JSONObject?a.getJSONObject(0):new JSONObject().put("data",a);}return new JSONObject();
        }finally{if(c!=null)c.disconnect();}
    }
    private static String read(InputStream in)throws Exception{try(InputStream input=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=input.read(b))!=-1)out.write(b,0,n);return new String(out.toByteArray(),StandardCharsets.UTF_8);}}
    private static long retryAfter(String value){if(value==null)return 0;try{return Math.max(0,Long.parseLong(value.trim()));}catch(Exception ignored){try{return Math.max(0,(java.time.ZonedDateTime.parse(value,java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toEpochSecond()-java.time.Instant.now().getEpochSecond()));}catch(Exception e){return 0;}}}
    private static JSONObject normalizeError(String text,int status){try{JSONObject raw=new JSONObject(text);String message=raw.optString("msg",raw.optString("message",raw.optString("error_description",raw.optString("error","Request failed"))));return new JSONObject().put("error",new JSONObject().put("code",raw.optString("code",raw.optString("error_code",String.valueOf(status))).toUpperCase()).put("message",message).put("details",raw.optString("details","")));}catch(Exception ignored){try{return new JSONObject().put("error",new JSONObject().put("code",String.valueOf(status)).put("message",text.isEmpty()?"Request failed":text));}catch(Exception e){return new JSONObject();}}}
    private static JSONObject sessionResult(JSONObject raw,String userId,String email)throws Exception{return new JSONObject().put("userId",userId).put("email",email).put("accessToken",raw.getString("access_token")).put("refreshToken",raw.getString("refresh_token")).put("tokenType","Bearer").put("expiresInSeconds",raw.optLong("expires_in",3600));}
    public static final class ApiException extends Exception { public final int status;public final JSONObject response;public final long retryAfterSeconds;ApiException(int s,JSONObject r,long retryAfter){super(r.optJSONObject("error")!=null?r.optJSONObject("error").optString("message"):"Request failed");status=s;response=r;retryAfterSeconds=retryAfter;} }
}
