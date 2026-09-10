package com.kitchenkeeper.android;
import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
final class PrivateKey {
    private static final String ALIAS="kitchenkeeper-user-api";
    private final android.content.SharedPreferences prefs;
    PrivateKey(Context context){prefs=context.getSharedPreferences("private-ai",Context.MODE_PRIVATE);}
    private SecretKey secret()throws Exception{KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);if(!store.containsAlias(ALIAS)){KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");gen.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());gen.generateKey();}return (SecretKey)store.getKey(ALIAS,null);}
    boolean connected(){return prefs.contains("ciphertext");}
    String hint(){return prefs.getString("hint","");}
    void save(String key)throws Exception{Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,secret());String data=Base64.encodeToString(cipher.doFinal(key.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);if(!prefs.edit().putString("ciphertext",data).putString("iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)).putString("hint",key.substring(key.length()-4)).commit())throw new Exception("Could not save the encrypted key.");}
    String read()throws Exception{if(!connected())throw new Exception("Connect your OpenAI API key in Settings → AI connection first.");try{Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,secret(),new GCMParameterSpec(128,Base64.decode(prefs.getString("iv",""),Base64.NO_WRAP)));return new String(cipher.doFinal(Base64.decode(prefs.getString("ciphertext",""),Base64.NO_WRAP)),StandardCharsets.UTF_8);}catch(Exception e){throw new Exception("Reconnect your API key in AI settings.");}}
    void clear(){prefs.edit().clear().apply();}
}
