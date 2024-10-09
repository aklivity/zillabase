package io.aklivity.zillabase;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Iterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.risingwave.functions.DataTypeHint;
import com.risingwave.functions.PeriodDuration;
import com.risingwave.functions.ScalarFunction;
import com.risingwave.functions.TableFunction;
import com.risingwave.functions.UdfServer;

public class UdfExample
{

    public static class Int42 implements ScalarFunction
    {
        public int eval() {
            return 42;
        }
    }

    public static class Gcd implements ScalarFunction
    {
        public int eval(int a, int b)
        {
            while (b != 0)
            {
                int temp = b;
                b = a % b;
                a = temp;
            }
            return a;
        }
    }

    public static class Gcd3 implements ScalarFunction
    {
        public int eval(int a, int b, int c)
        {
            var gcd = new Gcd();
            return gcd.eval(gcd.eval(a, b), c);
        }
    }

    public static class ExtractTcpInfo implements ScalarFunction
    {
        public static class TcpPacketInfo
        {
            public String srcAddr;
            public String dstAddr;
            public short srcPort;
            public short dstPort;
        }

        public TcpPacketInfo eval(byte[] tcpPacket)
        {
            var info = new TcpPacketInfo();
            var buffer = ByteBuffer.wrap(tcpPacket);
            info.srcAddr = intToIpAddr(buffer.getInt(12));
            info.dstAddr = intToIpAddr(buffer.getInt(16));
            info.srcPort = buffer.getShort(20);
            info.dstPort = buffer.getShort(22);
            return info;
        }

        static String intToIpAddr(int addr)
        {
            return String.format(
                    "%d.%d.%d.%d",
                    (addr >> 24) & 0xff, (addr >> 16) & 0xff, (addr >> 8) & 0xff, addr & 0xff);
        }
    }

    public static class HexToDec implements ScalarFunction
    {
        public BigDecimal eval(String hex)
        {
            if (hex == null)
            {
                return null;
            }
            return new BigDecimal(new BigInteger(hex, 16));
        }
    }

    public static class ArrayAccess implements ScalarFunction
    {
        public String eval(String[] array, int index) {
            return array[index - 1];
        }
    }

    public static class JsonbConcat implements ScalarFunction
    {
        public @DataTypeHint("JSONB") String eval(@DataTypeHint("JSONB[]") String[] jsons)
        {
            if (jsons == null)
            {
                return null;
            }
            return "[" + String.join(",", jsons) + "]";
        }
    }

    public static class JsonbArrayIdentity implements ScalarFunction
    {
        public @DataTypeHint("JSONB[]") String[] eval(@DataTypeHint("JSONB[]") String[] jsons) {
            return jsons;
        }
    }

    public static class JsonbArrayStructIdentity implements ScalarFunction
    {
        public static class Row
        {
            public @DataTypeHint("JSONB[]") String[] v;
            public int len;
        }

        public Row eval(Row s) {
            return s;
        }
    }

    public static class ReturnAll implements ScalarFunction
    {
        public static class Row
        {
            public Boolean bool;
            public Short i16;
            public Integer i32;
            public Long i64;
            public Float f32;
            public Double f64;
            public BigDecimal decimal;
            public LocalDate date;
            public LocalTime time;
            public LocalDateTime timestamp;
            public PeriodDuration interval;
            public String str;
            public byte[] bytes;
            public @DataTypeHint("JSONB") String jsonb;
        }

        public Row eval(
                Boolean bool,
                Short i16,
                Integer i32,
                Long i64,
                Float f32,
                Double f64,
                BigDecimal decimal,
                LocalDate date,
                LocalTime time,
                LocalDateTime timestamp,
                PeriodDuration interval,
                String str,
                byte[] bytes,
                @DataTypeHint("JSONB") String jsonb)
        {
            var row = new Row();
            row.bool = bool;
            row.i16 = i16;
            row.i32 = i32;
            row.i64 = i64;
            row.f32 = f32;
            row.f64 = f64;
            row.decimal = decimal;
            row.date = date;
            row.time = time;
            row.timestamp = timestamp;
            row.interval = interval;
            row.str = str;
            row.bytes = bytes;
            row.jsonb = jsonb;
            return row;
        }
    }

    public static class ReturnAllArrays implements ScalarFunction
    {
        public static class Row
        {
            public Boolean[] bool;
            public Short[] i16;
            public Integer[] i32;
            public Long[] i64;
            public Float[] f32;
            public Double[] f64;
            public BigDecimal[] decimal;
            public LocalDate[] date;
            public LocalTime[] time;
            public LocalDateTime[] timestamp;
            public PeriodDuration[] interval;
            public String[] str;
            public byte[][] bytes;
            public @DataTypeHint("JSONB[]") String[] jsonb;
        }

        public Row eval(
                Boolean[] bool,
                Short[] i16,
                Integer[] i32,
                Long[] i64,
                Float[] f32,
                Double[] f64,
                BigDecimal[] decimal,
                LocalDate[] date,
                LocalTime[] time,
                LocalDateTime[] timestamp,
                PeriodDuration[] interval,
                String[] str,
                byte[][] bytes,
                @DataTypeHint("JSONB[]") String[] jsonb)
        {
            var row = new Row();
            row.bool = bool;
            row.i16 = i16;
            row.i32 = i32;
            row.i64 = i64;
            row.f32 = f32;
            row.f64 = f64;
            row.decimal = decimal;
            row.date = date;
            row.time = time;
            row.timestamp = timestamp;
            row.interval = interval;
            row.str = str;
            row.bytes = bytes;
            row.jsonb = jsonb;
            return row;
        }
    }

    public static class Series implements TableFunction
    {
        public Iterator<Integer> eval(int n) {
            return IntStream.range(0, n).iterator();
        }
    }

    public static class Split implements TableFunction
    {
        public static class Row
        {
            public String word;
            public int length;
        }

        public Iterator<Row> eval(String str)
        {
            return Stream.of(str.split(" "))
                    .map(
                            s -> {
                                Row row = new Row();
                                row.word = s;
                                row.length = s.length();
                                return row;
                            })
                    .iterator();
        }
    }
}
