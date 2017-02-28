// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   NumberUtils.java

package ciugen.utils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *  * This file is part of TWIMExtract
 *
 */
public class NumberUtils
{

    private NumberUtils()
    {
    }

    public static float roundNumber(float fUnrounded)
    {
        return roundNumber(fUnrounded, 4);
    }

    public static float roundNumber(float fUnrounded, int nDecimalPlaces)
    {
        return (float)roundNumber((double)fUnrounded, nDecimalPlaces);
    }

    public static double roundNumber(double dUnrounded)
    {
        return roundNumber(dUnrounded, 4);
    }

    public static double roundNumber(double dUnrounded, int nDecimalPlaces)
    {
        double d1 = Math.pow(10D, nDecimalPlaces);
        double d2 = dUnrounded * d1 + 0.5D;
        long n = (long)d2;
        return (double)n / d1;
    }

    public static int compareTo(double thisd1, double extd2)
    {
        int i = 1;
        if(thisd1 < extd2)
            i = -1;
        if(thisd1 == extd2)
            i = 0;
        return i;
    }

    public static double[] convert(byte bytes[], int precision, ByteOrder byteOrder)
        throws IOException
    {
        int FLOAT_PRECISION = 4;
        if(byteOrder.equals(ByteOrder.LITTLE_ENDIAN))
            bytes = convertToBigEndian(bytes, precision);
        DataInputStream is = new DataInputStream(new ByteArrayInputStream(bytes));
        double values[] = new double[bytes.length / precision];
        for(int i = 0; i < values.length; i++)
            values[i] = precision != 4 ? is.readDouble() : is.readFloat();

        return values;
    }

    public static byte[] convert(double values[])
    {
        int DOUBLE_PRECISION = 8;
        ByteBuffer buffer = ByteBuffer.allocate(values.length * 8);
        for(int i = 0; i < values.length; i++)
            buffer.putDouble(values[i]);

        return buffer.array();
    }

    private static byte[] convertToBigEndian(byte bytes[], int precision)
    {
        byte reversed[] = new byte[bytes.length];
        for(int j = 0; j < bytes.length / precision; j++)
        {
            for(int i = 0; i < precision; i++)
                reversed[j * precision + (precision - 1 - i)] = bytes[j * precision + i];

        }

        return reversed;
    }

    public static double round(double value, int decimalPlace)
    {
        double power_of_ten = 1.0D;
        double fudge_factor;
        for(fudge_factor = 0.050000000000000003D; decimalPlace-- > 0; fudge_factor /= 10D)
            power_of_ten *= 10D;

        return (double)Math.round((value + fudge_factor) * power_of_ten) / power_of_ten;
    }

    public static final int DECIMAL_PLACES = 4;
}
