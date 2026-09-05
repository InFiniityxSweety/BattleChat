# BattleChat

BattleChat is a client-side Minecraft chat enhancement mod based on ChatPlus, targeting Fabric 26.2 first.

## BattleChat 0.1.0-alpha

### Chat routing
- Default `Chat` and `Server` tabs.
- Modern player-chat metadata is used where available.
- Legacy/ViaVersion servers use a conservative fallback classifier.
- Each tab can explicitly use `Any`, `Player`, or `Server` as its message source.
- `Ctrl + right click` a chat line to show classifier diagnostics (`Message Info`).

### Translation
- Incoming messages are never translated automatically.
- `Ctrl + left click` translates the selected/hovered incoming message on demand.
- Incoming source language is auto-detected and the default target is German.
- Outgoing translation is controlled by the visible language selector in the chat bar; active mode is highlighted green.
- Slash commands bypass translation completely.
- Multiple independent translation backends are used with provider-local cooldowns, retries and a short result cache.

### Text styles
The `✨` selector supports:
- Normal
- Enchantment / Standard Galactic Alphabet style
- Small Caps
- Fullwidth
- Bold
- Italic
- Bold Italic
- Monospace
- Fraktur
- Double Struck
- Script
- Circled
- Upside Down

When translation and a text style are both enabled, BattleChat translates readable text first and applies the selected style afterwards. Supported stylized incoming text is normalized back to readable text before manual translation.

### Emoji picker
The `😀` chat-bar picker includes categories, search, recent emojis and favorites. Left click inserts at the current cursor position; right click toggles a favorite.

### Config
BattleChat stores its configuration under:

`config/battlechat/`

On first startup it can import a compatible ChatPlus configuration from `config/chatplus` without deleting or modifying the original files.

## Translation privacy
Manual incoming messages and outgoing messages that you choose to translate are sent to external translation services. BattleChat does not automatically translate incoming chat. Do not translate text you do not want sent to a third-party translation provider.

## Upstream and license
BattleChat is based on ChatPlus by ebicep and remains licensed under GPL-3.0. Upstream attribution and licensing are preserved.

## Status

`0.1.0-alpha` is still a test build. Fabric / Minecraft 26.2 is the initial supported target.
