package io.arcblock.walletkit.utils;

/**
 * Created by Nate Gu on 2019/2/16
 */
public class ArrayUtils {
  public static void arrayCopy(byte[] src, int srcPos, byte[] dest, int destPos, int length) {
    if (length >= 0) System.arraycopy(src, srcPos, dest, destPos, length);
  }

  public static void arrayCopy(int[] src, int srcPos, int[] dest, int destPos, int length) {
    if (length >= 0) System.arraycopy(src, srcPos, dest, destPos, length);
  }

  public static void arrayCopy(long[] src, int srcPos, long[] dest, int destPos, int length) {
    if (length >= 0) System.arraycopy(src,  srcPos, dest, destPos, length);
  }

  public static void fill(byte[] a, byte val) {
    for (int i = 0, len = a.length; i < len; i++) a[i] = val;
  }

  public static void fill(int[] a, int val) {
    for (int i = 0, len = a.length; i < len; i++) a[i] = val;
  }

  public static void fill(long[] a, long val) {
    for (int i = 0, len = a.length; i < len; i++) a[i] = val;
  }
}
