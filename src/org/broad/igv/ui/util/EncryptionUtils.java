// Copied from genomagic-api project
package org.broad.igv.ui.util;

import com.google.crypto.tink.*;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.AeadFactory;
import com.google.crypto.tink.aead.AeadKeyTemplates;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.Base64;

public class EncryptionUtils {
  public static class Key {
    private final KeysetHandle keysetHandle;

    private Key(KeysetHandle keysetHandle) {
      this.keysetHandle = keysetHandle;
    }

    public String getAsBase64String() {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      try {
        CleartextKeysetHandle.write(keysetHandle, BinaryKeysetWriter.withOutputStream(outputStream));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return new String(Base64.getEncoder().encode(outputStream.toByteArray()));
    }

    public static Key getFromBase64String(String base64String) {
      InputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(base64String));
      try {
        return new Key(CleartextKeysetHandle.read(BinaryKeysetReader.withInputStream(inputStream)));
      } catch (GeneralSecurityException | IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public EncryptionUtils() {
    try {
      Security.setProperty("crypto.policy", "unlimited");
      AeadConfig.register();
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  public Key generateNewKey() {
    try {
      return new Key(KeysetHandle.generateNew(AeadKeyTemplates.AES256_GCM));
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  public byte[] encrypt(byte[] data, Key key) {
    try {
      Aead aead = AeadFactory.getPrimitive(key.keysetHandle);
      return aead.encrypt(data, new byte[0]);
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  public byte[] decrypt(byte[] encryptedData, Key key) {
    try {
      Aead aead = AeadFactory.getPrimitive(key.keysetHandle);
      return aead.decrypt(encryptedData, new byte[0]);
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }
}

