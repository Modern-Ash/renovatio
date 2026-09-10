#!/usr/bin/env bash
set -euo pipefail

if (( $# < 2 )); then
  echo "usage: $0 <baseline> <candidate> [candidate ...]" >&2
  exit 64
fi

baseline=$1
shift

git rev-parse --verify "${baseline}^{commit}" >/dev/null

printf '# Branch convergence audit\n\n'
printf 'Baseline: `%s` (`%s`)\n\n' "$baseline" "$(git rev-parse "$baseline")"
printf '| Candidate | Head | Merge base | Relation | Raw left/right | Non-equivalent left/right | Range-diff (= / < / > / !) |\n'
printf '|---|---|---|---|---:|---:|---:|\n'

for candidate in "$@"; do
  git rev-parse --verify "${candidate}^{commit}" >/dev/null
  head_sha=$(git rev-parse "$candidate")
  merge_base=$(git merge-base "$candidate" "$baseline")
  raw=$(git rev-list --left-right --count "$candidate...$baseline" | tr '\t' '/')
  non_equivalent=$(git rev-list --left-right --cherry-pick --count \
    "$candidate...$baseline" | tr '\t' '/')

  if git merge-base --is-ancestor "$candidate" "$baseline"; then
    relation=ancestor
  elif git merge-base --is-ancestor "$baseline" "$candidate"; then
    relation=descendant
  else
    relation=diverged
  fi

  if [[ "$merge_base" == "$head_sha" ]]; then
    range_summary="0/0/$(git rev-list --no-merges --count "$merge_base..$baseline")/0"
  elif [[ "$merge_base" == "$(git rev-parse "$baseline")" ]]; then
    range_summary="0/$(git rev-list --no-merges --count "$merge_base..$candidate")/0/0"
  else
    range_summary=$(git range-diff --no-color "$merge_base..$candidate" "$merge_base..$baseline" |
      awk '/^[ 0-9-]+:/ {
        if ($0 ~ / = /) equal++;
        else if ($0 ~ / < /) left++;
        else if ($0 ~ / > /) right++;
        else if ($0 ~ / ! /) changed++;
      } END { printf "%d/%d/%d/%d", equal, left, right, changed }')
  fi

  printf '| `%s` | `%s` | `%s` | %s | %s | %s | %s |\n' \
    "$candidate" "$head_sha" "$merge_base" "$relation" "$raw" "$non_equivalent" "$range_summary"
done

printf '\n## Candidate-exclusive commits and stable patch IDs\n\n'

for candidate in "$@"; do
  printf '### `%s`\n\n' "$candidate"
  printf '| Mark | Commit | Stable patch ID | Subject |\n'
  printf '|:---:|---|---|---|\n'

  while read -r mark commit subject; do
    patch_id=$(git show --pretty=format: "$commit" | git patch-id --stable | awk 'NR == 1 { print $1 }')
    if [[ -z "$patch_id" ]]; then
      patch_id='n/a (empty or merge)'
    fi
    printf '| `%s` | `%s` | `%s` | %s |\n' "$mark" "$commit" "$patch_id" "$subject"
  done < <(git log --left-only --cherry-mark --reverse --format='%m %H %s' \
    "$candidate...$baseline")

  printf '\n'
done
