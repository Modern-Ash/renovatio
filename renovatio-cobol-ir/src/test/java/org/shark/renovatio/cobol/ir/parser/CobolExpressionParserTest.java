package org.shark.renovatio.cobol.ir.parser;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.model.BinaryArithmeticExpression;
import org.shark.renovatio.cobol.ir.model.DataReferenceExpression;
import org.shark.renovatio.cobol.ir.model.LiteralExpression;
import org.shark.renovatio.cobol.ir.model.UnaryArithmeticExpression;

import static org.junit.jupiter.api.Assertions.*;

class CobolExpressionParserTest {

    @Test
    void parse_shouldHonorPrecedenceParenthesesAndUnarySigns() {
        BinaryArithmeticExpression root = assertInstanceOf(BinaryArithmeticExpression.class,
                CobolExpressionParser.parse("-(WS-A + 2) * 3 / +WS-B"));

        assertEquals(BinaryArithmeticExpression.ArithmeticOperator.DIVIDE, root.operator());
        BinaryArithmeticExpression product = assertInstanceOf(BinaryArithmeticExpression.class, root.left());
        assertEquals(BinaryArithmeticExpression.ArithmeticOperator.MULTIPLY, product.operator());
        UnaryArithmeticExpression negative = assertInstanceOf(UnaryArithmeticExpression.class, product.left());
        assertEquals(UnaryArithmeticExpression.UnaryOperator.MINUS, negative.operator());
        BinaryArithmeticExpression sum = assertInstanceOf(BinaryArithmeticExpression.class, negative.operand());
        assertEquals(BinaryArithmeticExpression.ArithmeticOperator.ADD, sum.operator());
        assertInstanceOf(DataReferenceExpression.class, sum.left());
        assertInstanceOf(LiteralExpression.class, sum.right());
        assertInstanceOf(UnaryArithmeticExpression.class, root.right());
    }

    @Test
    void parse_shouldRecognizeDecimalStringAndFigurativeLiterals() {
        assertEquals(LiteralExpression.LiteralKind.NUMERIC,
                assertInstanceOf(LiteralExpression.class, CobolExpressionParser.parse("12.50")).kind());
        assertEquals("HELLO",
                assertInstanceOf(LiteralExpression.class, CobolExpressionParser.parse("'HELLO'")).value());
        assertEquals(LiteralExpression.LiteralKind.ZERO,
                assertInstanceOf(LiteralExpression.class, CobolExpressionParser.parse("ZEROS")).kind());
        assertEquals(LiteralExpression.LiteralKind.SPACE,
                assertInstanceOf(LiteralExpression.class, CobolExpressionParser.parse("SPACES")).kind());
    }

    @Test
    void parse_shouldConsumeDoubledCobolStringDelimiters() {
        LiteralExpression apostrophe = assertInstanceOf(LiteralExpression.class,
                CobolExpressionParser.parse("'DON''T'"));
        LiteralExpression quote = assertInstanceOf(LiteralExpression.class,
                CobolExpressionParser.parse("\"A\"\"B\""));

        assertEquals("DON'T", apostrophe.value());
        assertEquals("A\"B", quote.value());
        assertEquals(8, apostrophe.sourceSpan().endColumn());
        assertEquals(6, quote.sourceSpan().endColumn());
    }

    @Test
    void parse_shouldFailClosedOnUnsupportedOrMalformedInput() {
        assertThrows(CobolExpressionParser.ParseException.class,
                () -> CobolExpressionParser.parse("A ** B"));
        assertThrows(CobolExpressionParser.ParseException.class,
                () -> CobolExpressionParser.parse("(A + B"));
        assertThrows(CobolExpressionParser.ParseException.class,
                () -> CobolExpressionParser.parse("1.2.3"));
        assertThrows(CobolExpressionParser.ParseException.class,
                () -> CobolExpressionParser.parse(""));
    }
}
