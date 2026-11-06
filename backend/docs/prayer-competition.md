# Prayer challenge

Requested by YourLife on 4 November 2026 for both Partner One and Your Life One.

The dashboard compares prayer completion for a calendar month (default), Monday–Sunday week, or day. Previous periods are browsable. Competition points use self-reported timing: prompt 12, within-window 8, outside-window 2; missed prayers earn zero. Points available are 12 times the eligible on-time, late and missed prayers. Completion remains a separate on-time plus late ratio. Historical completed outcomes with null timing retain 12 points, preserving prior win records. Pending, unclassified, pre-tracking and non-obligatory prayers are excluded by the existing prayer statistics service. Exact fractions decide the leader; rounded percentages are display only. An empty denominator has no score or winner.

The card refreshes on confirmed prayer changes, once per minute while mounted, and through Refresh. Account/session changes and superseded requests cannot reuse another account or period’s response. No totals or credentials are persisted in the browser. A missing partner is shown as unavailable, never as a zero score.

## Narrow sharing boundary

The two products keep separate Auth, databases, services and deployment identities. This requested comparison introduces only an explicitly configured, read-only exchange of period totals. The summary contains `from`, `to`, `asOf`, `onTime`, `late`, `missed`, `pointsEarned`, and `pointsAvailable`. The additive point fields are paired integers; a rolling legacy peer may omit both, but a weighted consumer must not rank that peer until both fields are present. It contains no account IDs, individual prayer records, cycle dates, exemption counts, notes, location or other account data. Totals can still reveal patterns; pairing requires explicit owner activation.

- `GET /api/prayer/v1/competition?from=YYYY-MM-DD&to=YYYY-MM-DD` uses this product’s signed-in account. Only the configured owner receives the configured partner. Other accounts see their own totals and `not_connected`. OAuth client tokens are refused until a prayer agent contract is delivered.
- `GET /api/prayer/v1/competition/summary` uses a separate server-only `X-Prayer-Competition-Key`, scoped to the configured owner and this one read-only route. It cannot authorize ordinary prayer reads or writes. Requests without the exact key are rejected before database access.
- Both responses use `Cache-Control: no-store`. Ranges are bounded to 31 days. Peer reads have 2-second connection and 3-second response limits, refuse redirects, and validate matching dates, recent timestamps and bounded counts. Failures preserve the owner’s totals and show an unavailable partner.
- Shared fixtures `competition.json`, `competition-summary.json` and `competition-not-connected.json` are checked by provider integration tests and consumer runtime schemas. Synthetic browser tests cover period navigation, keyboard operation, responsive designs, ties, empty scores and failures.

## Activation and rollback

Implemented code is disabled until a reviewed pair is configured. No credentials are created or account pairing activated by the source change. After final owner approval, configure each product’s existing root-owned prayer environment independently:

| Environment variable | Value |
| --- | --- |
| `APP_PRAYER_COMPETITION_OWNER_ID` | Verified UUID of this product’s intended account |
| `APP_PRAYER_COMPETITION_READ_KEY` | New independent random secret, at least 32 characters; permits only this summary |
| `APP_PRAYER_COMPETITION_PEER_URL` | Approved partner HTTPS URL ending exactly in `/api/prayer/v1/competition/summary` |
| `APP_PRAYER_COMPETITION_PEER_KEY` | Partner’s dedicated summary key; never an Auth, database or application credential |

Use the existing root-only prayer environment file and product-specific restart path. Do not include values in repositories, web builds, CLI output, traces, exports or memory. The Partner repository does not configure YourLife resources; each deployment operator supplies only its own reviewed competition configuration. Account mapping must be established through an authorized identity route; there is no direct database or prayer-record lookup fallback for agents.

Remove the read key and restart that product’s prayer service to revoke inbound sharing. Remove its peer settings to stop outbound reads. Account records and prayer outcomes remain untouched.

## Acceptance boundary

Source and synthetic checks prove the feature only within their exercised boundaries. Real pairing and live account comparison remain pending explicit activation. A narrow `prayer_get_competition` semantic MCP tool is not yet published; external-agent acceptance is explicitly blocked. Agents must not inspect real prayer accounts through lower-level APIs, database access, session-token reuse or browser automation.

## Completion timing compatibility

`POST /outcomes` and `PATCH /outcomes/{id}` accept nullable `timing` values
`prompt`, `within_window`, and `outside_window`. Prompt and within-window require
`on_time`; outside-window requires `late`; missed outcomes have no timing. Timing
is self-reported and is never inferred from the logging timestamp. Existing
before-start and timetable guards still apply. New untimed writes default to
within-window or outside-window. An untimed correction that keeps the same status
preserves the previous timing, including a historical null. An explicit timing
correction does not issue another planner XP reward.

The nullable V7 migration does not backfill or reclassify historical records.
Competition points pass through the same statistics eligibility path as counts;
no individual outcome timing or prayer details cross the product boundary.
Deploy this additive backend before the weighted browser consumer.

`allPrayed` is an additive nullable boolean for early settlement. The provider sets
it true only when every day in the requested range has its known timetable, all
five obligatory prayers per day are completed, no pending/missed/unclassified
opportunity remains, and the range has no future date in its timetable zone.
Both participants must report true before a current period enters the win record.
An ended historical period settles by calendar end regardless of this flag.
