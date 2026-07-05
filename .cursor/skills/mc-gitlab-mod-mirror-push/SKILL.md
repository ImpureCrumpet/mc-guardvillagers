---
name: mc-gitlab-mod-mirror-push
description: Pushes local main to a GitLab mirror remote via SSH (workspace default) or HTTPS+PAT.
triggers:
  - GitLab mirror
  - git push mirror
  - PAT
  - .env.gitlab-mirror
  - SSH remote
  - HTTPS remote
  - force push mirror
dependencies: []
version: "1.4.0"
---

# GitLab mod mirror push

**Scope:** Any **Git** repo where **`main`** is the branch to publish (typical Fabric mod layout). The **canonical development remote** may be GitHub; this workflow adds or updates a **GitLab mirror** so the project stays open source while **only** the GitLab account that hosts your Minecraft mods exposes the tree.

**Not** Modrinth uploads, **not** registry-driven — deploy to mod repos via **`mm-deploy-fabric-skills-to-repo`** (same **`mc-gitlab-mod-mirror-push`** name on the target). The skill **embeds** a committed example template; Minder also ships **`config/gitlab-mirror.env.example`** for an optional **shared PAT** layout (mirror URL stays per mod).

## When to use

- You have **published** (or merged) work on **`main`** on your primary forge (often GitHub) and want the same **`main`** on an **existing GitLab project** (empty or to be overwritten).

## Auth method — check this first

| Method | When | How |
|--------|------|-----|
| **SSH** (workspace default) | An SSH **`gitlab`** remote already exists, or `ssh -T git@gitlab.com` greets you | `git push gitlab main:main` — no env file, no token URL |
| **HTTPS + PAT** (portable fallback) | SSH unreliable, or no key on this host | Source **`.env.gitlab-mirror`**; push via `oauth2:$TOKEN@…` URL |

**This workspace uses SSH.** Mods push through an SSH **`gitlab`** remote `git@gitlab.com:<group>/<repo>.git`, where **`<group>`** is the single GitLab group that hosts your Minecraft mods; no **`.env.gitlab-mirror`** or shared PAT file is kept here. Learn **`<group>`** by reading the host/path of any populated **`mods[].gitlab_url`** in **`mods.yaml`** (they all share one group). Confirm SSH once with `ssh -o StrictHostKeyChecking=accept-new -T git@gitlab.com` (a `Welcome to GitLab, @user!` line means keys are set up), then prefer the SSH path. Only fall back to HTTPS+PAT when SSH does not authenticate.

## Running from Minecraft Minder

When the user names a mod (or **`executive_focus`** applies) and the task is “push to GitLab”:

1. Resolve **`mods[].local_path`** → mod repo root (expand `~/…`).
2. Read **`mods[].gitlab_url`** — canonical project URL. **It may be empty even though the GitLab project already exists.** When empty, derive **`<group>`** from another populated **`mods[].gitlab_url`** and probe `https://gitlab.com/<group>/<repo-name>` with an **anonymous** `git ls-remote <url>.git HEAD` (public projects answer without auth); if it resolves, use it and write it back to **`gitlab_url`** in the registry.
3. Read **`mods[].github_url`** — primary remote; **`main`** should be synced to **GitHub `origin`** before mirroring.
4. After a successful mirror push, append a dated **`mods[].notes`** line and set **`gitlab_url`** if it was empty (Minder does not auto-edit the registry).

**Do not** run the mirror push from the Minder repo root unless that repo is the target.

## Bootstrap (example file + `.gitignore`) — agent duty

