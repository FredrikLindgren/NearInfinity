// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.awt.Dimension;
import java.awt.Font;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.JComponent;

import org.infinity.NearInfinity;

/**
 * A general-purpose class containing useful function not fitting elsewhere.
 */
public class Misc
{
  /** The default ANSI charset (Windows-1252). */
  public static final Charset CHARSET_DEFAULT = Charset.forName("windows-1252");
  /** The UTF-8 charset. */
  public static final Charset CHARSET_UTF8    = Charset.forName("UTF-8");
  /** The US-ASCII charset. */
  public static final Charset CHARSET_ASCII   = Charset.forName("US-ASCII");

  /** Returns the line separator string which is used by the current operating system. */
  public static final String LINE_SEPARATOR = System.getProperty("line.separator");

  /** Can be used to slightly expand dialog message strings to force a bigger initial dialog width. */
  public static final String MSG_EXPAND_SMALL = "        \t";
  /** Can be used to expand dialog message strings to force a bigger initial dialog width. */
  public static final String MSG_EXPAND_MEDIUM = "                \t";
  /** Can be used to greatly expand dialog message strings to force a bigger initial dialog width. */
  public static final String MSG_EXPAND_LARGE = "                                \t";

  /**
   * Returns a comparator that compares the string representation of the specified objects
   * in a case-insensitive way. */
  public static <T> Comparator<T> getIgnoreCaseComparator()
  {
    return new Comparator<T>() {
      @Override
      public int compare(T o1, T o2)
      {
        return (o1.toString().compareToIgnoreCase(o2.toString()));
      }

      @Override
      public boolean equals(Object obj)
      {
        return toString().equalsIgnoreCase(obj.toString());
      }
    };
  }

  /**
   * Attempts to detect the character set of the text data in the specified byte buffer.
   * @param data Text data as byte array.
   * @return The detected character set or the ANSI charset "windows-1252"
   *         if autodetection was not successful.
   */
  public static Charset detectCharset(byte[] data)
  {
    return detectCharset(data, CHARSET_DEFAULT);
  }

  /**
   * Attempts to detect the character set of the text data in the specified byte buffer.
   * @param data Text data as byte array.
   * @param defaultCharset The default charset to return if autodetection is not successful.
   *                       (Default: windows-1252)
   * @return The detected character set or {@code defaultCharset}
   *         if autodetection was not successful.
   */
  public static Charset detectCharset(byte[] data, Charset defaultCharset)
  {
    if (defaultCharset == null) {
      defaultCharset = CHARSET_DEFAULT;
    }

    Charset retVal = null;
    if (data != null) {
      if (data.length >= 3 &&
          data[0] == -17 && data[1] == -69 && data[2] == -65) { // UTF-8 BOM (0xef, 0xbb, 0xbf)
        retVal = Charset.forName("utf-8");
      } else if (data.length >= 2 &&
                 data[0] == -2 && data[1] == -1) {  // UTF-16 BOM (0xfeff) in big-endian order
        retVal = Charset.forName("utf-16be");
      } else if (data.length >= 2 &&
                 data[0] == -1 && data[1] == -2) {  // UTF-16 BOM (0xfeff) in little-endian order
        retVal = Charset.forName("utf-16le");
      }
    }

    if (retVal == null) {
      retVal = defaultCharset;
    }

    return retVal;
  }


  /**
   * Attempts to convert the specified string into a numeric value. Returns defValue of value does
   * not contain a valid number.
   */
  public static int toNumber(String value, int defValue)
  {
    if (value != null) {
      try {
        return Integer.parseInt(value);
      } catch (NumberFormatException e) {
      }
    }
    return defValue;
  }

  /**
   * Attempts to convert the specified string of given base "radix" into a numeric value.
   * Returns defValue of value does not contain a valid number.
   */
  public static int toNumber(String value, int radix, int defValue)
  {
    if (value != null) {
      try {
        return Integer.parseInt(value, radix);
      } catch (NumberFormatException e) {
      }
    }
    return defValue;
  }

  /** Swaps byte order of the specified short value. */
  public static final short swap16(short v)
  {
    return (short)(((v & 0xff) << 8) | ((v >> 8) & 0xff));
  }

  /** Swaps byte order of the specified int value. */
  public static final int swap32(int v)
  {
    return ((v << 24) & 0xff000000) |
        ((v << 8)  & 0x00ff0000) |
        ((v >> 8)  & 0x0000ff00) |
        ((v >> 24) & 0x000000ff);
  }

