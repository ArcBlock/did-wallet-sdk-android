package io.arcblock.walletkit.utils;

/**
 * Created by Nate Gu on 2019/2/16
 */
public class BytesUtils {
  /**
   * Convert a byte array to an int array
   *
   * @param in the byte array to read from
   * @param inOfs The offset into the input byte array.
   * @param out The array to store the result.
   * @param outOfs The index of the first element of out to be written.
   * @param len The number of bytes to convert.
   */
  public static void b2iLittle(byte[] in, int inOfs, int[] out, int outOfs, int len) {
    if ((inOfs < 0) || ((in.length - inOfs) < len) ||
        (outOfs < 0) || ((out.length - outOfs) < len / 4)) {
      throw new ArrayIndexOutOfBoundsException();
    }
    len += inOfs;
    while (inOfs < len) {
      out[outOfs++] = ((in[inOfs] & 0xff))
          | ((in[inOfs + 1] & 0xff) << 8)
          | ((in[inOfs + 2] & 0xff) << 16)
          | ((in[inOfs + 3]) << 24);
      inOfs += 4;
    }
  }

  /**
   * Given a byte array and an offset, convert the bytes to a long array
   *
   * @param in the byte array to read from
   * @param inOfs The offset into the input byte array.
   * @param out the array to write the results to
   * @param outOfs The index of the first element of out to be written.
   * @param len The number of bytes to convert.
   */
  public static void b2iLittle(byte[] in, int inOfs, long[] out, int outOfs, int len) {
    if ((inOfs < 0) || ((in.length - inOfs) < len) ||
        ((outOfs < 0) || (out.length - outOfs) < len / 8)) {
      throw new ArrayIndexOutOfBoundsException();
    }

    len += inOfs;
    while (inOfs < len) {
      out[outOfs++] = ((in[inOfs] & 0xffL)
          | ((in[inOfs + 1] & 0xffL) << 8)
          | ((in[inOfs + 2] & 0xffL) << 16)
          | ((in[inOfs + 3] & 0xffL) << 24)
          | ((in[inOfs + 4] & 0xffL) << 32)
          | ((in[inOfs + 5] & 0xffL) << 40)
          | ((in[inOfs + 6] & 0xffL) << 48)
          | ((in[inOfs + 7] & 0xffL) << 56));
      inOfs += 8;
    }
  }

  /**
   * Convert a byte array to an int array
   *
   * @param in the byte array to read from
   * @param inOfs The offset into the input byte array.
   * @param out the array to write the results to
   * @param outOfs The index of the first element of out that will be written.
   * @param len The length of the input array.
   */
  public static void b2iBig(byte[] in, int inOfs, int[] out, int outOfs, int len) {
    if ((inOfs < 0) || ((in.length - inOfs) < len) ||
        (outOfs < 0) || ((out.length - outOfs) < len / 4)) {
      throw new ArrayIndexOutOfBoundsException();
    }
    len += inOfs;
    while (inOfs < len) {
      out[outOfs++] = ((in[inOfs + 3] & 0xff))
          | ((in[inOfs + 2] & 0xff) << 8)
          | ((in[inOfs + 1] & 0xff) << 16)
          | ((in[inOfs]) << 24);
      inOfs += 4;
    }
  }

  /**
   * Convert a byte array to a long array
   *
   * @param in the byte array to read from
   * @param inOfs The offset into the input byte array.
   * @param out the array to write the results to
   * @param outOfs The index of the first element of out that is to be written.
   * @param len The number of bytes to be converted.
   */
  public static void b2iBig(byte[] in, int inOfs, long[] out, int outOfs, int len) {
    if ((inOfs < 0) || ((in.length - inOfs) < len) ||
        (outOfs < 0) || ((out.length - outOfs) < len / 8)) {
      throw new ArrayIndexOutOfBoundsException();
    }
    len += inOfs;
    while (inOfs < len) {
      int i1 = ((in[inOfs + 3] & 0xff))
          | ((in[inOfs + 2] & 0xff) << 8)
          | ((in[inOfs + 1] & 0xff) << 16)
          | ((in[inOfs]) << 24);
      inOfs += 4;
      int i2 = ((in[inOfs + 3] & 0xff))
          | ((in[inOfs + 2] & 0xff) << 8)
          | ((in[inOfs + 1] & 0xff) << 16)
          | ((in[inOfs]) << 24);
      out[outOfs++] = ((long) i1 << 32) | (i2 & 0xffffffffL);
      inOfs += 4;
    }
  }

