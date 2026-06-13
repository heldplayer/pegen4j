package blue.heldplayer.pegen4j.gradle.internal;

import blue.heldplayer.pegen4j.gradle.Pegen4JSourceDirectorySet;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.tasks.TaskDependencyFactory;

import javax.inject.Inject;

public abstract class DefaultPegen4JSourceDirectorySet extends DefaultSourceDirectorySet implements Pegen4JSourceDirectorySet {

  @Inject
  public DefaultPegen4JSourceDirectorySet(SourceDirectorySet sourceSet, TaskDependencyFactory taskDependencyFactory) {
    super(sourceSet, taskDependencyFactory);
  }

}
