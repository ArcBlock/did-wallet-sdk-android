package io.arcblock.walletkit.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemObjectParser;
import org.bouncycastle.util.io.pem.PemWriter;

public class RSAUtil {


  private static String RSA_TRANSFORM = "RSA/ECB/OAEPPadding";
  /**
   * 生产秘钥 秘钥长度建议不要小于1024
   * @param keyLength 秘钥长度，范围512 —— 2048
   * @return the secretKey
   * @throws Exception
   */
  public static KeyPair generateKey(int keyLength) throws NoSuchAlgorithmException {
    // 获取秘钥生成器
    KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
    keyGenerator.initialize(keyLength);
    // 生成秘钥并返回
    return keyGenerator.genKeyPair();
  }


  public static byte[] toPKCS1PrivateKey(KeyPair pair) throws IOException {
    PrivateKey priv = pair.getPrivate();
    byte[] privBytes = priv.getEncoded();

    PrivateKeyInfo pkInfo = PrivateKeyInfo.getInstance(privBytes);
    ASN1Encodable encodable = pkInfo.parsePrivateKey();
    ASN1Primitive primitive = encodable.toASN1Primitive();
    return primitive.getEncoded();
  }

  public static String PrivatePKCS1ToPEM(byte[] privateKeyPKCS1) throws IOException {
    PemObject pemObject = new PemObject("RSA PRIVATE KEY", privateKeyPKCS1);
    StringWriter stringWriter = new StringWriter();
    PemWriter pemWriter = new PemWriter(stringWriter);
    pemWriter.writeObject(pemObject);
    pemWriter.close();
    return stringWriter.toString();
  }
  public static byte[] toPKCS1PublicKey(KeyPair pair) throws IOException {
    PublicKey pub = pair.getPublic();
    byte[] pubBytes = pub.getEncoded();

    SubjectPublicKeyInfo spkInfo = SubjectPublicKeyInfo.getInstance(pubBytes);
    ASN1Primitive primitive = spkInfo.parsePublicKey();
    return primitive.getEncoded();
  }


  public static String PublicPKCS1ToPEM(byte[] publicKeyPKCS1) throws IOException {
    PemObject pemObject = new PemObject("RSA PUBLIC KEY", publicKeyPKCS1);
    StringWriter stringWriter = new StringWriter();
    PemWriter pemWriter = new PemWriter(stringWriter);
    pemWriter.writeObject(pemObject);
    pemWriter.close();
    return stringWriter.toString();
  }

    /**
     * 生产秘钥, 默认秘钥长度1024
     *
     * @return the secretKey
     * @throws Exception
     */
  public static KeyPair generateKey() throws NoSuchAlgorithmException {
    return generateKey(1024);
  }
  /**
   *  将公钥的byte[]数据还原为PublicKey
   * @param publicKeyBytes
   * @return PublicKey
   * @throws Exception
   */
  public static PublicKey getPublicKey(byte[] publicKeyBytes) throws Exception{
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    return keyFactory.generatePublic(keySpec);
  }
  /**
   * 将私钥的byte[]数据还原为PrivateKey
   * @param privateKey
   * @return PrivateKey
   * @throws Exception
   */
  public static PrivateKey getPrivateKey(byte[] privateKey) throws Exception{
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKey);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    return keyFactory.generatePrivate(keySpec);
  }

  /**
   * 用公钥加密,默认填充模式 RSA/ECB/PKCS1Padding
   * <br>每次加密的字节数，不能超过密钥的长度值减去11
   *
   * @param data  需加密数据的byte数据
   * @param publicKey 公钥
   * @return 加密后的byte型数据
   */
  public static byte[] encrypt(byte[] data, Key publicKey) throws Exception {
    return encrypt(data, publicKey, RSA_TRANSFORM);
  }

  /**
   * 用公钥加密
   * <br>每次加密的字节数，不能超过密钥的长度值减去11
   *
   * @param data  需加密数据的byte数据
   * @param publicKey 公钥
   * @param transformation 加密模式和填充方式
   * @return 加密后的byte型数据
   */
  public static byte[] encrypt(byte[] data, Key publicKey, String transformation) throws Exception {
    Cipher cipher = Cipher.getInstance(transformation);
    // 编码前设定编码方式及密钥
    cipher.init(Cipher.ENCRYPT_MODE, publicKey);
    // 传入编码数据并返回编码结果
    return cipher.doFinal(data);
  }

  /**
   * 用私钥解密，默认加密模式和填充方式为 RSA/ECB/PKCS1Padding"
   *
   * @param encryptedData 经过encrypt()加密返回的byte数据
   * @param privateKey 私钥
   * @return
   */
  public static byte[] decrypt(byte[] encryptedData, PrivateKey privateKey) throws Exception {
    return decrypt(encryptedData, privateKey,RSA_TRANSFORM);
  }

  /**
   * 用私钥解密
   *
   * @param encryptedData 经过encrypt()加密返回的byte数据
   * @param privateKey 私钥
   * @param transformation 加密模式和填充方式
   * @return
   */
  public static byte[] decrypt(byte[] encryptedData, PrivateKey privateKey, String transformation) throws Exception {
    Cipher cipher = Cipher.getInstance(transformation);
    cipher.init(Cipher.DECRYPT_MODE, privateKey);
    return cipher.doFinal(encryptedData);
  }




}
