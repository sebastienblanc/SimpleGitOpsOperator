package org.sebi.operators;

import io.fabric8.kubernetes.api.model.KubernetesList;
import java.util.Objects;

public class GitCommittedResources {
  private final GitOpsRepoSpec spec;
  private final String commit;
  private final KubernetesList resources;

  public GitCommittedResources(GitOpsRepoSpec spec, String commit, KubernetesList resources) {
    this.spec = spec;
    this.commit = commit;
    this.resources = resources;
  }

  public KubernetesList getResources() {
    return resources;
  }

  public String getCommit() {
    return commit;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GitCommittedResources that = (GitCommittedResources) o;
    return spec.equals(that.spec) && commit.equals(that.commit) && resources.equals(that.resources);
  }

  @Override
  public int hashCode() {
    return Objects.hash(spec, commit, resources);
  }
}
