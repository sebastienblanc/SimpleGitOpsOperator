package org.sebi.operators;

import java.util.Objects;

public class GitOpsRepoSpec {
    
    private String repo;

    private String ref;

    private String namespace;

    private String resourceDir;

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getResourceDir() {
        return resourceDir;
    }

    public void setResourceDir(String resourceDir) {
        this.resourceDir = resourceDir;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GitOpsRepoSpec that = (GitOpsRepoSpec) o;
        return repo.equals(that.repo) && Objects.equals(ref, that.ref)
            && Objects.equals(namespace, that.namespace) && resourceDir.equals(
            that.resourceDir);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repo, ref, namespace, resourceDir);
    }
}
