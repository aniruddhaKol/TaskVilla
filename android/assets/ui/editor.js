/* Draft editing and local attachments. Aniruddha DuttaChowdhury · ADChowdhury. */
(() => {
  const MAX_FILES=10, MAX_BYTES=10000000, browserFiles=new Map();
  let session=null, bypass=false, choosing=false, pickerToken=0;
  const copy=value=>JSON.parse(JSON.stringify(value));
  const value=id=>document.getElementById(id)?.value||'';
  function key(){return ['task','newtask','listoptions'].includes(state.screen)?state.screen+':'+state.list+':'+(state.screen==='task'?state.task:''):null}
  function taskDraft(t={}){return {title:t.title||'',due:t.due||'',reminder:t.reminder||'No reminder',tags:t.tags||'',note:t.note||'',who:t.who||'',done:!!t.done,star:!!t.star,files:copy(t.files||[])}}
  function capture(){if(!session||session.key!==key())return;const d=session.draft;
    if(state.screen==='listoptions'){Object.assign(d,{name:value('list-name'),icon:state.newIcon,colour:state.newColour,invitees:[...(state.optionEmails||[])],email:value('invite-email')});return}
    const p=state.screen==='task'?'edit':'task';Object.assign(d,{title:value(p==='edit'?'edit-title':'task-title'),due:value(p+'-due'),reminder:value(p+'-reminder'),tags:value(p+'-tags'),note:value(p+'-note'),who:value(p+'-who')});
    if(p==='task')d.star=!!document.getElementById('task-star')?.checked;
  }
  function afterRender(){const k=key();if(!k)return;if(!session||session.key!==k){const d=state.screen==='listoptions'?{name:list().name,icon:state.newIcon,colour:state.newColour,invitees:[...(state.optionEmails||[])],email:''}:taskDraft(state.screen==='task'?task():{});session={key:k,draft:copy(d),original:copy(d),staged:[],origin:document.querySelector('.mobile-header [data-go]')?.dataset.go||'home'};}
    const d=session.draft;
    if(state.screen==='listoptions'){document.getElementById('list-name').value=d.name;const email=document.getElementById('invite-email');if(email)email.value=d.email;return}
    const p=state.screen==='task'?'edit':'task';for(const [id,v] of [[p==='edit'?'edit-title':'task-title',d.title],[p+'-due',d.due],[p+'-reminder',d.reminder],[p+'-tags',d.tags],[p+'-note',d.note],[p+'-who',d.who]]){const el=document.getElementById(id);if(el)el.value=v}
    if(p==='task')document.getElementById('task-star').checked=d.star;
    const done=document.querySelector('.task-title-edit .check');if(done){done.classList.toggle('checked',d.done);done.innerHTML=d.done?ic('check'):''}
    const star=document.querySelector('.header-actions .star');if(star)star.classList.toggle('on',d.star);
    const date=document.querySelector('.date-control input');if(date){const label=date.closest('.date-control').querySelector('.date-display');label.textContent=formatDueDate(date.value)||'dd-MMM-yyyy';label.classList.toggle('date-placeholder',!date.value)}
    drawFiles();
  }
  function dirty(){capture();return !!session&&JSON.stringify(session.draft)!==JSON.stringify(session.original)}
  function closeDialog(){const overlay=document.querySelector('.unsaved-overlay');if(!overlay)return;overlay.remove();session?.lastFocus?.focus({preventScroll:true})}
  function end(){if(!session)return;const ids=[...session.staged,...(session.original.files||[]).filter(f=>typeof f==='object').map(f=>f.id)];if(window.LocalDatabase?.releaseAttachments)LocalDatabase.releaseAttachments(JSON.stringify(ids));if(session.key.startsWith('listoptions:')){state.newIcon=session.original.icon;state.newColour=session.original.colour;state.optionEmails=[...session.original.invitees]}session=null;choosing=false;pickerToken++;}
  function leave(destination){closeDialog();end();bypass=true;try{go(destination)}finally{bypass=false}}
  function requestLeave(destination){if(bypass||!session||session.key!==key()||!['task','listoptions'].includes(state.screen)||destination===state.screen)return false;if(state.screen==='task'&&!task())return false;if(!dirty())return false;
    if(document.querySelector('.unsaved-overlay'))return true;
    window.LocalDatabase?.hideKeyboard();session.lastFocus=document.activeElement;
    const overlay=document.createElement('div');overlay.className='confirm-overlay unsaved-overlay';overlay.innerHTML=`<section class="confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="unsaved-title"><div class="palette-header"><h2 id="unsaved-title">Save changes?</h2><button class="icon-btn" data-unsaved="close" aria-label="Close without leaving">×</button></div><p>Your changes have not been saved.</p><div class="confirm-actions"><button class="button-primary secondary-action" data-unsaved="discard">Discard</button><button class="button-primary" data-unsaved="save">${ic('save')} Save</button></div></section>`;
    overlay.addEventListener('click',e=>{const action=e.target.closest('[data-unsaved]')?.dataset.unsaved;if(action==='close')closeDialog();if(action==='discard')leave(destination);if(action==='save'){closeDialog();save(destination)}});
    overlay.addEventListener('keydown',e=>{if(e.key==='Escape'){e.preventDefault();closeDialog()}if(e.key==='Tab'){const buttons=[...overlay.querySelectorAll('button')],i=buttons.indexOf(document.activeElement);e.preventDefault();buttons[(i+(e.shiftKey?-1:1)+buttons.length)%buttons.length].focus()}});
    document.querySelector('.device').append(overlay);overlay.querySelector('[data-unsaved="discard"]').focus();return true;
  }
  function save(destination){if(!session)return false;if(choosing){notice('Please wait for the files to finish attaching.');return false}capture();
    if(state.screen==='listoptions'){
      const name=document.getElementById('list-name');if(!requireValue(name))return false;
      if(value('invite-email').trim()&&!addInviteeDraft())return false;capture();const d=session.draft,l=list(),old=copy(l);
      Object.assign(l,{name:d.name.trim(),icon:d.icon,colour:d.colour,invitees:l.joined?l.invitees:[...d.invitees],shared:!!l.joined||d.invitees.length>0});
      if(!persistLocal()){Object.assign(l,old);notice('Could not save. Your changes are still here.');return false}
    }else{
      const p=state.screen==='task'?'edit':'task',name=document.getElementById(p==='edit'?'edit-title':'task-title');if(!requireValue(name))return false;
      for(const el of document.querySelectorAll('.detail-sheet input:not([type=file]),.detail-sheet textarea,.detail-sheet select'))if(!el.checkValidity()){markFieldInvalid(el);el.focus();return false}
      const d=copy(session.draft);d.title=d.title.trim();if(d.files.length>MAX_FILES){notice('Attach no more than 10 files per task.');return false}
      const creating=state.screen==='newtask',t=creating?{id:nextId++,list:state.list,subtasks:[]}:task(),old=copy(t);Object.assign(t,d);if(creating)tasks.push(t);
      if(!persistLocal()){if(creating)tasks.splice(tasks.indexOf(t),1);else Object.assign(t,old);notice('Could not save. Your changes are still here.');return false}
    }
    if(state.screen==='listoptions')session.original=copy(session.draft);
    leave(destination||session.origin);return true;
  }
  function attachmentUI(prefix){return `<div class="attachment-actions"><button class="button-primary" data-attach-files>${ic('plus')} Attach files</button><input hidden type="file" multiple id="${prefix}-file" aria-label="Choose attachments"></div><div id="attachment-items"></div><aside class="app-disclaimer" role="note"><img class="disclaimer-icon" src="assets/disclaimer-warning.png" alt=""><p>Max 10 files of 10 MB each.</p></aside>`}
  function drawFiles(){const box=document.getElementById('attachment-items');if(!box||!session)return;box.innerHTML=session.draft.files.map((f,i)=>{const old=typeof f==='string',name=old?f:f.name;return `<div class="attachment-item"><span><strong>${esc(name)}</strong><small>${old?'Original file unavailable':(f.size/1000000).toFixed(2)+' MB'}</small></span><button class="icon-btn" data-share-file="${i}" aria-label="Share ${esc(name)}" ${old||choosing?'disabled':''}>${ic('share')}</button><button class="icon-btn" data-remove-file="${i}" aria-label="Remove ${esc(name)}" ${choosing?'disabled':''}>${ic('trash')}</button></div>`}).join('');const heading=box.closest('.detail-field')?.querySelector('label');if(heading)heading.textContent='Files ('+session.draft.files.length+'/10)';const add=document.querySelector('[data-attach-files]');if(add){add.disabled=choosing;add.innerHTML=choosing?'Attaching…':ic('plus')+' Attach files'}}
  async function choose(){if(!session||choosing)return;capture();if(session.draft.files.length>=MAX_FILES){notice('Maximum 10 files per task. Remove a file before adding another.');return}if(window.LocalDatabase?.pickAttachments){choosing=true;drawFiles();LocalDatabase.pickAttachments(++pickerToken,session.draft.files.length,JSON.stringify(session.draft.files.filter(f=>typeof f==='object').map(f=>f.id)))}else document.querySelector('input[type=file][multiple]').click()}
  window.attachmentsSelected=(token,result)=>{if(token!==pickerToken||!session){if(result.files?.length)window.LocalDatabase?.releaseAttachments(JSON.stringify(result.files.map(f=>f.id)));return}choosing=false;if(result.files){session.draft.files.unshift(...result.files.map((f,i)=>({...f,addedAt:Date.now()+i})).reverse());session.staged.push(...result.files.map(f=>f.id))}drawFiles();if(result.error)notice(result.error)};
  async function share(indices){const files=indices.map(i=>session.draft.files[i]).filter(f=>typeof f==='object');if(!files.length)return;if(window.LocalDatabase?.shareAttachments){LocalDatabase.shareAttachments(JSON.stringify(files.map(f=>f.id)));return}const actual=files.map(f=>browserFiles.get(f.id));if(actual.every(Boolean)&&navigator.canShare?.({files:actual})){try{await navigator.share({files:actual})}catch(e){if(e.name!=='AbortError')notice('Could not share the selected files.')}}else notice('File sharing is available in the Android app or a browser with file sharing support.')}
  const crcTable=Uint32Array.from({length:256},(_,n)=>{for(let i=0;i<8;i++)n=n&1?(n>>>1)^0xedb88320:n>>>1;return n>>>0});
  async function crc32(file){let crc=0xffffffff;for(let offset=0;offset<file.size;offset+=65536){const bytes=new Uint8Array(await file.slice(offset,offset+65536).arrayBuffer());for(const byte of bytes)crc=(crc>>>8)^crcTable[(crc^byte)&255]}return ((crc^0xffffffff)>>>0).toString(16).padStart(8,'0')}
  document.addEventListener('change',async e=>{if(!e.target.matches('input[type=file][multiple]')||!session||choosing)return;const selected=[...e.target.files];e.target.value='';if(session.draft.files.length+selected.length>MAX_FILES){notice('Maximum 10 files per task. No files were added.');return}if(selected.some(f=>f.size>MAX_BYTES)){notice('Each file must be 10 MB or smaller. No files were added.');return}
    const active=session;choosing=true;drawFiles();try{
      const seen=new Set();for(const f of active.draft.files){if(typeof f==='string')continue;const checksum=f.crc32||(browserFiles.has(f.id)?await crc32(browserFiles.get(f.id)):null);if(checksum)seen.add(f.size+':'+checksum)}
      const additions=[];for(const f of selected){const checksum=await crc32(f),fingerprint=f.size+':'+checksum;if(seen.has(fingerprint))throw Error('Same file already attached');seen.add(fingerprint);additions.push({file:f,crc32:checksum})}
      if(session!==active)return;for(const item of additions){const f=item.file,id=crypto.randomUUID();browserFiles.set(id,f);active.draft.files.unshift({id,name:f.name,size:f.size,crc32:item.crc32,mime:f.type||'application/octet-stream',browserOnly:true,addedAt:Date.now()})}
    }catch(error){if(session===active)notice(error.message||'Could not read the selected files. No files were added.')}finally{if(session===active){choosing=false;drawFiles()}}
  });
  document.addEventListener('click',e=>{const b=e.target.closest('button');if(!b)return;
    if(b.hasAttribute('data-attach-files')){e.stopImmediatePropagation();choose();return}
    if(b.hasAttribute('data-share-file')){e.stopImmediatePropagation();share([+b.dataset.shareFile]);return}
    if(b.hasAttribute('data-remove-file')){e.stopImmediatePropagation();const active=session,index=+b.dataset.removeFile,file=session.draft.files[index];window.LocalDatabase?.hideKeyboard();confirmAction('Remove attachment?',`Remove “${typeof file==='string'?file:file.name}” from this task? Save applies the change.`,'Remove',()=>{if(session!==active)return;session.draft.files.splice(index,1);drawFiles()});return}
    if(!session||session.key!==key())return;
    if(['saveTask','createTask','saveListOptions'].includes(b.dataset.action)){e.stopImmediatePropagation();save();return}
    if(state.screen==='task'&&(b.dataset.star||b.dataset.check)){e.stopImmediatePropagation();capture();const k=b.dataset.star?'star':'done';session.draft[k]=!session.draft[k];if(k==='star')b.classList.toggle('on',session.draft[k]);else{b.classList.toggle('checked',session.draft[k]);b.innerHTML=session.draft[k]?ic('check'):''}}
  },true);
  window.TaskEditor={beforeRender:capture,afterRender,requestLeave,end,closeDialog,attachmentUI,files:()=>copy(session?.draft.files||[])};
  afterRender();
})();
