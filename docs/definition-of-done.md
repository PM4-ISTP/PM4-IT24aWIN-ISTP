# Definition of Done

A user story is considered **done** when all of the following are true:

## Code Quality

- [ ] Code follows project formatting and style rules (Prettier / Spotless / Checkstyle / PMD / SpotBugs all pass)
- [ ] No new linting errors or warnings introduced
- [ ] All CI checks are green on the PR branch

## Code Review

- [ ] Pull request has a meaningful description explaining what was changed and why
- [ ] At least **2 team members** have approved the PR
- [ ] All review comments are resolved

## Testing

- [ ] Relevant tests exist for the new/changed behavior (unit and/or integration)
- [ ] All tests pass in CI

## Deployment

- [ ] Feature is successfully deployed to the **staging environment** after merge to `main`
- [ ] No regressions observed on staging

## Documentation

- [ ] If the story changes or adds an API endpoint: OpenAPI/Scalar docs reflect the change
- [ ] If the story changes setup, configuration, or user-facing behavior: README or wiki is updated
- [ ] PR description is filled in (linked above)
