package app.adc.taskvilla;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import org.json.JSONObject;
import java.io.FileNotFoundException;

/** Read-only provider; recipients require transient Android URI grants. */
public final class AttachmentProvider extends ContentProvider {
 @Override public boolean onCreate(){return true;}
 private String id(Uri u)throws FileNotFoundException{if(!u.getAuthority().equals(getContext().getPackageName()+".attachments")||u.getPathSegments().size()!=2)throw new FileNotFoundException("Invalid attachment");return u.getPathSegments().get(0);}
 @Override public ParcelFileDescriptor openFile(Uri u,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException("Read only");try{return ParcelFileDescriptor.open(AttachmentStore.content(getContext(),id(u)),ParcelFileDescriptor.MODE_READ_ONLY);}catch(Exception e){throw new FileNotFoundException("Attachment unavailable");}}
 @Override public String getType(Uri u){try{return AttachmentStore.metadata(getContext(),id(u)).getString("mime");}catch(Exception e){return "application/octet-stream";}}
 @Override public Cursor query(Uri u,String[] projection,String selection,String[] args,String sort){String[] cols=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor cursor=new MatrixCursor(cols);try{JSONObject m=AttachmentStore.metadata(getContext(),id(u));Object[] row=new Object[cols.length];for(int i=0;i<cols.length;i++)row[i]=cols[i].equals(OpenableColumns.DISPLAY_NAME)?m.getString("name"):cols[i].equals(OpenableColumns.SIZE)?m.getLong("size"):null;cursor.addRow(row);}catch(Exception ignored){}return cursor;}
 @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException("Read only");}
 @Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException("Read only");}
 @Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException("Read only");}
}
