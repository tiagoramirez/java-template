# GitHub Actions Setup Guide

This guide provides complete setup instructions for configuring the CI/CD workflows in this repository.

## Overview

This repository uses a single GitHub Actions workflow:

**CI/CD Pipeline** (`.github/workflows/ci-cd.yml`)
- Validates branch names on PR open/update
- Runs tests with 100% coverage enforcement
- Creates Release Candidate tags for release branches on PR open/update
- Submits dependency graph for security scanning
- Creates final version tag when a release branch merges to `main`
- Automatically creates backport PRs when hotfixes merge to `main`

All jobs are event-driven: an `info` job always runs as a gate, pre-merge jobs execute on `opened/synchronize/reopened`, and post-merge jobs execute on `closed` (merged only).

## Prerequisites

Before setting up these workflows, ensure:

- ✅ Repository uses **Java 25** and **Gradle 9.2.0**
- ✅ JaCoCo coverage reporting is configured in `build.gradle`
- ✅ GitHub Actions is enabled for the repository
- ✅ You have **admin access** to configure repository settings

## Part 1: Repository Configuration

### Step 1: Enable GitHub Actions Permissions

1. Navigate to: `Settings` → `Actions` → `General`
2. Under **Workflow permissions**:
   - Select: ✅ **Read and write permissions**
   - Enable: ✅ **Allow GitHub Actions to create and approve pull requests**
3. Click **Save**

**Why**: Allows workflows to create RC tags and backport PRs.

### Step 2: Enable Dependency Graph

1. Navigate to: `Settings` → `Advanced Security`
2. Enable: ✅ **Dependency graph**
3. Enable: ✅ **Dependabot alerts** (recommended)

**Why**: Required for dependency submission job to work.

### Step 3: Configure Branch Protection Rules

Protect `main` and `develop` branches:

#### Protection for `main` branch:

1. Navigate to: `Settings` → `Branches` → **Add rule**
2. **Branch name pattern**: `main`
3. Enable the following:
   - ✅ **Require a pull request before merging**
     - Required approvals: 1 (adjust as needed)
   - ✅ **Require status checks to pass**
     - Select: `check-branches`, `test-and-build`, `dependency-submission`
4. Click **Create** or **Save changes**

#### Protection for `develop` branch:

1. Navigate to: `Settings` → `Branches` → **Add rule**
2. **Branch name pattern**: `develop`
3. Enable the following:
   - ✅ **Require a pull request before merging**
     - Required approvals: 1 (adjust as needed)
   - ✅ **Require status checks to pass**
     - Select: `check-branches`, `test-and-build`, `dependency-submission`
4. Click **Create** or **Save changes**

**Why**: Ensures all PRs pass validation before merging.

### Step 4: Configure Tag Protection

See detailed instructions in [GITHUB_TAG_PROTECTION_SETUP.md](./GITHUB_TAG_PROTECTION_SETUP.md).

**Why**: Protects RC tags from accidental deletion, preserving release audit trail.

## Part 2: Branch Naming Conventions

The workflows enforce these branch naming rules:

| Target Branch | Allowed Source Branches | Example | Purpose |
|---------------|------------------------|---------|---------|
| `develop` | `feature/*`, `hotfix/*` | `feature/add-auth` | Development work |
| `main` | `release/*`, `hotfix/*` | `release/1.2.3` | Production releases |

### Valid Branch Names:

✅ `feature/add-user-authentication`
✅ `feature/improve-monitoring`
✅ `hotfix/fix-health-endpoint`
✅ `release/1.0.0` (semantic versioning required)

### Invalid Branch Names:

