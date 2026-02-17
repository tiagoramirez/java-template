# GitHub Tag Protection Setup Guide

This guide provides step-by-step instructions for repository administrators to configure tag protection for Release Candidate (RC) tags.

> **Note**: This is part of the complete CI/CD setup. See [GITHUB_ACTIONS_SETUP.md](./GITHUB_ACTIONS_SETUP.md) for the full workflow configuration.

## Overview

Release Candidate tags (`x.y.z-rc.N`) are automatically created by the CI/CD pipeline when PRs from `release/*` branches are opened or updated. These tags must be protected to maintain an immutable audit trail of release candidates.

## Why Tag Protection?

- **Immutability**: Prevents accidental deletion or modification of RC tags
- **Audit Trail**: Preserves historical snapshots of release branches at each PR update
- **Traceability**: Links PR numbers to specific tagged commits for debugging
- **Compliance**: Ensures release history cannot be tampered with

## Setup Instructions

### Step 1: Navigate to Repository Settings

1. Go to your GitHub repository
2. Click **Settings** (top navigation bar)
3. In the left sidebar, navigate to **Rules** → **Rulesets**

### Step 2: Create New Ruleset

1. Click **New ruleset** button (top right)
2. Choose **New tag ruleset**

### Step 3: Configure Ruleset Basics

**Ruleset Name**: `Protect Release Candidate Tags`

**Enforcement status**: ✅ Active

**Target**:
- Select **Tags**
- **Include by pattern**: Enter `*-rc.*`
  - This matches all tags like `1.2.3-rc.1`, `2.0.0-rc.15`, etc.

### Step 4: Configure Rules

Enable the following rules:

#### Required Rules

| Rule | Setting | Purpose |
|------|---------|---------|
| **Restrict deletions** | ✅ Enabled | Prevents tag deletion |
| **Restrict updates** | ✅ Enabled | Prevents force-push to tags |

#### Optional Rules (Recommended)

| Rule | Setting | Purpose |
|------|---------|---------|
| **Block force pushes** | ✅ Enabled | Additional protection layer |

### Step 5: Configure Bypass List

**Who can bypass these rules?**

- ✅ **Repository administrators**
  - Allows emergency tag management if absolutely necessary
  - All bypass actions are logged in audit trail

⚠️ **Do not add**:
- ❌ `github-actions[bot]` (bot already has necessary permissions)
- ❌ Regular contributors or maintainers

### Step 6: Review and Save

1. Review all settings in the preview pane
2. Ensure pattern `*-rc.*` is correctly configured
3. Click **Create ruleset**

## Verification

After creating the ruleset, verify it's working:

### Test 1: Verify Ruleset is Active

1. Navigate to **Settings** → **Rules** → **Rulesets**
2. Confirm `Protect Release Candidate Tags` shows **Active** status
3. Click the ruleset name to view configuration

### Test 2: Attempt Tag Deletion (Optional)

If an RC tag already exists:

```bash
# This should fail with a protected tag error
git push origin :refs/tags/1.0.0-rc.1

# Expected error: "Cannot delete a protected tag"
```

### Test 3: Create Release PR

1. Create a branch: `git checkout -b release/0.0.1`
2. Push to remote: `git push -u origin release/0.0.1`
3. Open PR to `main`
4. Wait for CI to complete
5. Verify tag `0.0.1-rc.1` was created: `git fetch --tags && git tag -l "0.0.1-rc.*"`
6. Attempt to delete the tag (should fail)

## Troubleshooting

### Issue: Ruleset Not Applying

**Symptoms**: Can still delete RC tags

**Solutions**:
1. Check **Enforcement status** is set to **Active** (not Draft)
2. Verify pattern is `*-rc.*` (not `*-rc*` which is too broad)
3. Refresh browser cache and try again
4. Check repository audit log for ruleset changes

### Issue: GitHub Actions Cannot Create Tags

**Symptoms**: CI fails with "Permission denied" when pushing tags

**Solutions**:
1. Verify `github-actions[bot]` is **NOT** in the bypass list
2. Ensure workflow has `permissions: contents: write` (already configured)
3. Check workflow logs for specific error message
4. Repository must have Actions enabled with write permissions

### Issue: Need to Delete an RC Tag

**Symptoms**: Accidentally created wrong RC tag

**Solutions**:
1. Only repository administrators can bypass protection
2. Navigate to tag in GitHub UI: `Repository` → `Tags`
3. Click **Delete** (prompts for confirmation)
4. Action is logged in audit trail
5. Consider creating corrective tag instead of deletion

## Pattern Examples

The pattern `*-rc.*` matches:

✅ **Valid Matches**:
- `1.0.0-rc.1`
- `2.5.3-rc.15`
- `10.20.30-rc.1`
- `1.0.0-beta-rc.1` (broader match, acceptable)

❌ **Does NOT Match**:
- `1.0.0` (release tags without `-rc` are not protected)
- `1.0.0-alpha.1` (different suffix)
- `rc-1.0.0` (pattern is at the end)

## Best Practices

1. **Never bypass protection** unless absolutely necessary (emergency rollback)
2. **Document bypass actions** in PR or issue when they occur
3. **Review audit log** periodically for unauthorized bypass attempts
4. **Test protection** after initial setup with a dummy release branch
5. **Communicate to team** that RC tags are immutable

## Additional Configuration (Optional)

### Protect Final Release Tags

If you want to protect final release tags (`x.y.z` without `-rc`):

1. Create a second ruleset: `Protect Release Tags`
2. Target pattern: `[0-9]*.[0-9]*.[0-9]*` (matches semantic versions)
3. Apply same rules (restrict deletions, restrict updates)
4. Bypass: Repository administrators only

### Slack Notifications on Bypass

Set up GitHub audit log monitoring to alert on bypass events:

1. Use GitHub Apps like "Audit Log Streaming"
2. Filter for ruleset bypass events
3. Send alerts to team Slack channel

## Related Documentation

- [CONTRIBUTING.md](../CONTRIBUTING.md) - Branch naming and workflow
- [CLAUDE.md](../CLAUDE.md) - Full CI/CD pipeline documentation
- `.github/workflows/ci-cd.yml` - RC tag creation, release tagging, hotfix backport automation

## Questions?

If you encounter issues not covered in this guide:
1. Check GitHub's [official documentation on rulesets](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets)
2. Review repository audit log for relevant events
3. Contact repository administrators

---

**Last Updated**: 2026-02-17
**CI/CD Version**: Pre-Merge Validation v2.0 (with RC tagging)
