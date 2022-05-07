package io.arcblock.walletkit.utils;

/**
 * Created by Nate Gu on 2019/2/16
 */
public class ConvertUtils {

  public static String bytes2hex(byte[] bytes) {
    char[] table = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    int length = bytes.length;
    char[] temp = new char[length << 1];
    int i = 0;
    int var = 0;
    while (i < length) {
      temp[var++] = table[(240 & bytes[i]) >>> 4];
      temp[var++] = table[15 & bytes[i++]];
    }
    return new String(temp);
  }
}