❌ `add-feature` (missing `feature/` prefix)
❌ `bugfix/fix-something` (use `hotfix/` instead)
❌ `release/v1.0.0` (don't use `v` prefix)
❌ `Feature/add-auth` (must be lowercase)

## Part 3: Pre-Merge Validation Workflow

### How It Works

Triggers on: **Pull requests to `develop` or `main`**

**Job 1: check-branches**
- Validates branch naming conventions
- For `release/*` → `main`: Creates RC tags (`x.y.z-rc.N`)

**Job 2: test-and-build**
- Runs: `./gradlew clean test --no-daemon`
- Uploads HTML coverage report as artifact
- Enforces 100% coverage (except `hotfix/*` branches)

**Job 3: dependency-submission**
- Submits dependency graph to GitHub for security scanning

### Release Candidate (RC) Tagging

When you create a PR from `release/1.2.3` to `main`:

- **First commit**: Creates tag `1.2.3-rc.1`
- **Update PR**: Creates tag `1.2.3-rc.2`
- **Another update**: Creates tag `1.2.3-rc.3`

Each tag points to the commit at that PR update and includes the PR number in the tag message.

**Use cases**:
- QA can test specific RC versions
- Immutable snapshots for deployment verification
- Audit trail of release branch evolution

### Coverage Enforcement

| Branch Type | Coverage Required | Behavior |
|-------------|------------------|----------|
| `feature/*` | ✅ 100% | Build fails if < 100% |
| `release/*` | ✅ 100% | Build fails if < 100% |
| `hotfix/*` | ⚠️ Exempt | Report generated, not enforced |

**Exemption rationale**: Hotfixes prioritize speed for emergency production fixes.

### Coverage Exclusions

These patterns are automatically excluded from coverage:
- `com/yourorg/*/Application*` (Spring Boot main class)
- `com/yourorg/**/constants/**` (Constants)
- `com/yourorg/**/**/Dto.java` (DTOs ending with `Dto.java`)
- `com/yourorg/**/exceptions/**` (Exception classes)

Configure in `build.gradle`:

```gradle
jacocoTestReport {
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                'com/yourorg/*/Application*',
                'com/yourorg/**/constants/**',
                'com/yourorg/**/**/Dto.java',
                'com/yourorg/**/exceptions/**'
            ])
        }))
    }
}
```

## Part 4: Post-Merge Automation Workflow

### How It Works

Triggers on: **Pull request closed (merged) to `main`**

Two independent jobs run based on the source branch type:

---

#### Job: create-release-tag

**Condition**: Source branch is `release/*`

**Actions**:
1. Extracts version from branch name (e.g., `release/1.2.3` → `1.2.3`)
2. Creates annotated tag `1.2.3` pointing to the merge commit on `main`
3. Tag message includes PR number for traceability

**Tag lifecycle for a release**:
```
release/1.2.3 PR opened    → 1.2.3-rc.1  (pre-merge, by ci-cd.yml)
release/1.2.3 PR updated   → 1.2.3-rc.2  (pre-merge, by ci-cd.yml)
release/1.2.3 PR merged    → 1.2.3       (post-merge, by ci-cd.yml)
```

---

#### Job: create-hotfix-pr

**Condition**: Source branch is `hotfix/*`

**Actions**:
1. Checks if hotfix branch still exists (may be auto-deleted)
2. Checks if backport PR already exists (prevents duplicates)
3. Creates PR: `hotfix/*` → `develop`
   - Title: `[hotfix/branch-name] Backport to develop`
   - Labels: `automated`, `hotfix-backport`
   - Body includes original PR details

**Conflict handling**: If conflicts exist, GitHub UI shows them. Resolve before merging.

---

### Release Workflow Example

```bash
# 1. Create release branch
git checkout -b release/1.2.3 develop
git push -u origin release/1.2.3

# 2. Open PR: release/1.2.3 → main
# ✅ Pre-merge: tag 1.2.3-rc.1 created

# 3. Push additional commits if needed
git push
# ✅ Pre-merge: tag 1.2.3-rc.2 created

# 4. PR approved and merged to main
# ✅ Post-merge: final tag 1.2.3 created on main
```

### Hotfix Workflow Example

```bash
# 1. Create hotfix branch
git checkout -b hotfix/fix-critical-bug main
git push -u origin hotfix/fix-critical-bug

# 2. Make fix and commit
git commit -m "fix: resolve critical production bug"
git push

# 3. Create PR: hotfix/fix-critical-bug → main
# (via GitHub UI)

# 4. PR approved and merged to main
# ✅ Post-merge: backport PR created → hotfix/fix-critical-bug → develop

# 5. Review and merge the automated PR to develop
# (resolve conflicts if any)
```

### Why Post-Merge Automation?

- **Consistency**: Ensures production fixes reach development branch (hotfix)
- **Traceability**: Final version tag marks exact commit released to production (release)
- **Speed**: No manual tagging or PR creation needed
- **Safety**: Shows conflicts in GitHub UI, doesn't auto-merge

## Part 5: Testing Your Setup

### Test 1: Feature Branch Workflow

```bash
# Create feature branch
git checkout -b feature/test-ci develop
echo "# Test" >> README.md
git add README.md
git commit -m "test: verify CI setup"
git push -u origin feature/test-ci

# Create PR: feature/test-ci → develop
# Expected: ✅ All checks pass
```

### Test 2: Release Branch with RC Tagging

```bash
# Create release branch
git checkout -b release/0.0.1 develop
git push -u origin release/0.0.1

# Create PR: release/0.0.1 → main
# Expected: ✅ RC tag 0.0.1-rc.1 created

# Push another commit
echo "# Update" >> README.md
git add README.md
git commit -m "docs: update readme"
git push

# Expected: ✅ RC tag 0.0.1-rc.2 created

# Verify tags
git fetch --tags
git tag -l "0.0.1-rc.*"
```

### Test 3: Hotfix with Automated Backport

```bash
# Create hotfix branch
git checkout -b hotfix/test-backport main
echo "# Hotfix" >> README.md
git add README.md
git commit -m "fix: test hotfix automation"
git push -u origin hotfix/test-backport

# Create PR: hotfix/test-backport → main
# Merge the PR

# Expected: ✅ Automated PR created: hotfix/test-backport → develop
# Check: Labels should include "automated" and "hotfix-backport"
```

### Test 4: Coverage Enforcement

```bash
# Create feature with untested code
git checkout -b feature/test-coverage develop

# Add uncovered code (intentionally)
# Create PR: feature/test-coverage → develop

# Expected: ❌ JaCoCo check fails with < 100% coverage
# Fix by adding tests, then push again
# Expected: ✅ JaCoCo check passes with 100% coverage
```

## Part 6: Troubleshooting

### Issue: Workflow Doesn't Run

**Symptoms**: No checks appear on PR

**Solutions**:
1. Verify workflow files exist in `.github/workflows/`
2. Check Actions tab for errors
3. Ensure branch protection includes workflow as required check
4. Verify GitHub Actions is enabled: `Settings` → `Actions` → `General`

### Issue: RC Tag Creation Fails

**Symptoms**: Error "Permission denied" when pushing tags

**Solutions**:
1. Check workflow permissions: `Settings` → `Actions` → `General`
   - Must be "Read and write permissions"
2. Verify `permissions: contents: write` in workflow file
3. Check if tag pattern is protected (shouldn't be for `*-rc.*` pattern)

### Issue: Backport PR Not Created

**Symptoms**: Hotfix merged to `main` but no automated PR

**Solutions**:
1. Check Actions tab → CI/CD Pipeline workflow
2. Verify branch name starts with `hotfix/`
3. Check if PR was merged (not just closed)
4. Look for errors in workflow logs
5. Verify branch wasn't auto-deleted before workflow ran

### Issue: Coverage Check Always Fails

**Symptoms**: JaCoCo reports < 100% even with tests

**Solutions**:
1. Download coverage report artifact from Actions
2. Open `build/reports/jacoco/test/html/index.html`
3. Identify uncovered lines (highlighted in red)
4. Verify exclusions are configured correctly
5. Check DTOs end with `Dto.java` suffix
6. Check exceptions are in `exceptions/` package

### Issue: Branch Validation Fails

**Symptoms**: Valid branch name rejected

**Solutions**:
1. Verify exact regex match:
   - `feature/name` ✅
   - `Feature/name` ❌ (capital F)
   - `feature-name` ❌ (no slash)
2. Check base branch is exactly `main` or `develop`
3. Review workflow logs for specific error

## Part 7: Customization

### Adjust Coverage Threshold

To change from 100% to 90%:

```yaml
# In .github/workflows/ci-cd.yml
- name: JaCoCo Code Coverage Report
  if: ${{ !startsWith(github.head_ref, 'hotfix/') }}
  uses: PavanMudigonda/jacoco-reporter@v5.0
  with:
    minimum_coverage: 90  # Changed from 100
    fail_below_threshold: true
```

### Add Branch Name Patterns

To support `bugfix/*` branches:

```yaml
# In .github/workflows/ci-cd.yml
- name: Validate branch names and create RC tags
  run: |
    branch_regex_bugfix='^bugfix/.+$'  # Add pattern

    # Update validation logic
    if [[ "$base" == "develop" ]]; then
      if [[ ! "$head" =~ $branch_regex_feature ]] &&
         [[ ! "$head" =~ $branch_regex_hotfix ]] &&
         [[ ! "$head" =~ $branch_regex_bugfix ]]; then
        echo "❌ Invalid branch name"
        exit 1
      fi
    fi
```

### Disable Hotfix Coverage Exemption

Remove the conditional:

```yaml
# Remove this line:
# if: ${{ !startsWith(github.head_ref, 'hotfix/') }}

# JaCoCo will now enforce coverage for all branches
- name: JaCoCo Code Coverage Report
  uses: PavanMudigonda/jacoco-reporter@v5.0
  with:
    minimum_coverage: 100
    fail_below_threshold: true
```

## Part 8: Monitoring and Maintenance

### View Workflow Runs

1. Navigate to: `Actions` tab
2. Select workflow: **CI/CD Pipeline**
3. Review recent runs, success rates, duration

### Monitor RC Tags

```bash
# List all RC tags
git fetch --tags
git tag -l "*-rc.*"

# View tag details
git show 1.2.3-rc.1

# Clean up old RC tags after release (optional)
git tag -d 1.2.3-rc.1
git push origin :refs/tags/1.2.3-rc.1
```

### Update Workflows

When modifying workflows:

1. Create branch: `feature/update-ci`
2. Edit workflow files
3. Test with draft PR
4. Review workflow logs
5. Merge to `develop` after validation

## Best Practices

1. **Never bypass branch protection**: Ensures all code is validated
2. **Test CI changes with draft PRs**: Prevents breaking main workflows
3. **Monitor workflow duration**: Optimize if runs exceed 10 minutes
4. **Keep action versions updated**: Check for security updates monthly
5. **Review automated PRs promptly**: Don't let backport PRs accumulate
6. **Clean up RC tags**: Archive or delete old RC tags after release merges
7. **Document custom changes**: Update this guide if you customize workflows

## Related Documentation

- [GITHUB_TAG_PROTECTION_SETUP.md](./GITHUB_TAG_PROTECTION_SETUP.md) - Detailed tag protection configuration
- [CONTRIBUTING.md](../../CONTRIBUTING.md) - Branch naming and contribution guidelines
- [CLAUDE.md](../../CLAUDE.md) - Full project documentation

## Support

If you encounter issues not covered in this guide:

1. **Check workflow logs**: Actions tab → Select run → View logs
2. **Review audit log**: Settings → Audit log (for permission issues)
3. **GitHub Actions docs**: https://docs.github.com/en/actions
4. **Contact repository admins**: For permission or configuration help

---

**Last Updated**: 2026-02-17
**Workflows Version**: v2.0 (RC tagging + hotfix automation)
