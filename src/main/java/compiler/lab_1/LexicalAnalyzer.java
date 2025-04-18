package compiler.lab_1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LexicalAnalyzer {
    // Типы токенов
    public enum TokenType {
        IDENTIFIER, BOOLEAN, OPERATOR, ASSIGN, PIPE, LPAREN, RPAREN, ERROR
    }

    // Класс для хранения информации о токене
    public static class Token {
        public final TokenType type;
        public final String name;
        public final int value;
        public final int line;
        public final int column;
       // private Token other;

        public Token(TokenType type, String name, int line, int column, int value) {
            this.type = type;
            this.name = name;
            this.line = line;
            this.column = column;
            this.value = value;
        }

        @Override
        public String toString() {
            return String.format("%-12s %-10s %s",
                    type, "'" + name + "'", (value == -1 ? " " : value == 0 ? name : name +": " + value));
        }

        public boolean equals (Token other) {
           return this.type.equals(other.type) && this.column == other.column && this.line == other.line &&
                   this.name.equals(other.name);
        }

        public boolean equals (String otherValue) {
            return  this.name.equals(otherValue);
        }

    }

    // Ошибка лексического анализа
    public static class LexicalException extends Exception {
        public LexicalException(String message, int line, int column) {
            super(String.format("Error at line %d, column %d: %s", line, column, message));
        }
    }

    // Таблицы для хранения результатов
    private final List<Token> tokens = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    // Шаблоны для распознавания
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z]{0,15}$");
    private static final Set<String> OPERATORS = Set.of("OR", "XOR", "AND", "NOT");
    private static final Set<String> BOOLEANS = Set.of("TRUE", "FALSE");


    // Основной метод анализа
    public void  analyze(String input) {
        tokens.clear();
        errors.clear();

        int line = 1;
        int column = 1;
        int pos = 0;
        int counter  =1;
        boolean flagOfBracket = false;
        int bracketsCounter = 0;
        int inputLength = input.length();

        while (pos < inputLength) {
            char current = input.charAt(pos);

            // Пропускаем пробелы
            if (Character.isWhitespace(current)) {
                if (current == '\n') {
                    line++;
                    column = 1;
                } else {
                    column++;
                }
                pos++;
                continue;
            }

            // Обработка комментариев (форма /* ... */)
            if (current == '/' && pos + 1 < inputLength && input.charAt(pos + 1) == '*') {
                int commentStartLine = line;
                int commentStartColumn = column;
                pos += 2;
                column += 2;

                boolean commentClosed = false;
                while (pos < inputLength) {
                    if (input.charAt(pos) == '*' && pos + 1 < inputLength && input.charAt(pos + 1) == '/') {
                        pos += 2;
                        column += 2;
                        commentClosed = true;
                        break;
                    }
                    if (input.charAt(pos) == '\n') {
                        line++;
                        column = 1;
                    } else {
                        column++;
                    }
                    pos++;
                }

                if (!commentClosed) {
                    errors.add(String.format("Unclosed comment starting at line %d, column %d",
                            commentStartLine, commentStartColumn));
                }
                continue;
            }

            // Обработка идентификаторов и ключевых слов
            if (Character.isLetterOrDigit(current)) {
                StringBuilder sb = new StringBuilder();
                int startColumn = column;

                while (pos < inputLength && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_')){
                    sb.append(input.charAt(pos++));
                    column++;
                }

                String word = sb.toString();

                // Проверка длины идентификатора
                if (word.length() > 16) {
                    errors.add(String.format("Identifier too long at line %d, column %d", line, startColumn));
                   // tokens.add(new Token(TokenType.ERROR, word, line, startColumn));
                    continue;
                }

                // Проверка на латинские символы
                if (bracketsCounter != 0 && word.endsWith(")")){
                    //отсекли скобку
                    word = word.substring(0, word.length()-1);
                    flagOfBracket = true;
                }
                if (!IDENTIFIER_PATTERN.matcher(word).matches()) {
                    errors.add(String.format("Invalid identifier '%s' at line %d, column %d",
                            word, line, startColumn));
                   // tokens.add(new Token(TokenType.ERROR, word, line, startColumn));
                    continue;
                }

                // Определение типа токена
                String upperWord = word.toUpperCase();
                if (BOOLEANS.contains(upperWord)) {
                    tokens.add(new Token(TokenType.BOOLEAN, upperWord, line, startColumn, 0));
                } else if (OPERATORS.contains(upperWord)) {
                    tokens.add(new Token(TokenType.OPERATOR, upperWord, line, startColumn, -1));
                } else {
//                    if(!tokens.isEmpty()){
//                        if(tokens.getLast().type.equals(TokenType.ASSIGN)){
//                            if(!identifierExists(word)){
//                                errors.add(String.format("Unexpected identifier '%s' at line %d, column %d",
//                                        word, line, column));
//                                continue;
//                            }
//                        }
//                    }
                    String finalWord = word;
                    var valueOfToken = tokens.stream().filter(
                            token -> token.type == TokenType.IDENTIFIER && token.name.equals(finalWord)
                    ).findFirst();
                    tokens.add(new Token(TokenType.IDENTIFIER, word, line, startColumn, valueOfToken.isPresent()?valueOfToken.get().value : counter++));
                }
                if(flagOfBracket){
                    flagOfBracket = false;
                    tokens.add(new Token(TokenType.RPAREN, ")", line, column, -1));
                }
                continue;
            }

            // Обработка операторов и разделителей
            switch (current) {
                case '|':
                    if(!tokens.getLast().equals("|")) {
                        tokens.add(new Token(TokenType.PIPE, "|", line, column, -1));
                    }
                    pos++; column++;
                    break;
                case ':':
                    if (pos + 1 < inputLength && input.charAt(pos + 1) == '=') {
                        tokens.add(new Token(TokenType.ASSIGN, ":=", line, column, -1));
                        pos += 2; column += 2;
                    } else {
                        errors.add(String.format("Invalid token ':' at line %d, column %d", line, column));
                        //tokens.add(new Token(TokenType.ERROR, ":", line, column, -1));
                        pos++; column++;
                    }
                    break;
                case '(':
                    tokens.add(new Token(TokenType.LPAREN, "(", line, column, -1));
                    pos++; column++; bracketsCounter++;
                    break;
                case ')':
                    tokens.add(new Token(TokenType.RPAREN, ")", line, column, -1));
                    pos++; column++; bracketsCounter--;
                    break;
                default:
                    errors.add(String.format("Unexpected character '%c' at line %d, column %d",
                            current, line, column));
                    //tokens.add(new Token(TokenType.ERROR, String.valueOf(current), line, column, ni;));
                    pos++; column++;
            }
        }
    }
    private Object getValue(String name){
        return switch (name){
            case ("TRUE"),("FALSE") ->  Boolean.parseBoolean(name);
            default -> null;
        };
    }
    private boolean identifierExists(String identifier) {
        return tokens.stream()
                .anyMatch(token -> token.type == TokenType.IDENTIFIER && token.name.equals(identifier));
    }

    // Методы для получения результатов
    public List<Token> getTokens() {
        return Collections.unmodifiableList(tokens);
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public void printResults() {
        if (!errors.isEmpty()) {
            System.out.println("\n=== Errors ===");
            for (String error : errors) {
                System.out.println(error);
            }
        }

        System.out.println("=== Tokens Table ===");
        System.out.printf("%-12s %-10s %s\n", "TYPE", "NAME",  "VALUE");
        for (Token token : tokens) {
            System.out.println(token);
        }

    }

    //считываем из файла
    public static String readProgramFromFile(String filePath) throws IOException {
        return Files.lines(Paths.get(filePath)).filter(line -> !line.isEmpty()) // Игнорируем пустые строки
                .collect(Collectors.joining(" ")); // Объединяем с пробелами
    }

    // Примеры программ на входном языке
    public static final String[] TEST_PROGRAMS = {
            // Правильная программа
            "x := TRUE | y := FALSE | z := x OR y",

            // С ошибками
            "var_with_very_long_name := TRUE | x := 123 | y := TRUEE",

            // С комментариями
            "/* This is a comment */ x := TRUE /* Another comment */ | y := FALSE",

            // Комплексное выражение
            "a := (TRUE AND FALSE) XOR (NOT TRUE) | b := a OROR FALSE",

            // c незакрытым комментарием
            "x := TRUE /* Unclosed comment",

            /* Пример многострочной программы на тестовом языке */
            """
            vara := TRUE | varb := FALSE |
            result := (vara OR varb) AND
            (NOT vara XOR varb) | 
            """
    };

    public static final String[] TEST_FILEPATHS ={
        "C:\\Users\\LOBAN\\Desktop\\Политех\\ТА\\semester_2\\lexical_analyzer_lab\\src\\main\\java\\compiler\\lab_1\\commentProgramm.txt",
        "C:\\Users\\LOBAN\\Desktop\\Политех\\ТА\\semester_2\\lexical_analyzer_lab\\src\\main\\java\\compiler\\lab_1\\complexProgramm.txt",
        "C:\\Users\\LOBAN\\Desktop\\Политех\\ТА\\semester_2\\lexical_analyzer_lab\\src\\main\\java\\compiler\\lab_1\\correctProgramm.txt",
        "C:\\Users\\LOBAN\\Desktop\\Политех\\ТА\\semester_2\\lexical_analyzer_lab\\src\\main\\java\\compiler\\lab_1\\incorrectProgramm.txt",
        "C:\\Users\\LOBAN\\Desktop\\Политех\\ТА\\semester_2\\lexical_analyzer_lab\\src\\main\\java\\compiler\\lab_1\\multilineProgramm.txt",
        "C:\\Users\\LOBAN\\Desktop\\Политех\\ТА\\semester_2\\lexical_analyzer_lab\\src\\main\\java\\compiler\\lab_1\\unclosedCommentProgramm.txt"
    };

    public static void main(String[] args) {
        LexicalAnalyzer analyzer = new LexicalAnalyzer();

        try {
            for (var programm : TEST_FILEPATHS){
                // Читаем программу из файла
                String program = readProgramFromFile(programm);

                System.out.println("\n=== Source Program ===");
                System.out.println(program);
                System.out.println("\nAnalysis results:");

                // Анализируем программу
                analyzer.analyze(program);
                analyzer.printResults();


            }

        } catch (IOException e) {
            System.err.println("Failed to read file: " + e.getMessage());
        }
    }
}
