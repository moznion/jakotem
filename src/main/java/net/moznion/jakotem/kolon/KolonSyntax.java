package net.moznion.jakotem.kolon;

import net.moznion.jakotem.IllegalSyntaxException;
import net.moznion.jakotem.Node;
import net.moznion.jakotem.OpcodeSequence;
import net.moznion.jakotem.Source;
import net.moznion.jakotem.Syntax;
import net.moznion.jakotem.Token;

import java.util.List;

public class KolonSyntax implements Syntax {
  private final String openTag;
  private final String closeTag;
  private final String codeLineDelimiter;

  public KolonSyntax(String openTag, String closeTag, String codeLineDelimiter) {
    this.openTag = openTag;
    this.closeTag = closeTag;
    this.codeLineDelimiter = codeLineDelimiter;
  }

  public KolonSyntax() {
    this("<:", ":>", ":");
  }

  @Override
  public List<Token> tokenize(Source src, String srcString) {
    return KolonLexer.builder()
        .src(src)
        .srcString(srcString)
        .openTag(openTag)
        .closeTag(closeTag)
        .codeLineDelimiter(codeLineDelimiter)
        .build()
        .tokenize();
  }
  
  @Override
  public Node parse(Source source, List<Token> tokens) throws IllegalSyntaxException {
    // FIXME this is fake
    return new Node();
  }
  
  @Override
  public OpcodeSequence compile(Source source, Node ast) throws IllegalSyntaxException {
    // FIXME this is fake
    return new OpcodeSequence();
  }
}
