# Server-wide kill announcements

Request (2026-09-28): announce wither and warden kills to the whole server.

## Design

- Scope: a player's kill of a mob MobClash summoned, the same kills `/kills` counts. A wither
  farm elsewhere on the server stays quiet.
- `config.yml` `announce-kills: [wither, warden]`, entity names as in `/summon`, with or without
  `minecraft:`. An empty list turns it off. Read at each kill, so `/mobclash reload` applies it.
- `language.yml` `kill-announcement`, `{0}` the killer, `{1}` the mob's name (a custom name if
  it has one). Sent with `Server.broadcast`, which reaches every player in every world and the
  console.
- Lives in `MobDeathListener`, after the kill is recorded.

## Checklist

- [x] Listener, config default, language key
- [x] Unit tests: announced, not in list, not a MobClash mob
- [x] `mvn -o clean verify`
- [x] Live: wither and warden kill announced to a second player in another world, zombie not
- [x] Docs: configuration.md, README, CHANGELOG, TEST-SUMMARY

## Results

- Live on the `mobclash` instance, 2.1.0 over the 2.0.0 config.yml (no `announce-kills` key, so
  the shipped default applied). Clasher in the overworld killed a summoned warden and wither:
  "Clasher has slain the Warden!" and "...the Wither!" reached Clasher, Visitor in the nether,
  and the console. Two summoned pigs were counted as kills and not announced. A warden from
  vanilla `/summon`, killed by Clasher, was not announced.
