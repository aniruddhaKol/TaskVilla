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

/** Android local review shell. Aniruddha DuttaChowdhury · ADChowdhury. */
public final class MainActivity extends Activity {
    private WebView web;
    private LocalStore store;
    private ValueCallback<Uri[]> fileCallback;
    private String pendingExportContent;
    private String pendingExportName;
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState!=null){pendingExportContent=savedInstanceState.getString("exportContent");pendingExportName=savedInstanceState.getString("exportName");}
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().getDecorView().setSystemUiVisibility(0);
        WebView.setWebContentsDebuggingEnabled(true);
        store=new LocalStore(this); web=new WebView(this); web.setBackgroundColor(Color.WHITE);
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
        @JavascriptInterface public void showKeyboard() { runOnUiThread(()->{web.requestFocus();web.postDelayed(()->web.evaluateJavascript("(()=>{const e=document.activeElement;return !!e&&!e.readOnly&&(e.tagName==='TEXTAREA'||e.isContentEditable||(e.tagName==='INPUT'&&['text','email','search','tel','url','number','password'].includes(e.type)))})()",focused->{if("true".equals(focused)&&!isFinishing()&&!isDestroyed())((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(web,InputMethodManager.SHOW_IMPLICIT);}),80);}); }
        @JavascriptInterface public void hideKeyboard() { runOnUiThread(()->((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(web.getWindowToken(),0)); }
        @JavascriptInterface public String load() { try{return store.load();}catch(Exception e){return "{\"error\":\"Could not read local database\"}";} }
        @JavascriptInterface public String save(String json) { try{store.save(json);return "ok";}catch(Exception e){return "error";} }
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
    private void exportNotice(String message){runOnUiThread(()->web.evaluateJavascript("notice("+JSONObject.quote(message)+")",null));}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);if(pendingExportContent!=null){state.putString("exportContent",pendingExportContent);state.putString("exportName",pendingExportName);}}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==41&&fileCallback!=null){fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));fileCallback=null;}if(request==42){final String content=pendingExportContent,name=pendingExportName;pendingExportContent=null;pendingExportName=null;if(result!=RESULT_OK||data==null||data.getData()==null){exportNotice("Export cancelled");return;}if(content==null){exportNotice("Export interrupted. Please try again.");return;}final Uri uri=data.getData();new Thread(()->{try(java.io.OutputStream output=getContentResolver().openOutputStream(uri,"wt")){if(output==null)throw new java.io.IOException("No output stream");output.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));output.flush();exportNotice("Backup saved: "+name);}catch(Exception e){exportNotice("Could not save backup. Please try again.");}}).start();}}
    @Override public void onBackPressed() { web.evaluateJavascript("if(document.querySelector('.export-overlay')){closeExportOptions();'stay'}else if(document.querySelector('.list-menu-overlay')){closeListMenu();'stay'}else if(document.querySelector('.confirm-overlay')){closeConfirmation();'stay'}else if(document.querySelector('.palette-overlay')){closePalette();'stay'}else if(state.screen==='home'){LocalDatabase.save(JSON.stringify({lists,tasks,state}));'exit'}else{go(document.querySelector('.mobile-header [data-go]')?.dataset.go||'home');'stay'}",value->{if("\"exit\"".equals(value))finish();}); }
    @Override protected void onDestroy(){ if(fileCallback!=null)fileCallback.onReceiveValue(null);web.removeJavascriptInterface("LocalDatabase");web.destroy();store.close();super.onDestroy(); }
}
