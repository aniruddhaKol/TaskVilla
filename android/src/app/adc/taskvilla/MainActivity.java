package app.adc.taskvilla;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import android.content.ClipData;
import android.content.ClipboardManager;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.*;

/** Android local review shell. Aniruddha DuttaChowdhury · ADChowdhury. */
public final class MainActivity extends Activity {
    private WebView web;
    private LocalStore store;
    private SecureSessionStore sessions;
    private TaskVillaApi api;
    private ValueCallback<Uri[]> fileCallback;
    private String pendingExportContent;
    private String pendingExportName;
    private int attachmentToken=-1, attachmentCount;
    private String attachmentExistingIds="[]";
    private final Set<String> stagedAttachments=Collections.synchronizedSet(new HashSet<String>());
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState!=null){pendingExportContent=savedInstanceState.getString("exportContent");pendingExportName=savedInstanceState.getString("exportName");}
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().getDecorView().setSystemUiVisibility(0);
        WebView.setWebContentsDebuggingEnabled(true);
        store=new LocalStore(this); sessions=new SecureSessionStore(this); try{api=new TaskVillaApi(this,sessions);}catch(Exception ignored){} AttachmentStore.clean(this,store.load(),new HashSet<String>()); web=new WebView(this); web.setBackgroundColor(Color.WHITE);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setAllowFileAccess(true);
        web.getSettings().setAllowContentAccess(true);
        web.getSettings().setAllowFileAccessFromFileURLs(false);
        web.getSettings().setAllowUniversalAccessFromFileURLs(false);
        web.getSettings().setDomStorageEnabled(false);
        web.addJavascriptInterface(new Bridge(),"LocalDatabase");
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri=request.getUrl(); if(uri.toString().startsWith("file:///android_asset/ui/"))return false;
                String scheme=uri.getScheme(),host=uri.getHost(); Intent intent=null;
                if("taskvilla".equals(scheme)&&"cloud-storage-connected".equals(host)){runOnUiThread(()->web.evaluateJavascript("window.refreshCloudStorage&&refreshCloudStorage(true)",null));return true;}
                if("https".equals(scheme) && ("www.linkedin.com".equals(host)||"wa.me".equals(host)))intent=new Intent(Intent.ACTION_VIEW,uri);
                else if("mailto".equals(scheme) && "a.d.chowdhury@gmail.com".equals(android.net.MailTo.parse(uri.toString()).getTo()))intent=new Intent(Intent.ACTION_SENDTO,uri);
                else if("tel:+919987484090".equals(uri.toString()))intent=new Intent(Intent.ACTION_DIAL,uri);
                if(intent!=null)try{startActivity(intent);}catch(android.content.ActivityNotFoundException e){Toast.makeText(MainActivity.this,"No app available for this link",Toast.LENGTH_SHORT).show();}
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if(fileCallback!=null) fileCallback.onReceiveValue(null); fileCallback=callback;
                try { startActivityForResult(params.createIntent(),41); return true; }
                catch(Exception e) { fileCallback=null; return false; }
            }
        });
        setContentView(web); web.loadUrl("file:///android_asset/ui/index.html");
    }
    private final class Bridge {
        @JavascriptInterface public void showDatePicker(final String fieldId,final String value) {
            if(!fieldId.matches("(edit|task|activity)-due"))return;
            runOnUiThread(()->{
                ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(web.getWindowToken(),0);
                java.time.LocalDate initial;try{initial=java.time.LocalDate.parse(value);}catch(Exception e){initial=java.time.LocalDate.now();}
                android.app.DatePickerDialog picker=new android.app.DatePickerDialog(MainActivity.this,(view,year,month,day)->dateResult(fieldId,String.format(java.util.Locale.US,"%04d-%02d-%02d",year,month+1,day)),initial.getYear(),initial.getMonthValue()-1,initial.getDayOfMonth());
                picker.setButton(android.content.DialogInterface.BUTTON_NEUTRAL,"Clear",(dialog,which)->dateResult(fieldId,""));
                picker.show();
            });
        }
        @JavascriptInterface public void showKeyboard() { runOnUiThread(()->{web.requestFocus();web.postDelayed(()->web.evaluateJavascript("(()=>{const e=document.activeElement;return !!e&&!e.readOnly&&(e.tagName==='TEXTAREA'||e.isContentEditable||(e.tagName==='INPUT'&&['text','email','search','tel','url','number','password'].includes(e.type)))})()",focused->{if("true".equals(focused)&&!isFinishing()&&!isDestroyed())((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(web,InputMethodManager.SHOW_IMPLICIT);}),80);}); }
        @JavascriptInterface public void hideKeyboard() { runOnUiThread(()->((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(web.getWindowToken(),0)); }
        @JavascriptInterface public String load() { try{return store.load();}catch(Exception e){return "{\"error\":\"Could not read local database\"}";} }
        @JavascriptInterface public String save(String json) { try{long revision=store.save(json);synchronized(stagedAttachments){stagedAttachments.removeAll(AttachmentStore.referenced(json));AttachmentStore.clean(MainActivity.this,json,stagedAttachments);}return "ok:"+revision;}catch(Exception e){return "STALE_DOCUMENT".equals(e.getMessage())?"stale":"error";} }
        @JavascriptInterface public void pickAttachments(final int token,final int existingCount,final String existingIds) {
            runOnUiThread(()->{if(attachmentToken!=-1||existingCount<0||existingCount>=AttachmentStore.MAX_FILES){attachmentResult(token,"{\"error\":\"Maximum 10 files per task.\"}");return;}
                attachmentToken=token;attachmentCount=existingCount;attachmentExistingIds=existingIds;
                try{Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT);picker.addCategory(Intent.CATEGORY_OPENABLE);picker.setType("*/*");picker.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);startActivityForResult(picker,43);}catch(Exception e){attachmentToken=-1;attachmentResult(token,"{\"error\":\"Could not open the file picker.\"}");}
            });
        }
        @JavascriptInterface public void releaseAttachments(String json) {
            try{JSONArray ids=new JSONArray(json);synchronized(stagedAttachments){for(int i=0;i<ids.length();i++)stagedAttachments.remove(ids.getString(i));AttachmentStore.clean(MainActivity.this,store.load(),stagedAttachments);}}catch(Exception ignored){}
        }
        @JavascriptInterface public void shareAttachments(String json) {
            runOnUiThread(()->{try{startActivity(AttachmentStore.share(MainActivity.this,new JSONArray(json)));}catch(Exception e){exportNotice("Could not share the files. Reattach missing files and try again.");}});
        }
        @JavascriptInterface public void openAttachment(String id){runOnUiThread(()->{try{startActivity(AttachmentStore.open(MainActivity.this,id));}catch(Exception e){exportNotice("No app available to open this attachment.");}});}
        @JavascriptInterface public void downloadCloudAttachment(final int token,String taskLocalId,String remoteId,String name,String mime,long size){new Thread(()->{JSONObject result;try{if(api==null)throw new IllegalStateException("Cloud storage is unavailable.");String localId=java.util.UUID.randomUUID().toString();result=api.downloadCloudAttachment(remoteId,localId,name,mime,size);store.cacheCloudAttachment(taskLocalId,remoteId,result);result.put("ok",true);runOnUiThread(()->{try{startActivity(AttachmentStore.open(MainActivity.this,localId));}catch(Exception ignored){}});}catch(Exception e){result=errorResult(e);}callback("taskCloudDownloadResult",Integer.toString(token),result.toString());},"TaskVilla attachment download").start();}
        @JavascriptInterface public void cloudStorage(final int token,String operation,String payload){new Thread(()->{JSONObject result;try{if(api==null)throw new IllegalStateException("Cloud storage is unavailable.");result=api.cloudOperation(operation,new JSONObject(payload));result.put("ok",true);}catch(Exception e){result=errorResult(e);}callback("taskCloudStorageResult",Integer.toString(token),result.toString());},"TaskVilla cloud storage").start();}
        @JavascriptInterface public void openCloudAuthorization(String value){runOnUiThread(()->{try{Uri uri=Uri.parse(value);String host=uri.getHost();if(!"https".equals(uri.getScheme())||host==null||!(host.equals("accounts.google.com")||host.equals("www.dropbox.com")||host.equals("login.microsoftonline.com")))throw new IllegalArgumentException();Intent tab=new Intent(Intent.ACTION_VIEW,uri);android.os.Bundle extras=new android.os.Bundle();extras.putBinder("android.support.customtabs.extra.SESSION",null);tab.putExtras(extras);tab.putExtra("android.support.customtabs.extra.TITLE_VISIBILITY",1);tab.putExtra("android.support.customtabs.extra.TOOLBAR_COLOR",0xff2b1b12);try{tab.setPackage("com.android.chrome");startActivity(tab);}catch(android.content.ActivityNotFoundException e){tab.setPackage(null);startActivity(tab);}}catch(Exception e){exportNotice("Could not open the cloud sign-in page.");}});}
        @JavascriptInterface public String exportBackup(String filename,String content) {
            if(!filename.matches("TaskVilla-backup-[A-Za-z0-9T-]+Z\\.(json|xml)")||content==null||pendingExportContent!=null)return "error";
            pendingExportContent=content;pendingExportName=filename;
            runOnUiThread(()->{try{Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType(filename.endsWith(".json")?"application/json":"application/xml");intent.putExtra(Intent.EXTRA_TITLE,filename);startActivityForResult(intent,42);}catch(Exception e){pendingExportContent=null;pendingExportName=null;exportNotice("Could not open the save dialog.");}});
            return "pending";
        }
        @JavascriptInterface public String copyHtml(String text,String html) { try { ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newHtmlText("TaskVilla incomplete tasks",text,html)); return "ok"; } catch(Exception e) { return "error"; } }
        @JavascriptInterface public String copyText(String text) { try { ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("TaskVilla incomplete tasks",text)); return "ok"; } catch(Exception e) { return "error"; } }
        @JavascriptInterface public void copyCode(String code) { runOnUiThread(()->{((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Demo list invitation",code));Toast.makeText(MainActivity.this,"Demo code copied",Toast.LENGTH_SHORT).show();}); }
        @JavascriptInterface public String sessionState(){try{JSONObject s=api==null?null:api.session();return s==null?"null":new JSONObject().put("email",s.optString("email")).put("userId",s.optString("userId")).toString();}catch(Exception e){return "null";}}
        @JavascriptInterface public void requestOtp(String email){new Thread(()->{JSONObject result;try{if(api==null)throw new IllegalStateException("Sync configuration is unavailable.");result=api.requestOtp(email,true);result.put("ok",true);}catch(Exception e){result=errorResult(e);}callback("taskAuthResult",JSONObject.quote("request"),result.toString());},"TaskVilla OTP request").start();}
        @JavascriptInterface public void verifyOtp(String email,String code){new Thread(()->{JSONObject result;boolean verified=false;try{if(api==null)throw new IllegalStateException("Sync configuration is unavailable.");result=api.verifyOtp(email,code);store.storeUser(result.optString("userId"));result=new JSONObject().put("ok",true).put("email",result.optString("email")).put("userId",result.optString("userId"));verified=true;}catch(Exception e){result=errorResult(e);}callback("taskAuthResult",JSONObject.quote("verify"),result.toString());if(verified)new Thread(()->{JSONObject sync;try{sync=SyncEngine.synchronize(MainActivity.this,store,api);cleanupLocalAttachmentCopies();}catch(Exception e){sync=errorResult(e);}callback("taskSyncResult",sync.toString());},"TaskVilla first sign-in sync").start();},"TaskVilla OTP verify").start();}
        @JavascriptInterface public void signOut(){new Thread(()->{JSONObject result;try{if(sessions!=null)sessions.clear();store.clearSignedInData();stagedAttachments.clear();AttachmentStore.clean(MainActivity.this,store.load(),stagedAttachments);result=new JSONObject().put("ok",true);}catch(Exception e){result=errorResult(e);}callback("taskAuthResult",JSONObject.quote("signout"),result.toString());},"TaskVilla sign out").start();}
        @JavascriptInterface public void syncNow(){new Thread(()->{JSONObject result;try{if(api==null)throw new IllegalStateException("Sync configuration is unavailable.");result=SyncEngine.synchronize(MainActivity.this,store,api);cleanupLocalAttachmentCopies();}catch(Exception e){result=errorResult(e);}callback("taskSyncResult",result.toString());},"TaskVilla sync").start();}
        @JavascriptInterface public void manageSharing(String requestJson){new Thread(()->{JSONObject result;try{if(api==null)throw new IllegalStateException("Sync configuration is unavailable.");result=SyncEngine.sharing(MainActivity.this,store,api,new JSONObject(requestJson));result.put("ok",true);}catch(Exception e){result=errorResult(e);}callback("taskSharingResult",result.toString());},"TaskVilla sharing").start();}
        @JavascriptInterface public void loadSharedLists(){new Thread(()->{JSONObject result;try{if(api==null)throw new IllegalStateException("Sync configuration is unavailable.");result=SyncEngine.synchronize(MainActivity.this,store,api);cleanupLocalAttachmentCopies();result.put("ok",true);}catch(Exception e){result=errorResult(e);}callback("taskSharedListsResult",result.toString());},"TaskVilla shared lists").start();}
        @JavascriptInterface public void shareChanges(String localListId,String beforeJson,String afterJson){new Thread(()->{JSONObject result;try{if(api==null)throw new IllegalStateException("Sync configuration is unavailable.");result=SyncEngine.synchronize(MainActivity.this,store,api);result.put("ok",true).put("status","shared");}catch(Exception e){result=errorResult(e);}callback("taskSharingResult",result.toString());},"TaskVilla sharing update").start();}
        @JavascriptInterface public void loadMembers(String localListId){new Thread(()->{JSONObject result;try{if(api==null)throw new IllegalStateException("Sync configuration is unavailable.");String serverId=store.serverIdFor("list",localListId);result=api.sharing(new JSONObject().put("action","list").put("listId",serverId));result.put("ok",true).put("localListId",localListId);}catch(Exception e){result=errorResult(e);}callback("taskMembersResult",result.toString());},"TaskVilla members").start();}
        @JavascriptInterface public void revokeMember(String localListId,String memberUserId){new Thread(()->{JSONObject result;try{if(api==null)throw new IllegalStateException("Sync configuration is unavailable.");String serverId=store.serverIdFor("list",localListId);result=SyncEngine.sharing(MainActivity.this,store,api,new JSONObject().put("action","revoke").put("listId",serverId).put("memberUserId",memberUserId));result.put("ok",true);}catch(Exception e){result=errorResult(e);}callback("taskSharingResult",result.toString());},"TaskVilla remove member").start();}
    }
    private void cleanupLocalAttachmentCopies(){try{AttachmentStore.clean(this,store.load(),stagedAttachments);}catch(Exception ignored){}}
    private JSONObject errorResult(Exception e){try{JSONObject out=new JSONObject().put("ok",false).put("message",e.getMessage()==null?"Could not connect. Your local data is safe.":e.getMessage());if(e instanceof TaskVillaApi.ApiException){TaskVillaApi.ApiException a=(TaskVillaApi.ApiException)e;out.put("statusCode",a.status).put("error",a.response.optJSONObject("error"));}return out;}catch(Exception ignored){return new JSONObject();}}
    private void callback(String name,String... args){if(web==null||isFinishing()||isDestroyed())return;StringBuilder call=new StringBuilder("window.").append(name).append("&&window.").append(name).append('(');for(int i=0;i<args.length;i++){if(i>0)call.append(',');call.append(args[i]);}call.append(')');runOnUiThread(()->{if(web!=null&&!isFinishing()&&!isDestroyed())web.evaluateJavascript(call.toString(),null);});}
    private void dateResult(String id,String value){if(!isFinishing()&&!isDestroyed())web.evaluateJavascript("window.taskDateSelected&&taskDateSelected("+JSONObject.quote(id)+","+JSONObject.quote(value)+")",null);}
    private void attachmentResult(int token,String result){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())web.evaluateJavascript("window.attachmentsSelected&&attachmentsSelected("+token+","+result+")",null);});}
    private void receiveAttachments(int result,Intent data){
        final int token=attachmentToken,existing=attachmentCount;final String existingIds=attachmentExistingIds;attachmentToken=-1;if(token<0)return;
        if(result!=RESULT_OK||data==null){attachmentResult(token,"{}");return;}
        final LinkedHashSet<Uri> selected=new LinkedHashSet<>();if(data.getClipData()!=null){for(int i=0;i<data.getClipData().getItemCount();i++)selected.add(data.getClipData().getItemAt(i).getUri());}else if(data.getData()!=null)selected.add(data.getData());
        if(selected.isEmpty()){attachmentResult(token,"{}");return;}
        if(selected.size()+existing>AttachmentStore.MAX_FILES){attachmentResult(token,"{\"error\":\"Maximum 10 files per task. No files were added.\"}");return;}
        new Thread(()->{try{JSONArray files=AttachmentStore.importSelection(this,selected,existing,new JSONArray(existingIds),stagedAttachments);attachmentResult(token,new JSONObject().put("files",files).toString());}
            catch(Exception e){String detail=e.getMessage();String message=detail!=null&&(detail.startsWith("Each file must")||detail.equals("Same file is already attached")||detail.startsWith("Maximum 10"))?detail:"Could not read or store the selected files. No files were added.";try{attachmentResult(token,new JSONObject().put("error",message).toString());}catch(Exception ignored){}}
        },"TaskVilla attachments").start();
    }
    private void exportNotice(String message){runOnUiThread(()->web.evaluateJavascript("notice("+JSONObject.quote(message)+")",null));}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);if(pendingExportContent!=null){state.putString("exportContent",pendingExportContent);state.putString("exportName",pendingExportName);}}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);Uri uri=intent==null?null:intent.getData();if(uri!=null&&"taskvilla".equals(uri.getScheme())&&"cloud-storage-connected".equals(uri.getHost()))web.evaluateJavascript("window.refreshCloudStorage&&refreshCloudStorage(true)",null);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==43){receiveAttachments(result,data);return;}if(request==41&&fileCallback!=null){fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));fileCallback=null;}if(request==42){final String content=pendingExportContent,name=pendingExportName;pendingExportContent=null;pendingExportName=null;if(result!=RESULT_OK||data==null||data.getData()==null){exportNotice("Export cancelled");return;}if(content==null){exportNotice("Export interrupted. Please try again.");return;}final Uri uri=data.getData();new Thread(()->{try(java.io.OutputStream output=getContentResolver().openOutputStream(uri,"wt")){if(output==null)throw new java.io.IOException("No output stream");output.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));output.flush();exportNotice("Backup saved: "+name);}catch(Exception e){exportNotice("Could not save backup. Please try again.");}}).start();}}
    @Override public void onBackPressed() { web.evaluateJavascript("if(document.querySelector('.help-overlay')){closeTaskVillaHelp();'stay'}else if(document.querySelector('.unsaved-overlay')){TaskEditor.closeDialog();'stay'}else if(document.querySelector('.export-overlay')){closeExportOptions();'stay'}else if(document.querySelector('.list-menu-overlay')){closeListMenu();'stay'}else if(document.querySelector('.confirm-overlay')){closeConfirmation();'stay'}else if(document.querySelector('.palette-overlay')){closePalette();'stay'}else if(document.querySelector('.tracker-dropdown[open]')){document.querySelectorAll('.tracker-dropdown[open]').forEach(menu=>menu.open=false);'stay'}else if(state.screen==='home'){LocalDatabase.save(JSON.stringify({lists,tasks,state,tracker}));'exit'}else{go(document.querySelector('.mobile-header [data-go]')?.dataset.go||'home');'stay'}",value->{if("\"exit\"".equals(value))finish();}); }
    @Override protected void onDestroy(){ if(fileCallback!=null)fileCallback.onReceiveValue(null);web.removeJavascriptInterface("LocalDatabase");web.destroy();store.close();super.onDestroy(); }
}
