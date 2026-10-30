import "jsr:@supabase/functions-js/edge-runtime.d.ts";

// TaskVilla provider-backed attachment gateway. No file bytes are written to Supabase.
const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SERVICE = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const MAX = 2_000_000;
const cors = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info, x-task-id, x-client-attachment-id, x-file-name, x-file-mime, x-file-size", "Access-Control-Allow-Methods": "GET, POST, DELETE, OPTIONS" };
type Provider = "google_drive" | "dropbox" | "onedrive";
type Conn = { connection_id: string; user_id: string; provider: Provider; provider_account_id: string; display_name: string; account_email: string; credential_ciphertext: string; folder_id: string; status: string; is_default: boolean; quota_total_bytes: number | null; quota_used_bytes: number | null };

class Fault extends Error { constructor(public code: string, public status: number, message = code) { super(message); } }
const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: { ...cors, "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store" } });
function fail(e: unknown) { const f = e instanceof Fault ? e : new Fault("PROVIDER_UNAVAILABLE", 503); return json({ error: { code: f.code, message: f.message } }, f.status); }
const env = (k: string) => Deno.env.get(k) ?? "";
function config(p: Provider) {
  const id = p === "google_drive" ? env("GOOGLE_DRIVE_CLIENT_ID") : p === "dropbox" ? env("DROPBOX_CLIENT_ID") : env("MICROSOFT_CLIENT_ID");
  const secret = p === "google_drive" ? env("GOOGLE_DRIVE_CLIENT_SECRET") : p === "dropbox" ? env("DROPBOX_CLIENT_SECRET") : env("MICROSOFT_CLIENT_SECRET");
  return { id, secret };
}
function provider(v: unknown): Provider { if (v === "google_drive" || v === "dropbox" || v === "onedrive") return v; throw new Fault("PROVIDER_NOT_CONFIGURED", 400, "Choose a supported provider."); }
function callbackUrl() { return `${SUPABASE_URL}/functions/v1/taskvilla-attachments?op=callback`; }
function key() { const v = env("TASKVILLA_TOKEN_ENCRYPTION_KEY"); if (!v) throw new Fault("PROVIDER_NOT_CONFIGURED", 503, "Cloud storage is not configured yet."); try { const b = Uint8Array.from(atob(v), c => c.charCodeAt(0)); if (b.length !== 32) throw 0; return crypto.subtle.importKey("raw", b, "AES-GCM", false, ["encrypt", "decrypt"]); } catch { throw new Fault("PROVIDER_UNAVAILABLE", 503, "Cloud storage configuration is unavailable."); } }
async function seal(text: string) { const iv = crypto.getRandomValues(new Uint8Array(12)); const k = await key(); const encrypted = new Uint8Array(await crypto.subtle.encrypt({ name: "AES-GCM", iv }, await k, new TextEncoder().encode(text))); const all = new Uint8Array(iv.length + encrypted.length); all.set(iv); all.set(encrypted, iv.length); return btoa(String.fromCharCode(...all)); }
async function unseal(value: string) { const all = Uint8Array.from(atob(value), c => c.charCodeAt(0)); const k = await key(); return new TextDecoder().decode(await crypto.subtle.decrypt({ name: "AES-GCM", iv: all.slice(0, 12) }, await k, all.slice(12))); }
async function digest(text: string) { const b = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text)); return [...new Uint8Array(b)].map(x => x.toString(16).padStart(2, "0")).join(""); }
async function pkceChallenge(text: string) { const b = new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text))); return btoa(String.fromCharCode(...b)).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", ""); }

