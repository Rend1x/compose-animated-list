# Releasing

Releases are published by GitHub Actions when a tag matching `v*` is pushed.
The tag version must match `VERSION_NAME` in `gradle.properties`, and each
release must be prepared on a matching release branch: `release/<version>`.

For example:

- `VERSION_NAME=0.1.1`
- release branch: `release/0.1.1`
- release tag: `v0.1.1`

## Recommended Flow

1. Create or switch to the release branch:

   ```bash
   git switch -c release/0.1.1
   ```

2. Update `VERSION_NAME` in `gradle.properties`.

3. Commit the version change:

   ```bash
   git add gradle.properties
   git commit -m "Release 0.1.1"
   ```

4. Create and push the release branch and tag:

   ```bash
   ./gradlew releaseTag -Prelease.push=true
   ```

The `releaseTag` task runs `releaseCheck`, verifies that the working tree is
clean, verifies that the current branch is `release/<VERSION_NAME>`, creates an
annotated tag from `VERSION_NAME`, pushes the release branch, and pushes the
tag.

After the tag is pushed, GitHub Actions starts the publish workflow
automatically. The workflow verifies that the tag points to the HEAD of the
matching release branch, then publishes and releases the artifacts to Maven
Central.

After the release succeeds, GitHub Actions creates a GitHub Release for the tag
and then creates a pull request from the release branch back to `main`.

Automatic pull request creation requires this repository setting:
`Settings -> Actions -> General -> Workflow permissions -> Allow GitHub Actions
to create and approve pull requests`.

If that setting is disabled, Maven Central publishing and GitHub Release
creation can still succeed, but the PR must be created manually.

## Manual Flow

If you do not want `releaseTag` to push automatically:

```bash
./gradlew releaseTag
git push origin HEAD:refs/heads/release/0.1.1
git push origin v0.1.1
```

The tag must point to the HEAD of `release/0.1.1`. If the branch and tag do not
match, GitHub Actions fails before publishing.

If automatic PR creation is not enabled for GitHub Actions, create the PR
manually after the release succeeds:

```bash
gh pr create \
  --base main \
  --head release/0.1.1 \
  --title "Release 0.1.1" \
  --body "Merge release branch after Maven Central publication succeeded."
```

## Release Gate

The publish job does not start until the release checks pass. Release checks
include tests, compilation, API checks, lint, and static analysis. If the GitHub
Actions workflow fails, the release should be considered failed and the
artifacts were not successfully released.

Maven Central publishing can stay in `PUBLISHING` for a while after validation.
The workflow waits up to 60 minutes before treating that as a timeout.

## Manual GitHub Actions Run

The publish workflow can still be started manually from GitHub Actions, but only
from a `release/<version>` branch. The branch version must match `VERSION_NAME`
in `gradle.properties`.

Use `automatic_release=false` to publish to Central Portal without releasing,
or `automatic_release=true` to publish and release immediately. A pull request
back to `main` and a GitHub Release are created only after an actual release, so
they run for tag releases and manual runs with `automatic_release=true`.
