# Contributing
Welcome to the FTCLib project! We hope we have something useful for you!

## Project Contributors

- [Jackson](https://github.com/jiceberg) from ARC Robotics
- [Pranav](https://github.com/pranavavva) from TecHounds
- [Daniel](https://github.com/dansman805) from JDroids
- [Noah](https://github.com/NoahBres) from Radical Raiders
- [Peter](https://github.com/codeNinjaDev) from E-Lemon-ators

## Branch Naming

| Instance | Branch | Description, Instructions, Notes |
|---------|---------|----------------------------------|
|Stable|`master`|Accepts merges from Working and Hotfixes|
|Working|`dev`|Accepts merges from Features/Issues and Hotfixes|
|GitHub Pages|`gh-pages`|Reserved for GitHub Pages
|Features/Issues|`feat-*`|Always branches off HEAD of Working|
|Hotfix|`hotfix-*`|Always branches off of Stable|

## Commit Naming

| Commit type | Commit prefix| Commit body | Use when... |
|-------------|--------------|-------------|-------------|
| New Feature | `feat: ***`  | Describe the new feature added| A new feature is added|
| Fixed bug   | `fix: ***`   | Describe bug fix | A bug is fixed|
| Documentation | `docs: ***`| Describe what documentation was added| New docs are added or existing docs are updated|
| Refactor | `refactor: ***` | Describe what was moved around | Nothing new added but files or file content reorganized|

## Main Branches

The main repository will always hold three evergreen branches:

- `dev`
- `master`
- `gh-pages`

The main branch should be considered `origin/dev` and will be the main branch where the source code of `HEAD` always reflects a state with the latest delivered development changes for the next release. As a developer, you will be branching and merging from `dev`.

Consider `origin/master` to always represent the latest code deployed to production. During day to day development, the `master` branch will not be interacted with.

When the source code in `dev` is stable and has been deployed, all of the changes will be merged into `master` and tagged with a release number.

`gh-pages` is reserved for the use of GitHub Pages. No deployable code will be maintained on `gh-pages`.

## Supporting Branches

Supporting branches are used to aid parallel development between team members, ease tracking of features, and to assist in quickly fixing live production problems. Unlike the main branches, these branches always have a limited life time, since they will be removed eventually.

The different types of branches we may use are:

* Feature branches
* Bug branches
* Hotfix branches

Each of these branches have a specific purpose and are bound to strict rules as to which branches may be their originating branch and which branches must be their merge targets. Each branch and its usage is explained below.

### Feature Branches

Feature branches are used when developing a new feature or enhancement which has the potential of a development lifespan longer than a single deployment. When starting development, the deployment in which this feature will be released may not be known. No matter when the feature branch will be finished, it will always be merged back into the master branch.

During the lifespan of the feature development, the lead should watch the `dev` branch (network tool or branch tool in GitHub) to see if there have been commits since the feature was branched. Any and all changes to `dev` should be merged into the feature before merging back to `dev`; this can be done at various times during the project or at the end, but time to handle merge conflicts should be accounted for.

`<tbd number>` represents the project to which Project Management will be tracked.

* Must branch from: `dev`
* Must merge back into: `dev`
* Branch naming convention: `feature-<tbd number>`

#### Working with a feature branch

If the branch does not exist yet (check with the Lead), create the branch locally and then push to GitHub. A feature branch should always be 'publicly' available. That is, development should never exist in just one developer's local branch.

```
$ git checkout -b feature-id dev                    // creates a local branch for the new feature
$ git push origin feature-id                        // makes the new feature remotely available
```

Periodically, changes made to `dev` (if any) should be merged back into your feature branch.

```
$ git merge dev                                     // merges changes from master into feature branch
```

When development on the feature is complete, the lead (or engineer in charge) should merge changes into `dev` and then make sure the remote branch is deleted.

```
$ git checkout dev                                  // change to the master branch  
$ git merge --no-ff feature-id                      // makes sure to create a commit object during merge
$ git push origin dev                               // push merge changes
$ git push origin :feature-id                       // deletes the remote branch
```

### Bug Branches

Bug branches differ from feature branches only semantically. Bug branches will be created when there is a bug on the live site that should be fixed and merged into the next deployment. For that reason, a bug branch typically will not last longer than one deployment cycle. Additionally, bug branches are used to explicitly track the difference between bug development and feature development. No matter when the bug branch will be finished, it will always be merged back into `dev`.

Although likelihood will be less, during the lifespan of the bug development, the lead should watch the `dev` branch (network tool or branch tool in GitHub) to see if there have been commits since the bug was branched. Any and all changes to `dev` should be merged into the bug before merging back to `dev`; this can be done at various times during the project or at the end, but time to handle merge conflicts should be accounted for.

`<tbd number>` represents the Basecamp project to which Project Management will be tracked. 

* Must branch from: `dev`
* Must merge back into: `dev`
* Branch naming convention: `bug-<tbd number>`

#### Working with a bug branch

If the branch does not exist yet (check with the Lead), create the branch locally and then push to GitHub. A bug branch should always be 'publicly' available. That is, development should never exist in just one developer's local branch.

```
$ git checkout -b bug-id dev                        // creates a local branch for the new bug
$ git push origin bug-id                            // makes the new bug remotely available
```

Periodically, changes made to `dev` (if any) should be merged back into your bug branch.

```
$ git merge dev                                     // merges changes from master into bug branch
```

When development on the bug is complete, [the Lead] should merge changes into `master` and then make sure the remote branch is deleted.

```
$ git checkout dev                                  // change to the master branch  
$ git merge --no-ff bug-id                          // makes sure to create a commit object during merge
$ git push origin dev                               // push merge changes
$ git push origin :bug-id                           // deletes the remote branch
```

### Hotfix Branches

A hotfix branch comes from the need to act immediately upon an undesired state of a live production version. Additionally, because of the urgency, a hotfix is not required to be be pushed during a scheduled deployment. Due to these requirements, a hotfix branch is always branched from a tagged `master` branch. This is done for two reasons:

* Development on the `dev` branch can continue while the hotfix is being addressed.
* A tagged `master` branch still represents what is in production. At the point in time where a hotfix is needed, there could have been multiple commits to `dev` which would then no longer represent production.

`<tbd number>` represents the Basecamp project to which Project Management will be tracked. 

* Must branch from: tagged `master`
* Must merge back into: `dev` and `master`
* Branch naming convention: `hotfix-<tbd number>`

#### Working with a hotfix branch

If the branch does not exist yet (check with the Lead), create the branch locally and then push to GitHub. A hotfix branch should always be 'publicly' available. That is, development should never exist in just one developer's local branch.

```
$ git checkout -b hotfix-id master                  // creates a local branch for the new hotfix
$ git push origin hotfix-id                         // makes the new hotfix remotely available
```

When development on the hotfix is complete, [the Lead] should merge changes into `stable` and then update the tag.

```
$ git checkout master                               // change to the stable branch
$ git merge --no-ff hotfix-id                       // forces creation of commit object during merge
$ git tag -a <tag>                                  // tags the fix
$ git push origin master --tags                     // push tag changes
```

Merge changes into `dev` so not to lose the hotfix and then delete the remote hotfix branch.

```
$ git checkout dev                                  // change to the master branch
$ git merge --no-ff hotfix-id                       // forces creation of commit object during merge
$ git push origin dev                               // push merge changes
$ git push origin :hotfix-id                        // deletes the remote branch
```
## Setting up a local environment

### 1. Clone

Clone this project [on GitHub](https://github.com/FTCLib/FTCLib).

```bash
$ git clone git@github.com:FTCLib/FTCLib.git
$ cd FTCLib
```

### 2. Build

Develop and build this project using Android Studio or another IDE with a properly set-up Anroid SDK. A standardized code style is to be determined.

### 3. Branch

Checkout the `dev` branch to start contributing. Follow the [Branch Naming Conventions](#branch-naming) and [Branch Types](#main-branches) above. _NEVER_ make a PR directly to the `master` branch. All such PRs will be categorically rejected.

## Making Changes

### 4. Commit

Please keep your changes grouped logically witihng individual commits. Each commit should be prefixed with an appropriate prefix, as noted [above](#commit-naming). A commit could contain a new feature, or new documentation, but never both, especially not in different files. Following these guidelines make it easier to review changes split across multiple commits.

### 5. Rebase

Before pushing your commits to the repo to make a PR, please remember to synchronize your work with the repo. It is preffered to use `git rebase` over `git merge` to do this.

```bash
$ git fetch origin
$ git rebase origin/dev     # Replace "dev" with appropriate branch
```

This ensures that your working branch has the latest changes.

### 6. Test

Please make sure your latest code builds correctly. For the purposes of FTCLib, you only need to build the `:ftclib` module. If you are building on the command line, ensure you are cleaning and building only the `:ftclib` module:

```bash
./gradlew :ftclib:clean :ftclib:build
```

### 7. Push

Time to push! Before pushing your code, make sure all files you changed are formatted (`Ctrl+Alt+L`), commits are properly named according to the above naming conventions, and you are on the correct branch. Once you are ready, push your changes to the appropriate branch.

```bash
$ git push origin dev    # Replace "dev" with appropriate branch
```

### 8. Pull Request

From the GitHub web interface, open a new Pull Request and detail what changes have been made. Repo owners may require you to change some code before adding your changes to the repo codebase.
