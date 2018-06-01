package com.flipkart.chatheads.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

class Strings {
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    private static <T> String join(final String delimiter, final Collection<T> objs) {
      if (objs == null || objs.isEmpty())
        return "";

      final Iterator<T> iter = objs.iterator();
      final StringBuilder buffer = new StringBuilder(Strings.toString(iter.next()));

      while (iter.hasNext()) {
          final T obj = iter.next();
          if(notEmpty(obj)) buffer.append(delimiter).append(Strings.toString(obj));
      }
      return buffer.toString();
    }

    @SafeVarargs
    private static <T> String join(final String delimiter, final T... objects) {
        return join(delimiter, Arrays.asList(objects));
    }

    private static String toString(InputStream input) {
        StringWriter sw = new StringWriter();
        copy( new InputStreamReader(input), sw);
        return sw.toString();
    }

    private static String toString(Reader input) {
        StringWriter sw = new StringWriter();
        copy(input, sw);
        return sw.toString();
    }

    private static int copy(Reader input, Writer output) {
        long count = copyLarge(input, output);
        return count > Integer.MAX_VALUE ? -1 : (int)count;
    }

    private static long copyLarge(Reader input, Writer output) throws RuntimeException {
        try {
            char[] buffer = new char[DEFAULT_BUFFER_SIZE];
            long count = 0;
            int n;
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
                count += n;
            }
            return count;
        } catch( IOException e ) {
            throw new RuntimeException(e);
        }
    }

    public static String toString( final Object o ) {
        return toString(o,"");
    }

    private static String toString(final Object o, final String def) {
        return o==null ? def :
                o instanceof InputStream ? toString((InputStream)o) :
                o instanceof Reader ? toString((Reader)o) :
                o instanceof Object[] ? Strings.join(", ",(Object[])o) :
                o instanceof Collection ? Strings.join(", ", (Collection<?>)o) : o.toString();
    }

    private static boolean notEmpty(final Object o) {
        return toString(o).trim().length()!=0;
    }

}
