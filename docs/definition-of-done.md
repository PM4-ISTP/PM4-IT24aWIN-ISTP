# Definition of Done

A user story is considered **done** when all of the following are true:

## Code Quality

- [ ] Code follows project formatting and style rules (Prettier / ESLint / Spotless / Checkstyle)
- [ ] PMD and SpotBugs reports are reviewed when backend code is changed
- [ ] No new relevant linting errors or warnings introduced
- [ ] All CI checks are green on the PR branch

## Code Review

- [ ] Pull request title is clear; a description is added when the change is not self-explanatory
- [ ] At least **1 team member** has approved the PR
- [ ] All review comments are resolved

## Testing

- [ ] Relevant tests exist for the new/changed behavior (unit, E2E, or integration tests where applicable)
- [ ] All tests pass in CI

## Deployment

- [ ] Feature is successfully deployed to the **staging environment** after merge to `dev`
- [ ] No regressions observed on staging

## Documentation

- [ ] If the story changes or adds an API endpoint: OpenAPI/Scalar docs reflect the change
- [ ] If the story changes setup, configuration, or user-facing behavior: README or wiki is updated
