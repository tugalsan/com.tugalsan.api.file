package com.tugalsan.api.file.client;

import com.tugalsan.api.list.client.*;
import java.util.*;

@Deprecated
public class TGS_FileUtilsEng {

    public static String toSafe(CharSequence fileName) {
        return toSafe(fileName, SAFE_DEFAULT_CHAR());
    }

    public static String toSafe(CharSequence fileName, char safeDefaultChar) {
        var fileNameStr = fileName.toString();
        for (var i = 0; i < SAFE_PAIRS().size(); i++) {
            if (fileNameStr.indexOf(SAFE_PAIRS().get(i).unsafe()) != -1) {
                fileNameStr = fileNameStr.replace(SAFE_PAIRS().get(i).unsafe(), SAFE_PAIRS().get(i).safe());
            }
        }
        for (var i = 0; i < fileNameStr.length(); i++) {
            if (SAFE_CHARS().indexOf(fileNameStr.charAt(i)) == -1) {
                fileNameStr = fileNameStr.replace(fileNameStr.charAt(i), safeDefaultChar);
            }
        }
        while (fileNameStr.contains(SAFE_DEFAULT_CHAR_DBL())) {
            fileNameStr = fileNameStr.replace(SAFE_DEFAULT_CHAR_DBL(), SAFE_DEFAULT_CHAR_AS_STR());
        }
        return fileNameStr;
    }

    public static String SAFE_CHARS() {
        return " ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-.()";
    }

    public static char SAFE_DEFAULT_CHAR() {
        return '_';
    }

    public static String SAFE_DEFAULT_CHAR_AS_STR() {
        return String.valueOf(SAFE_DEFAULT_CHAR());
    }

    public static String SAFE_DEFAULT_CHAR_DBL() {
        return SAFE_DEFAULT_CHAR_AS_STR() + SAFE_DEFAULT_CHAR_AS_STR();
    }

    public static List<TGS_FileUtilsPair> SAFE_PAIRS() {
        return TGS_ListUtils.of(
                new TGS_FileUtilsPair('İ', 'i'), new TGS_FileUtilsPair('ı', 'i'),
                new TGS_FileUtilsPair('Ç', 'C'), new TGS_FileUtilsPair('ç', 'c'),
                new TGS_FileUtilsPair('Ş', 'S'), new TGS_FileUtilsPair('ş', 's'), new TGS_FileUtilsPair('ƒ', 's'),
                new TGS_FileUtilsPair('Ö', 'O'), new TGS_FileUtilsPair('ö', 'o'),
                new TGS_FileUtilsPair('Ü', 'U'), new TGS_FileUtilsPair('ü', 'u'),
                new TGS_FileUtilsPair('Ğ', 'G'), new TGS_FileUtilsPair('ğ', 'g'),
                new TGS_FileUtilsPair('{', '('), new TGS_FileUtilsPair('}', ')'),
                new TGS_FileUtilsPair('[', '('), new TGS_FileUtilsPair(']', ')')
        );
    }
}