The **`.env.gitlab-mirror`** is consumed only by the **HTTPS+PAT** path; an SSH push never reads it. Still, bootstrapping keeps the repo ready for either method and is cheap, so create the template + `.gitignore` entries even when you push via SSH (these files stay **uncommitted** in the mod working tree — they don't affect the `main` HEAD you push). Skip only if the user says not to.

When **`gitlab-mirror.env.example`** is missing and a template is needed:

1. **Repository root** — the directory that contains **`.git`** (the mod project root).

2. **Create `gitlab-mirror.env.example`** at the **repo root** — copy the **Embedded example** block below verbatim (placeholders only). This file **is** meant to be **committed**.

3. **`.gitignore`** — ensure the file exists at the repo root. Ensure it includes both **`.env`** and **`.env.*`** (add them if missing). That is enough for real secrets in **`.env.gitlab-mirror`**, which matches **`.env.*`**. Do not add GitLab-specific ignore lines beyond that.

4. **`chmod 600`** on **`.env.gitlab-mirror`** once the user creates it from the example.

Many mod repos in this workspace **lack** **`.env` / `.env.*` in `.gitignore`** and **lack** **`gitlab-mirror.env.example`** — run the bootstrap steps when setting up or before the first mirror push.

## Environment file

Never commit secrets. **`chmod 600`** on env files you keep on disk.

### Per-mod (default)

**Mod repo root:**

- **`.env.gitlab-mirror`** — **`GITLAB_MIRROR_URL`** + token; ignored when **`.env.*`** is in **`.gitignore`**.
- **`gitlab-mirror.env.example`** — placeholders only; **committed** (generated from **Embedded example** below).

### Optional shared PAT (Minecraft Minder)

Same spirit as Modrinth token layout:

- **`~/.config/minecraft-minder/gitlab-mirror.env`** — **`GITLAB_TOKEN`** / **`GITLAB_PAT`** only (copy from **`config/gitlab-mirror.env.example`**).
- **`GITLAB_MIRROR_URL`** still comes from the mod’s **`.env.gitlab-mirror`** or **`mods.yaml`** **`gitlab_url`** when invoking from Minder.

Load order when sourcing env for a push:

1. User-supplied path (if any).
2. Mod repo **`.env.gitlab-mirror`**.
3. For token only: **`~/.config/minecraft-minder/gitlab-mirror.env`** if the mod file has URL but no token.

The user may point to another path (e.g. **`~/.config/...`**); load whatever path they give.

### Variables

| Variable | Required | Meaning |
|----------|----------|---------|
| **`GITLAB_MIRROR_URL`** | Yes | HTTPS URL of the GitLab repo **without** credentials, e.g. `https://gitlab.com/your-group/your-mod.git` |
| **`GITLAB_TOKEN`** or **`GITLAB_PAT`** | Yes for HTTPS | [Personal access token](https://docs.gitlab.com/user/profile/personal_access_tokens.html) with **`write_repository`** (minimal for push) or **`api`**. |
| **`GITLAB_PROJECT_ID`** | No | Numeric project ID from the GitLab project page. **Not used by `git push`** — optional for your notes, scripts, or future API steps. |
| **`GITLAB_PUSH_MODE`** | No | `normal` (default), `force-with-lease`, or `force` — see **Push modes**. |

### Embedded example (`gitlab-mirror.env.example`)

Use this exact content when generating **`gitlab-mirror.env.example`** at the repo root:

```dotenv
# Copy to .env.gitlab-mirror (gitignored via .env.*) and fill in. chmod 600 on the real file.
# GitLab → Preferences → Access Tokens: scope write_repository (or api).

# HTTPS URL of the GitLab project — no credentials in the URL
GITLAB_MIRROR_URL=https://gitlab.com/your-minecraft-mods-group/your-mod.git

# Optional: numeric project ID from GitLab project overview (not required for git push)
# GITLAB_PROJECT_ID=12345678

# Personal access token (GITLAB_PAT is an alias)
GITLAB_TOKEN=glpat_replace_me

# Optional: normal | force-with-lease | force
# GITLAB_PUSH_MODE=normal
```

### SSH path (recommended in this workspace)

No token URL, no env file. Add a persistent **`gitlab`** remote once, then push:

```bash
git remote get-url gitlab 2>/dev/null \
  || git remote add gitlab git@gitlab.com:<group>/<repo>.git
git ls-remote gitlab HEAD        # pick push mode (see table); anon HTTPS ls-remote also works
git push gitlab main:main        # add --force-with-lease / --force per push mode
```

The persistent remote also lets you verify and re-push later without rebuilding any URL. Push modes still apply (see **Push modes**). Use this whenever SSH authenticates; only build the HTTPS token URL (below) when it does not.

## Agent workflow

1. **Resolve repo path** — mod repo root containing **`.git`**. From Minder: **`mods[].local_path`**; in a deployed mod repo: user path or cwd.

2. **Bootstrap** — if needed, write **`gitlab-mirror.env.example`** from **Embedded example**; ensure **`.gitignore`** contains **`.env`** and **`.env.*`**.

3. **Load env (HTTPS path only)** — skip entirely when pushing via SSH. For HTTPS+PAT: `set -a && source /path/to.env && set +a` (see **Load order**); do not print token values. If **`GITLAB_MIRROR_URL`** is unset, set it from **`mods.yaml`** **`gitlab_url`** (strip trailing slash; append **`.git`** if missing).

4. **Sync primary remote (GitHub)** — in the mod repo:
   - `git fetch origin`
   - `git checkout main`
   - `git status` — working tree **clean** (default: stop if dirty unless user accepts WIP).
   - If local **`main`** is **behind** **`origin/main`**: pull/rebase per user preference before mirroring.
   - If local **`main`** is **ahead** of **`origin/main`**: push **`git push origin main`** first so GitLab mirrors what is already on GitHub (unless user explicitly wants mirror-only).

5. **Compare with GitLab (choose push mode)** — read the remote HEAD without logging credentials. With an SSH `gitlab` remote just `git ls-remote gitlab HEAD`; otherwise an anonymous HTTPS read works for public projects:

   ```bash
   git ls-remote gitlab HEAD 2>/dev/null \
     || git ls-remote "${GITLAB_MIRROR_URL%.git}.git" HEAD
   git rev-parse main
   ```

   | GitLab `HEAD` | Local `main` | Suggested **`GITLAB_PUSH_MODE`** |
   |---------------|--------------|----------------------------------|
   | missing / empty repo | any | **`normal`** |
   | ancestor of local (fast-forward) | ahead | **`normal`** |
   | diverged or behind local | ahead | **`force-with-lease`** (or **`force`** with user confirmation) |
   | same as local | equal | **skip** — already mirrored |

6. **Push** — choose target by auth method (see **Auth method** table):

   - **SSH (default):** ensure a **`gitlab`** remote exists (`git remote add gitlab git@gitlab.com:<group>/<repo>.git`), then push to it directly:
     - **`normal`**: `git push gitlab main:main`
     - **`force-with-lease`**: `git push --force-with-lease gitlab main:main`
     - **`force`**: `git push --force gitlab main:main` — **destructive**; confirm with user.
   - **HTTPS+PAT (fallback):** build a one-shot auth URL `https://oauth2:${GITLAB_TOKEN}@${HOST_AND_PATH}` (where **`${HOST_AND_PATH}`** is everything after `https://` in **`GITLAB_MIRROR_URL`**, e.g. `gitlab.com/group/repo.git`; use **`GITLAB_PAT`** if **`GITLAB_TOKEN`** is unset). Push with the same modes against `<auth-url>`. **Do not** log the authenticated URL or store the token in **`git config`** / shell history.

7. **Verify** — confirm the mirror caught up (and matches GitHub) in one shot:

   ```bash
   echo "gitlab: $(git ls-remote gitlab HEAD | awk '{print $1}')"
   echo "github: $(git ls-remote origin main | awk '{print $1}')"
   echo "local:  $(git rev-parse main)"
   ```

   All three SHAs should match.

8. **Registry (Minder only)** — append **`mods[].notes`** (mirror push date, the pushed SHA, GitLab URL) and set **`gitlab_url`** if it was empty. If the project is linked from Modrinth **`source_url`**, remind the user to confirm GitLab **public** visibility (private projects break anonymous source browsing).

## Push modes (choosing safely)

| Mode | Use when |
|------|----------|
| **normal** | Empty GitLab repo, or fast-forward update (remote **`main`** is an ancestor of local **`main`**). |
| **force-with-lease** | Remote has commits you are **deliberately replacing** with local **`main`**, but you want Git to **refuse** if someone else pushed since your last fetch. |
| **force** | You accept **unconditionally** resetting GitLab **`main`** to match local **`main`** (mirror overwrite). Confirm with the user when possible. |

## Checklist

- [ ] **Auth chosen:** SSH greets via `ssh -T git@gitlab.com` (preferred) → use a **`gitlab`** remote; else HTTPS+PAT with **`GITLAB_MIRROR_URL`** + token set.
- [ ] GitLab project URL known — from **`mods.yaml`** **`gitlab_url`** or, if empty, the probed **`<group>/<repo>`** convention (anon `ls-remote`).
- [ ] **`gitlab-mirror.env.example`** exists (from **Embedded example**) and **`.gitignore`** includes **`.env`** and **`.env.*`**.
- [ ] **`origin/main`** is synced — local **`main`** matches intended GitHub state.
- [ ] **`git ls-remote`** used to pick the push mode (or skip if already equal).
- [ ] Working tree clean **or** user explicitly accepts committing/stashing (default: clean).
- [ ] Token (HTTPS path only) never appears in commits or command output.
- [ ] Post-push **3-way SHA verify** (gitlab == github == local) passed.
- [ ] **`mods.yaml`** **`notes`** updated and **`gitlab_url`** set when working from Minder.

## Examples

- "Push main to GitLab mirror" → confirm SSH (`ssh -T git@gitlab.com`); `git fetch origin`; verify clean `main`; add/confirm `gitlab` remote; `git ls-remote gitlab HEAD` to pick mode; `git push gitlab main:main`; 3-way SHA verify.
- "GitLab mirror is out of date" → compare SHAs; usually `normal` if fast-forward, else `force-with-lease`.
- "Set up mirroring for a new mod" → confirm/probe `<group>/<repo>` URL; bootstrap `gitlab-mirror.env.example` + `.gitignore`; add SSH `gitlab` remote; push; set **`mods.yaml`** **`gitlab_url`**.
- **From Minder, empty `gitlab_url` (SSH)** → registry `gitlab_url` blank but the project is already live: derive `<group>` from another mod's `gitlab_url`, add the SSH `gitlab` remote, fast-forward `git push gitlab main:main` (`normal`, GitLab `HEAD` was an ancestor of local `main`), then write `gitlab_url` back to the registry.

## Related

- **`mods.yaml`** — **`gitlab_url`**, **`local_path`**, **`github_url`**; update **`notes`** after mirror push.
- **`mm-deploy-fabric-skills-to-repo`** — deploy this skill into mod repos (portable bundle).
- Primary **GitHub** / **GitLab** development remotes are unchanged; this skill only describes **pushing `main`** to the **mirror** URL.
