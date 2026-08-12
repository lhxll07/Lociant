# Vendored SlumberGuard crates

These crates are vendored from `https://github.com/lhxll07/slumberguard.git`
at commit `c697f32e89f6a6973a8392f91f33897891c448f8`.

They are kept in-tree because `lociant-server` uses the baby-monitor runtime
in release builds, and a sibling checkout or authenticated Git dependency
would make Lociant builds non-reproducible. When updating them, replace all
three crates from one upstream commit and update the commit recorded here.
