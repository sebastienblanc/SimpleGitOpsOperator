# Simple GitOps Operator

A simple implementation in Quarkus of the GitOps pattern. Once the operator is depoloyed, you can apply a `GitOpsRepo` Custom Resource to start watching a Git Repo and applying all the related resources to your cluster.

# Custom Resource

```
apiVersion: "operators.sebi.org/v1"
kind: GitOpsRepo
metadata:
  name: my-gitops-repo
spec:
  repo: https://github.com/sebastienblanc/my-gitops-test.git
  ref: origin/main
  resourceDir: yamls
  namespace: prod
```
* `ref` is optional, if not presente it will checkout the `head` of your repo. When set, be sure to use a remote revision.
* `resourceDir` is optional, if not present it will scan for your yamls resource at the root of the repo.
* `namespace` is optional, if not present, `default` namespace will be used.

# Using in dev mode

Make sure to have k8s cluster running (i.e Minikube)

`mvn quarkus:dev` , the CRD will be automatically applied.

`kubectl apply gitOpsRepo -f cr-sample.yaml`

Try with your own Git repo, make a change or add a new yaml resource, commit, push and see the magic happen ...