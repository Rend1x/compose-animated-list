# Releasing

Releases are published by GitHub Actions when a tag matching `v*` is pushed.
The tag version should match `VERSION_NAME` in `gradle.properties`.

## Manual release

1. Update `VERSION_NAME` in `gradle.properties`.
2. Commit the version change.
3. Run release checks:

   ```bash
   ./gradlew releaseCheck
   ```

4. Create and push the release tag:

   ```bash
   git tag -a v0.1.1 -m "Release v0.1.1"
   git push origin v0.1.1
   ```

GitHub Actions will publish and release the artifacts to Maven Central.

## Gradle helper

The `releaseTag` task runs `releaseCheck`, verifies that the working tree is
clean, creates an annotated tag from `VERSION_NAME`, and can optionally push it.

Recommended release flow:

1. Update `VERSION_NAME` in `gradle.properties`.
2. Commit and push the version change:

   ```bash
   git add gradle.properties
   git commit -m "Release 0.1.1"
   git push origin main
   ```

3. Create and push the release tag:

   ```bash
   ./gradlew releaseTag -Prelease.push=true
   ```

After the tag is pushed, GitHub Actions starts the publish workflow
automatically. The workflow publishes and releases the artifacts to Maven
Central.

The publish job does not start until the release checks pass. Release checks
include tests, compilation, API checks, lint, and static analysis. If the GitHub
Actions workflow fails, the release should be considered failed and the
artifacts were not successfully released.

Create the tag locally:

```bash
./gradlew releaseTag
```

Create and push the tag:

```bash
./gradlew releaseTag -Prelease.push=true
```

For example, `VERSION_NAME=0.1.1` creates the tag `v0.1.1`.

## Manual GitHub Actions run

The publish workflow can still be started manually from GitHub Actions.
Use `automatic_release=false` to publish to Central Portal without releasing,
or `automatic_release=true` to publish and release immediately.
