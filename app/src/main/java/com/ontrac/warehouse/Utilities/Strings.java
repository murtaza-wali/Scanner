package com.ontrac.warehouse.Utilities;

import java.util.ArrayList;
import java.util.List;

public class Strings {

    public static boolean IsNullOrWhiteSpace(String value) {
        if (value == null) {
            return true;
        }

        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    public static TrimCharArrayResult TrimCharArray(char[] array, int left, int right)
    {
        TrimCharArrayResult result = new TrimCharArrayResult();

        for (int i = 0; i < array.length; i++)
        {
            if (i < left)
            {
                result.Left.add(array[i]);
            }
            else if (i > array.length - right - 1)
            {
                result.Right.add(array[i]);
            }
            else
            {
                result.Center.add(array[i]);
            }
        }

        return result;
    }

    public static class TrimCharArrayResult
    {
        public TrimCharArrayResult()
        {
            Left = new ArrayList<Character>(0);
            Center = new ArrayList<Character>();
            Right = new ArrayList<Character>();
        }

        public ArrayList<Character> Left;
        public List<Character> Center;
        public List<Character> Right;
    }

    public static Integer TryParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Boolean IsNumber(String value)
    {
        boolean result = false;

        for(char c : value.toCharArray())
        {
            result = Character.isDigit(c);

            if (!result){
                break;
            }
        }

        return result;
    }

    public static String CapitalizeFirst(String value) {
        String result = value;
        if (!IsNullOrWhiteSpace(value)) {
            result = value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
        }
        return result;
    }

    public static String CaesarCipher(String value, int shift) {
        String s = "";
        for(int i = 0; i < value.length(); i++) {
            char c = (char)(value.charAt(i));
            if (c >= 'A' && c <= 'Z') {
                s += (char)((c - 'A' + shift) % 26 + 'A');
            } else if (c >= 'a' && c <= 'z') {
                s += (char)((c - 'a' + shift) % 26 + 'a');
            } else {
                s += c;
            }
        }
        return s;
    }
}
