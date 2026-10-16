package app.adc.taskvilla;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.zip.CRC32;

/** Bounded private copies, never base64 across the bridge. Aniruddha DuttaChowdhury · ADChowdhury. */
final class AttachmentStore {
 static final long MAX_BYTES=10000000L; static final int MAX_FILES=10;
 static File root(Context c){return new File(c.getFilesDir(),"attachments");}
 static File directory(Context c,String id)throws IOException{if(id==null||!id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))throw new IOException("Invalid attachment");return new File(root(c),id);}
 static File content(Context c,String id)throws IOException{File f=new File(directory(c,id),"content");if(!f.isFile())throw new FileNotFoundException("Attachment unavailable");return f;}
 static JSONObject metadata(Context c,String id)throws Exception{
  try(InputStream in=new FileInputStream(new File(directory(c,id),"metadata.json"));ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[1024];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>16384)throw new IOException("Invalid file details");}return new JSONObject(out.toString("UTF-8"));}
 }
 static JSONObject importFile(Context c,Uri uri,Set<String> staged)throws Exception{
  String name="Attachment",mime=c.getContentResolver().getType(uri);long declared=-1;
  try(Cursor rows=c.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE},null,null,null)){if(rows!=null&&rows.moveToFirst()){int ni=rows.getColumnIndex(OpenableColumns.DISPLAY_NAME),si=rows.getColumnIndex(OpenableColumns.SIZE);if(ni>=0&&!rows.isNull(ni))name=rows.getString(ni);if(si>=0&&!rows.isNull(si))declared=rows.getLong(si);}}
  if(declared>MAX_BYTES)throw new IOException("Each file must be 10 MB or smaller. No files were added.");
  name=name.replaceAll("[\\p{Cntrl}/\\\\]","_");if(name.length()>180)name=name.substring(0,180);if(name.trim().isEmpty())name="Attachment";
  String id=UUID.randomUUID().toString();File dir=directory(c,id);staged.add(id);if(!dir.mkdirs()){staged.remove(id);throw new IOException("Could not store the file");}
  try{long total=0;CRC32 crc=new CRC32();try(InputStream in=c.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(new File(dir,"content"))){if(in==null)throw new IOException("Could not read the selected file");byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1){total+=n;if(total>MAX_BYTES)throw new IOException("Each file must be 10 MB or smaller. No files were added.");crc.update(b,0,n);out.write(b,0,n);}}
   JSONObject meta=new JSONObject().put("id",id).put("name",name).put("size",total).put("crc32",String.format(Locale.US,"%08x",crc.getValue())).put("mime",mime==null?"application/octet-stream":mime);writeMetadata(c,id,meta);return meta;
  }catch(Exception e){staged.remove(id);remove(c,id);throw e;}
 }
 static void writeMetadata(Context c,String id,JSONObject meta)throws Exception{try(OutputStream out=new FileOutputStream(new File(directory(c,id),"metadata.json"))){out.write(meta.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}}
 static String fingerprint(Context c,String id)throws Exception{
  JSONObject meta=metadata(c,id);File file=content(c,id);String hex=meta.optString("crc32","");
  if(!hex.matches("[0-9a-f]{8}")||meta.optLong("size",-1)!=file.length()){
   CRC32 crc=new CRC32();try(InputStream in=new FileInputStream(file)){byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)crc.update(b,0,n);}
   hex=String.format(Locale.US,"%08x",crc.getValue());meta.put("crc32",hex).put("size",file.length());writeMetadata(c,id,meta);
  }
  return file.length()+":"+hex;
 }
 static JSONArray importSelection(Context c,Collection<Uri> selected,int existingCount,JSONArray existingIds,Set<String> staged)throws Exception{
  if(existingCount<0||existingCount>MAX_FILES||selected.size()+existingCount>MAX_FILES)throw new IOException("Maximum 10 files per task. No files were added.");
  Set<String> seen=new HashSet<>();for(int i=0;i<existingIds.length();i++)try{seen.add(fingerprint(c,existingIds.getString(i)));}catch(Exception ignored){}
  JSONArray files=new JSONArray();ArrayList<String> created=new ArrayList<>();
  try{for(Uri uri:selected){JSONObject file=importFile(c,uri,staged);String id=file.getString("id");created.add(id);
    if(!seen.add(file.getLong("size")+":"+file.getString("crc32")))throw new IOException("Same file already attached");files.put(file);
   }return files;
  }catch(Exception error){for(String id:created){staged.remove(id);remove(c,id);}throw error;}
 }
 static void remove(Context c,String id){try{File d=directory(c,id);new File(d,"content").delete();new File(d,"metadata.json").delete();d.delete();}catch(Exception ignored){}}
 static Set<String> referenced(String json)throws Exception{Set<String> ids=new HashSet<>();if(json==null||json.equals("null"))return ids;JSONArray tasks=new JSONObject(json).optJSONArray("tasks");if(tasks!=null)for(int i=0;i<tasks.length();i++){JSONArray files=tasks.getJSONObject(i).optJSONArray("files");if(files!=null)for(int j=0;j<files.length();j++){JSONObject f=files.optJSONObject(j);if(f!=null)ids.add(f.optString("id"));}}return ids;}
 static void clean(Context c,String json,Set<String> staged){try{Set<String> keep=referenced(json);keep.addAll(staged);File[] dirs=root(c).listFiles();if(dirs!=null)for(File d:dirs)if(!keep.contains(d.getName()))remove(c,d.getName());}catch(Exception ignored){}}
 static Intent share(Context c,JSONArray ids)throws Exception{
  if(ids.length()<1||ids.length()>MAX_FILES)throw new IOException("Choose up to 10 files to share");ArrayList<Uri> uris=new ArrayList<>();String mime=null;
  for(int i=0;i<ids.length();i++){String id=ids.getString(i);JSONObject m=metadata(c,id);content(c,id);String type=m.getString("mime");mime=mime==null?type:mime.equals(type)?mime:"*/*";uris.add(new Uri.Builder().scheme("content").authority(c.getPackageName()+".attachments").appendPath(id).appendPath(m.getString("name")).build());}
  Intent send=new Intent(uris.size()==1?Intent.ACTION_SEND:Intent.ACTION_SEND_MULTIPLE).setType(mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);if(uris.size()==1)send.putExtra(Intent.EXTRA_STREAM,uris.get(0));else send.putParcelableArrayListExtra(Intent.EXTRA_STREAM,uris);
  ClipData clip=ClipData.newUri(c.getContentResolver(),"TaskVilla files",uris.get(0));for(int i=1;i<uris.size();i++)clip.addItem(new ClipData.Item(uris.get(i)));send.setClipData(clip);return Intent.createChooser(send,"Share files").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
 }
}
