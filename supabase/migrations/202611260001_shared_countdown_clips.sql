create table public.countdown_albums (
    id uuid primary key,
    owner_id uuid not null references auth.users(id),
    title text not null check (char_length(btrim(title)) > 0),
    target_date date not null
);
create table public.countdown_members (
    album_id uuid not null references public.countdown_albums(id) on delete cascade,
    user_id uuid not null references auth.users(id),
    primary key (album_id, user_id)
);
create index on public.countdown_members (user_id);
create table public.countdown_invites (
    album_id uuid primary key references public.countdown_albums(id) on delete cascade,
    code uuid not null unique default gen_random_uuid(),
    expires_at timestamptz not null
);

alter table public.countdown_albums enable row level security;
alter table public.countdown_members enable row level security;
alter table public.countdown_invites enable row level security;
revoke all on public.countdown_albums, public.countdown_members, public.countdown_invites
    from anon, authenticated;
grant select on public.countdown_albums, public.countdown_members to authenticated;
create policy "read own membership" on public.countdown_members for select to authenticated
    using (user_id = (select auth.uid()));
create policy "read joined albums" on public.countdown_albums for select to authenticated
    using (exists (select 1 from public.countdown_members m where m.album_id = id));

create function public.create_countdown_album(album uuid, label text, day date)
returns void language plpgsql security definer set search_path = '' as $$
begin
    if auth.uid() is null then raise exception 'sign in required'; end if;
    insert into public.countdown_albums(id, owner_id, title, target_date)
        values (album, auth.uid(), label, day) on conflict (id) do nothing;
    if not exists (select 1 from public.countdown_albums a where a.id = album and a.owner_id = auth.uid()) then
        raise exception 'album unavailable';
    end if;
    insert into public.countdown_members values (album, auth.uid()) on conflict do nothing;
end;
$$;

create function public.invite_to_countdown_album(album uuid)
returns uuid language plpgsql security definer set search_path = '' as $$
declare invitation uuid;
begin
    if not exists (select 1 from public.countdown_albums a where a.id = album and a.owner_id = auth.uid()) then
        raise exception 'album unavailable';
    end if;
    insert into public.countdown_invites (album_id, expires_at)
        values (album, now() + interval '24 hours')
        on conflict (album_id) do update set code = gen_random_uuid(), expires_at = excluded.expires_at
        returning code into invitation;
    return invitation;
end;
$$;

create function public.join_countdown_album(invitation uuid)
returns uuid language plpgsql security definer set search_path = '' as $$
declare album uuid;
begin
    if auth.uid() is null then raise exception 'sign in required'; end if;
    delete from public.countdown_invites where code = invitation and expires_at > now()
        returning album_id into album;
    if album is null then raise exception 'invalid invitation'; end if;
    insert into public.countdown_members values (album, auth.uid()) on conflict do nothing;
    return album;
end;
$$;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
    values ('countdown-clips', 'countdown-clips', false, 6291456, array['video/mp4']);

create policy "read album videos" on storage.objects for select to authenticated
    using (bucket_id = 'countdown-clips' and exists (
        select 1 from public.countdown_members m
        where m.album_id::text = (storage.foldername(name))[1] and m.user_id = (select auth.uid())
    ));
create policy "upload own album videos" on storage.objects for insert to authenticated
    with check (bucket_id = 'countdown-clips'
        and name ~ '^[0-9a-f-]{36}/[0-9a-f-]{36}/[0-9a-f-]{36}[.]mp4$'
        and (storage.foldername(name))[2] = (select auth.uid())::text
        and exists (select 1 from public.countdown_members m
            where m.album_id::text = (storage.foldername(name))[1] and m.user_id = (select auth.uid())));
create policy "delete own album videos" on storage.objects for delete to authenticated
    using (bucket_id = 'countdown-clips'
        and (storage.foldername(name))[2] = (select auth.uid())::text
        and exists (select 1 from public.countdown_members m
            where m.album_id::text = (storage.foldername(name))[1] and m.user_id = (select auth.uid())));

create function public.list_countdown_clips(album uuid)
returns table (path text, uploaded_at timestamptz)
language sql stable security invoker set search_path = '' as $$
    select name, created_at from storage.objects
    where bucket_id = 'countdown-clips' and (storage.foldername(name))[1] = album::text
    order by created_at, name;
$$;

revoke execute on function public.create_countdown_album(uuid, text, date),
    public.invite_to_countdown_album(uuid), public.join_countdown_album(uuid),
    public.list_countdown_clips(uuid) from public, anon, authenticated;
grant execute on function public.create_countdown_album(uuid, text, date),
    public.invite_to_countdown_album(uuid), public.join_countdown_album(uuid),
    public.list_countdown_clips(uuid) to authenticated;
