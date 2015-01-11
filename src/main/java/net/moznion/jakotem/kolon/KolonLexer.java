package net.moznion.jakotem.kolon;

import lombok.Setter;
import lombok.experimental.Accessors;

import net.moznion.jakotem.LexerMode;
import net.moznion.jakotem.Source;
import net.moznion.jakotem.Token;
import net.moznion.jakotem.TokenType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KolonLexer {
  private final Source src;
  private final String srcString;
  private final String openTag;
  private final String closeTag;
  private final String codeLineDelimiter;

  private final Pattern codeLineRe;
  private final Pattern openTagRe;
  private final Pattern closeTagRe;
  private final Pattern commentInTagRe;

  private static final Pattern IDENT_RE = Pattern.compile("\\A(?:[a-zA-Z][_a-zA-Z0-9]*)");
  private static final Pattern POSITION_RE = Pattern.compile("\\A__(FILE|LINE|ROOT)__");
  private static final Pattern DOUBLE_RE = Pattern.compile("\\A(?:(?:[1-9][0-9]*|0)\\.[0-9]+)");
  private static final Pattern INT_RE = Pattern.compile("\\A(?:[1-9][0-9]*|0)");
  private static final Pattern HEX_RE = Pattern.compile("\\A0[xX][0-9a-fA-F]+");
  private static final Pattern OCTAL_RE = Pattern.compile("\\A0[0-7]+");
  private static final Pattern BINARY_RE = Pattern.compile("\\A0[bB][10]+");

  private static final EnumMap<TokenType, String> keywords;
  static {
    keywords = new EnumMap<>(TokenType.class);
    keywords.put(TokenType.NIL, "nil");
    keywords.put(TokenType.TRUE, "true");
    keywords.put(TokenType.FALSE, "false");
    keywords.put(TokenType.FOR, "for");
    keywords.put(TokenType.WHILE, "while");
    keywords.put(TokenType.MIN, "min");
    keywords.put(TokenType.MAX, "max");
    keywords.put(TokenType.IF, "if");
    keywords.put(TokenType.ELSE, "else");
    keywords.put(TokenType.SWITCH, "switch");
    keywords.put(TokenType.CASE, "case");
    keywords.put(TokenType.INCLUDE, "include");
    keywords.put(TokenType.BLOCK, "block");
    keywords.put(TokenType.CASCADE, "cascade");
    keywords.put(TokenType.AROUND, "around");
    keywords.put(TokenType.BEFORE, "before");
    keywords.put(TokenType.AFTER, "after");
    keywords.put(TokenType.SUPER, "super");
  }

  private List<Token> tokens;
  private LexerMode mode;
  private int pos;
  private int lineNumber;

  @Setter
  @Accessors(fluent = true)
  public static class Builder {
    private Source src;
    private String srcString;
    private String openTag;
    private String closeTag;
    private String codeLineDelimiter;

    public KolonLexer build() {
      return new KolonLexer(this);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private KolonLexer(Builder b) {
    src = b.src;
    srcString = b.srcString;
    openTag = b.openTag;
    closeTag = b.closeTag;
    codeLineDelimiter = b.codeLineDelimiter;

    openTagRe = Pattern.compile(new StringBuilder()
        .append("\\A")
        .append(openTag)
        .toString());
    closeTagRe = Pattern.compile(new StringBuilder()
        .append("\\A")
        .append(closeTag)
        .toString());
    codeLineRe = Pattern.compile(new StringBuilder()
        .append("\\A[ \t]*")
        .append(codeLineDelimiter)
        .toString());
    commentInTagRe = Pattern.compile(new StringBuilder()
        .append("\\A.*?")
        .append(closeTag)
        .toString(), Pattern.DOTALL);
  }

  public List<Token> tokenize() {
    tokens = new ArrayList<Token>();
    mode = LexerMode.IN_RAW;
    pos = 0;
    lineNumber = 1;

    while (pos < srcString.length()) {
      if (mode == LexerMode.IN_RAW) {
        this.tokenizeRaw(srcString);
      } else if (mode == LexerMode.IN_TAG || mode == LexerMode.IN_CODE_LINE) {
        this.tokenizeTagBody(srcString, tokens, mode);
      } else {
        // TODO
        throw new RuntimeException("SHOULD NOT REACH HERE");
      }
    }

    if (mode == LexerMode.IN_TAG) {
      // TODO
      throw new RuntimeException();
    }

    return tokens;
  }

  private void tokenizeTagBody(String srcString, List<Token> tokens, LexerMode modeWhenEntered) {
    while (pos < srcString.length()) {
      if (modeWhenEntered == LexerMode.IN_TAG) {
        Matcher matcher = closeTagRe.matcher(srcString.substring(pos));
        if (matcher.find()) {
          // hit end of tag
          pos += matcher.group(0).length();
          mode = LexerMode.IN_RAW;
          tokens.add(createToken(TokenType.CLOSE));
          return;
        }
      }

      switch (srcString.charAt(pos)) {
        case '\n':
          ++lineNumber;
          ++pos;

          if (modeWhenEntered == LexerMode.IN_CODE_LINE) {
            mode = LexerMode.IN_RAW;
            return;
          }

          break;
        case ' ':
        case '\t':
          ++pos;
          break;
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
          tokens.add(tokenizeNumber());
          break;
        case '"':
          tokens.add(tokenizeString());
          break;
        case '?':
          tokens.add(createToken(TokenType.CONDITIONAL));
          break;
        case ':':
          tokens.add(createToken(TokenType.CONDITIONAL_SELECTOR));
          break;
        case '<':
          if (pos + 1 < srcString.length()) {
            if (srcString.charAt(pos + 1) == '=') {
              tokens.add(createToken(TokenType.LE));
              pos += 2;
            } else {
              tokens.add(createToken(TokenType.LT));
              pos++;
            }
          } else {
            tokens.add(createToken(TokenType.LT));
            ++pos;
          }
          break;
        case '>':
          if (pos + 1 < srcString.length()) {
            if (srcString.charAt(pos + 1) == '=') {
              tokens.add(createToken(TokenType.GE));
              pos += 2;
            } else {
              tokens.add(createToken(TokenType.GT));
              pos++;
            }
          } else {
            tokens.add(createToken(TokenType.GT));
            ++pos;
          }
          break;
        case '!':
          if (pos + 1 < srcString.length()) {
            if (srcString.charAt(pos + 1) == '=') {
              // !=
              tokens.add(createToken(TokenType.NE));
              pos += 2;
            } else {
              tokens.add(createToken(TokenType.NOT));
              ++pos;
            }
          } else {
            tokens.add(createToken(TokenType.NOT));
            ++pos;
          }
          break;
        case '=':
          if (pos + 1 < srcString.length()) {
            if (srcString.charAt(pos + 1) == '=') {
              // ==
              tokens.add(createToken(TokenType.EQUALEQUAL));
              pos += 2;
            } else {
              tokens.add(createToken(TokenType.EQUAL));
              ++pos;
            }
          } else {
            tokens.add(createToken(TokenType.EQUAL));
            ++pos;
          }
          break;
        case '+':
          if (pos + 1 < srcString.length()) {
            switch (srcString.charAt(pos + 1)) {
              case '&':
                tokens.add(createToken(TokenType.BIT_AND));
                pos += 2;
                break;
              case '|':
                tokens.add(createToken(TokenType.BIT_OR));
                pos += 2;
                break;
              case '^':
                tokens.add(createToken(TokenType.BIT_XOR));
                pos += 2;
                break;
              default:
                tokens.add(createToken(TokenType.PLUS));
                pos++;
                break;
            }
          } else {
            tokens.add(createToken(TokenType.PLUS));
            ++pos;
          }
          break;
        case '%':
          tokens.add(createToken(TokenType.MODULO));
          ++pos;
          break;
        case '*':
          tokens.add(createToken(TokenType.MUL));
          ++pos;
          break;
        case '-':
          if (pos + 1 < srcString.length()) {
            if (srcString.charAt(pos + 1) == '>') {
              tokens.add(createToken(TokenType.ARROW));
              pos += 2;
            } else {
              tokens.add(createToken(TokenType.MINUS));
              ++pos;
            }
          } else {
            tokens.add(createToken(TokenType.MINUS));
            ++pos;
          }
          break;
        case '/':
          if (pos + 1 < srcString.length()) {
            if (srcString.charAt(pos + 1) == '/') {
              tokens.add(createToken(TokenType.NULL_OR));
              pos += 2;
            } else {
              tokens.add(createToken(TokenType.DIVIDE));
              ++pos;
            }
          } else {
            tokens.add(createToken(TokenType.DIVIDE));
            ++pos;
          }
          break;
        case '&':
          if (pos + 1 < srcString.length()) {
            if (srcString.charAt(pos + 1) == '&') {
              tokens.add(createToken(TokenType.ANDAND));
              pos += 2;
            } else {
              // TODO
              throw new RuntimeException("Invalid operator '&'");
            }
          } else {
            // TODO
            throw new RuntimeException("Invalid operator '&'");
          }
          break;
        case '|':
          if (pos + 1 < srcString.length()) {
            if (srcString.charAt(pos + 1) == '|') {
              tokens.add(createToken(TokenType.OROR));
              pos += 2;
            } else {
              tokens.add(createToken(TokenType.PIPE));
              pos++;
            }
          } else {
            tokens.add(createToken(TokenType.PIPE));
            pos++;
          }
          break;
        case '~':
          tokens.add(createToken(TokenType.CONCAT));
          break;
        case '[':
          tokens.add(this.createToken(TokenType.LBRACKET));
          ++pos;
          break;
        case ']':
          tokens.add(this.createToken(TokenType.RBRACKET));
          ++pos;
          break;
        case '{':
          tokens.add(this.createToken(TokenType.LBRACE));
          ++pos;
          break;
        case '}':
          tokens.add(this.createToken(TokenType.RBRACE));
          ++pos;
          break;
        case '(':
          tokens.add(this.createToken(TokenType.LPAREN));
          ++pos;
          break;
        case ')':
          tokens.add(this.createToken(TokenType.RPAREN));
          ++pos;
          break;
        case ',':
          tokens.add(this.createToken(TokenType.COMMA));
          ++pos;
          break;
        default:
          tokens.add(tokenizeOthers());
          break;
      }
    }
  }

  private void tokenizeRaw(String srcString) {
    StringBuilder builder = new StringBuilder();

    while (pos < srcString.length()) {
      Matcher matcher = openTagRe.matcher(srcString.substring(pos));
      if (matcher.find()) {
        // hit open tag
        mode = LexerMode.IN_TAG;
        pos += matcher.group(0).length();

        if (builder.length() > 0) {
          // push raw context string
          tokens.add(createToken(TokenType.RAW, builder.toString()));
        }

        if (pos < srcString.length() && srcString.charAt(pos) == '#') {
          // hit comments (e.g. <:# comment :>)
          skipCommentInTag();
        } else {
          tokens.add(createToken(TokenType.OPEN));
        }

        return;
      }

      // in raw context
      char c = srcString.charAt(pos);
      builder.append(c);
      ++pos;

      if (c == '\n') {
        // if (mode == LexerMode.IN_CODE_LINE) {
        // mode = LexerMode.IN_RAW;
        // }

        if (pos + 1 < srcString.length()) {
          Matcher matcherForCodeLineDelimiter = codeLineRe.matcher(srcString.substring(pos + 1));
          if (matcherForCodeLineDelimiter.find()) {
            pos += matcherForCodeLineDelimiter.group(0).length();
            mode = LexerMode.IN_CODE_LINE;
          }
        }

        ++lineNumber;
      }
    }

    // When all of character had eaten
    if (builder.toString().length() > 0) {
      tokens.add(createToken(TokenType.RAW, builder.toString()));
    }
  }

  private void skipCommentInTag() {
    Matcher commentMatcher = commentInTagRe.matcher(srcString.substring(pos));

    if (commentMatcher.find()) {
      this.pos += commentMatcher.group(0).length();
      this.mode = LexerMode.IN_RAW;
    } else {
      // TODO
      // missing closing tag after tag comments
      throw new RuntimeException();
    }
  }

  private Token tokenizeNumber() {
    String target = srcString.substring(pos);

    {
      Matcher matcher = INT_RE.matcher(target);
      if (matcher.find()) {
        String s = matcher.group(0);
        pos += s.length();
        return this.createToken(TokenType.INTEGER, s);
      }
    }

    {
      Matcher matcher = DOUBLE_RE.matcher(target);
      if (matcher.find()) {
        String s = matcher.group(0);
        pos += s.length();
        return createToken(TokenType.DOUBLE, s);
      }
    }

    {
      Matcher matcher = HEX_RE.matcher(target);
      if (matcher.find()) {
        String s = matcher.group(0);
        pos += s.length();
        return createToken(TokenType.HEX, s);
      }
    }

    {
      Matcher matcher = OCTAL_RE.matcher(target);
      if (matcher.find()) {
        String s = matcher.group(0);
        pos += s.length();
        return createToken(TokenType.OCTAL, s);
      }
    }

    {
      Matcher matcher = BINARY_RE.matcher(target);
      if (matcher.find()) {
        String s = matcher.group(0);
        pos += s.length();
        return createToken(TokenType.BINARY, s);
      }
    }

    // TODO
    throw new RuntimeException("Cannot tokenize number");
  }

  private Token tokenizeString() {
    ++pos;

    StringBuilder builder = new StringBuilder();
    boolean finished = false;
    while (pos < srcString.length() && !finished) {
      switch (srcString.charAt(pos)) {
        case '\\':
          if (pos + 1 < srcString.length()) {
            switch (srcString.charAt(pos + 1)) {
              case 't':
                builder.append("\t");
                ++pos;
                break;
              case 'n':
                builder.append("\n");
                ++pos;
                break;
              default:
                ++pos;
                builder.append(srcString.charAt(pos));
                ++pos;
                break;
            }
          } else {
            // TODO
            throw new RuntimeException("Cannot token source after '\\'");
          }
          break;
        case '"':
          ++pos;
          finished = true;
          break;
        default:
          builder.append(srcString.charAt(pos));
          ++pos;
      }
    }

    return this.createToken(TokenType.STRING, builder.toString());
  }

  private Token tokenizeOthers() {
    // keywords
    for (final Entry<TokenType, String> entry : keywords.entrySet()) {
      String keyword = entry.getValue();
      if (srcString.substring(pos).startsWith(keyword)) { // XXX too loose!
        pos += keyword.length();
        return createToken(entry.getKey());
      }
    }

    // ident.
    {
      Matcher matcher = IDENT_RE.matcher(srcString.substring(pos));
      if (matcher.find()) {
        String s = matcher.group(0);
        pos += s.length();
        return createToken(TokenType.IDENT, s);
      }
    }

    // TODO
    throw new RuntimeException("Cannot tokenize template.");
  }

  private Token createToken(TokenType type) {
    return createToken(type, null); // null will be Optional<String>
  }

  private Token createToken(TokenType type, String tokenString) {
    return new Token(type, Optional.ofNullable(tokenString), lineNumber, src.getFileName());
  }
}
