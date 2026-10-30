package app.adc.taskvilla;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Stores Supabase session data encrypted with an Android Keystore key. */
public final class SecureSessionStore {
    private static final String PREFS = "taskvilla_secure_session";
    private static final String KEY_ALIAS = "taskvilla.session.v1";
    private static final String VALUE = "encrypted";
    private final Context context;
    private int generation=0;
    public synchronized int generation(){return generation;}
    public synchronized boolean putIfGeneration(String value,int expected)throws Exception{if(generation!=expected)return false;put(value);return true;}

    public SecureSessionStore(Context context) { this.context=context.getApplicationContext(); }

    public synchronized void put(String value) throws Exception {
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] encrypted=cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        String encoded=Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(encrypted,Base64.NO_WRAP);
        prefs().edit().putString(VALUE,encoded).commit();
    }

    public synchronized String get() throws Exception {
        String stored=prefs().getString(VALUE,null); if(stored==null)return null;
        String[] parts=stored.split(":",2); if(parts.length!=2)throw new IllegalStateException("Invalid saved session");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));
        return new String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),StandardCharsets.UTF_8);
    }

    public synchronized void clear() {generation++; prefs().edit().remove(VALUE).commit(); }
    private SharedPreferences prefs(){return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    private SecretKey key() throws Exception {
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if(store.containsAlias(KEY_ALIAS))return (SecretKey)store.getKey(KEY_ALIAS,null);
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
}
