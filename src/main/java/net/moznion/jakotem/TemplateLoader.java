package net.moznion.jakotem;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.management.RuntimeErrorException;

public class TemplateLoader {
  final List<Path> includePaths;
  final TemplateCache templateCache;

  public TemplateLoader(List<Path> includePaths, TemplateCache templateCache) {
    this.includePaths = includePaths;
    this.templateCache = templateCache;
  }

  public OpcodeSequence compile(String fileName, Syntax syntax) throws IllegalSyntaxException {
    for (Path path : includePaths) {
      String fullpath = path.toString() + "/" + fileName;

      // TODO
      // Support cache mode
      {
        OpcodeSequence opcodeSequence = this.templateCache.get(fullpath.toString());
        if (opcodeSequence != null) {
          return opcodeSequence;
        }
      }

      File fullpathFile = new File(fullpath);
      if (fullpathFile.exists()) {
        OpcodeSequence opcodeSequence = this.compileFile(fullpath, syntax);
        this.templateCache.set(fullpath.toString(), opcodeSequence);
        return opcodeSequence;
      }
    }

    // TODO throw more suitable exception
    throw new RuntimeErrorException(null, "Nanka okashi yo!");
  }

  private OpcodeSequence compileFile(String fullpath, Syntax syntax) throws IllegalSyntaxException {
    try {
      byte[] bytes = Files.readAllBytes(Paths.get(fullpath));
      String src = new String(bytes, StandardCharsets.UTF_8); // TODO
      Source source = Source.fromFile(fullpath.toString());
      List<Token> tokens = syntax.tokenize(source, src);
      Node ast = syntax.parse(source, tokens);
      return syntax.compile(source, ast);
    } catch (IOException e) {
      // TODO throw more suitable exception
      throw new RuntimeException("Cannot load " + fullpath + " : " + e.getMessage());
    }
  }
}