async function db(table: string, method = "GET", params = "", body?: unknown, prefer = "") {
  if (!SUPABASE_URL || !SERVICE) throw new Fault("PROVIDER_UNAVAILABLE", 503, "Cloud storage service is unavailable.");
  const response = await fetch(`${SUPABASE_URL}/rest/v1/${table}${params ? `?${params}` : ""}`, { method, headers: { apikey: SERVICE, Authorization: `Bearer ${SERVICE}`, "Content-Type": "application/json", "Accept-Profile": "taskvilla", "Content-Profile": "taskvilla", ...(prefer ? { Prefer: prefer } : {}) }, body: body === undefined ? undefined : JSON.stringify(body) });
  if (!response.ok) throw new Fault("PROVIDER_UNAVAILABLE", 503, "Cloud storage service is unavailable.");
  const text = await response.text(); return text ? JSON.parse(text) : [];
}
async function authenticate(req: Request) {
  const bearer = req.headers.get("authorization") ?? ""; if (!bearer.startsWith("Bearer ")) throw new Fault("AUTH_REQUIRED", 401, "Sign in to use cloud storage.");
  const apiKey = req.headers.get("apikey") ?? env("SUPABASE_ANON_KEY");
  const r = await fetch(`${SUPABASE_URL}/auth/v1/user`, { headers: { apikey: apiKey, Authorization: bearer } });
  if (!r.ok) throw new Fault("AUTH_REQUIRED", 401, "Sign in to use cloud storage.");
  const user = await r.json(); if (!user?.id) throw new Fault("AUTH_REQUIRED", 401, "Sign in to use cloud storage.");
  return { id: String(user.id), email: String(user.email ?? "").toLowerCase() };
}
async function rows(table: string, filters: Record<string, string>) { return await db(table, "GET", new URLSearchParams({ select: "*", ...filters }).toString()) as Record<string, unknown>[]; }
async function connection(userId: string, p: Provider) { const a = await rows("user_cloud_connections", { user_id: `eq.${userId}`, provider: `eq.${p}`, select: "*" }); return a[0] as unknown as Conn | undefined; }
async function allConnections(userId: string) { return await rows("user_cloud_connections", { user_id: `eq.${userId}`, select: "connection_id,provider,provider_account_id,display_name,account_email,status,is_default,quota_total_bytes,quota_used_bytes,updated_at" }); }
async function taskFor(taskId: string) { const a = await rows("entities", { entity: "eq.task", entity_id: `eq.${taskId}`, select: "entity_id,owner_id,list_id,operation,payload" }); return a[0] as { entity_id: string; owner_id: string; list_id: string; operation: string; payload: Record<string, unknown> } | undefined; }
async function canAccess(userId: string, taskId: string) {
  if (!/^[0-9a-f-]{36}$/i.test(taskId)) throw new Fault("FILE_NOT_FOUND", 404, "Attachment not found.");
  const task = await taskFor(taskId); if (!task || task.operation !== "upsert") throw new Fault("FILE_NOT_FOUND", 404, "Attachment not found.");
  if (task.owner_id === userId) return task;
  const members = await rows("list_members", { list_id: `eq.${task.list_id}`, member_user_id: `eq.${userId}`, status: "eq.accepted", select: "invitation_id" });
  if (!members.length) throw new Fault("ACCESS_DENIED", 403, "You no longer have access to this task.");
  return task;
}
async function connectionForAttachment(a: Record<string, unknown>) { const result = await rows("user_cloud_connections", { connection_id: `eq.${a.connection_id}`, select: "*" }); return result[0] as unknown as Conn | undefined; }
function bearerToken(r: Request) { return (r.headers.get("authorization") ?? "").slice(7); }

