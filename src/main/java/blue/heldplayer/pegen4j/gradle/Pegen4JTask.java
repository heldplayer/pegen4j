package blue.heldplayer.pegen4j.gradle;

import blue.heldplayer.pegen4j.GrammarException;
import blue.heldplayer.pegen4j.generator.JavaParserGenerator;
import blue.heldplayer.pegen4j.generator.JavaVisitorGenerator;
import blue.heldplayer.pegen4j.peg.Pegen4JParser;
import blue.heldplayer.pegen4j.peg.Pegen4JParserAstBuilder;
import blue.heldplayer.pegen4j.peg.ast.Grammar;
import blue.heldplayer.pegen4j.unit.GrammarUnit;
import blue.heldplayer.pegen4j.util.DiagnosticMessage;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileType;
import org.gradle.api.tasks.*;
import org.gradle.internal.UncheckedException;
import org.gradle.internal.file.Deleter;
import org.gradle.internal.instrumentation.api.annotations.ToBeReplacedByLazyProperty;
import org.gradle.work.ChangeType;
import org.gradle.work.FileChange;
import org.gradle.work.InputChanges;
import org.jspecify.annotations.NonNull;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

public abstract class Pegen4JTask extends SourceTask {

  private final FileCollection stableSources = getProject().files((Callable<Object>) this::getSource);

  private File outputDirectory;

  /**
   * Returns the directory to generate the parser source files into.
   *
   * @return The output directory.
   */
  @OutputDirectory
  @ToBeReplacedByLazyProperty
  public File getOutputDirectory() {
    return this.outputDirectory;
  }

  /**
   * Specifies the directory to generate the parser source files into.
   *
   * @param outputDirectory The output directory. Must not be null.
   */
  public void setOutputDirectory(File outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  @TaskAction
  public void execute(InputChanges inputChanges) throws IOException {
    Set<File> grammarFiles = new HashSet<>();
    FileCollection stableSources = getStableSources();
    if (inputChanges.isIncremental()) {
      boolean rebuildRequired = false;
      for (FileChange fileChange : inputChanges.getFileChanges(stableSources)) {
        if (fileChange.getFileType() == FileType.FILE) {
          if (fileChange.getChangeType() == ChangeType.REMOVED) {
            rebuildRequired = true;
            break;
          }
          grammarFiles.add(fileChange.getFile());
        }
      }
      if (rebuildRequired) {
        try {
          getDeleter().ensureEmptyDirectory(getOutputDirectory());
        } catch (IOException ex) {
          throw UncheckedException.throwAsUncheckedException(ex);
        }
        grammarFiles.addAll(stableSources.getFiles());
      }
    } else {
      grammarFiles.addAll(stableSources.getFiles());
    }

    Path outputDir = this.getOutputDirectory().toPath();

    for (File grammarFile : grammarFiles) {
      var inputPath = grammarFile.toPath();

      var source = Files.readString(inputPath);
      var parser = new Pegen4JParser(inputPath.toUri(), source);
      var tree = parser.grammar_unit();

      // grammar_unit() == null is the only reliable failure signal: hasError() tracks
      // the furthest *backtracked* expect failure, which is set even on a
      // successful parse (e.g. the grammar_element* loop terminating at EOF).
      if (tree == null) {
        var failure = parser.getFurthestFailure();
        throw new GrammarException(DiagnosticMessage.render(failure == null ? java.util.List.of() : java.util.List.of(failure), source, false));
      }
      Grammar grammar = (Grammar) new Pegen4JParserAstBuilder().visit(tree);

      var grammarUnit = GrammarUnit.from(grammar);
      var problems = grammarUnit.getProblems();
      if (!problems.isEmpty()) {
        throw new GrammarException(problems.size() + " problem(s) in grammar:\n" + DiagnosticMessage.render(problems, source, false));
      }

      var cst = new JavaParserGenerator(grammarUnit);
      var generated = new java.util.LinkedHashMap<>(cst.generateFiles());
      generated.putAll(new JavaVisitorGenerator(cst).generateFiles());
      for (var entry : generated.entrySet()) {
        var outputPath = outputDir.resolve(entry.getKey().replace('.', File.separatorChar) + ".java");
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, entry.getValue());
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Internal("tracked via stableSources")
  @ToBeReplacedByLazyProperty
  public @NonNull FileTree getSource() {
    return super.getSource();
  }

  /**
   * The sources for incremental change detection.
   *
   * @since 6.0
   */
  @SkipWhenEmpty
  @IgnoreEmptyDirectories
  @PathSensitive(PathSensitivity.RELATIVE)
  @InputFiles
  protected FileCollection getStableSources() {
    return stableSources;
  }

  @Inject
  protected abstract Deleter getDeleter();

}