  /** Swaps byte order of the specified long value. */
  public static final long swap64(long v)
  {
    return ((v << 56) & 0xff00000000000000L) |
           ((v << 40) & 0x00ff000000000000L) |
           ((v << 24) & 0x0000ff0000000000L) |
           ((v << 8)  & 0x000000ff00000000L) |
           ((v >> 8)  & 0x00000000ff000000L) |
           ((v >> 24) & 0x0000000000ff0000L) |
           ((v >> 40) & 0x000000000000ff00L) |
           ((v >> 56) & 0x00000000000000ffL);
  }

  /** Swaps byte order of every short value in the specified array. */
  public static final void swap(short[] array)
  {
    if (array != null) {
      for (int i = 0, cnt = array.length; i < cnt; i++) {
        array[i] = swap16(array[i]);
      }
    }
  }

  /** Swaps byte order of every int value in the specified array. */
  public static final void swap(int[] array)
  {
    if (array != null) {
      for (int i = 0, cnt = array.length; i < cnt; i++) {
        array[i] = swap32(array[i]);
      }
    }
  }

  /** Swaps byte order of every long value in the specified array. */
  public static final void swap(long[] array)
  {
    if (array != null) {
      for (int i = 0, cnt = array.length; i < cnt; i++) {
        array[i] = swap64(array[i]);
      }
    }
  }

  /** Converts a short value into a byte array (little-endian). */
  public static final byte[] shortToArray(short value)
  {
    return new byte[]{(byte)(value & 0xff),
                      (byte)((value >> 8) & 0xff)};
  }

  /** Converts an int value into a byte array (little-endian). */
  public static final byte[] intToArray(int value)
  {
    return new byte[]{(byte)(value & 0xff),
                      (byte)((value >> 8) & 0xff),
                      (byte)((value >> 16) & 0xff),
                      (byte)((value >> 24) & 0xff)};
  }

  /** Converts a long value into a byte array (little-endian). */
  public static final byte[] longToArray(long value)
  {
    return new byte[]{(byte)(value & 0xffL),
                      (byte)((value >> 8) & 0xffL),
                      (byte)((value >> 16) & 0xffL),
                      (byte)((value >> 24) & 0xffL),
                      (byte)((value >> 32) & 0xffL),
                      (byte)((value >> 40) & 0xffL),
                      (byte)((value >> 48) & 0xffL),
                      (byte)((value >> 56) & 0xffL)};
  }

  /**
   * Sign-extends the specified {@code int} value consisting of the specified number of significant bits.
   * @param value The {@code int} value to sign-extend.
   * @param bits Size of {@code value} in bits.
   * @return A sign-extended version of {@code value}.
   */
  public static final int signExtend(int value, int bits)
  {
    return (value << (32 - bits)) >> (32 - bits);
  }

  /**
   * Sign-extends the specified {@code long} value consisting of the specified number of significant bits.
   * @param value The {@code long} value to sign-extend.
   * @param bits Size of {@code value} in bits.
   * @return A sign-extended version of {@code value}.
   */
  public static final long signExtend(long value, int bits)
  {
    return (value << (64 - bits)) >> (64 - bits);
  }


  /**
   * Creates a thread pool with a pool size depending on the number of available CPU cores.<br>
   * <br>
   * <b>numThreads:</b>   Number of available CPU cores.<br>
   * <b>maxQueueSize:</b> 2 x {@code numThreads}.<br>
   * @return A ThreadPoolExecutor instance.
   */
  public static ThreadPoolExecutor createThreadPool()
  {
    int numThreads = Runtime.getRuntime().availableProcessors();
    return createThreadPool(numThreads, numThreads*2);
  }

  /**
   * Creates a thread pool with the specified parameters.
   * @param numThreads Max. number of parallel threads to execute. Must be >= 1.
   * @param maxQueueSize Max. size of the working queue. Must be >= {@code numThreads}.
   * @return A ThreadPoolExecutor instance.
   */
  public static ThreadPoolExecutor createThreadPool(int numThreads, int maxQueueSize)
  {
    numThreads = Math.max(1, numThreads);
    maxQueueSize = Math.max(numThreads, maxQueueSize);
    return new ThreadPoolExecutor(numThreads, numThreads, 0L, TimeUnit.MILLISECONDS,
                                  new ArrayBlockingQueue<Runnable>(maxQueueSize));
  }

