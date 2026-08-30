# Governed committed-cache hit verification

Date: 2026-08-30

## Result

After promotion commits A/B/C and manifest commit `d547e8f`, Agora invoked the exact canonical
identity previously used to create cache key
`f13b4bf91f60efadf4c87977b54d162cb5a370384ec2d9f86c93cf43e3eeffc5`.

Tool-run `tool-20260830t19371788129470z` completed successfully with empty standard output and empty
standard error. This is the cache-hit path: it returns before starting miss attribution or calling
the configured offline provider. The committed envelope, Git-derived index and verified promotion
manifest were loaded as one matching authority set.

The focused Java 17 dependency reactor also passed 131 tests with zero failures after promotion.
