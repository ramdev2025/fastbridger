# Contributing to FastBridger

Thanks for wanting to contribute! Here's how to get started.

## Development Setup

```bash
git clone https://github.com/ramdev2025/fastbridger-spring-boot-starter
cd fastbridger-spring-boot-starter
mvn clean verify   # builds and runs all tests
```

Requirements: Java 17+, Maven 3.9+

## Making Changes

1. Fork the repo and create a branch off `main`
2. Make your changes
3. Add or update tests
4. Run `mvn clean verify` — all tests must pass
5. Open a pull request against `main`

## Releasing (maintainers only)

1. Update the version in `pom.xml` if needed
2. Create a GitHub Release with a tag like `v1.2.3`
3. The publish workflow triggers automatically and pushes to Maven Central

## Secrets needed for publishing

Set these in **GitHub → Settings → Secrets → Actions**:

| Secret | Description |
|--------|-------------|
| `OSSRH_USERNAME` | Sonatype OSSRH username |
| `OSSRH_PASSWORD` | Sonatype OSSRH token (not your password) |
| `MAVEN_GPG_PRIVATE_KEY` | ASCII-armored GPG key (`gpg --armor --export-secret-keys KEY_ID`) |
| `MAVEN_GPG_PASSPHRASE` | GPG key passphrase |

## Code Style

- Follow existing patterns
- Javadoc on all public classes and methods
- No wildcard imports
