begin;

alter table public.yourlifeone_account_state
  alter column minimum_client_version set default '0.2.83';

update public.yourlifeone_account_state
set minimum_client_version = '0.2.83', updated_at = now()
where minimum_client_version <> '0.2.83';

create or replace function yourlifeone_private.initialize_account_state()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.yourlifeone_account_state (
    user_id, schema_version, account_version, minimum_client_version, updated_at
  )
  values (new.id, 1, 0, '0.2.83', now())
  on conflict (user_id) do nothing;
  return new;
end;
$$;

revoke execute on function yourlifeone_private.initialize_account_state() from public, anon, authenticated;

commit;