async function providerToken(p: Provider, tokens: Record<string, unknown>) {
  const expiry = Number(tokens.expires_at ?? 0); if (expiry > Date.now() + 60_000) return String(tokens.access_token);
  const c = config(p); const refresh = String(tokens.refresh_token ?? ""); if (!refresh || !c.id) throw new Fault("CONNECTION_DISCONNECTED", 409, "Reconnect this cloud account.");
  let endpoint = "", form: Record<string, string> = { client_id: c.id, grant_type: "refresh_token", refresh_token: refresh };
  if (p === "google_drive") { endpoint = "https://oauth2.googleapis.com/token"; form.client_secret = c.secret; }
  else if (p === "dropbox") { endpoint = "https://api.dropboxapi.com/oauth2/token"; form.client_secret = c.secret; }
  else { endpoint = "https://login.microsoftonline.com/common/oauth2/v2.0/token"; form.client_secret = c.secret; form.scope = "offline_access Files.ReadWrite.AppFolder User.Read"; }
  const response = await fetch(endpoint, { method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded" }, body: new URLSearchParams(form) });
  if (!response.ok) throw new Fault("CONNECTION_DISCONNECTED", 409, "Reconnect this cloud account.");
  const next = await response.json(); Object.assign(tokens, next, { expires_at: Date.now() + Number(next.expires_in ?? 3600) * 1000, refresh_token: next.refresh_token || refresh });
  return String(next.access_token);
}
async function tokensFor(c: Conn) { if (c.status !== "connected") throw new Fault("CONNECTION_DISCONNECTED", 409, "Reconnect this cloud account."); const t = JSON.parse(await unseal(c.credential_ciphertext)); let access: string; try { access=await providerToken(c.provider,t); } catch(e) { if(e instanceof Fault&&e.code==="CONNECTION_DISCONNECTED") await db("user_cloud_connections","PATCH",`connection_id=eq.${c.connection_id}`,{status:"reauthorization_required",updated_at:new Date().toISOString()},"return=minimal").catch(()=>{}); throw e; } await db("user_cloud_connections", "PATCH", `connection_id=eq.${c.connection_id}`, { credential_ciphertext: await seal(JSON.stringify(t)), updated_at: new Date().toISOString() }, "return=minimal"); return { access, tokens: t }; }
async function pFetch(p: Provider, path: string, token: string, init: RequestInit = {}) {
  let url = path; const headers = new Headers(init.headers); if (p === "google_drive") { url = `https://www.googleapis.com${path}`; headers.set("Authorization", `Bearer ${token}`); }
  else if (p === "dropbox") { url = path.startsWith("https://") ? path : `https://api.dropboxapi.com${path}`; headers.set("Authorization", `Bearer ${token}`); }
  else { url = `https://graph.microsoft.com/v1.0${path}`; headers.set("Authorization", `Bearer ${token}`); }
  return await fetch(url, { ...init, headers });
}
async function profile(p: Provider, token: string) {
  let r: Response, data: Record<string, unknown>;
  if (p === "google_drive") { r = await pFetch(p, "/oauth2/v2/userinfo", token); data = await r.json(); const q=await pFetch(p,"/drive/v3/about?fields=storageQuota",token);const quota=q.ok?(await q.json()).storageQuota??{}:{};return { id: data.id, name: data.name, email: data.email, quotaUsed: quota.usage??null, quotaTotal: quota.limit??null }; }
  if (p === "dropbox") { r = await pFetch(p, "/2/users/get_current_account", token, { method: "POST" }); data = await r.json(); const n = (data.name ?? {}) as Record<string, unknown>; const q=await pFetch(p,"/2/users/get_space_usage",token,{method:"POST"});const quota=q.ok?await q.json():{};const allocation=(quota.allocation??{}) as Record<string,unknown>;return { id: data.account_id, name: n.display_name, email: data.email, quotaUsed: quota.used??null, quotaTotal: allocation.allocated??null }; }
  r = await pFetch(p, "/me?$select=id,displayName,mail,userPrincipalName", token); data = await r.json();
  const q = await pFetch(p, "/me/drive?$select=quota", token); const quota = q.ok ? (await q.json()).quota ?? {} : {};
  return { id: data.id, name: data.displayName, email: data.mail ?? data.userPrincipalName, quotaUsed: quota.used ?? null, quotaTotal: quota.total ?? null };
}
async function ensureFolder(p: Provider, token: string, existing: string) {
  if (existing) return existing;
  if (p === "google_drive") {
    const q = encodeURIComponent("name='TaskVilla' and mimeType='application/vnd.google-apps.folder' and trashed=false"); const f = await pFetch(p, `/drive/v3/files?q=${q}&fields=files(id)&pageSize=1`, token); if (f.ok) { const a = (await f.json()).files ?? []; if (a[0]?.id) return String(a[0].id); }
    const c = await pFetch(p, "/drive/v3/files?fields=id", token, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ name: "TaskVilla", mimeType: "application/vnd.google-apps.folder" }) }); if (!c.ok) throw new Fault("PROVIDER_UNAVAILABLE", 503); return String((await c.json()).id);
  }
  if (p === "dropbox") { const r = await pFetch(p, "/2/files/get_metadata", token, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ path: "/TaskVilla" }) }); if (r.ok) return "/TaskVilla"; const c = await pFetch(p, "/2/files/create_folder_v2", token, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ path: "/TaskVilla", autorename: false }) }); if (!c.ok && c.status !== 409) throw new Fault("PROVIDER_UNAVAILABLE", 503); return "/TaskVilla"; }
  const r = await pFetch(p, "/me/drive/special/approot", token); if (!r.ok) throw new Fault("PROVIDER_UNAVAILABLE", 503); const root = await r.json();
  const c = await pFetch(p, `/me/drive/items/${root.id}/children`, token, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ name: "TaskVilla", folder: {}, "@microsoft.graph.conflictBehavior": "fail" }) }); if (c.ok) return String((await c.json()).id);
  const children = await pFetch(p, `/me/drive/items/${root.id}/children?$filter=name%20eq%20'TaskVilla'`, token); if (children.ok) { const a = (await children.json()).value ?? []; if (a[0]?.id) return String(a[0].id); } throw new Fault("PROVIDER_UNAVAILABLE", 503);
}
function safeName(n: string) { const s = n.replace(/[\u0000-\u001f\\/]/g, "_").trim().slice(0, 180); return s || "Attachment"; }
async function uploadProvider(c: Conn, token: string, folder: string, id: string, name: string, mime: string, bytes: Uint8Array) {
  const filename = `${id}_${safeName(name)}`;
  if (c.provider === "google_drive") { const boundary = `tv_${crypto.randomUUID().replaceAll("-", "")}`; const meta = new TextEncoder().encode(`--${boundary}\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n${JSON.stringify({ name: filename, parents: [folder], appProperties: { taskvillaAttachment: id } })}\r\n--${boundary}\r\nContent-Type: ${mime}\r\n\r\n`); const end = new TextEncoder().encode(`\r\n--${boundary}--`); const body = new Uint8Array(meta.length + bytes.length + end.length); body.set(meta); body.set(bytes, meta.length); body.set(end, meta.length + bytes.length); const r = await pFetch(c.provider, "/upload/drive/v3/files?uploadType=multipart&fields=id", token, { method: "POST", headers: { "Content-Type": `multipart/related; boundary=${boundary}` }, body }); if (!r.ok) throw await providerFault(r); return String((await r.json()).id); }
  if (c.provider === "dropbox") { const r = await pFetch(c.provider, "https://content.dropboxapi.com/2/files/upload", token, { method: "POST", headers: { "Content-Type": "application/octet-stream", "Dropbox-API-Arg": JSON.stringify({ path: `${folder}/${filename}`, mode: "add", autorename: false, mute: true }) }, body: bytes }); if (!r.ok) throw await providerFault(r); return String((await r.json()).id); }
  const path = `/me/drive/items/${encodeURIComponent(folder)}:/${encodeURIComponent(filename)}:/content`; const r = await pFetch(c.provider, path, token, { method: "PUT", headers: { "Content-Type": mime }, body: bytes }); if (!r.ok) throw await providerFault(r); return String((await r.json()).id);
}
async function providerFault(r: Response): Promise<Fault> { if (r.status === 429) return new Fault("PROVIDER_RATE_LIMITED", 429, "Cloud provider rate limit reached. Try again shortly."); if (r.status === 507 || r.status === 413) return new Fault("QUOTA_EXCEEDED", 507, "Cloud storage is full. Choose another provider."); const detail=await r.clone().text().catch(()=>"");if((r.status===403||r.status===507)&&/quota|storage.?full|insufficient.?space/i.test(detail))return new Fault("QUOTA_EXCEEDED",507,"Cloud storage is full. Choose another provider.");if(r.status===401)return new Fault("CONNECTION_DISCONNECTED",409,"Reconnect this cloud account.");if (r.status >= 500) return new Fault("PROVIDER_UNAVAILABLE", 503, "Cloud provider is temporarily unavailable."); return new Fault("UPLOAD_FAILED", 502, "The file could not be uploaded."); }
async function deleteProvider(p: Provider, token: string, id: string) { const path = p === "google_drive" ? `/drive/v3/files/${encodeURIComponent(id)}` : p === "dropbox" ? "/2/files/delete_v2" : `/me/drive/items/${encodeURIComponent(id)}`; const r = await pFetch(p, path, token, p === "dropbox" ? { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ path: id }) } : { method: "DELETE" }); return r.ok || r.status === 404; }
async function downloadProvider(p: Provider, token: string, id: string) { const path = p === "google_drive" ? `/drive/v3/files/${encodeURIComponent(id)}?alt=media` : p === "dropbox" ? "https://content.dropboxapi.com/2/files/download" : `/me/drive/items/${encodeURIComponent(id)}/content`; const r = await pFetch(p, path, token, p === "dropbox" ? { method: "POST", headers: { "Dropbox-API-Arg": JSON.stringify({ path: id }) } } : {}); if (r.status === 404) throw new Fault("FILE_UNAVAILABLE", 404, "This attachment is no longer available in cloud storage."); if (!r.ok || !r.body) throw new Fault("DOWNLOAD_FAILED", 502, "The attachment could not be downloaded."); return r; }
async function readLimited(req: Request, limit: number) {
  const reader = req.body?.getReader(); if (!reader) return new Uint8Array();
  const chunks: Uint8Array[] = []; let size = 0;
  try {
    while (true) {
      const { done, value } = await reader.read(); if (done) break;
      size += value.byteLength;
      if (size > limit) { await reader.cancel(); throw new Fault("FILE_TOO_LARGE", 413, "Each file must be 2 MB or smaller."); }
      chunks.push(value);
    }
  } finally { reader.releaseLock(); }
  const result = new Uint8Array(size); let offset = 0;
  for (const chunk of chunks) { result.set(chunk, offset); offset += chunk.byteLength; }
  return result;
}

