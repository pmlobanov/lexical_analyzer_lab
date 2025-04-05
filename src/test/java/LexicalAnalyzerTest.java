import compiler.lab_1.LexicalAnalyzer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

public class LexicalAnalyzerTest {
    private final LexicalAnalyzer analyzer = new LexicalAnalyzer();

    @Test
    void testSimpleAssignment() {
        String input = "x := TRUE";
        analyzer.analyze(input);

        List<LexicalAnalyzer.Token> tokens = analyzer.getTokens();
        assertEquals(3, tokens.size());
        assertEquals("IDENTIFIER", tokens.get(0).type.name());
        assertEquals("ASSIGN", tokens.get(1).type.name());
        assertEquals("BOOLEAN", tokens.get(2).type.name());
        assertTrue(analyzer.getErrors().isEmpty());
    }

    @Test
    void testMultipleExpressions() {
        String input = "a := TRUE | b := FALSE | c := a AND b";
        analyzer.analyze(input);

        List<LexicalAnalyzer.Token> tokens = analyzer.getTokens();
        assertEquals(13, tokens.size());
        assertEquals("PIPE", tokens.get(3).type.name());
        assertEquals("ASSIGN", tokens.get(9).type.name());
        assertTrue(analyzer.getErrors().isEmpty());
    }

    @Test
    void testParentheses() {
        String input = "x := (TRUE OR FALSE) AND (NOT TRUE)";
        analyzer.analyze(input);

        List<LexicalAnalyzer.Token> tokens = analyzer.getTokens();
        assertEquals(12, tokens.size());
        assertEquals("LPAREN", tokens.get(2).type.name());
        assertEquals("RPAREN", tokens.get(6).type.name());
        assertTrue(analyzer.getErrors().isEmpty());
    }

    @Test
    void testComments() {
        String input = "/* Start */ x := TRUE /* Middle */ | y := FALSE /* End */";
        analyzer.analyze(input);

        List<LexicalAnalyzer.Token> tokens = analyzer.getTokens();
        assertEquals(7, tokens.size());
        assertTrue(analyzer.getErrors().isEmpty());
    }

    @Test
    void testInvalidIdentifier() {
        String input = "123var := TRUE";
        analyzer.analyze(input);

        assertFalse(analyzer.getErrors().isEmpty());
        assertTrue(analyzer.getErrors().get(0).contains("Unexpected character"));
    }

    @Test
    void testLongIdentifier() {
        String input = "very_long_identifier_name := TRUE";
        analyzer.analyze(input);

        assertFalse(analyzer.getErrors().isEmpty());
        assertTrue(analyzer.getErrors().get(0).contains("Identifier too long"));
    }

    @Test
    void testInvalidBoolean() {
        String input = "x := TRUEE";
        analyzer.analyze(input);

        assertFalse(analyzer.getErrors().isEmpty());
        assertTrue(analyzer.getErrors().get(0).contains("Unexpected identifier"));
    }

    @Test
    void testUnclosedComment() {
        String input = "x := TRUE /* Unclosed comment";
        analyzer.analyze(input);

        assertFalse(analyzer.getErrors().isEmpty());
        assertTrue(analyzer.getErrors().get(0).contains("Unclosed comment"));
    }

    @Test
    void testMixedCaseOperators() {
        String input = "x := true or false";
        analyzer.analyze(input);

        List<LexicalAnalyzer.Token> tokens = analyzer.getTokens();
        assertEquals(5, tokens.size());
        assertEquals("BOOLEAN", tokens.get(2).type.name());
        assertEquals("OPERATOR", tokens.get(3).type.name());
        assertTrue(analyzer.getErrors().isEmpty());
    }

    @Test
    void testComplexExpression() {
        String input = "result := (a XOR b) OR (NOT (c AND d))";
        analyzer.analyze(input);

        List<LexicalAnalyzer.Token> tokens = analyzer.getTokens();
        assertEquals(16, tokens.size());
        assertEquals(3, tokens.stream().filter(t -> t.type == LexicalAnalyzer.TokenType.LPAREN).count());
        assertTrue(analyzer.getErrors().isEmpty());
    }

    @Test
    void testEmptyInput() {
        String input = "";
        analyzer.analyze(input);

        assertTrue(analyzer.getTokens().isEmpty());
        assertTrue(analyzer.getErrors().isEmpty());
    }

    @Test
    void testOnlyComments() {
        String input = "/* Only comment */";
        analyzer.analyze(input);

        assertTrue(analyzer.getTokens().isEmpty());
        assertTrue(analyzer.getErrors().isEmpty());
    }

    @Test
    void testInvalidAssignment() {
        String input = "x : TRUE";
        analyzer.analyze(input);

        assertFalse(analyzer.getErrors().isEmpty());
        assertTrue(analyzer.getErrors().get(0).contains("Invalid token"));
    }

    @Test
    void testSpecialCharacters() {
        String input = "x@ := TRUE";
        analyzer.analyze(input);

        assertFalse(analyzer.getErrors().isEmpty());
        assertTrue(analyzer.getErrors().get(0).contains("Unexpected character"));
    }

    @Test
    void testNestedParentheses() {
        String input = "x := ((TRUE AND FALSE) OR (NOT (TRUE XOR FALSE)))";
        analyzer.analyze(input);

        // Проверяем баланс скобок через счетчик
        long open = analyzer.getTokens().stream().filter(t -> t.name.equals("(")).count();
        long close = analyzer.getTokens().stream().filter(t -> t.name.equals(")")).count();
        assertEquals(open, close);
    }

    @Test
    void testMultiplePipes() {
        String input = "a:=TRUE||b:=FALSE";
        analyzer.analyze(input);

        assertTrue(analyzer.getErrors().isEmpty());
        assertEquals(1,analyzer.getTokens().stream().filter( e ->
                e.type.equals(LexicalAnalyzer.TokenType.PIPE)
        ).count());
    }

    @Test
    void testEdgeCaseIdentifiers() {
        String input = "a := A | b := B | z := Z";
        analyzer.analyze(input);

        assertEquals(8, analyzer.getTokens().size());
        assertEquals(3, analyzer.getErrors().size());
    }

    @Test
    void testMaxLengthIdentifier() {
        String input = "abcdefghijklmnop := TRUE"; // 16 символов
        analyzer.analyze(input);

        assertTrue(analyzer.getErrors().isEmpty());
    }

    @Test
    void testJustAboveMaxLengthIdentifier() {
        String input = "abcdefghijklmnopq := TRUE"; // 17 символов
        analyzer.analyze(input);

        assertFalse(analyzer.getErrors().isEmpty());
    }


}