# compose-animated-list

Diff-driven animated column for Jetpack Compose: each item exposes a **lifecycle** (`phase` + `progress`) so you can build custom transitions without guessing enter/exit flags.

## Usage

```kotlin
AnimatedColumn(
    items = items,
    key = { it.id },
    transitionSpec = AnimatedItemDefaults.fadeSlide(),
) { item ->
    Card(
        modifier = Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 24f
        }
    ) {
        Text(item.title)
    }

    if (phase == ItemPhase.Exiting) {
        Text("Removing…")
    }
}
```

### Item lifecycle

Each item exposes:

- **`phase`**: `Entering` / `Visible` / `Exiting`
- **`progress`**: `0f..1f` — entering moves `0 → 1`, visible stays `1`, exiting moves `1 → 0` (derived from the active transition: fade, slide, and optional height placement).

This allows fully custom animations on top of the library-driven container transition.

### Modifier helper

Optional preset that maps `progress` to a simple fade + slide (useful with `AnimatedItemDefaults.none()` if you want the list to only handle diffing and height):

```kotlin
Card(modifier = Modifier.animatedItem(this)) { … }
```

### Transition presets

```kotlin
AnimatedItemDefaults.fade()
AnimatedItemDefaults.slide()
AnimatedItemDefaults.fadeSlide() // default for AnimatedColumn
AnimatedItemDefaults.none()
```

## Modules

- **`animatedlist`** — library
- **`sample`** — playground (transition tuning, tag chips, list demos)

## License

See repository license (if any).
