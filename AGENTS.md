## Design Decisions and Approval

- Before implementation, ask the user to resolve all required design decisions. Recommend options with brief trade-offs or invite alternatives; never decide for the user.
- Obtain explicit approval after each proposal and before proceeding, including new decisions or design changes. Silence is not approval.
- If the user cannot resolve a required decision or approve a solution, decline the task and state what remains unresolved. Do not assume a design.

## Branches and Pull Requests

- Make all changes on the `dev` branch. If the current branch is `main`, switch to `dev` before making changes.
- Target `dev` as the base branch for all pull requests.

## Code Style

- Declare types explicitly; do not use `var`. Minimize `Optional` usage.
- Always use braces for control structures. Keep braces and logic on separate lines, and leave at least one blank line between methods.
- Qualify instance fields with `this.`; use `super.` when explicitly accessing superclass fields.
- Do not add comments unless requested, except for core logic in complex methods, which should be explained with `//` comments.
- Prefer reusing or consolidating general-purpose utilities in `core.util` or `bukkit.util` over private static helper methods in business classes. Class-specific business logic need not be extracted.
- Use parentheses around nested ternary expressions in complex ternary operations to improve readability.

## Minecraft APIs

- Prefer CraftEngine NMS proxies (`bukkit/proxy`); avoid Bukkit/Paper APIs where possible.
- Do not use Bukkit `ItemMeta`, `BlockState`, `BlockData`, or Plugin Channel APIs.
- Do not use `new ItemStack(...)`.

## Version Compatibility

- Verify proxy methods and fields for signature, type, and behavioral compatibility from Minecraft 1.20 through the latest version. State any unverified cases.
- Proxy classes and members with version differences must explicitly declare `activeIf` conditions (such as `min_version` and `max_version`). Use `VersionHelper` to adapt calling code.
