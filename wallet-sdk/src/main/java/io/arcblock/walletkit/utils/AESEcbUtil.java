package io.arcblock.walletkit.utils;

import android.util.Base64;
import android.util.Log;
import io.arcblock.walletkit.did.HashType;
import io.arcblock.walletkit.hash.Hasher;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Nate Gu on 2019/3/26
 *
 *
 * 与Node js 相同的AES 加密
 */
public class AESEcbUtil {
  //-- 算法/模式/填充
  private static final String CipherMode = "AES/ECB/PKCS5Padding";

  //--创建密钥
  private static SecretKeySpec createKey(String password) {
    byte[] data = Hasher.INSTANCE.hash(HashType.SHA3, password.getBytes(StandardCharsets.UTF_8), 1);
    return new SecretKeySpec(data, "AES");
  }


  //--加密字节数组到字节数组
  public static byte[] encryptByte2Byte(byte[] content,String password){
    try {
      SecretKeySpec key = createKey(password);
      Cipher cipher = Cipher.getInstance(CipherMode);
      cipher.init(Cipher.ENCRYPT_MODE, key);
      byte[] result = cipher.doFinal(content);
      return result;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
  //--加密字节数组到字符串
  public static String encryptByte2String(byte[] content,String password){
    byte[] data =encryptByte2Byte(content,password);
    String result =new String(data);
    return  result;
  }

  //--加密字节数组到base64
  public static String encryptByte2Base64(byte[] content,String password){
    byte[] data =encryptByte2Byte(content,password);
    String result = new String(Base64.encode(data,Base64.DEFAULT));
    return result;
  }

  //--加密字符串到字节数组
  public static byte[] encryptString2Byte(String content, String password){
    byte[] data = null;
    try {
      data = content.getBytes("UTF-8");
    } catch (Exception e) {
      e.printStackTrace();
    }
    data = encryptByte2Byte(data,password);
    return data;
  }
  //--加密字符串到字符串
  public static String encryptString2String(String content, String password){
    byte[] data = null;
    try {
      data = content.getBytes("UTF-8");
    } catch (Exception e) {
      e.printStackTrace();
    }
    data = encryptByte2Byte(data,password);
    String result =new String(data);
    return result;
  }
  //--加密字符串到base64
  public static String encryptString2Base64(String content, String password){
    byte[] data = null;
    try {
      data = content.getBytes("UTF-8");
    } catch (Exception e) {
      e.printStackTrace();
    }
    data = encryptByte2Byte(data,password);
    String result =new String(Base64.encode(data,Base64.DEFAULT));
    return result;
  }

  //-- 解密字节数组到字节数组
  public static byte[] decryptByte2Byte(byte[] content, String password) {
    try {
      SecretKeySpec key = createKey(password);
      Cipher cipher = Cipher.getInstance(CipherMode);
      cipher.init(Cipher.DECRYPT_MODE, key);
      byte[] result = cipher.doFinal(content);
      return result;
    } catch (Exception e) {
      Log.e("test", e.getMessage());
      e.printStackTrace();
    }
    return null;
  }

  //--解密字符串到字节数组
  public static byte[] decryptString2Byte(String content, String password) {
    byte[] data = null;
    try {
      data = content.getBytes("UTF-8");
    } catch (Exception e) {
      e.printStackTrace();
    }
    data = decryptByte2Byte(data,password);
    return data;
  }

  //--解密base64到字节数组
  public static byte[] decryptBase642Byte(String content, String password) {
    byte[] data = null;
    try {
      data = Base64.decode(content,Base64.DEFAULT);
    } catch (Exception e) {
      e.printStackTrace();
    }
    data = decryptByte2Byte(data,password);
    return data;
  }

  //-- 解密字节数组到字符串
  public static String decryptByte2String(byte[] content, String password) {
    byte[] data =decryptByte2Byte(content,password);
    if(data == null || data.length == 0) return "";
    String result =new String(data);
    return result;
  }

  //-- 解密字节数组到字符串
  // public static String decryptBase642String(String content, String password, String iv) {
  //   byte[] data =Base64.decode(content,Base64.DEFAULT);
  //   String result=decryptByte2String(data,password,iv);
  //   return result;
  // }
}