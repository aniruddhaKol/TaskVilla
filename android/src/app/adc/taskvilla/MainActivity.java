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
        store=new LocalStore(this); AttachmentStore.clean(this,store.load(),new HashSet<String>()); web=new WebView(this); web.setBackgroundColor(Color.WHITE);
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
                if("https".equals(scheme) && ("www.linkedin.com".equals(host)||"wa.me".equals(host)))intent=new Intent(Intent.ACTION_VIEW,uri);
                else if("mailto:a.d.chowdhury@gmail.com".equals(uri.toString()))intent=new Intent(Intent.ACTION_SENDTO,uri);
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
            if(!fieldId.matches("(edit|task)-due"))return;
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
        @JavascriptInterface public String save(String json) { try{store.save(json);synchronized(stagedAttachments){stagedAttachments.removeAll(AttachmentStore.referenced(json));AttachmentStore.clean(MainActivity.this,json,stagedAttachments);}return "ok";}catch(Exception e){return "error";} }
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
        @JavascriptInterface public String exportBackup(String filename,String content) {
            if(!filename.matches("TaskVilla-backup-[A-Za-z0-9T-]+Z\\.(json|xml)")||content==null||pendingExportContent!=null)return "error";
            pendingExportContent=content;pendingExportName=filename;
            runOnUiThread(()->{try{Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType(filename.endsWith(".json")?"application/json":"application/xml");intent.putExtra(Intent.EXTRA_TITLE,filename);startActivityForResult(intent,42);}catch(Exception e){pendingExportContent=null;pendingExportName=null;exportNotice("Could not open the save dialog.");}});
            return "pending";
        }
        @JavascriptInterface public String copyHtml(String text,String html) { try { ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newHtmlText("TaskVilla incomplete tasks",text,html)); return "ok"; } catch(Exception e) { return "error"; } }
        @JavascriptInterface public String copyText(String text) { try { ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("TaskVilla incomplete tasks",text)); return "ok"; } catch(Exception e) { return "error"; } }
        @JavascriptInterface public void copyCode(String code) { runOnUiThread(()->{((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Demo list invitation",code));Toast.makeText(MainActivity.this,"Demo code copied",Toast.LENGTH_SHORT).show();}); }
    }
    private void dateResult(String id,String value){if(!isFinishing()&&!isDestroyed())web.evaluateJavascript("window.taskDateSelected&&taskDateSelected("+JSONObject.quote(id)+","+JSONObject.quote(value)+")",null);}
    private void attachmentResult(int token,String result){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())web.evaluateJavascript("window.attachmentsSelected&&attachmentsSelected("+token+","+result+")",null);});}
    private void receiveAttachments(int result,Intent data){
        final int token=attachmentToken,existing=attachmentCount;final String existingIds=attachmentExistingIds;attachmentToken=-1;if(token<0)return;
        if(result!=RESULT_OK||data==null){attachmentResult(token,"{}");return;}
        final LinkedHashSet<Uri> selected=new LinkedHashSet<>();if(data.getClipData()!=null){for(int i=0;i<data.getClipData().getItemCount();i++)selected.add(data.getClipData().getItemAt(i).getUri());}else if(data.getData()!=null)selected.add(data.getData());
        if(selected.isEmpty()){attachmentResult(token,"{}");return;}
        if(selected.size()+existing>AttachmentStore.MAX_FILES){attachmentResult(token,"{\"error\":\"Maximum 10 files per task. No files were added.\"}");return;}
        new Thread(()->{try{JSONArray files=AttachmentStore.importSelection(this,selected,existing,new JSONArray(existingIds),stagedAttachments);attachmentResult(token,new JSONObject().put("files",files).toString());}
            catch(Exception e){String detail=e.getMessage();String message=detail!=null&&(detail.startsWith("Each file must")||detail.equals("Same file already attached")||detail.startsWith("Maximum 10"))?detail:"Could not read or store the selected files. No files were added.";try{attachmentResult(token,new JSONObject().put("error",message).toString());}catch(Exception ignored){}}
        },"TaskVilla attachments").start();
    }
    private void exportNotice(String message){runOnUiThread(()->web.evaluateJavascript("notice("+JSONObject.quote(message)+")",null));}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);if(pendingExportContent!=null){state.putString("exportContent",pendingExportContent);state.putString("exportName",pendingExportName);}}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==43){receiveAttachments(result,data);return;}if(request==41&&fileCallback!=null){fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));fileCallback=null;}if(request==42){final String content=pendingExportContent,name=pendingExportName;pendingExportContent=null;pendingExportName=null;if(result!=RESULT_OK||data==null||data.getData()==null){exportNotice("Export cancelled");return;}if(content==null){exportNotice("Export interrupted. Please try again.");return;}final Uri uri=data.getData();new Thread(()->{try(java.io.OutputStream output=getContentResolver().openOutputStream(uri,"wt")){if(output==null)throw new java.io.IOException("No output stream");output.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));output.flush();exportNotice("Backup saved: "+name);}catch(Exception e){exportNotice("Could not save backup. Please try again.");}}).start();}}
    @Override public void onBackPressed() { web.evaluateJavascript("if(document.querySelector('.unsaved-overlay')){TaskEditor.closeDialog();'stay'}else if(document.querySelector('.export-overlay')){closeExportOptions();'stay'}else if(document.querySelector('.list-menu-overlay')){closeListMenu();'stay'}else if(document.querySelector('.confirm-overlay')){closeConfirmation();'stay'}else if(document.querySelector('.palette-overlay')){closePalette();'stay'}else if(state.screen==='home'){LocalDatabase.save(JSON.stringify({lists,tasks,state}));'exit'}else{go(document.querySelector('.mobile-header [data-go]')?.dataset.go||'home');'stay'}",value->{if("\"exit\"".equals(value))finish();}); }
    @Override protected void onDestroy(){ if(fileCallback!=null)fileCallback.onReceiveValue(null);web.removeJavascriptInterface("LocalDatabase");web.destroy();store.close();super.onDestroy(); }
}
