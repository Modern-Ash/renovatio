package org.shark.renovatio.cobol.ir.parser;

import org.shark.renovatio.cobol.ir.model.BinaryArithmeticExpression;
import org.shark.renovatio.cobol.ir.model.CobolExpression;
import org.shark.renovatio.cobol.ir.model.DataReferenceExpression;
import org.shark.renovatio.cobol.ir.model.LiteralExpression;
import org.shark.renovatio.cobol.ir.model.SourceSpan;
import org.shark.renovatio.cobol.ir.model.UnaryArithmeticExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Bounded deterministic parser for the arithmetic expression subset supported by v1. */
public final class CobolExpressionParser {

    private final List<Token> tokens;
    private final String sourceName;
    private int current;

    private CobolExpressionParser(String expression, String sourceName) {
        if (expression == null || expression.isBlank()) {
            throw new ParseException("expression must not be blank", 0);
        }
        this.tokens = tokenize(expression);
        this.sourceName = sourceName == null || sourceName.isBlank() ? "<memory>" : sourceName;
    }

    public static CobolExpression parse(String expression) {
        return parse(expression, "<memory>");
    }

    public static CobolExpression parse(String expression, String sourceName) {
        CobolExpressionParser parser = new CobolExpressionParser(expression, sourceName);
        CobolExpression result = parser.additive();
        if (!parser.isAtEnd()) {
            throw new ParseException("unexpected token " + parser.peek().lexeme(), parser.peek().offset());
        }
        return result;
    }

    private CobolExpression additive() {
        CobolExpression expression = multiplicative();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token operator = previous();
            CobolExpression right = multiplicative();
            expression = new BinaryArithmeticExpression(expression,
                    operator.type() == TokenType.PLUS
                            ? BinaryArithmeticExpression.ArithmeticOperator.ADD
                            : BinaryArithmeticExpression.ArithmeticOperator.SUBTRACT,
                    right, span(operator));
        }
        return expression;
    }

    private CobolExpression multiplicative() {
        CobolExpression expression = unary();
        while (match(TokenType.STAR, TokenType.SLASH)) {
            Token operator = previous();
            CobolExpression right = unary();
            expression = new BinaryArithmeticExpression(expression,
                    operator.type() == TokenType.STAR
                            ? BinaryArithmeticExpression.ArithmeticOperator.MULTIPLY
                            : BinaryArithmeticExpression.ArithmeticOperator.DIVIDE,
                    right, span(operator));
        }
        return expression;
    }

    private CobolExpression unary() {
        if (match(TokenType.PLUS, TokenType.MINUS)) {
            Token operator = previous();
            return new UnaryArithmeticExpression(
                    operator.type() == TokenType.PLUS
                            ? UnaryArithmeticExpression.UnaryOperator.PLUS
                            : UnaryArithmeticExpression.UnaryOperator.MINUS,
                    unary(), span(operator));
        }
        return primary();
    }

    private CobolExpression primary() {
        if (match(TokenType.NUMBER)) {
            Token token = previous();
            return new LiteralExpression(LiteralExpression.LiteralKind.NUMERIC, token.lexeme(), span(token));
        }
        if (match(TokenType.STRING)) {
            Token token = previous();
            return new LiteralExpression(LiteralExpression.LiteralKind.ALPHANUMERIC, token.lexeme(), span(token));
        }
        if (match(TokenType.IDENTIFIER)) {
            Token token = previous();
            String value = token.lexeme().toUpperCase(Locale.ROOT);
            if (value.equals("ZERO") || value.equals("ZEROS") || value.equals("ZEROES")) {
                return new LiteralExpression(LiteralExpression.LiteralKind.ZERO, "0", span(token));
            }
            if (value.equals("SPACE") || value.equals("SPACES")) {
                return new LiteralExpression(LiteralExpression.LiteralKind.SPACE, " ", span(token));
            }
            return new DataReferenceExpression(value, span(token));
        }
        if (match(TokenType.LEFT_PAREN)) {
            CobolExpression expression = additive();
            consume(TokenType.RIGHT_PAREN, "expected ')' after expression");
            return expression;
        }
        throw new ParseException("expected expression", peek().offset());
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                current++;
                return true;
            }
        }
        return false;
    }

    private void consume(TokenType type, String message) {
        if (!match(type)) {
            throw new ParseException(message, peek().offset());
        }
    }

    private boolean check(TokenType type) {
        return peek().type() == type;
    }

    private boolean isAtEnd() {
        return check(TokenType.END);
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private SourceSpan span(Token token) {
        int start = token.offset() + 1;
        return new SourceSpan(sourceName, 1, start, 1, start + Math.max(0, token.sourceLength() - 1));
    }

    private static List<Token> tokenize(String source) {
        List<Token> result = new ArrayList<>();
        int index = 0;
        while (index < source.length()) {
            char c = source.charAt(index);
            if (Character.isWhitespace(c)) {
                index++;
                continue;
            }
            TokenType symbol = switch (c) {
                case '+' -> TokenType.PLUS;
                case '-' -> TokenType.MINUS;
                case '*' -> TokenType.STAR;
                case '/' -> TokenType.SLASH;
                case '(' -> TokenType.LEFT_PAREN;
                case ')' -> TokenType.RIGHT_PAREN;
                default -> null;
            };
            if (symbol != null) {
                result.add(new Token(symbol, String.valueOf(c), index++, 1));
                continue;
            }
            if (c == '\'' || c == '"') {
                int start = index++;
                char quote = c;
                StringBuilder value = new StringBuilder();
                boolean closed = false;
                while (index < source.length()) {
                    char current = source.charAt(index);
                    if (current != quote) {
                        value.append(current);
                        index++;
                        continue;
                    }
                    if (index + 1 < source.length() && source.charAt(index + 1) == quote) {
                        value.append(quote);
                        index += 2;
                        continue;
                    }
                    index++;
                    closed = true;
                    break;
                }
                if (!closed) {
                    throw new ParseException("unterminated string literal", start);
                }
                result.add(new Token(TokenType.STRING, value.toString(), start, index - start));
                continue;
            }
            if (Character.isDigit(c)) {
                int start = index++;
                while (index < source.length()
                        && (Character.isDigit(source.charAt(index)) || source.charAt(index) == '.')) {
                    index++;
                }
                String number = source.substring(start, index);
                if (number.chars().filter(ch -> ch == '.').count() > 1 || number.endsWith(".")) {
                    throw new ParseException("invalid numeric literal", start);
                }
                result.add(new Token(TokenType.NUMBER, number, start, index - start));
                continue;
            }
            if (Character.isLetter(c)) {
                int start = index++;
                while (index < source.length()
                        && (Character.isLetterOrDigit(source.charAt(index)) || source.charAt(index) == '-')) {
                    index++;
                }
                result.add(new Token(TokenType.IDENTIFIER, source.substring(start, index), start, index - start));
                continue;
            }
            throw new ParseException("unsupported character '" + c + "'", index);
        }
        result.add(new Token(TokenType.END, "", source.length(), 0));
        return List.copyOf(result);
    }

    private enum TokenType { NUMBER, STRING, IDENTIFIER, PLUS, MINUS, STAR, SLASH, LEFT_PAREN, RIGHT_PAREN, END }

    private record Token(TokenType type, String lexeme, int offset, int sourceLength) {}

    public static final class ParseException extends IllegalArgumentException {
        private final int offset;

        public ParseException(String message, int offset) {
            super(message + " at offset " + offset);
            this.offset = offset;
        }

        public int offset() {
            return offset;
        }
    }
}