  /**
   * Helper routine which can be used to check or block execution of new threads while the
   * blocking queue is full.
   * @param executor The executor to query.
   * @param block Specify {@code true} to block execution as long as the queue is full.
   * @param maxWaitMs Specify max. time to block queue, in milliseconds. Specify -1 to block indefinitely.
   * @return {@code true} if queue is ready for new elements, {@code false} otherwise.
   */
  public static boolean isQueueReady(ThreadPoolExecutor executor, boolean block, int maxWaitMs)
  {
    if (executor != null) {
      if (block) {
        if (maxWaitMs < 0) { maxWaitMs = Integer.MAX_VALUE; }
        int curWaitMs = 0;
        while (curWaitMs < maxWaitMs && executor.getQueue().size() > executor.getCorePoolSize()) {
          try { Thread.sleep(1); } catch (InterruptedException e) {}
          curWaitMs++;
        }
      }
      return executor.getQueue().size() <= executor.getCorePoolSize();
    }
    return false;
  }

  /**
   * Returns a prototype dimension object based on the height of {@code c} and the width of (@code prototype}.
   * @param c The component to derive height and properties for calculating width.
   * @param prototype The prototype string used to derive width.
   * @return The {@link Dimension} object with calculated width and height.
   */
  public static Dimension getPrototypeSize(JComponent c, String prototype)
  {
    Dimension d = null;
    if (c != null) {
      d = new Dimension();
      d.height = c.getPreferredSize().height;
      d.width = c.getFontMetrics(c.getFont()).stringWidth(prototype);
    }
    return d;
  }

  /**
   * Returns the specified font scaled to the global font scale value.
   * @param font The font to scale.
   * @return The scaled font.
   */
  public static Font getScaledFont(Font font)
  {
    int scale = (NearInfinity.getInstance() != null) ? NearInfinity.getInstance().getGlobalFontSize() : 100;
    return getScaledFont(font, scale);
  }

  /**
   * Returns the specified font scaled to the specified scale value.
   * @param font The font to scale.
   * @param scale The scale factor (in percent).
   * @return The scaled font.
   */
  public static Font getScaledFont(Font font, int scale)
  {
    Font ret = null;
    if (font != null) {
      ret = (scale != 100) ? font.deriveFont(font.getSize2D() * scale / 100.0f) : font;
    }
    return ret;
  }

  /**
   * Returns the specified Dimension structure scaled to the global font scale value.
   * @param dim The Dimension structure to scale.
   * @return The scaled Dimension structure.
   */
  public static Dimension getScaledDimension(Dimension dim)
  {
    Dimension ret = null;
    if (dim != null) {
      int scale = 100;
      if (NearInfinity.getInstance() != null) {
        scale = NearInfinity.getInstance().getGlobalFontSize();
      }
      ret = (scale != 100) ? new Dimension(dim.width * scale / 100, dim.height * scale / 100) : dim;
    }
    return ret;
  }

  /**
   * Returns the specified numeric value scaled to the global font scale value.
   * @param value The numeric value to scale.
   * @return The scaled value.
   */
  public static float getScaledValue(float value)
  {
    float scale = (NearInfinity.getInstance() != null) ? NearInfinity.getInstance().getGlobalFontSize() : 100.0f;
    return value * scale / 100.0f;
  }

  /**
   * Returns the specified numeric value scaled to the global font scale value.
   * @param value The numeric value to scale.
   * @return The scaled value.
   */
  public static int getScaledValue(int value)
  {
    int scale = (NearInfinity.getInstance() != null) ? NearInfinity.getInstance().getGlobalFontSize() : 100;
    return value * scale / 100;
  }

  /**
   * Attempts to format the specified symbolic name, so that it becomes easier to
   * read. E.g. by replaceing underscores by spaces, or using an appropriate mix of upper/lower case
   * characters.
   * @param symbol The symbolic name to convert.
   * @return A prettified version of the symbolic name.
   */
  public static String prettifySymbol(String symbol)
  {
    if (symbol != null) {
      StringBuilder sb = new StringBuilder(symbol);
      boolean upper = true;
      for (int idx = 0, len = sb.length(); idx < len; idx++) {
        char ch = sb.charAt(idx);
        if (upper) {
          ch = Character.toUpperCase(ch);
          upper = false;
        } else {
          ch = Character.toLowerCase(ch);
        }
        if (" ,-_".indexOf(ch) >= 0) {
          if (ch == '_') ch =  ' ';
          if (ch == '-') {
            sb.insert(idx, " -");
            ch = ' ';
            idx += 2;
            len += 2;
          }
          upper = true;
        }
        sb.setCharAt(idx, ch);
      }
      symbol = sb.toString();
    }
    return symbol;
  }

  // Contains static functions only
  private Misc() {}
}