  /**
   * Convert a block of 4-byte integers to a block of 4-byte bytes
   *
   * @param in the input array
   * @param inOfs The offset into the input array.
   * @param out the byte array to write to
   * @param outOfs The offset into the output array where the first byte of the
   * @param len The number of bytes to be converted.
   */
  public static void i2bLittle(int[] in, int inOfs, byte[] out, int outOfs, int len) {
    if ((inOfs < 0) || ((in.length - inOfs) < len / 4) ||
        (outOfs < 0) || ((out.length - outOfs) < len)) {
      throw new ArrayIndexOutOfBoundsException();
    }
    len += outOfs;
    while (outOfs < len) {
      int i = in[inOfs++];
      out[outOfs++] = (byte) (i);
      out[outOfs++] = (byte) (i >> 8);
      out[outOfs++] = (byte) (i >> 16);
      out[outOfs++] = (byte) (i >> 24);
    }
  }

  /**
   * Convert a long array to a byte array, using little-endian byte order
   *
   * @param in the long[] to read from
   * @param inOfs The offset into the input array.
   * @param out the byte array to write to
   * @param outOfs The offset into the output array where the first byte of the
   * @param len The number of bytes to be converted.
   */
  public static void i2bLittle(long[] in, int inOfs, byte[] out, int outOfs, int len) {
    if ((inOfs < 0) || ((in.length - inOfs) < len / 8) ||
        (outOfs < 0) || ((out.length - outOfs) < len)) {
      throw new ArrayIndexOutOfBoundsException();
    }
    len += outOfs;
    while (outOfs < len) {
      long i = in[inOfs++];
      out[outOfs++] = (byte) (i);
      out[outOfs++] = (byte) (i >> 8);
      out[outOfs++] = (byte) (i >> 16);
      out[outOfs++] = (byte) (i >> 24);
      out[outOfs++] = (byte) (i >> 32);
      out[outOfs++] = (byte) (i >> 40);
      out[outOfs++] = (byte) (i >> 48);
      out[outOfs++] = (byte) (i >> 56);
    }
  }



  public static void i2bBig(int[] in, int inOfs, byte[] out, int outOfs, int len) {
    if ((inOfs < 0) || ((in.length - inOfs) < len / 4) ||
        (outOfs < 0) || ((out.length - outOfs) < len)) {
      throw new ArrayIndexOutOfBoundsException();
    }
    len += outOfs;
    while (outOfs < len) {
      int i = in[inOfs++];
      out[outOfs++] = (byte) (i >> 24);
      out[outOfs++] = (byte) (i >> 16);
      out[outOfs++] = (byte) (i >> 8);
      out[outOfs++] = (byte) (i);
    }
  }

  public static void i2bBig(long[] in, int inOfs, byte[] out, int outOfs, int len) {
    if ((inOfs < 0) || ((in.length - inOfs) < len / 8) ||
        (outOfs < 0) || ((out.length - outOfs) < len)) {
      throw new ArrayIndexOutOfBoundsException();
    }
    len += outOfs;
    while (outOfs < len) {
      long i = in[inOfs++];
      out[outOfs++] = (byte) (i >> 56);
      out[outOfs++] = (byte) (i >> 48);
      out[outOfs++] = (byte) (i >> 40);
      out[outOfs++] = (byte) (i >> 32);
      out[outOfs++] = (byte) (i >> 24);
      out[outOfs++] = (byte) (i >> 16);
      out[outOfs++] = (byte) (i >> 8);
      out[outOfs++] = (byte) (i);

    }
  }


}
