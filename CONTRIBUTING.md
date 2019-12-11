# Contributing
Welcome to the FTCLib project! We hope we have something useful for you!

## Project Contributors

- [Jackson](https://github.com/jiceberg) from ARC Robotics
- [Pranav](https://github.com/pranavavva) from TecHounds
- [Daniel](https://github.com/dansman805) from JDroids
- [Noah](https://github.com/NoahBres) from Radical Raiders

## Branch Naming

| Instance | Branch | Description, Instructions, Notes |
|---------|---------|----------------------------------|
|Stable|`master`|Accepts merges from Working and Hotfixes|
|Working|`dev`|Accepts merges from Features/Issues and Hotfixes|
|Features/Issues|`topic-*`|Always branches off HEAD of Working|
|Hotfix|`hotfix-*`|Always branches off of Stable|

## Commit Naming

| Commit type | Commit prefix| Commit body | Use when... |
|-------------|--------------|-------------|-------------|
| New Feature | `Feat: ***`  | Describe the new feature added| A new feature is added|
| Fixed bug   | `Fix: ***`   | Describe bug fix | A bug is fixed|
| Documentation | `Docs: ***`| Describe what documentation was added| New docs are added or existing docs are updated|
| Refactor | `Refactor: ***` | Describe what was moved around | Nothing new added but files or file content reorganized|

## Main Branches

The main repository will always hold two evergreen branches:

- `dev`
- `master`

The main branch should be considered `origin/dev` and will be the main branch where the source code of `HEAD` always reflects a state with the latest delivered development changes for the next release. As a developer, you will be branching and merging from `dev`.

TODO: finish rest from [here](https://gist.github.com/digitaljhelms/4287848)
