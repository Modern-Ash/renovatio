# Issue 177 accessibility report

The Renovatio shell uses semantic `main`, `header`, `nav`, `aside`, `section`, `article`, heading,
button, list and link elements. Activity buttons expose `aria-pressed`; selected project assets use
`aria-current`; project and activity navigation have explicit accessible labels.

Keyboard operation is available through focusable native buttons, the command palette, menu entries,
and five documented keybindings. Existing focus-visible styling is retained; shell controls have an
active/focus state independent of hover. The layout collapses into a horizontal scrollable activity
rail and then a single-column shell below narrow breakpoints.

Automated contract coverage asserts labels, all six project asset classes, persistence keys, explicit
error/permission states, command registration and absence of direct wizard imports. Interactive
screen-reader/browser validation remains subject to the browser evidence limitation recorded in
issue #176.
