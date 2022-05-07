package io.arcblock.walletkit.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Nate Gu on 2019/2/16
 */
public class ArcSha2Hasher {

  private ArcSha2Hasher() {
    throw new UnsupportedOperationException("u can't instantiate me...");
  }

  /**
   * default with sha256 and the round = 2
   */
  public static byte[] sha(byte[] input) {
    return sha256(input, 2);
  }


  /**
   * Return the bytes of hash encryption.
   *
   * @param data The data.
   * @param algorithm The name of hash encryption.
   * @return the bytes of hash encryption
   */
  private static byte[] hashTemplate(final byte[] data, final String algorithm) {
    if (data == null || data.length <= 0) return null;
    try {
      MessageDigest md = MessageDigest.getInstance(algorithm);
      md.update(data);
      return md.digest();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * sha224 with custom round
   */
  public static byte[] sha224ForJavaTest(byte[] input, int round) {
    if (round < 1) {
      throw new RuntimeException("round can't less than 1");
    }
    if (round == 1) {
      return encryptSHA224ForJavaTest(input);
    } else {
      return sha224ForJavaTest(encryptSHA224ForJavaTest(input), round - 1);
    }
  }

  public static byte[] encryptSHA224ForJavaTest(final byte[] data) {
    return hashTemplate(data, "SHA-224");
  }

  /**
   * default round = 2
   */
  public static byte[] sha256(byte[] input) {
    return sha256(input, 2);
  }

  /**
   * sha256 with custom round
   */
  public static byte[] sha256(byte[] input, int round) {
    if (round < 1) {
      throw new RuntimeException("round can't less than 1");
    }
    if (round == 1) {
      return encryptSHA256(input);
    } else {
      return sha256(encryptSHA256(input), round - 1);
    }
  }

  public static byte[] encryptSHA256(final byte[] data) {
    return hashTemplate(data, "SHA-256");
  }

  /**
   * Return the hex string of SHA256 encryption.
   *
   * @param data The data.
   * @return the hex string of SHA256 encryption
   */
  //public static String encryptSHA256ToString(final String data) {
  //  if (data == null || data.length() == 0) return "";
  //  return encryptSHA256ToString(data.getBytes());
  //}

  /**
   * default round = 2
   */
  public static byte[] sha384(byte[] input) {
    return sha384(input, 2);
  }

  /**
   * sha384 with custom round
   */
  public static byte[] sha384(byte[] input, int round) {
    if (round < 1) {
      throw new RuntimeException("round can't less than 1");
    }
    if (round == 1) {
      return encryptSHA384(input);
    } else {
      return sha384(encryptSHA384(input), round - 1);
    }
  }

  public static byte[] encryptSHA384(final byte[] data) {
    return hashTemplate(data, "SHA-384");
  }

  /**
   * default round = 2
   */
  public static byte[] sha512(byte[] input) {
    return sha512(input, 2);
  }

  /**
   * sha512 with custom round
   */
  public static byte[] sha512(byte[] input, int round) {
    if (round < 1) {
      throw new RuntimeException("round can't less than 1");
    }
    if (round == 1) {
      return encryptSHA512(input);
    } else {
      return sha512(encryptSHA512(input), round - 1);
    }
  }

  public static byte[] encryptSHA512(final byte[] data) {
    return hashTemplate(data, "SHA-512");
  }
}
