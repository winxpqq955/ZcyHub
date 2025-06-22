# ZcyHub

## Setup

1. Place the plugin jar in your Velocity plugins folder.
2. Configure your lobbies in two places:

### Plugin Config (config.yml)
This file is located in `src/main/resources/config.yml` (it will be copied to `plugins/zcyhub/config.yml` on first run):

```yaml
lobbies:
  main:
    allowDirectConnect: true
    main:
      - "main_lobby_1": "127.0.0.1:30000"
  skywars:
    main:
      - "sw_hub_1": "127.0.0.1:40000"
    sub:
      solo_room:
        - "sw_sub_game1": "127.0.0.1:40001"
      solo_nokit:
        - "sw_solo_nokit_1": "127.0.0.1:40005"
      solo_1_9:
        - "solo_1_9_ver_1": "127.0.0.1:40010"
  bedwars:
    main:
      - "bw_hub_1": "127.0.0.1:50000"
    sub:
      bw_solo:
        - "bw_solo_game_1": "127.0.0.1:21001"
      bw_2_team:
        - "bw_2_team_game_1": "127.0.0.1:21002"
      bw_4_team:
        - "bw_4_team_game_1": "127.0.0.1:21003"
  practice:
    main:
      - "practice_all_in_one": "127.0.0.1:26001"
  vd:
    main:
      - "vd_quarry": "127.0.0.1:25001"
      - "vd_hollow": "127.0.0.1:25002"
  arcade:
    main:
      - "arcade_game_1": "127.0.0.1:20000"
  ffa:
    main:
      - "ffa_1": "127.0.0.1:32527"
  limbo:
    main:
      - "limbo_1": "127.0.0.1:25564"
  testsetver:
    main:
      - "test_server_1": "127.0.0.1:60000"
fallbackGroup: limbo
```
