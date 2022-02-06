package org.sebi.operators;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1")
@Group("operators.sebi.org")
@Kind("GitOpsRepo")
@Plural("gitopsrepoes")
public class GitOpsRepo extends CustomResource<GitOpsRepoSpec, GitOpsRepoStatus> implements Namespaced {}

