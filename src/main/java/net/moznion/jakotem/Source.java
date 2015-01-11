package net.moznion.jakotem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Source {
  private final SourceType type;
  private final String source;

  enum SourceType {
    FROM_FILE, FROM_STRING
  }

  private Source(SourceType type, String source) {
    this.type = type;
    this.source = source;
  }

  public static Source fromString(String source) {
    return new Source(SourceType.FROM_STRING, source);
  }

  public static Source fromFile(String filePath) {
    return new Source(SourceType.FROM_FILE, filePath);
  }

  public List<String> getSourceLines() throws IOException {
    if (type == SourceType.FROM_FILE) {
      return Files.readAllLines(new File(source).toPath());
    } else {
      return Arrays.asList(source.split("\r?\n"));
    }
  }

  public String getTargetLines(int line) throws IOException {
    StringBuilder buf = new StringBuilder();
    List<String> lines = getSourceLines();
    for (int i = Math.max(0, line - 3); i < Math.min(lines.size(), line + 3); ++i) {
      buf.append(i == line - 1 ? "* " : "  ");
      buf.append(lines.get(i) + "\n");
    }
    return new String(buf);
  }

  public Optional<String> getFileName() {
    if (this.type == SourceType.FROM_FILE) {
      return Optional.of(source);
    } else {
      return Optional.empty();
    }
  }
}
