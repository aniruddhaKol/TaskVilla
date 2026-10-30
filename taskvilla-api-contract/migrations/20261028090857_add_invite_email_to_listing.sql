-- Include the invite address in owner and invitee inbox projections.
-- Qualify Auth email column against the RPC email argument.
create or replace function taskvilla.manage_sharing(
  action text, "listId" uuid default null, email text default null,
  "inviteToken" text default null, "memberUserId" uuid default null, permission text default null)
returns jsonb
language plpgsql
security definer
set search_path = pg_catalog, taskvilla
as $function$
declare
  v_user uuid := auth.uid();
  v_email text;
  v_inv taskvilla.list_members%rowtype;
  v_list_owner uuid;
  v_result jsonb;
  v_members jsonb := '[]'::jsonb;
  v_invites jsonb := '[]'::jsonb;
  v_target uuid;
begin
  if v_user is null then raise sqlstate 'PT401' using message='Authentication required'; end if;
  if action not in ('invite','accept','revoke','decline','list') then
    raise sqlstate 'PT400' using message='Unsupported sharing action';
  end if;
  select lower(u.email) into v_email from auth.users u where u.id=v_user and u.email_confirmed_at is not null;
  if v_email is null then raise sqlstate 'PT403' using message='A verified email account is required'; end if;

  if action='invite' then
    if "listId" is null or email is null or permission not in ('viewer','editor') then
      raise sqlstate 'PT400' using message='listId, email and viewer/editor permission are required';
    end if;
    if lower(btrim(email)) !~ '^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$' then
      raise sqlstate 'PT400' using message='A valid email address is required';
    end if;
    select owner_id into v_list_owner from taskvilla.entities
      where entity='list' and entity_id="listId" and operation='upsert';
    if not found then raise sqlstate 'PT404' using message='List not found'; end if;
    if v_list_owner<>v_user then raise sqlstate 'PT403' using message='Only the list owner can invite people'; end if;
    if lower(btrim(email))=v_email then raise sqlstate 'PT400' using message='You cannot invite your own email'; end if;
    select * into v_inv from taskvilla.list_members where list_id="listId"
      and invited_email=lower(btrim(email)) and status='pending' for update;
    if not found then
      if exists(select 1 from taskvilla.list_members where list_id="listId"
          and invited_email=lower(btrim(email)) and status='accepted') then
        raise sqlstate 'PT409' using message='That person already has access';
      end if;
      insert into taskvilla.list_members(list_id,owner_user_id,invited_email,permission)
        values("listId",v_user,lower(btrim(email)),permission) returning * into v_inv;
    else
      update taskvilla.list_members set permission=manage_sharing.permission,owner_user_id=v_user
        where invitation_id=v_inv.invitation_id returning * into v_inv;
    end if;
    return jsonb_build_object('status','invited','invitationId',v_inv.invitation_id);
  elsif action in ('accept','decline') then
    if "inviteToken" is null then raise sqlstate 'PT400' using message='inviteToken is required'; end if;
    begin
      select * into v_inv from taskvilla.list_members where invitation_id=("inviteToken")::uuid for update;
    exception when invalid_text_representation then
      raise sqlstate 'PT404' using message='Invitation not found';
    end;
    if not found or v_inv.status<>'pending' then raise sqlstate 'PT404' using message='Invitation not found or no longer pending'; end if;
    if v_inv.invited_email<>v_email then raise sqlstate 'PT403' using message='Invitation belongs to another email address'; end if;
    if action='decline' then
      update taskvilla.list_members set status='declined',ended_at=clock_timestamp()
        where invitation_id=v_inv.invitation_id;
      return jsonb_build_object('status','declined','invitationId',v_inv.invitation_id);
    end if;
    update taskvilla.list_members set status='accepted',member_user_id=v_user,accepted_at=clock_timestamp()
      where invitation_id=v_inv.invitation_id;
    -- Publish an initial snapshot after acceptance, so an existing device cursor sees the list.
    insert into taskvilla.change_events(owner_id,list_id,entity,entity_id,revision,operation,payload,modified_at)
      select e.owner_id,e.list_id,e.entity,e.entity_id,e.revision,e.operation,e.payload,clock_timestamp()
        from taskvilla.entities e where (e.entity='list' and e.entity_id=v_inv.list_id)
          or (e.entity='task' and e.list_id=v_inv.list_id)
        order by case when e.entity='list' then 0 else 1 end,e.modified_at,e.entity_id;
    return jsonb_build_object('status','accepted','invitationId',v_inv.invitation_id);
  elsif action='revoke' then
    if "listId" is null then raise sqlstate 'PT400' using message='listId is required'; end if;
    select owner_id into v_list_owner from taskvilla.entities
      where entity='list' and entity_id="listId";
    if not found then raise sqlstate 'PT404' using message='List not found'; end if;
    if v_list_owner<>v_user and "memberUserId"<>v_user then
      raise sqlstate 'PT403' using message='Only the owner may remove another member';
    end if;
    if "memberUserId" is not null and "memberUserId"<>v_user then
      update taskvilla.list_members set status='revoked',ended_at=clock_timestamp()
       where list_id="listId" and member_user_id="memberUserId" and status='accepted'
       returning * into v_inv;
    elsif "inviteToken" is not null and v_list_owner=v_user then
      update taskvilla.list_members set status='revoked',ended_at=clock_timestamp()
       where list_id="listId" and invitation_id=("inviteToken")::uuid and status='pending'
       returning * into v_inv;
    else
      update taskvilla.list_members set status='revoked',ended_at=clock_timestamp()
       where list_id="listId" and member_user_id=v_user and status='accepted'
       returning * into v_inv;
    end if;
    if not found then raise sqlstate 'PT404' using message='Member or invitation not found'; end if;
    if v_inv.member_user_id is not null then
      insert into taskvilla.change_events(owner_id,list_id,entity,entity_id,revision,operation,payload,modified_at,recipient_user_id)
       select e.owner_id,e.list_id,e.entity,e.entity_id,e.revision,'delete',null,clock_timestamp(),v_inv.member_user_id
        from taskvilla.entities e where (e.entity='list' and e.entity_id="listId")
          or (e.entity='task' and e.list_id="listId") order by e.entity,e.entity_id;
    end if;
    return jsonb_build_object('status','revoked','invitationId',v_inv.invitation_id);
  else
    if "listId" is null then
      select coalesce(jsonb_agg(jsonb_build_object('invitationId',m.invitation_id,
        'listId',m.list_id,'inviterUserId',m.owner_user_id,'email',m.invited_email,'permission',m.permission,'createdAt',m.created_at)
        order by m.created_at desc),'[]'::jsonb) into v_invites
        from taskvilla.list_members m where m.invited_email=v_email and m.status='pending';
      v_result := jsonb_build_object('status','listed','members',v_members,'invitations',v_invites);
      return v_result;
    end if;
    select owner_id into v_list_owner from taskvilla.entities
      where entity='list' and entity_id="listId";
    if not found then raise sqlstate 'PT404' using message='List not found'; end if;
    if v_list_owner<>v_user and not exists(select 1 from taskvilla.list_members
        where list_id="listId" and member_user_id=v_user and status='accepted') then
      raise sqlstate 'PT403' using message='List is not shared with this account';
    end if;
    select jsonb_agg(jsonb_build_object('userId',z.user_id,'permission',z.permission)
      order by case when z.permission='owner' then 0 else 1 end,z.user_id) into v_members
      from (select v_list_owner as user_id,'owner'::text as permission
            union all select m.member_user_id,m.permission from taskvilla.list_members m
             where m.list_id="listId" and m.status='accepted') z;
    if v_list_owner=v_user then
      select coalesce(jsonb_agg(jsonb_build_object('invitationId',m.invitation_id,
        'listId',m.list_id,'inviterUserId',m.owner_user_id,'email',m.invited_email,'permission',m.permission,'createdAt',m.created_at)
        order by m.created_at desc),'[]'::jsonb) into v_invites
        from taskvilla.list_members m where m.list_id="listId" and m.status='pending';
    end if;
    return jsonb_build_object('status','listed','members',coalesce(v_members,'[]'::jsonb),'invitations',v_invites);
  end if;
end;
$function$;

