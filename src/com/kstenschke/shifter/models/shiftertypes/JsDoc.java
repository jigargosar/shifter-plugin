/*
 * Copyright 2011-2017 Kay Stenschke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kstenschke.shifter.models.shiftertypes;

import com.intellij.openapi.editor.Document;
import com.kstenschke.shifter.utils.UtilsEnvironment;
import com.kstenschke.shifter.utils.UtilsTextual;

import static org.apache.commons.lang.StringUtils.trim;

/**
 * JavaScript DOC @param comment
 */
public class JsDoc {

    public static boolean isJsDocBlock(String str) {
        str = trim(str);

        return UtilsTextual.isMultiLine(str)
            && (str.startsWith("/**") && str.endsWith("*/"))
            && (str.contains("@oaram") || str.contains("@return"));
    }

    /**
     * Check whether given string represents a JsDoc @param comment
     *
     * @param  str     String to be checked
     * @return boolean
     */
    public static Boolean isAtParamLine(String str) {
        str = trim(str);

        return str.startsWith("*") && str.contains("@param");
    }

    public static Boolean isInvalidAtReturnsLine(String str) {
        str = trim(str);

        return str.startsWith("*") && str.contains("@return") && !str.contains("@returns");
    }

    public static Boolean isAtReturnsLine(String str) {
        str = trim(str);

        return str.startsWith("*") && str.contains("@returns ");
    }

    public static Boolean isDataType(String str) {
        str = trim(str.toLowerCase());

        return str.equals("array")
                || str.equals("boolean")
                || str.equals("function")
                || str.equals("null")
                || str.equals("number")
                || str.equals("object")
                || str.equals("string")
                || str.equals("symbol")
                || str.equals("undefined");
    }

    public static Boolean containsDataType(String str, String lhs, boolean allowInvalidTypes) {
        str = trim(str.toLowerCase());

        if (str.contains(lhs + "array" )
         || str.contains(lhs + "boolean")
         || str.contains(lhs + "function")
         || str.contains(lhs + "null")
         || str.contains(lhs + "number")
         || str.contains(lhs + "object")
         || str.contains(lhs + "string")
         || str.contains(lhs + "symbol")
         || str.contains(lhs + "undefined")) {
            return true;
        }

        return allowInvalidTypes && (str.contains(lhs + "event" ) || str.contains(lhs + "int"));
    }

    public static Boolean containsCompounds(String str) {
        return str.contains("{") && str.contains("}");
    }

    /**
     * Actual shifting method
     *
     * @param  word
     * @param  document
     * @param  caretOffset
     * @return boolean
     */
    public static boolean addCompoundsAroundDataTypeAtCaretInDocument(String word, Document document, int caretOffset) {
        return UtilsEnvironment.replaceWordAtCaretInDocument(document, caretOffset, "{" + word + "}");
    }

    /**
     * @param line
     * @param docCommentType        "@param" or "@returns"
     * @param wrapInvalidDataTypes
     * @return
     */
    private static String addCompoundsToDataType(String line, String docCommentType, boolean wrapInvalidDataTypes) {
        line = line.replaceAll("(?i)(" + docCommentType + "\\s*)(array|bool|function|null|number|object|string|undefined)", "$1{$2}");

        if (wrapInvalidDataTypes) {
            line = line.replaceAll("(?i)(\" + docCommentType + \"\\s*)(event|int|integer)", "$1{$2}");
        }

        return line;
    }

    public static boolean correctInvalidReturnsCommentInDocument(Document document, int caretOffset) {
        return UtilsEnvironment.replaceWordAtCaretInDocument(document, caretOffset, "returns");
    }

    /**
     * Correct invalid JsDoc block comment
     *
     * Correct "@return" into "@returns"
     * Add curly brackets around data types in "@param" and "@returns" lines
     * Correct invalid data types into existing primitive data types (event => Object, int(eger) => number)
     *
     * @param document
     * @param offsetStart
     * @param offsetEnd
     * @return
     */
    public static boolean correctDocBlockInDocument(Document document, int offsetStart, int offsetEnd) {
        String documentText = document.getText();
        String docBlock = documentText.substring(offsetStart, offsetEnd);
        String lines[] = docBlock.split("\n");

        String docBlockCorrected = "";
        int index = 0;
        for (String line : lines) {
            if (isAtParamLine(line)) {
                line = correctParamLine(line);
            } else if (isInvalidAtReturnsLine(line)) {
                line = line.replace("@return ", "@returns ");
            }
            if (isAtReturnsLine(line)) {
                line = correctReturnLine(line);
            }

            docBlockCorrected += (index > 0 ? "\n" : "") + line;
            index++;
        }
        if (!docBlockCorrected.equals(docBlock)) {
            document.replaceString(offsetStart, offsetEnd, docBlockCorrected);
            return true;
        }

        return false;
    }

    private static String correctParamLine(String line) {
        if (containsDataType(line, " ", true) && !containsCompounds(line)) {
            line = addCompoundsToDataType(line, "@param", true);
        }
        return correctInvalidDataTypes(line);
    }

    private static String correctReturnLine(String line) {
        if (containsDataType(line, " ", true) && !containsCompounds(line)) {
            line = addCompoundsToDataType(line, "@returns", true);
        }
        return correctInvalidDataTypes(line);
    }

    private static String correctInvalidDataTypes(String line) {
        return line
                .replace("{bool}", "{boolean}")
                .replace("{int}", "{number}")
                .replace("{integer}", "{number}")
                .replace("{event}", "{Object}");
    }
}