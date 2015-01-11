package net.moznion.jakotem;

import java.util.Optional;

public class Token {
  private final TokenType type;
  private final Optional<String> tokenString;
  private final int lineNumber;
  private final Optional<String> fileName;

  public Token(TokenType type, Optional<String> tokenString, int lineNumber, Optional<String> fileName) {
    this.type = type;
    this.tokenString = tokenString;
    this.lineNumber = lineNumber;
    this.fileName = fileName;
  }
}
