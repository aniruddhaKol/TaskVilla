/* Daylist visual prototype — deliberately local-only with sample data.
   No Firebase, network requests, backend, real authentication, or live sharing. */
'use strict';
const iconPaths={
 arrowLeft:'<path d="m15 18-6-6 6-6"/><path d="M9 12h12"/>',
 arrowRight:'<path d="M5 12h14"/><path d="m13 6 6 6-6 6"/>',
 chevronRight:'<path d="m9 18 6-6-6-6"/>',
 chevronDown:'<path d="m6 9 6 6 6-6"/>',
 check:'<path d="m5 12 4 4L19 6"/>',
 plus:'<path d="M12 5v14M5 12h14"/>',
 more:'<circle cx="5" cy="12" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/>',
 search:'<circle cx="11" cy="11" r="7"/><path d="m16.5 16.5 4 4"/>',
 list:'<path d="M9 6h11M9 12h11M9 18h11"/><path d="M4 6h.01M4 12h.01M4 18h.01"/>',
 home:'<path d="m3 10 9-7 9 7v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1z"/><path d="M9 21v-8h6v8"/>',
 calendar:'<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 10h18"/>',
 bell:'<path d="M18 8a6 6 0 1 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4"/>',
 settings:'<path d="M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z"/><path d="M19.4 15a1.7 1.7 0 0 0 .35 1.85l.08.08-2 2-.08-.08a1.7 1.7 0 0 0-1.85-.35 1.7 1.7 0 0 0-1.05 1.55V21h-3v-.95a1.7 1.7 0 0 0-1.05-1.55 1.7 1.7 0 0 0-1.85.35l-.08.08-2-2 .08-.08A1.7 1.7 0 0 0 6.6 15a1.7 1.7 0 0 0-1.55-1.05H4v-3h1.05A1.7 1.7 0 0 0 6.6 9.9a1.7 1.7 0 0 0-.35-1.85l-.08-.08 2-2 .08.08A1.7 1.7 0 0 0 10.1 6.4 1.7 1.7 0 0 0 11.15 4.85V4h3v.85A1.7 1.7 0 0 0 15.2 6.4a1.7 1.7 0 0 0 1.85-.35l.08-.08 2 2-.08.08A1.7 1.7 0 0 0 18.7 9.9a1.7 1.7 0 0 0 1.55 1.05H21v3h-.75A1.7 1.7 0 0 0 19.4 15Z"/>',
 user:'<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/>',
 users:'<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>',
 userPlus:'<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M19 8v6M16 11h6"/>',
 share:'<circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><path d="m8.7 10.6 6.5-4M8.7 13.4l6.5 4"/>',
 link:'<path d="M10 13a5 5 0 0 0 7 .5l3-3a5 5 0 0 0-7-7l-1.7 1.7"/><path d="M14 11a5 5 0 0 0-7-.5l-3 3a5 5 0 0 0 7 7l1.7-1.7"/>',
 copy:'<rect x="8" y="8" width="13" height="13" rx="2"/><path d="M16 8V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h3"/>',
 lock:'<rect x="5" y="10" width="14" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/>',
 shield:'<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10Z"/><path d="m9 12 2 2 4-4"/>',
 repeat:'<path d="m17 2 4 4-4 4M3 11V9a3 3 0 0 1 3-3h15M7 22l-4-4 4-4M21 13v2a3 3 0 0 1-3 3H3"/>',
 tag:'<path d="M2 12V3h9l11 11-8 8z"/><circle cx="7.5" cy="7.5" r="1"/>',
 clock:'<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
 edit:'<path d="m13 5 6 6M4 20l4-.7L20 7a2.1 2.1 0 0 0-3-3L5 16z"/>',
 message:'<path d="M21 11.5a8.5 8.5 0 0 1-8.5 8.5 10 10 0 0 1-4-.8L3 21l1.8-5.5a8.5 8.5 0 1 1 16.2-4Z"/>',
 sparkle:'<path d="m12 2 2.5 7.5L22 12l-7.5 2.5L12 22l-2.5-7.5L2 12l7.5-2.5Z"/>',
 sun:'<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.93 4.93l1.42 1.42m11.3 11.3 1.42 1.42M2 12h2m16 0h2M4.93 19.07l1.42-1.42m11.3-11.3 1.42-1.42"/>',
 folder:'<path d="M3 7a2 2 0 0 1 2-2h5l2 2h7a2 2 0 0 1 2 2v10H3Z"/>',
 trash:'<path d="M3 6h18M8 6V4h8v2M6 6l1 15h10l1-15M10 10v7m4-7v7"/>',
 wifi:'<path d="M2 8a16 16 0 0 1 20 0M5 12a11 11 0 0 1 14 0M9 16a5 5 0 0 1 6 0"/><circle cx="12" cy="20" r="1"/>',
 help:'<circle cx="12" cy="12" r="10"/><path d="M9.5 9a2.5 2.5 0 1 1 4 2c-1 .7-1.5 1.1-1.5 2.5M12 17h.01"/>',
 logOut:'<path d="M10 17l5-5-5-5M15 12H3M13 3h5a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3h-5"/>',
 filter:'<path d="M4 7h16M7 12h10M10 17h4"/>',
 paperclip:'<path d="m21 11-9.5 9.5a6 6 0 0 1-8.5-8.5l10-10a4 4 0 0 1 5.7 5.7L8.5 18a2 2 0 0 1-2.8-2.8L15 6"/>',
 upload:'<path d="M12 16V3m-5 5 5-5 5 5M4 16v4h16v-4"/>',
 moon:'<path d="M20 15.5A8 8 0 0 1 8.5 4 8.5 8.5 0 1 0 20 15.5Z"/>',
 flag:'<path d="M4 22V4m0 1s4-3 8 0 8 0 8 0v12s-4 3-8 0-8 0-8 0"/>',
 mail:'<rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 7 9 6 9-6"/>',
 circleInfo:'<circle cx="12" cy="12" r="10"/><path d="M12 10v7M12 7h.01"/>',
 hamburger:'<path d="M4 7h16M4 12h16M4 17h16"/>',
 gift:'<rect x="3" y="8" width="18" height="13" rx="2"/><path d="M12 8v13M3 12h18M12 8c-8 0-8-7-4-7s4 7 4 7Zm0 0c8 0 8-7 4-7s-4 7-4 7Z"/>',
 alert:'<path d="M12 3 2 21h20Z"/><path d="M12 9v5m0 4h.01"/>',
 download:'<path d="M12 3v12m-5-5 5 5 5-5M4 17v4h16v-4"/>',
 sliders:'<path d="M4 7h16M4 17h16"/><circle cx="9" cy="7" r="3" fill="white"/><circle cx="15" cy="17" r="3" fill="white"/>'
};
function ic(name,cls=''){return `<svg class="ic ${cls}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.85" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${iconPaths[name]||iconPaths.list}</svg>`}
function avatar(person,cls=''){return `<img class="avatar ${cls}" src="assets/avatars/${person}.svg" alt="${person}">`}
// Mobile design prototype. Author: Aniruddha DuttaChowdhury · ADChowdhury
// Application identifier reserved for native conversion: app.adc.wunderlistmobileapp
const screens=[['welcome','Welcome'],['signin','Sign in'],['home','My lists'],['lists','Personal list'],['shared','Shared list'],['task','Task details'],['newtask','Create task'],['invite','Invite people'],['members','Members'],['activity','Activity'],['upcoming','Planned'],['settings','Settings'],['search','Search'],['empty','Inbox'],['sharedlists','Shared lists'],['newlist','Create list']];
const state={screen:'home',list:'personal',task:1,filter:'open',notify:true,notice:'',newColour:'saffron',newIcon:'list',newShared:false};
const lists=[{id:'inbox',name:'Inbox',icon:'mail',builtIn:true,shared:false},{id:'personal',name:'Personal',icon:'home',shared:false},{id:'work',name:'Work',icon:'folder',shared:false},{id:'groceries',name:'Family groceries',icon:'list',shared:true},{id:'travel',name:'Weekend trips',icon:'sun',shared:true},{id:'renovation',name:'Home renovation',icon:'home',shared:true}];
let nextId=100;
const tasks=[
 {id:1,list:'personal',title:'Plan summer vacation',due:'2026-10-15',reminder:'1 day before',tags:'Travel, Personal',note:'Look at flight options and hotels. Consider Kerala or Goa.',star:true,done:false,subtasks:['Compare flights','Choose a hotel'],files:[]},
 {id:2,list:'personal',title:'Book dentist appointment',due:'2026-10-12',star:false,done:false},
 {id:3,list:'personal',title:'Renew passport',due:'2026-10-16',star:false,done:false},
 {id:4,list:'personal',title:'Call Alex',star:false,done:false},
 {id:5,list:'personal',title:'Read a book',star:true,done:true},
 {id:6,list:'groceries',title:'Tomatoes',note:'Prefer Roma tomatoes',tags:'Groceries, Produce',due:'2026-10-09',star:true,done:false,who:'maya'},
 {id:7,list:'groceries',title:'Milk',star:false,done:false,who:'you'},
 {id:8,list:'groceries',title:'Eggs',star:false,done:false},
 {id:9,list:'groceries',title:'Bread',star:false,done:false},
 {id:10,list:'groceries',title:'Coffee',star:false,done:true},
 {id:11,list:'travel',title:'Research destinations',star:false,done:false},
 {id:12,list:'travel',title:'Check flight prices',star:true,done:false},
 {id:13,list:'work',title:'Review project brief',due:'2026-10-10',done:false}
];
function esc(s=''){return String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}
function list(){return lists.find(l=>l.id===state.list)||lists[1]}
function task(){return tasks.find(t=>t.id===state.task)}
function btn(icon,attrs,label){return `<button class="icon-btn" ${attrs} aria-label="${label}">${ic(icon)}</button>`}
function status(){return `<div class="status"><b>9:41</b><span>▮▮▮ ${ic('wifi','sm')} ▰</span></div>`}
function header(title,back='home',right=''){return `<header class="mobile-header">${btn('arrowLeft',`data-go="${back}"`,'Back')}<h1>${esc(title)}</h1>${right||'<span class="icon-spacer"></span>'}</header>`}
function shell(body,cls=''){return `<div class="device ${cls}" style="--list-accent:${({saffron:'#a94b00',green:'#138808',navy:'#000080'})[list().colour||'saffron']}">${status()}<div class="mobile-content">${body}</div><div class="home-indicator"></div></div>`}
function listRow(l){return `<button class="navigation-row" data-list="${l.id}"><span class="nav-symbol ${l.shared?'green':''}">${ic(l.icon)}</span><span>${esc(l.name)}</span>${l.shared?'<span class="mini-members">'+avatar('maya','sm')+avatar('arjun','sm')+'</span>':''}<small>${tasks.filter(t=>t.list===l.id&&!t.done).length}</small>${ic('chevronRight','sm')}</button>`}
function row(t){return `<div class="task-row ${t.done?'done':''}"><button class="check ${t.done?'checked':''}" data-check="${t.id}" aria-label="Toggle ${esc(t.title)}">${t.done?ic('check'):''}</button><button class="task-text" data-task="${t.id}"><strong>${esc(t.title)}</strong>${t.due?`<small>${ic('calendar','sm')} ${esc(t.due)}</small>`:''}</button>${t.who?avatar(t.who,'sm'):''}<button class="star ${t.star?'on':''}" data-star="${t.id}" aria-label="Star ${esc(t.title)}">${t.star?'★':'☆'}</button></div>`}
function navigation(){return shell(`<header class="brand-header"><img src="assets/brand-mark.svg" alt="Daylist"><h1>Daylist</h1>${btn('settings','data-go="settings"','Settings')}</header><button class="search-entry" data-go="search">${ic('search')} Search tasks and lists</button><div class="nav-section">MY LISTS</div>${listRow(lists[0])}<button class="navigation-row" data-go="upcoming"><span class="nav-symbol">${ic('sun')}</span><span>Today</span><small>${tasks.filter(t=>t.due==='2026-10-09'&&!t.done).length}</small>${ic('chevronRight','sm')}</button><button class="navigation-row" data-go="upcoming"><span class="nav-symbol">${ic('calendar')}</span><span>Planned</span>${ic('chevronRight','sm')}</button>${lists.filter(l=>!l.builtIn&&!l.shared).map(listRow).join('')}<button class="section-link" data-go="sharedlists">${ic('users')} Shared lists ${ic('chevronRight','sm')}</button>${lists.filter(l=>l.shared).map(listRow).join('')}<button class="add-list" data-go="newlist">${ic('plus')} New list</button><footer class="quiet-footer">Small lists. A little more life.</footer>`,'navigation-screen')}
function listScreen(){const l=list(),all=tasks.filter(t=>t.list===l.id),open=all.filter(t=>!t.done),done=all.filter(t=>t.done);return shell(`${header(l.name,'home',l.shared?btn('users','data-go="members"','List members'):btn('more','data-action="listInfo"','List information'))}<div class="list-caption">${l.shared?'Shared with 3 people':'Your private list'} · ${open.length} tasks</div><div class="list-stack"><button class="inline-add" data-action="beginTask">${ic('plus')} Add a task…</button>${open.map(row).join('')}${!all.length?`<div class="empty-message">${ic('list','xl')}<h2>A fresh start</h2><p>Add your first task above.</p></div>`:''}${done.length?`<button class="completed-heading" data-action="showDone">${ic('chevronDown','sm')} Completed · ${done.length}</button>${state.filter==='all'?done.map(row).join(''):''}`:''}</div>${l.shared?'<button class="list-bottom" data-go="invite">'+ic('userPlus')+' Invite people</button>':''}`,'list-screen')}
function field(label,icon,input){return `<div class="detail-field"><span class="field-icon">${ic(icon)}</span><div><label>${label}</label>${input}</div></div>`}
function taskFields(t={},prefix='edit'){return field('Due date','calendar',`<input aria-label="Due date" type="date" id="${prefix}-due" value="${esc(t.due||'')}">`)+field('Remind me','bell',`<select aria-label="Reminder" id="${prefix}-reminder">${['No reminder','At due time','1 day before','1 week before'].map(v=>`<option ${v===(t.reminder||'No reminder')?'selected':''}>${v}</option>`).join('')}</select>`)+field('Tags','tag',`<input id="${prefix}-tags" aria-label="Tags" placeholder="Travel, Personal…" value="${esc(t.tags||'')}">`)+field('Subtasks','list',`<textarea id="${prefix}-subtasks" aria-label="Subtasks" placeholder="One step per line">${esc((t.subtasks||[]).join('\n'))}</textarea>`)+field('Notes','message',`<textarea id="${prefix}-note" aria-label="Notes" placeholder="Add a note…">${esc(t.note||'')}</textarea>`)+ (list().shared?field('Assigned to','user',`<select id="${prefix}-who" aria-label="Assignee">${[['','Unassigned'],['you','You'],['maya','Maya Sharma'],['arjun','Arjun Mehta']].map(([v,n])=>`<option value="${v}" ${v===t.who?'selected':''}>${n}</option>`).join('')}</select>`):'')+field('File or photo','paperclip',`<label class="file-picker">Choose a file<input type="file" id="${prefix}-file" aria-label="File or photo"></label>${(t.files||[]).map(n=>`<small>${esc(n)}</small>`).join('')}`)}
function taskDetail(){const t=task();if(!t)return listScreen();return shell(`${header('Task details',list().shared?'shared':'lists',`<button class="star ${t.star?'on':''}" data-star="${t.id}" aria-label="Star task">${t.star?'★':'☆'}</button>`)}<div class="context-label">${esc(list().name)}</div><div class="task-title-edit"><button class="check ${t.done?'checked':''}" data-check="${t.id}" aria-label="Complete task">${t.done?ic('check'):''}</button><input id="edit-title" aria-label="Task name" value="${esc(t.title)}"></div><div class="detail-sheet">${taskFields(t)}<button class="delete-task" data-action="deleteTask">${ic('trash')} Delete task</button></div><div class="sticky-action"><button class="button-primary" data-action="saveTask">Save changes</button></div>`,'inner-screen')}
function createTask(){return shell(`${header('Create task',list().shared?'shared':'lists')}<div class="context-label">${esc(list().name)}</div><div class="detail-sheet create-sheet"><input id="task-title" class="large-input" aria-label="Task name" placeholder="What needs to get done?" autofocus><label class="important-option"><input id="task-star" type="checkbox"> <span>★</span> Mark as important</label>${taskFields({},'task')}</div><div class="sticky-action"><button class="button-primary" data-action="createTask">${ic('plus')} Add task</button></div>`,'inner-screen')}
function sharedLists(){return shell(`${header('Shared lists')}<p class="page-intro">Keep everyone on the same page.</p><div class="notice-card">${ic('users')}<p>Create a new shared list for family or friends. Your private lists stay private.</p></div>${lists.filter(l=>l.shared).map(listRow).join('')}<button class="add-list" data-go="newlist">${ic('plus')} Create a shared list</button>`,'inner-screen')}
function createList(){return shell(`${header('Create new list',state.newShared?'sharedlists':'home')}<div class="detail-sheet"><input id="list-name" class="large-input" aria-label="List name" placeholder="Name your list"><label class="section-label">COLOUR</label><div class="colour-options">${['saffron','green','navy'].map(c=>`<button class="colour ${c} ${state.newColour===c?'selected':''}" data-colour="${c}" aria-label="${c}"></button>`).join('')}</div><label class="section-label">ICON</label><div class="icon-options">${['list','sun','home','folder','gift','calendar','users','flag'].map(c=>`<button class="${state.newIcon===c?'selected':''}" data-icon="${c}" aria-label="${c}">${ic(c)}</button>`).join('')}</div><label class="share-switch">${ic('users')} Share this list <input id="list-shared" type="checkbox" ${state.newShared?'checked':''}></label><p class="helper">You can invite people after creating the list.</p></div><div class="sticky-action"><button class="button-primary" data-action="createList">Create list</button></div>`,'inner-screen')}
function invite(){return shell(`${header('Invite people',list().shared?'shared':'lists')}<div class="context-label">${esc(list().name)}</div><div class="detail-sheet"><div class="feature-symbol">${ic('userPlus','xl')}</div><h2>Better together</h2><p class="page-intro">Share a private invitation code with family or friends.</p><div class="invite-code"><small>DEMO INVITATION CODE</small><strong>8R2Q-6T</strong></div><button class="button-primary" data-action="copyCode">${ic('copy')} Copy code</button><div class="notice-card">${ic('shield')}<p>You approve each request before someone can access this list.</p></div><button class="plain-action" data-go="members">Manage members ${ic('chevronRight','sm')}</button></div>`,'inner-screen')}
function members(){return shell(`${header('Members','shared',btn('userPlus','data-go="invite"','Invite people'))}<div class="context-label">${esc(list().name)}</div><div class="detail-sheet">${[['you','You','Owner'],['maya','Maya Sharma','Editor'],['arjun','Arjun Mehta','Editor']].map(([a,n,r])=>`<div class="member-entry">${avatar(a)}<strong>${n}</strong><small>${r}</small></div>`).join('')}<label class="section-label">PENDING REQUEST</label><div class="member-entry">${avatar('nina')}<strong>Nina Patel</strong><small>Pending</small></div><button class="button-primary" data-action="approve">Approve as editor</button><button class="plain-action" data-action="decline">Decline request</button><p class="helper">Sample members. Changes here are preview only.</p></div>`,'inner-screen')}
function activity(){return shell(`${header('Activity')}<div class="detail-sheet"><label class="section-label">TODAY</label>${[['maya','Maya completed Coffee','11:24 AM'],['arjun','Arjun added Check flight prices','10:40 AM'],['you','You starred Plan summer vacation','9:15 AM']].map(([a,n,time])=>`<div class="member-entry">${avatar(a)}<div><strong>${n}</strong><small>${time}</small></div></div>`).join('')}</div>`,'inner-screen')}
function upcoming(){return shell(`${header('Planned')}<div class="context-label">October 2026 · sample dates</div><div class="detail-sheet">${tasks.filter(t=>t.due&&!t.done).sort((a,b)=>a.due.localeCompare(b.due)).map(t=>`<label class="section-label">${esc(t.due)} · ${esc(lists.find(l=>l.id===t.list).name)}</label>${row(t)}`).join('')}</div>`,'inner-screen')}
function settings(){return shell(`${header('Settings')}<div class="detail-sheet"><div class="member-entry">${avatar('you','lg')}<div><strong>Alex Morgan</strong><small>alex@example.com · sample account</small></div></div><label class="share-switch">${ic('bell')} Task reminders <input id="notify" type="checkbox" ${state.notify?'checked':''}></label><div class="plain-row">${ic('sun')} Appearance <strong>Indian tricolour</strong></div><button class="plain-action" data-go="activity">${ic('bell')} Activity ${ic('chevronRight','sm')}</button><div class="notice-card">${ic('circleInfo')}<p>Mobile UI preview · Round 02<br>Sample data only</p></div><p class="helper">Aniruddha DuttaChowdhury<br>ADChowdhury</p></div>`,'inner-screen')}
function search(){return shell(`${header('Search')}<div class="detail-sheet"><input id="query" class="large-input" type="search" aria-label="Search tasks" placeholder="Search tasks and lists"><div id="search-results">${tasks.filter(t=>!t.done).map(row).join('')}</div></div>`,'inner-screen')}
function welcome(){return shell(`<div class="onboarding"><img src="assets/brand-mark.svg" alt="Daylist"><h1>Less juggling.<br>More living.</h1><p>Your lists, tasks and people.<br>Beautifully together.</p><img class="hero-art" src="assets/hero-board.svg" alt="Task board"><h2>One thing at a time.</h2><button class="button-primary" data-go="signin">Get started ${ic('arrowRight')}</button><button class="plain-action" data-go="signin">Already here? Sign in</button></div>`,'inner-screen')}
function signin(){return shell(`${header('Welcome','welcome')}<div class="detail-sheet"><img class="signin-logo" src="assets/brand-mark.svg" alt="Daylist"><h2>Sign in to Daylist</h2><label class="section-label" for="email">EMAIL</label><input id="email" class="large-input" type="email" placeholder="you@example.com"><label class="section-label" for="password">PASSWORD</label><input id="password" class="large-input" type="password" placeholder="Password"><button class="button-primary" data-go="home">Continue ${ic('arrowRight')}</button><p class="helper">Preview only. No account is created.</p></div>`,'inner-screen')}
const renderers={welcome,signin,home:navigation,lists:listScreen,shared:listScreen,task:taskDetail,newtask:createTask,invite,members,activity,upcoming,settings,search,empty:listScreen,sharedlists:sharedLists,newlist:createList};
function go(id){state.notice='';state.screen=id in renderers?id:'home';if(id==='sharedlists')state.newShared=true;try{const p=new URLSearchParams(location.search);p.set('screen',state.screen);p.set('list',state.list);history.replaceState(null,'','?'+p)}catch{}render()}
function openList(id){state.list=id;state.filter='open';go(id==='inbox'?'empty':list().shared?'shared':'lists')}
function render(){const capture=new URLSearchParams(location.search).has('capture');document.body.classList.toggle('capture',capture);document.getElementById('app').innerHTML=`${capture?'':`<div class="preview-tools"><span>MOBILE REVIEW · ROUND 02</span><select id="screen-review" aria-label="Review screen">${screens.map(([id,n])=>`<option value="${id}" ${state.screen===id?'selected':''}>${n}</option>`).join('')}</select></div>`}${renderers[state.screen]()}${state.notice?'<div class="toast" role="status">'+esc(state.notice)+'</div>':''}`}
function notice(s){state.notice=s;render()}
function readFields(prefix,t){t.title=document.getElementById(prefix==='edit'?'edit-title':'task-title').value.trim();t.due=document.getElementById(prefix+'-due').value;t.reminder=document.getElementById(prefix+'-reminder').value;t.tags=document.getElementById(prefix+'-tags').value;t.note=document.getElementById(prefix+'-note').value;t.subtasks=document.getElementById(prefix+'-subtasks').value.split('\n').filter(Boolean);t.who=document.getElementById(prefix+'-who')?.value||'';t.files=[...(t.files||[]),...Array.from(document.getElementById(prefix+'-file').files).map(f=>f.name)]}
document.addEventListener('click',e=>{
 const el=e.target.closest('button');if(!el)return;
 if(el.dataset.list){openList(el.dataset.list);return}
 if(el.dataset.task){const t=tasks.find(t=>t.id===+el.dataset.task);state.task=t.id;state.list=t.list;go('task');return}
 if(el.dataset.check){const t=tasks.find(t=>t.id===+el.dataset.check);t.done=!t.done;render();return}
 if(el.dataset.star){const t=tasks.find(t=>t.id===+el.dataset.star);t.star=!t.star;el.classList.toggle('on',t.star);el.textContent=t.star?'★':'☆';return}
 if(el.dataset.colour){state.newColour=el.dataset.colour;document.querySelectorAll('[data-colour]').forEach(n=>n.classList.toggle('selected',n===el));return}
 if(el.dataset.icon){state.newIcon=el.dataset.icon;document.querySelectorAll('[data-icon]').forEach(n=>n.classList.toggle('selected',n===el));return}
 if(el.dataset.go){if(el.dataset.go==='newlist')state.newShared=state.screen==='sharedlists';go(el.dataset.go);return}
 const action=el.dataset.action;
 if(action==='beginTask'){go('newtask');return}
 if(action==='showDone'){state.filter=state.filter==='all'?'open':'all';render();return}
 if(action==='createTask'||action==='saveTask'){const name=document.getElementById(action==='createTask'?'task-title':'edit-title');if(!name.value.trim()){name.focus();return}const t=action==='createTask'?{id:nextId++,list:state.list,done:false,star:document.getElementById('task-star').checked}:task();readFields(action==='createTask'?'task':'edit',t);if(action==='createTask')tasks.push(t);openList(state.list);return}
 if(action==='deleteTask'){tasks.splice(tasks.indexOf(task()),1);openList(state.list);return}
 if(action==='createList'){const name=document.getElementById('list-name');if(!name.value.trim()){name.focus();return}const l={id:'list'+nextId++,name:name.value.trim(),icon:state.newIcon,colour:state.newColour,shared:document.getElementById('list-shared').checked};lists.push(l);openList(l.id);return}
 if(action==='copyCode'){if(navigator.clipboard?.writeText)navigator.clipboard.writeText('8R2Q-6T').then(()=>notice('Demo code copied')).catch(()=>notice('Demo code: 8R2Q-6T'));else notice('Demo code: 8R2Q-6T');return}
 if(action==='approve'||action==='decline'){notice('Demo request '+(action==='approve'?'approved':'declined'));return}
 if(action==='listInfo')notice('Private list · '+list().name);
});
document.addEventListener('change',e=>{if(e.target.id==='screen-review'){const id=e.target.value;if(['shared','invite','members'].includes(id))state.list='groceries';if(['lists','newtask','task'].includes(id))state.list='personal';if(id==='task')state.task=1;if(id==='empty')state.list='inbox';go(id)}if(e.target.id==='notify')state.notify=e.target.checked});
document.addEventListener('input',e=>{if(e.target.id==='query'){const q=e.target.value.toLowerCase();document.getElementById('search-results').innerHTML=tasks.filter(t=>t.title.toLowerCase().includes(q)).map(row).join('')||'<p class="helper">No tasks found.</p>'}});
const params=new URLSearchParams(location.search);if(params.has('list')&&lists.some(l=>l.id===params.get('list')))state.list=params.get('list');else if(['shared','invite','members'].includes(params.get('screen')))state.list='groceries';if(params.get('screen')==='empty')state.list='inbox';if(params.has('screen')&&params.get('screen') in renderers)state.screen=params.get('screen');render();
