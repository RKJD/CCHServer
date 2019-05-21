/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xokundevs.cchmavenserver.model;

import java.nio.ByteBuffer;

/**
 *
 * @author Antonio
 */
public class Parser {
    public static String toHex(byte[] array) {
        StringBuilder str = new StringBuilder();

        for (int i = 0; i < array.length; i++) {
            int a = array[i] & 0xf;
            int b = (array[i] & 0xf0) >> 4;
            if (b < 10) {
                str.append(b);
            } else {
                str.append(Integer.toHexString(b));
            }
            if (a < 10) {
                str.append(a);
            } else {
                str.append(Integer.toHexString(a));
            }
        }

        return str.toString();
    }
    
    /**
     * Este méto
     *
     * @param hex
     * @return
     */
    public static byte[] fromHex(String hex) {
        int length = hex.length() / 2;
        byte[] array = new byte[length];
        for (int i = 0; i < length; i++) {
            String firstPart = hex.charAt(i * 2) + "", secondPart = hex.charAt(i * 2 + 1) + "";
            byte by = 0;
            try {
                by = (byte) (Integer.parseInt(firstPart) * 16);
            } catch (NumberFormatException e) {
                switch (firstPart) {
                    case "a":
                        by = (byte) (10 * 16);
                        break;
                    case "b":
                        by = (byte) (11 * 16);
                        break;
                    case "c":
                        by = (byte) (12 * 16);
                        break;
                    case "d":
                        by = (byte) (13 * 16);
                        break;
                    case "e":
                        by = (byte) (14 * 16);
                        break;
                    case "f":
                        by = (byte) (15 * 16);
                        break;
                }
            }
            try {
                by += (byte) (Integer.parseInt(secondPart));
            } catch (NumberFormatException e) {
                switch (secondPart) {
                    case "a":
                        by += 10;
                        break;
                    case "b":
                        by += 11;
                        break;
                    case "c":
                        by += 12;
                        break;
                    case "d":
                        by += 13;
                        break;
                    case "e":
                        by += 14;
                        break;
                    case "f":
                        by += 15;
                        break;
                }
            }
            array[i] = by;
        }
        return array;
    }
    
    public static String parseLongToHex(long number) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(number);
        return Parser.toHex(buffer.array());
    }

    public static long parseHexToLong(String hex) {
        byte[] array = Parser.fromHex(hex);
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(array);
        buffer.flip();
        return buffer.getLong();
    }
}
