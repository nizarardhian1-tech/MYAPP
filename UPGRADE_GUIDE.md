# AndroidTools — Upgrade Guide

## What Changed

### 1. `ToolItem.kt` — Extended Data Model
New fields you can add to your `tools.json`:

| Field | Type | Description |
|---|---|---|
| `category` | String | e.g. "Network", "Developer", "Utilities" |
| `developer` | String | Author/team name |
| `size` | String | e.g. "4.2 MB" |
| `changelog` | String | What's new in this version |
| `tags` | Array of strings | Searchable keywords |
| `screenshots` | Array of strings | URLs (future use) |

---

### 2. `ToolsViewModel.kt` — Search + Filter
- `setSearch(query)` — filter by name, shortDesc, tags
- `setCategory(cat)` — filter by category tab
- `categories` StateFlow — auto-built from JSON data
- `allTools` — unfiltered (used by Dashboard)
- `refresh()` — force re-fetch

---

### 3. `HomeFragment.kt` — New UI
- **Search bar** — real-time filter
- **Category chips** — horizontal scroll, auto-populated from JSON
- **SwipeRefreshLayout** — pull to refresh
- **Download progress** — live % progress bar per card
- **Card click** → shows `changelog` (extend to a bottom sheet if needed)

---

### 4. `ToolAdapter.kt` — Upgraded Cards
- Developer name shown under app name (in green)
- Category badge (top right of card)
- Tags row (small pills)
- File size shown
- Download progress bar (0–100%) with live % on button
- `onCardClick` callback for detail view

---

### 5. `DashboardFragment.kt` — My Apps
- Shows installed count ("3 apps installed")
- **Update banner** — appears when version mismatch detected

---

### 6. `ProfileFragment.kt` — Settings
- App version shown
- "Refresh Tool List" button
- GitHub link added
- Changelog shown in update dialog

---

## New Drawables Needed
Add these XML files to `res/drawable/`:
- `bg_search.xml`
- `bg_chip_active.xml`
- `bg_chip_inactive.xml`
- `bg_chip_selector.xml`
- `text_chip_selector.xml`
- `bg_category_badge.xml`
- `bg_tag.xml`

All files are included in this upgrade package.

## New Layouts Needed
- `item_category_chip.xml`
- `item_tag_chip.xml`

## Gradle — Add SwipeRefreshLayout
In `app/build.gradle`, add:
```groovy
implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'
```

---

## JSON Schema (tools.json)
```json
[
  {
    "name": "Tool Name",
    "short_desc": "One-liner",
    "desc": "Full <b>HTML</b> description",
    "version": "1.0.0",
    "package_name": "com.example.tool",
    "apk_url": "https://...",
    "icon_url": "https://...",
    "category": "Developer",
    "developer": "Your Name",
    "size": "5.2 MB",
    "changelog": "Bug fixes",
    "tags": ["tag1", "tag2"],
    "screenshots": []
  }
]
```

> Leave `apk_url` empty `""` to show "Coming Soon" button automatically.
