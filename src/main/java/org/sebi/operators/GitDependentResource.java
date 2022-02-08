package org.sebi.operators;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.polling.PerResourcePollingEventSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.eclipse.jgit.api.Git;

public class GitDependentResource implements DependentResource<GitCommittedResources, GitOpsRepo> {

  @Override
  public EventSource initEventSource(EventSourceContext<GitOpsRepo> context) {
    return new PerResourcePollingEventSource<>(
        gitOpsRepo -> desired(gitOpsRepo, null), context.getPrimaryCache(), 10 * 1000,
        GitCommittedResources.class);
  }

  @Override
  public Optional<GitCommittedResources> desired(GitOpsRepo primary, Context context) {
    final var spec = primary.getSpec();
    try {
      var dir = context.getMandatory("local-checkout", Path.class);

      final var resourceDir = spec.getResourceDir();
      if (resourceDir != null) {
        dir = dir.resolve(resourceDir);
      }

      final var list = new KubernetesListBuilder();
      Files.walk(dir).forEach(path -> {
        createResource(path.toFile()).ifPresent(list::addToItems);
      });

      final var head = context.getMandatory("head-commit", String.class);
      return Optional.of(new GitCommittedResources(spec, head, list.build()));
    } catch (Exception e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  private Optional<HasMetadata> createResource(File file) {
    if (!file.isDirectory() && isYaml(file.getName())) {
      try {
        return Optional.of(Serialization.unmarshal(new FileInputStream(file)));
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }
    return Optional.empty();
  }

  private boolean isYaml(String fileName) {
    return Optional.ofNullable(fileName)
        .filter(f -> f.contains("."))
        .map(f -> f.substring(fileName.lastIndexOf(".") + 1))
        .filter(s -> s.equals("yaml") || s.equals("yml"))
        .isPresent();
  }

  @Override
  public boolean match(GitCommittedResources actual, GitOpsRepo primary, Context context) {
    try {
      // ideally, we'd be able to retrieve the commit id associated with the repo / ref without cloning the repo locally
      // maybe using git ls-remote
      File tmpDir = Files.createTempDirectory("tmpgit").toFile();
      tmpDir.deleteOnExit();
      Path dir = Paths.get(tmpDir.getAbsolutePath());


      final var spec = primary.getSpec();
      final var repo = spec.getRepo();
      Git git = Git.cloneRepository().setDirectory(tmpDir).setURI(repo).call();
      final var ref = spec.getRef();
      if (ref != null) {
        git.checkout().setName(ref).call();
      }

      // add dir to context to access it later if needed
      context.put("local-checkout", dir);

      final var head = git.getRepository().resolve("HEAD").toObjectId().getName();
      // also store the head commit in context
      context.put("head-commit", head);

      return actual.getCommit().equals(head);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
