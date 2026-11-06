/** Shared destinations available from the shell on every Your Life One page. */
export const QUICK_LINKS = [
  {
    id: 'grafana',
    label: 'Grafana',
    icon: '\u{1F4C8}',
    description: 'Your Life One services: API and database latency',
    href: 'https://monitoring.example.test/d/your-life-one-services/your-life-one-services3a-api-and-database-latency?from=now-6h&to=now&timezone=browser&refresh=1m',
  },
] as const;
