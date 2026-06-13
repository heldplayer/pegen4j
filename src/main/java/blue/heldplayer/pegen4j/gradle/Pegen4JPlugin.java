package blue.heldplayer.pegen4j.gradle;

import blue.heldplayer.pegen4j.gradle.internal.DefaultPegen4JSourceDirectorySet;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.tasks.DefaultSourceSet;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

// Based on https://github.com/gradle/gradle/blob/master/platforms/jvm/antlr/src/main/java/org/gradle/api/plugins/antlr/AntlrPlugin.java
public class Pegen4JPlugin implements Plugin<@NotNull Project> {
  public static final String CONFIGURATION_NAME = "pegen4j";
  private final ObjectFactory objectFactory;

  @Inject
  public Pegen4JPlugin(ObjectFactory objectFactory) {
    this.objectFactory = objectFactory;
  }

  @Override
  public void apply(@NotNull Project project) {
    project.getPluginManager().apply(JavaLibraryPlugin.class);


    project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets().all(
        sourceSet -> {
          // for each source set we will:
          // 1) Add a new 'pegen4j' virtual directory mapping
          Pegen4JSourceDirectorySet pegen4jSourceSet = createSourceDirectorySet(((DefaultSourceSet) sourceSet).getDisplayName(), this.objectFactory);
          sourceSet.getExtensions().add(Pegen4JSourceDirectorySet.class, Pegen4JSourceDirectorySet.NAME, pegen4jSourceSet);
          pegen4jSourceSet.srcDir("src/" + sourceSet.getName() + "/pegen4j");
          sourceSet.getAllSource().source(pegen4jSourceSet);

          // 2) create a Pegen4JTask for this sourceSet following the gradle
          //    naming conventions via call to sourceSet.getTaskName()
          final String taskName = sourceSet.getTaskName("generate", "Pegen4JParser");

          // 3) Register a source-generating task, and
          TaskProvider<Pegen4JTask> generateTask = project.getTasks().register(taskName, Pegen4JTask.class, task -> {
            task.setDescription("Processes the " + sourceSet.getName() + " Pegen4J grammars.");
            task.setGroup("pegen4j");
            // 3.1) point the task at the antlr source set
            task.setSource(pegen4jSourceSet);
            // 3.2) Use convention mapping so layout.buildDirectory changes are
            //      picked up even if the task was realized eagerly.
            task.getConventionMapping().map("outputDirectory", () ->
                project.getLayout().getBuildDirectory()
                    .dir("generated-src/pegen4j/" + sourceSet.getName())
                    .get().getAsFile());
          });

          // 4) Add that task's outputs to the Java source set
          sourceSet.getJava().srcDir(generateTask.map(task -> project.file(project.relativePath(task.getOutputDirectory()))));
        }
    );
  }

  private static Pegen4JSourceDirectorySet createSourceDirectorySet(String parentDisplayName, ObjectFactory objectFactory) {
    String name = parentDisplayName + ".pegen4j";
    String displayName = parentDisplayName + " Pegen4J source";
    Pegen4JSourceDirectorySet sourceSet = objectFactory.newInstance(DefaultPegen4JSourceDirectorySet.class, objectFactory.sourceDirectorySet(name, displayName));
    sourceSet.getFilter().include("**/*.peg");
    return sourceSet;
  }
}
