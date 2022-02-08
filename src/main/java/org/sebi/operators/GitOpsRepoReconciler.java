package org.sebi.operators;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.config.dependent.Dependent;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import java.util.Optional;
import javax.inject.Inject;
import org.eclipse.jgit.api.Git;

@ControllerConfiguration(
    dependents = @Dependent(resourceType = Git.class, type = GitDependentResource.class)
)
public class GitOpsRepoReconciler implements Reconciler<GitOpsRepo> {

  @Inject
  KubernetesClient client;

  @Override
  public UpdateControl<GitOpsRepo> reconcile(GitOpsRepo gitOpsRepo, Context context) {
    // check whether we need to apply the resources associated with the specified git repo
    final var resources = context.getSecondaryResource(GitCommittedResources.class)
        .orElseThrow(() -> new IllegalStateException("Couldn't reconcile state"));

    final var lastAppliedCommit = resources.getCommit();
    if (lastAppliedCommit.equals(
        Optional.ofNullable(gitOpsRepo.getStatus()).map(GitOpsRepoStatus::getCommit)
            .orElse(null))) {
      return UpdateControl.noUpdate();
    } else {
      // apply resources
      client.resourceList(resources.getResources())
          .inNamespace(Optional.ofNullable(gitOpsRepo.getSpec().getNamespace()).orElse("default"))
          .createOrReplace();
      gitOpsRepo.setStatus(new GitOpsRepoStatus(lastAppliedCommit));
      return UpdateControl.updateStatus(gitOpsRepo);
    }
  }
}
