package com.tugalsan.api.file.client;

import com.tugalsan.api.list.client.*;
import com.tugalsan.api.tuple.client.*;
import java.util.*;

public class TGS_FileUtilsTur {

    private TGS_FileUtilsTur() {

    }

    public static String toSafe(CharSequence fileName) {
        return toSafe(fileName, SAFE_DEFAULT_CHAR());
    }

    public static String toSafe(CharSequence fileName, char safeDefaultChar) {
        var fileNameStr = fileName.toString();
        for (var i = 0; i < SAFE_PAIRS().size(); i++) {
            if (fileNameStr.indexOf(SAFE_PAIRS().get(i).value0) != -1) {
                fileNameStr = fileNameStr.replace(SAFE_PAIRS().get(i).value0, SAFE_PAIRS().get(i).value1);
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
        return " ABCÇDEFGĞHIİJKLMNOÖPQRSŞTUÜVWXYZabcçdefgğhıijklmnoöpqrsştuüvwxyz0123456789_-.()";
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

    public static List<TGS_Tuple2<Character, Character>> SAFE_PAIRS() {
        return TGS_ListUtils.of(
                new TGS_Tuple2('ƒ', 's'),
                new TGS_Tuple2('{', '('), new TGS_Tuple2('}', ')'),
                new TGS_Tuple2('[', '('), new TGS_Tuple2(']', ')')
        );
    }
}