async function callback(req: Request) {
  const u = new URL(req.url); const state = u.searchParams.get("state") ?? ""; const code = u.searchParams.get("code") ?? ""; const err = u.searchParams.get("error");
  const page = (message: string) => new Response(`<!doctype html><meta name="viewport" content="width=device-width"><title>TaskVilla Cloud Storage</title><p>${message}</p><script>setTimeout(()=>location.href='taskvilla://cloud-storage-connected',500)</script><a href="taskvilla://cloud-storage-connected">Return to TaskVilla</a>`, { headers: { ...cors, "Content-Type": "text/html; charset=utf-8", "Cache-Control": "no-store" } });
  if (err || !code || !state) return page("Cloud account connection was cancelled. Return to TaskVilla.");
  const hash = await digest(state); const states = await rows("cloud_oauth_states", { state_hash: `eq.${hash}`, expires_at: `gt.${new Date().toISOString()}`, select: "*" }); const saved = states[0]; if (!saved) return page("This connection request has expired. Return to TaskVilla and try again.");const p=provider(saved.provider);
  await db("cloud_oauth_states", "DELETE", `state_id=eq.${saved.state_id}`);
  const c = config(p); const tokenEndpoint = p === "google_drive" ? "https://oauth2.googleapis.com/token" : p === "dropbox" ? "https://api.dropboxapi.com/oauth2/token" : "https://login.microsoftonline.com/common/oauth2/v2.0/token";
  const form: Record<string, string> = { client_id: c.id, code, redirect_uri: callbackUrl(), grant_type: "authorization_code", code_verifier: String(saved.code_verifier) }; if (p !== "dropbox") form.client_secret = c.secret;
  const tr = await fetch(tokenEndpoint, { method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded" }, body: new URLSearchParams(form) }); if (!tr.ok) return page("Cloud account authorization failed. Check provider app configuration and try again.");
  const tok = await tr.json(); if (!tok.refresh_token) return page("The provider did not grant offline access. Reconnect and approve access.");
  const access = String(tok.access_token); const account = await profile(p, access); if (!account.id) return page("Could not identify this cloud account. Try again.");
  const connected = (await allConnections(String(saved.user_id)) as Record<string, unknown>[]).find(row => row.status === "connected" && row.provider !== p);
  if (connected) return page("Disconnect the connected cloud provider before switching providers.");
  const existing = await connection(String(saved.user_id), p); if (existing && existing.provider_account_id !== String(account.id)) return page("A different account is already connected for this provider. Disconnect it first.");
  const folder = await ensureFolder(p, access, existing?.folder_id ?? ""); Object.assign(tok, { expires_at: Date.now() + Number(tok.expires_in ?? 3600) * 1000 }); const cipher = await seal(JSON.stringify(tok));
  const values = { user_id: saved.user_id, provider: p, provider_account_id: String(account.id), display_name: String(account.name ?? ""), account_email: String(account.email ?? "").toLowerCase(), credential_ciphertext: cipher, folder_id: folder, status: "connected", is_default: true, updated_at: new Date().toISOString(), quota_total_bytes: account.quotaTotal, quota_used_bytes: account.quotaUsed };
  if (existing) await db("user_cloud_connections", "PATCH", `connection_id=eq.${existing.connection_id}`, values, "return=minimal"); else await db("user_cloud_connections", "POST", "", values, "return=minimal");
  return page("Cloud account connected. Return to TaskVilla to finish.");
}
async function handler(req: Request) {
  if (req.method === "OPTIONS") return new Response("", { headers: cors });
  const url = new URL(req.url); const op = url.searchParams.get("op") ?? "";
  if (req.method === "GET" && op === "callback") return await callback(req);
  const user = await authenticate(req); const body = req.method === "POST" && op !== "upload" ? await req.json().catch(() => ({})) : {};
  switch (op) {
    case "connect": {
      const p = provider(body.provider); const c = config(p); if (!c.id || !c.secret || !env("TASKVILLA_TOKEN_ENCRYPTION_KEY")) throw new Fault("PROVIDER_NOT_CONFIGURED", 503, "This cloud provider is not configured yet.");
      const active = (await allConnections(user.id) as Record<string, unknown>[]).find(row => row.status === "connected" && row.provider !== p);
      if (active) throw new Fault("PROVIDER_CONFLICT", 409, "Disconnect the connected cloud provider before switching providers.");
      const state = crypto.randomUUID() + crypto.randomUUID(); const verifierBytes = crypto.getRandomValues(new Uint8Array(32)); const verifier = btoa(String.fromCharCode(...verifierBytes)).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", ""); const challenge = await pkceChallenge(verifier); const stateHash = await digest(state);
      await db("cloud_oauth_states", "POST", "", { state_hash: stateHash, user_id: user.id, provider: p, code_verifier: verifier, expires_at: new Date(Date.now() + 10 * 60_000).toISOString() }, "return=minimal");
      let auth: URL;
      if (p === "google_drive") { auth = new URL("https://accounts.google.com/o/oauth2/v2/auth"); auth.search = new URLSearchParams({ client_id: c.id, redirect_uri: callbackUrl(), response_type: "code", scope: "https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/userinfo.email", access_type: "offline", prompt: "consent", state, code_challenge: challenge, code_challenge_method: "S256" }).toString(); }
      else if (p === "dropbox") { auth = new URL("https://www.dropbox.com/oauth2/authorize"); auth.search = new URLSearchParams({ client_id: c.id, redirect_uri: callbackUrl(), response_type: "code", token_access_type: "offline", state, code_challenge: challenge, code_challenge_method: "S256", scope: "account_info.read files.content.read files.content.write files.metadata.read" }).toString(); }
      else { auth = new URL("https://login.microsoftonline.com/common/oauth2/v2.0/authorize"); auth.search = new URLSearchParams({ client_id: c.id, redirect_uri: callbackUrl(), response_type: "code", response_mode: "query", scope: "offline_access User.Read Files.ReadWrite.AppFolder", state, code_challenge: challenge, code_challenge_method: "S256" }).toString(); }
      return json({ ok: true, authorizationUrl: auth.toString() });
    }
    case "connection-status": {
      const conns = await allConnections(user.id) as Record<string, unknown>[];
      const active = conns.filter(c => c.status === "connected");
      if (active.length === 1 && !active[0].is_default) {
        await db("user_cloud_connections", "PATCH", `connection_id=eq.${active[0].connection_id}`, { is_default: true, updated_at: new Date().toISOString() }, "return=minimal");
        active[0].is_default = true;
      }
      const counts = await rows("task_attachments", { uploader_id: `eq.${user.id}`, status: "neq.deleted", select: "connection_id" }); const providers = ["google_drive", "dropbox", "onedrive"].map(p => { const raw=conns.find(c => c.provider === p); const c=raw?{connectionId:raw.connection_id,provider:raw.provider,providerAccountId:raw.provider_account_id,displayName:raw.display_name,accountEmail:raw.account_email,status:raw.status,isDefault:raw.is_default,quotaTotalBytes:raw.quota_total_bytes,quotaUsedBytes:raw.quota_used_bytes,updatedAt:raw.updated_at}:null;return { provider:p, configured:!!config(p as Provider).id&&!!config(p as Provider).secret&&!!env("TASKVILLA_TOKEN_ENCRYPTION_KEY"), connection:c, attachmentCount:(counts as Record<string, unknown>[]).filter(a=>conns.some(row=>row.connection_id===a.connection_id&&row.provider===p)).length }; });
      return json({ ok: true, providers, defaultProvider: (conns as Record<string, unknown>[]).find(c => c.is_default)?.provider ?? null });
    }
    case "set-default": {
      const p = provider(body.provider); const c = await connection(user.id, p); if (!c || c.status !== "connected") throw new Fault("CONNECTION_DISCONNECTED", 409, "Connect this provider before making it the default.");
      await db("user_cloud_connections", "PATCH", `user_id=eq.${user.id}&is_default=eq.true`, { is_default: false, updated_at: new Date().toISOString() }, "return=minimal"); await db("user_cloud_connections", "PATCH", `connection_id=eq.${c.connection_id}`, { is_default: true, updated_at: new Date().toISOString() }, "return=minimal"); return json({ ok: true });
    }
    case "disconnect": {
      const p = provider(body.provider); const c = await connection(user.id, p); if (!c) throw new Fault("CONNECTION_DISCONNECTED", 409); const list = await rows("task_attachments", { connection_id: `eq.${c.connection_id}`, status: "neq.deleted", select: "attachment_id" });
      await db("user_cloud_connections", "PATCH", `connection_id=eq.${c.connection_id}`, { status: "disconnected", is_default: false, updated_at: new Date().toISOString() }, "return=minimal"); return json({ ok: true, dependentAttachments: list.length, filesRetained: true });
    }
    case "upload": {
      const taskId = req.headers.get("x-task-id") ?? "", clientId = req.headers.get("x-client-attachment-id") ?? ""; const task = await canAccess(user.id, taskId);
      if (!/^[0-9a-f-]{36}$/i.test(clientId)) throw new Fault("UPLOAD_FAILED", 400, "Could not upload this file.");
      const prior = await rows("task_attachments", { uploader_id: `eq.${user.id}`, client_attachment_id: `eq.${clientId}`, select: "*" }); if (prior[0]) return json({ ok: true, attachment: publicAttachment(prior[0]) });
      const declared = Number(req.headers.get("x-file-size") ?? req.headers.get("content-length") ?? 0); if (Number.isFinite(declared) && declared > MAX) throw new Fault("FILE_TOO_LARGE", 413, "Each file must be 2 MB or smaller.");
      const data = await readLimited(req, MAX); if (declared > 0 && declared !== data.length) throw new Fault("UPLOAD_FAILED", 400, "The selected file could not be read completely."); if (!data.length) throw new Fault("UPLOAD_FAILED", 400, "The selected file is empty.");
      const defaults = await rows("user_cloud_connections", { user_id: `eq.${user.id}`, is_default: "eq.true", status: "eq.connected", select: "*" }); if (!defaults[0]) throw new Fault("CONNECTION_DISCONNECTED", 409, "Connect a default cloud provider first."); const p = provider(defaults[0].provider); const c = defaults[0] as unknown as Conn;
      const sha = [...new Uint8Array(await crypto.subtle.digest("SHA-256", data))].map(x => x.toString(16).padStart(2, "0")).join(""); const duplicate = await rows("task_attachments", { task_id: `eq.${taskId}`, sha256: `eq.${sha}`, status: "neq.deleted", select: "attachment_id" }); if (duplicate.length) throw new Fault("UPLOAD_FAILED", 409, "Same file already attached."); const current = await rows("task_attachments", { task_id: `eq.${taskId}`, status: "neq.deleted", select: "attachment_id" }); if (current.length >= 10) throw new Fault("UPLOAD_FAILED", 400, "Maximum 10 files per task.");
      const { access } = await tokensFor(c); const folder = await ensureFolder(p, access, c.folder_id); if (!c.folder_id) await db("user_cloud_connections", "PATCH", `connection_id=eq.${c.connection_id}`, { folder_id: folder }, "return=minimal");
      const name = safeName(decodeURIComponent(req.headers.get("x-file-name") ?? "Attachment")); const mime = (req.headers.get("x-file-mime") ?? "application/octet-stream").slice(0, 120); let providerId = "";
      try { providerId = await uploadProvider(c, access, folder, clientId, name, mime, data); const row = { task_id: taskId, list_id: task.list_id, uploader_id: user.id, connection_id: c.connection_id, provider: p, provider_file_id: providerId, client_attachment_id: clientId, original_name: name, mime_type: mime, size_bytes: data.length, sha256: sha, status: "available" }; const added = await db("task_attachments", "POST", "", row, "return=representation"); return json({ ok: true, attachment: publicAttachment(added[0] ?? row) }); }
      catch (e) { if (providerId) await deleteProvider(p, access, providerId).catch(() => false); throw e; }
    }
    case "list": {
      if (Array.isArray(body.taskIds)) {
        const ids = [...new Set((body.taskIds as unknown[]).map(String))];
        if (ids.length > 100 || ids.some(id => !/^[0-9a-f-]{36}$/i.test(id))) throw new Fault("FILE_NOT_FOUND", 404, "Attachment not found.");
        if (!ids.length) return json({ ok: true, attachmentsByTask: [] });
        const inIds = `in.(${ids.join(",")})`;
        const tasks = await rows("entities", { entity: "eq.task", entity_id: inIds, operation: "eq.upsert", select: "entity_id,owner_id,list_id" });
        const listIds = [...new Set(tasks.map(t => String(t.list_id)))];
        const memberships = listIds.length ? await rows("list_members", { list_id: `in.(${listIds.join(",")})`, member_user_id: `eq.${user.id}`, status: "eq.accepted", select: "list_id" }) : [];
        const allowedLists = new Set(memberships.map(m => String(m.list_id)));
        const allowedTasks = tasks.filter(t => t.owner_id === user.id || allowedLists.has(String(t.list_id))).map(t => String(t.entity_id));
        if (!allowedTasks.length) throw new Fault("ACCESS_DENIED", 403, "You no longer have access to this task.");
        const attachments = await rows("task_attachments", { task_id: `in.(${allowedTasks.join(",")})`, status: "neq.deleted", order: "created_at.desc", select: "*" });
        const connectionIds = [...new Set(attachments.map(a => String(a.connection_id)))];
        const connections = connectionIds.length ? await rows("user_cloud_connections", { connection_id: `in.(${connectionIds.join(",")})`, select: "connection_id,status" }) : [];
        const statusByConnection = new Map(connections.map(c => [String(c.connection_id), String(c.status)]));
        const grouped = new Map<string, Record<string, unknown>[]>();
        for (const item of attachments) {
          const status = statusByConnection.get(String(item.connection_id)) ?? "disconnected";
          const group = grouped.get(String(item.task_id)) ?? [];
          group.push({ ...publicAttachment(item), accessible: status === "connected", connectionStatus: status });
          grouped.set(String(item.task_id), group);
        }
        return json({ ok: true, attachmentsByTask: [...grouped].map(([taskId, items]) => ({ taskId, attachments: items })) });
      }
      const taskId = String(body.taskId ?? url.searchParams.get("taskId") ?? ""); await canAccess(user.id, taskId); const a = await rows("task_attachments", { task_id: `eq.${taskId}`, status: "neq.deleted", order: "created_at.desc", select: "*" }); const out = [];
      for (const item of a) { const c = await connectionForAttachment(item); out.push({ ...publicAttachment(item), accessible: !!c && c.status === "connected", connectionStatus: c?.status ?? "disconnected" }); }
      return json({ ok: true, attachments: out });
    }
    case "download": {
      const id = String(body.attachmentId ?? url.searchParams.get("attachmentId") ?? ""); const a = (await rows("task_attachments", { attachment_id: `eq.${id}`, status: "neq.deleted", select: "*" }))[0]; if (!a) throw new Fault("FILE_NOT_FOUND", 404, "Attachment not found."); await canAccess(user.id, String(a.task_id)); const c = await connectionForAttachment(a); if (!c) throw new Fault("FILE_UNAVAILABLE", 404, "Cloud account is unavailable."); const { access } = await tokensFor(c); const r = await downloadProvider(c.provider, access, String(a.provider_file_id)); return new Response(r.body, { status: 200, headers: { ...cors, "Content-Type": String(a.mime_type), "Content-Length": String(a.size_bytes), "Content-Disposition": `attachment; filename*=UTF-8''${encodeURIComponent(String(a.original_name))}`, "Cache-Control": "private, no-store" } });
    }
    case "delete": {
      const id = String(body.attachmentId ?? ""); const a = (await rows("task_attachments", { attachment_id: `eq.${id}`, status: "neq.deleted", select: "*" }))[0]; if (!a) throw new Fault("FILE_NOT_FOUND", 404, "Attachment not found."); const task = await canAccess(user.id, String(a.task_id)); if (a.uploader_id !== user.id && task.owner_id !== user.id) throw new Fault("ACCESS_DENIED", 403, "You cannot remove this attachment."); const c = await connectionForAttachment(a); if (!c || c.status !== "connected") { await db("task_attachments", "PATCH", `attachment_id=eq.${id}`, { status: "delete_pending", updated_at: new Date().toISOString() }, "return=minimal"); throw new Fault("CLEANUP_PENDING", 409, "The attachment was removed here. Cloud cleanup is pending reconnection."); }
      const { access } = await tokensFor(c); if (!await deleteProvider(c.provider, access, String(a.provider_file_id))) { await db("task_attachments", "PATCH", `attachment_id=eq.${id}`, { status: "delete_pending", updated_at: new Date().toISOString() }, "return=minimal"); throw new Fault("CLEANUP_PENDING", 409, "Cloud cleanup is pending. The provider did not confirm deletion."); } await db("task_attachments", "PATCH", `attachment_id=eq.${id}`, { status: "deleted", updated_at: new Date().toISOString() }, "return=minimal"); return json({ ok: true, deleted: true });
    }
    case "cleanup": {
      const items = await rows("task_attachments", { status: "eq.delete_pending", select: "*" }); let cleaned = 0;
      for (const a of items) { const c = await connectionForAttachment(a); if (!c || c.status !== "connected") continue; try { const { access } = await tokensFor(c); if (await deleteProvider(c.provider, access, String(a.provider_file_id))) { await db("task_attachments", "PATCH", `attachment_id=eq.${a.attachment_id}`, { status: "deleted", updated_at: new Date().toISOString() }, "return=minimal"); cleaned++; } } catch { /* Retain metadata for a later retry. */ } } return json({ ok: true, cleaned, pending: items.length - cleaned });
    }
    case "migrate": throw new Fault("PROVIDER_NOT_CONFIGURED", 501, "Attachment migration is not available yet. Existing files remain in their original provider.");
    default: throw new Fault("FILE_NOT_FOUND", 404, "Unknown cloud storage operation.");
  }
}
function publicAttachment(a: Record<string, unknown>) { return { attachmentId: a.attachment_id, taskId: a.task_id, uploaderId: a.uploader_id, provider: a.provider, fileName: a.original_name, mimeType: a.mime_type, sizeBytes: a.size_bytes, status: a.status, createdAt: a.created_at }; }
Deno.serve(async req => { try { return await handler(req); } catch (e) { return fail(e); } });
