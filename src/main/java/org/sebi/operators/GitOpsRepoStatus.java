package org.sebi.operators;

public class GitOpsRepoStatus {

    // Add Status information here
  private final String commit;

  public GitOpsRepoStatus(String commit) {
    this.commit = commit;
  }

  public String getCommit() {
    return commit;
  }
}
