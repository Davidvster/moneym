# Kickoff prompt for Claude Code

Paste this into Claude Code at the start of your session to give it the right context.

---

I'm redesigning the MoneyM app — a Compose Multiplatform expense tracker that targets iOS and Android. I have a complete high-fidelity design handoff in this folder.

Please:

1. **Read `README.md` end-to-end first.** It contains every design token, screen breakdown, component spec, and interaction note.
2. **Reference `compose/MoneyMTheme.kt`** — a ready-to-use Compose theme file with colors, typography, spacing, radii, and category palette already defined as Kotlin data. Drop this into `commonMain/designsystem/` and adapt the Geist font registration to my Resources setup.
3. **Treat `source/screens.jsx` as the source of truth for layout.** When you're not sure how a screen composes (padding, order, exact font weights), look there. The HTML version is at `MoneyM Redesign.html` — open it in a browser to see all 13 screens at once.
4. **Do NOT use Material3 `NavigationBar`, `FloatingActionButton`, `BottomAppBar`, or `FilledTonalButton`.** They're too opinionated for this design's platform-agnostic feel. Build custom composables from `androidx.compose.foundation` primitives.

Start by:
- Inspecting the current MoneyM codebase to learn the existing project structure.
- Setting up the design system (`MoneyMTheme.kt`, font resources, an `Icon` composable with the SVG paths from `source/ui-primitives.jsx`).
- Building the reusable components in this order: `Button`, `IconButton`, `Segmented`, `Chip`, `Field`, `Toggle`, `Row`, `Card`, `Money`, `CategoryIcon`, `TabBar`. Each has a spec in the README under "Reusable Components".
- Migrating screens in this order: PIN → Transactions → Add/Edit → Overview Month → Overview Year → Settings → Currency / Language / Manage categories / Display preferences.

When in doubt, ask me — but please don't drift from the design tokens. Every hex value, every dp, every font weight in the README is intentional.
