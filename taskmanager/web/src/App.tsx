import { FormEvent, useEffect, useState } from 'react';
import {
  ArrowDownToLine,
  ArrowLeft,
  Check,
  ChevronDown,
  FileText,
  FolderKanban,
  LogOut,
  Plus,
  Search,
  Shield,
  Trash2,
  Upload,
  UserPlus,
  Users,
  X,
} from 'lucide-react';
import { api, ManagedUser, Project, ProjectDocument, readSession, Session } from './api';

type AuthMode = 'login' | 'register';
function App() {
  const [session, setSession] = useState<Session | null>(() => readSession());
  const [projects, setProjects] = useState<Project[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState('');
  const [page, setPage] = useState<'workspace' | 'users'>('workspace');
  const [documents, setDocuments] = useState<ProjectDocument[]>([]);
  const [users, setUsers] = useState<ManagedUser[]>([]);
  const [query, setQuery] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const [refreshDocuments, setRefreshDocuments] = useState(0);
  const [projectDialog, setProjectDialog] = useState<'create' | 'edit' | null>(null);
  const [projectName, setProjectName] = useState('');
  const [projectDescription, setProjectDescription] = useState('');
  const [memberEmail, setMemberEmail] = useState('');

  const selectedProject = projects.find((project) => project.id === selectedProjectId) ?? null;

  useEffect(() => {
    if (!session) return;
    let cancelled = false;
    api.projects().then((result) => {
      if (!cancelled) {
        setProjects(result);
        setSelectedProjectId((current) => current || result[0]?.id || '');
      }
    }).catch((error: Error) => {
      if (!cancelled) setNotice(error.message);
    });
    return () => { cancelled = true; };
  }, [session?.accessToken]);

  useEffect(() => {
    if (!selectedProjectId || page !== 'workspace') {
      setDocuments([]);
      return;
    }
    let cancelled = false;
    const timer = window.setTimeout(() => {
      api.documents(selectedProjectId, query).then((result) => {
        if (!cancelled) setDocuments(result);
      }).catch((error: Error) => {
        if (!cancelled) setNotice(error.message);
      });
    }, query ? 220 : 0);
    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [selectedProjectId, query, refreshDocuments, page]);

  useEffect(() => {
    if (!session || page !== 'users' || session.systemRole !== 'ADMIN') return;
    api.users().then(setUsers).catch((error: Error) => setNotice(error.message));
  }, [session?.accessToken, page]);

  function notify(message: string) {
    setNotice(message);
    window.setTimeout(() => setNotice(''), 4000);
  }

  async function reloadProjects(selectId?: string) {
    const result = await api.projects();
    setProjects(result);
    if (selectId) setSelectedProjectId(selectId);
    else if (selectedProjectId && !result.some((project) => project.id === selectedProjectId)) {
      setSelectedProjectId(result[0]?.id ?? '');
    }
  }

  async function signOut() {
    await api.logout();
    setSession(null);
    setProjects([]);
    setSelectedProjectId('');
  }

  if (!session) {
    return <AuthScreen onAuthenticated={setSession} />;
  }

  async function submitProject(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    try {
      const project = projectDialog === 'edit' && selectedProject
        ? await api.updateProject(selectedProject.id, projectName.trim(), projectDescription.trim())
        : await api.createProject(projectName.trim(), projectDescription.trim());
      await reloadProjects(project.id);
      setProjectDialog(null);
      setProjectName('');
      setProjectDescription('');
      notify(projectDialog === 'edit' ? 'Project updated' : 'Project created');
    } catch (error) {
      notify((error as Error).message);
    } finally {
      setBusy(false);
    }
  }

  async function inviteMember(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedProject) return;
    setBusy(true);
    try {
      await api.addMember(selectedProject.id, memberEmail.trim());
      await reloadProjects(selectedProject.id);
      setMemberEmail('');
      notify('Member added to project');
    } catch (error) {
      notify((error as Error).message);
    } finally {
      setBusy(false);
    }
  }

  async function removeMember(userId: string) {
    if (!selectedProject || !window.confirm('Remove this member from the project?')) return;
    try {
      await api.removeMember(selectedProject.id, userId);
      await reloadProjects(selectedProject.id);
    } catch (error) {
      notify((error as Error).message);
    }
  }

  async function changeProjectMemberRole(userId: string, role: 'OWNER' | 'MEMBER') {
    if (!selectedProject) return;
    try {
      await api.updateProjectMemberRole(selectedProject.id, userId, role);
      await reloadProjects(selectedProject.id);
      notify('Project role updated');
    } catch (error) {
      notify((error as Error).message);
    }
  }

  async function uploadFiles(files: FileList | File[]) {
    if (!selectedProject || files.length === 0) return;
    setBusy(true);
    let uploaded = 0;
    try {
      for (const file of Array.from(files)) {
        await api.upload(selectedProject.id, file);
        uploaded += 1;
      }
      setRefreshDocuments((value) => value + 1);
      notify(`${uploaded} document${uploaded === 1 ? '' : 's'} uploaded`);
    } catch (error) {
      notify(`${uploaded} uploaded. ${(error as Error).message}`);
    } finally {
      setBusy(false);
    }
  }

  async function deleteDocument(document: ProjectDocument) {
    if (!selectedProject || !window.confirm(`Delete “${document.filename}”?`)) return;
    try {
      await api.deleteDocument(selectedProject.id, document.id);
      setRefreshDocuments((value) => value + 1);
    } catch (error) {
      notify((error as Error).message);
    }
  }

  async function deleteProject() {
    if (!selectedProject || !window.confirm(`Delete project “${selectedProject.name}” and its document records?`)) return;
    try {
      await api.deleteProject(selectedProject.id);
      setSelectedProjectId('');
      await reloadProjects();
      notify('Project deleted');
    } catch (error) {
      notify((error as Error).message);
    }
  }

  async function changeRole(userId: string, role: string) {
    try {
      await api.updateUser(userId, role);
      setUsers(await api.users());
    } catch (error) {
      notify((error as Error).message);
    }
  }

  async function deactivateUser(userId: string) {
    if (!window.confirm('Deactivate this account? The user will no longer be able to sign in.')) return;
    try {
      await api.deactivateUser(userId);
      setUsers(await api.users());
    } catch (error) {
      notify((error as Error).message);
    }
  }

  const canManageProject = session.systemRole === 'ADMIN'
    || selectedProject?.members.some((member) => member.userId === session.userId && member.projectRole === 'OWNER') === true;
  const canCreateProject = session.systemRole === 'ADMIN' || session.systemRole === 'OWNER';

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#workspace" onClick={() => { setPage('workspace'); setSelectedProjectId(''); }}>
          <span className="brand-mark">f</span>
          <span>fieldnote<span className="brand-period">.</span></span>
        </a>
        <div className="sidebar-label">Workspace</div>
        <button className={`nav-item ${page === 'workspace' && !selectedProject ? 'active' : ''}`} onClick={() => { setPage('workspace'); setSelectedProjectId(''); }}>
          <FolderKanban size={17} /> Overview
        </button>
        {session.systemRole === 'ADMIN' && (
          <button className={`nav-item ${page === 'users' ? 'active' : ''}`} onClick={() => setPage('users')}>
            <Shield size={17} /> User administration
          </button>
        )}
        <div className="project-label-row">
          <div className="sidebar-label">Projects</div>
          {canCreateProject && <button className="icon-button sidebar-add" title="Create project" aria-label="Create project" onClick={() => { setProjectName(''); setProjectDescription(''); setProjectDialog('create'); }}><Plus size={16} /></button>}
        </div>
        <div className="project-nav-list">
          {projects.map((project) => (
            <button key={project.id} className={`project-nav ${selectedProjectId === project.id && page === 'workspace' ? 'selected' : ''}`} onClick={() => { setPage('workspace'); setSelectedProjectId(project.id); }}>
              <span className="project-dot" />
              <span className="project-nav-name">{project.name}</span>
              <span className="project-count">{project.members.length}</span>
            </button>
          ))}
          {projects.length === 0 && <p className="sidebar-empty">No projects yet</p>}
        </div>
        <div className="sidebar-footer">
          <div className="avatar">{session.email.slice(0, 1).toUpperCase()}</div>
          <div className="account-copy"><strong>{session.email.split('@')[0]}</strong><span>{session.systemRole.toLowerCase()}</span></div>
          <button className="icon-button signout-button" title="Sign out" aria-label="Sign out" onClick={signOut}><LogOut size={17} /></button>
        </div>
      </aside>

      <main className="main-area">
        <header className="topbar">
          <div className="breadcrumb"><span>Workspace</span><span className="crumb-divider">/</span><strong>{page === 'users' ? 'User administration' : selectedProject?.name ?? 'Overview'}</strong></div>
          <div className="topbar-right"><span className="role-pill">{session.systemRole}</span><span className="topbar-email">{session.email}</span><button className="icon-button mobile-signout" title="Sign out" aria-label="Sign out" onClick={signOut}><LogOut size={17} /></button></div>
        </header>

        {notice && <div className="toast" role="status"><span>{notice}</span><button onClick={() => setNotice('')} aria-label="Dismiss"><X size={15} /></button></div>}

        {page === 'users' && session.systemRole === 'ADMIN' ? (
          <AdminUsers users={users} onRoleChange={changeRole} onDeactivate={deactivateUser} />
        ) : selectedProject ? (
          <section className="project-page">
            <div className="project-heading">
              <div className="project-heading-copy">
                <button className="back-link" onClick={() => setSelectedProjectId('')}><ArrowLeft size={15} /> All projects</button>
                <div className="eyebrow">Project workspace</div>
                <h1>{selectedProject.name}</h1>
                <p>{selectedProject.description || 'Shared project files, team members, and notes.'}</p>
              </div>
              <div className="project-heading-actions">
                <div className="project-stat"><Users size={16} /><strong>{selectedProject.members.length}</strong><span>members</span></div>
                {canManageProject && <><button className="button button-quiet" onClick={() => { setProjectName(selectedProject.name); setProjectDescription(selectedProject.description ?? ''); setProjectDialog('edit'); }}><ChevronDown size={15} /> Manage</button><button className="icon-button danger-hover" title="Delete project" aria-label="Delete project" onClick={() => void deleteProject()}><Trash2 size={16} /></button></>}
              </div>
            </div>
            <div className="workspace-grid">
              <section className="documents-section">
                <div className="section-title-row">
                  <div><div className="eyebrow">Shared library</div><h2>Documents <span className="count-chip">{documents.length}</span></h2></div>
                  <label className="search-field"><Search size={16} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search files or content" aria-label="Search documents" /><kbd>/</kbd></label>
                </div>
                <label className={`dropzone ${busy ? 'uploading' : ''}`} onDragOver={(event) => event.preventDefault()} onDrop={(event) => { event.preventDefault(); void uploadFiles(event.dataTransfer.files); }}>
                  <input type="file" multiple onChange={(event) => { if (event.target.files) void uploadFiles(event.target.files); event.currentTarget.value = ''; }} />
                  <span className="upload-icon"><Upload size={19} /></span>
                  <span className="dropzone-copy"><strong>{busy ? 'Working…' : 'Drop files here'}</strong><span>PDF, Office, CSV, text, images, and videos · limits set by workspace</span></span>
                  <span className="button button-outline">Browse files</span>
                </label>
                <div className="document-table-head"><span>Name</span><span>Added by</span><span>Date</span><span /></div>
                {documents.length === 0 ? (
                  <div className="empty-documents"><FileText size={22} /><strong>{query ? 'No matches found' : 'No documents in this project'}</strong><span>{query ? 'Try another search term.' : 'Upload a file to start the shared library.'}</span></div>
                ) : (
                  <div className="document-list">
                    {documents.map((document) => (
                      <article className="document-row" key={document.id}>
                        <span className="file-type"><FileText size={17} /></span>
                        <div className="document-name"><strong title={document.filename}>{document.filename}</strong><span>{formatBytes(document.sizeBytes)} · {shortType(document.mediaType)}</span></div>
                        <span className="document-uploader">{document.uploadedByName || 'Team member'}</span>
                        <time className="document-date">{formatDate(document.uploadedAt)}</time>
                        <div className="row-actions"><button className="icon-button" title="Download document" aria-label={`Download ${document.filename}`} onClick={() => api.download(selectedProject.id, document).catch((error: Error) => notify(error.message))}><ArrowDownToLine size={16} /></button>{(document.uploadedById === session.userId || canManageProject || session.systemRole === 'ADMIN') && <button className="icon-button danger-hover" title="Delete document" aria-label={`Delete ${document.filename}`} onClick={() => void deleteDocument(document)}><Trash2 size={15} /></button>}</div>
                      </article>
                    ))}
                  </div>
                )}
              </section>

              <aside className="right-rail">
                <section className="members-panel">
                  <div className="rail-heading"><div><div className="eyebrow">People</div><h2>Team <span className="count-chip">{selectedProject.members.length}</span></h2></div><Users size={18} /></div>
                  <div className="member-list">
                    {selectedProject.members.map((member) => (
                      <div className="member-row" key={member.userId}>
                        <span className="avatar avatar-small">{(member.fullName || member.email).slice(0, 1).toUpperCase()}</span>
                        <div className="member-copy"><strong>{member.fullName || member.email}</strong><span>{member.email}</span></div>
                        {session.systemRole === 'ADMIN' ? <select className="member-role-select" value={member.projectRole} aria-label={`Project role for ${member.email}`} onChange={(event) => void changeProjectMemberRole(member.userId, event.target.value as 'OWNER' | 'MEMBER')}><option value="OWNER">Owner</option><option value="MEMBER">Member</option></select> : <span className={`member-role ${member.projectRole === 'OWNER' ? 'owner-role' : ''}`}>{member.projectRole.toLowerCase()}</span>}
                        {canManageProject && member.userId !== session.userId && <button className="icon-button member-remove" title="Remove member" aria-label="Remove member" onClick={() => void removeMember(member.userId)}><X size={14} /></button>}
                      </div>
                    ))}
                  </div>
                  {canManageProject && <form className="invite-form" onSubmit={inviteMember}><label htmlFor="invite-email">Invite an existing account</label><div className="invite-input-row"><input id="invite-email" type="email" required placeholder="teammate@email.com" value={memberEmail} onChange={(event) => setMemberEmail(event.target.value)} /><button className="icon-button invite-submit" disabled={busy} title="Add member" aria-label="Add member"><UserPlus size={17} /></button></div></form>}
                </section>

              </aside>
            </div>
          </section>
        ) : (
          <Overview projects={projects} session={session} canCreateProject={canCreateProject} onCreate={() => { setProjectName(''); setProjectDescription(''); setProjectDialog('create'); }} onSelect={(projectId) => setSelectedProjectId(projectId)} />
        )}
      </main>

      {projectDialog && <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) setProjectDialog(null); }}><form className="dialog" onSubmit={submitProject}><div className="dialog-head"><div><div className="eyebrow">{projectDialog === 'create' ? 'New workspace' : 'Project settings'}</div><h2>{projectDialog === 'create' ? 'Create a project' : 'Edit project'}</h2></div><button type="button" className="icon-button" title="Close" aria-label="Close" onClick={() => setProjectDialog(null)}><X size={18} /></button></div><label>Project name<input required maxLength={255} value={projectName} onChange={(event) => setProjectName(event.target.value)} placeholder="e.g. Atlas redesign" /></label><label>Description<textarea rows={3} maxLength={5000} value={projectDescription} onChange={(event) => setProjectDescription(event.target.value)} placeholder="What is this project about?" /></label><div className="dialog-actions"><button type="button" className="button button-quiet" onClick={() => setProjectDialog(null)}>Cancel</button><button className="button button-primary" disabled={busy}>{busy ? 'Saving…' : projectDialog === 'create' ? 'Create project' : 'Save changes'} <Check size={15} /></button></div></form></div>}
    </div>
  );
}

function AuthScreen({ onAuthenticated }: { onAuthenticated: (session: Session) => void }) {
  const [mode, setMode] = useState<AuthMode>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    setBusy(true);
    try {
      const session = mode === 'login'
        ? await api.login(email, password)
        : await api.register(email, password, fullName);
      onAuthenticated(session);
    } catch (failure) {
      setError((failure as Error).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="auth-page">
      <div className="auth-art"><div className="auth-art-top"><span className="brand-mark">f</span><span>fieldnote<span className="brand-period">.</span></span><span className="auth-edition">TEAM WORKSPACE / 01</span></div><div className="auth-art-content"><div className="eyebrow light-eyebrow">A home for the work</div><h1>Keep every project<br />in the <em>conversation.</em></h1><p>Bring your team, files, and project knowledge into one shared place.</p><div className="art-index"><span>01</span><span className="art-rule" /><span>Projects · Documents · Context</span></div></div><div className="auth-art-stamp">FN<br /><span>EST. 2026</span></div><div className="auth-art-bottom"><span>PRIVATE BY DEFAULT</span><span>BUILD TOGETHER</span></div></div>
      <section className="auth-form-side"><div className="auth-form-wrap"><div className="auth-mobile-brand"><span className="brand-mark">f</span> fieldnote.</div><div className="eyebrow">{mode === 'login' ? 'Welcome back' : 'Start a workspace'}</div><h2>{mode === 'login' ? 'Sign in to your account' : 'Create your account'}</h2><p className="auth-subtitle">{mode === 'login' ? 'Your project workspace is waiting.' : 'Join your team’s shared project space.'}</p><div className="auth-tabs"><button className={mode === 'login' ? 'current' : ''} onClick={() => { setMode('login'); setError(''); }}>Sign in</button><button className={mode === 'register' ? 'current' : ''} onClick={() => { setMode('register'); setError(''); }}>Create account</button></div><form onSubmit={submit} className="auth-form">{mode === 'register' && <label>Full name<input required autoComplete="name" value={fullName} onChange={(event) => setFullName(event.target.value)} placeholder="Your name" /></label>}<label>Email address<input type="email" required autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@company.com" /></label><label>Password<input type="password" required minLength={8} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} value={password} onChange={(event) => setPassword(event.target.value)} placeholder="At least 8 characters" />{mode === 'register' && <span className="field-hint">Use at least 8 characters with a letter and a number.</span>}</label>{error && <div className="form-error" role="alert">{error}</div>}<button className="button button-primary auth-submit" disabled={busy}>{busy ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}<ArrowDownToLine size={16} className="submit-arrow" /></button></form><div className="auth-security"><Shield size={15} /><span>Encrypted credentials · JWT-secured workspace</span></div></div><footer className="auth-footer"><span>FIELDNOTE / PROJECT KNOWLEDGE</span><span>© 2026</span></footer></section>
    </main>
  );
}

function Overview({ projects, session, canCreateProject, onCreate, onSelect }: {
  projects: Project[]; session: Session; canCreateProject: boolean; onCreate: () => void; onSelect: (id: string) => void;
}) {
  const memberTotal = new Set(projects.flatMap((project) => project.members.map((member) => member.userId))).size;
  return (
    <section className="overview-page">
      <div className="overview-intro"><div><div className="eyebrow">Your workspace</div><h1>Good to see you, <em>{session.email.split('@')[0]}.</em></h1><p>Pick up where your team left off.</p></div>{canCreateProject && <button className="button button-primary" onClick={onCreate}><Plus size={17} /> New project</button>}</div>
      <div className="overview-metrics"><div><span>PROJECTS</span><strong>{projects.length.toString().padStart(2, '0')}</strong></div><div><span>YOUR ROLE</span><strong className="metric-role">{session.systemRole}</strong></div><div><span>TEAM MEMBERS</span><strong>{memberTotal.toString().padStart(2, '0')}</strong></div><div className="metrics-note"><span>ACCESS</span><strong><Shield size={18} /> Managed</strong></div></div>
      <div className="overview-section-title"><div><div className="eyebrow">Recently active</div><h2>Your projects</h2></div><span>{projects.length} total</span></div>
      {projects.length === 0 ? <div className="empty-projects"><div className="empty-icon"><FolderKanban size={22} /></div><h3>No project spaces yet</h3><p>{canCreateProject ? 'Create the first project and invite your team.' : 'Ask a project owner or admin to add you to a workspace.'}</p>{canCreateProject && <button className="button button-outline" onClick={onCreate}><Plus size={16} /> Create a project</button>}</div> : <div className="project-grid-cards">{projects.map((project, index) => <button className="project-card" key={project.id} onClick={() => onSelect(project.id)}><div className={`project-card-accent accent-${index % 4}`}><span>{project.name.slice(0, 1).toUpperCase()}</span><FolderKanban size={18} /></div><div className="project-card-content"><div className="project-card-meta"><span>PROJECT {String(index + 1).padStart(2, '0')}</span><span>{project.members.length} members</span></div><h3>{project.name}</h3><p>{project.description || 'A shared place for project files and team knowledge.'}</p><div className="project-card-footer"><span>Owner: {project.createdByName || 'Team'}</span><span className="project-open">Open workspace <ArrowLeft size={14} /></span></div></div></button>)}</div>}
      {session.systemRole === 'USER' && <div className="role-note"><Shield size={16} /><span>Need to create a project? Ask an administrator to change your account role to Owner.</span></div>}
    </section>
  );
}

function AdminUsers({ users, onRoleChange, onDeactivate }: {
  users: ManagedUser[];
  onRoleChange: (id: string, role: string) => void;
  onDeactivate: (id: string) => void;
}) {
  return (
    <section className="admin-page">
      <div className="admin-heading">
        <div>
          <div className="eyebrow">Access control</div>
          <h1>User administration</h1>
          <p>Manage roles and access for registered accounts.</p>
        </div>
      </div>
      <div className="admin-table">
        <div className="admin-table-head">
          <span>Account</span>
          <span>Role</span>
          <span>Status</span>
          <span>Joined</span>
          <span />
        </div>
        {users.map((user) => (
          <div
            className={`admin-user-row ${!user.active ? "inactive-row" : ""}`}
            key={user.id}
          >
            <div className="admin-user-account">
              <span className="avatar avatar-small">
                {user.email.slice(0, 1).toUpperCase()}
              </span>
              <span>
                <strong>{user.fullName || "Unnamed user"}</strong>
                <small>{user.email}</small>
              </span>
            </div>
            <select
              value={user.systemRole}
              disabled={!user.active}
              aria-label={`Role for ${user.email}`}
              onChange={(event) => onRoleChange(user.id, event.target.value)}
            >
              <option value="ADMIN">Admin</option>
              <option value="OWNER">Owner</option>
              <option value="USER">User</option>
            </select>
            <span
              className={`status-label ${user.active ? "status-active" : "status-inactive"}`}
            >
              <i />
              {user.active ? "Active" : "Deactivated"}
            </span>
            <time>{formatDate(user.createdAt)}</time>
            <button
              className="icon-button danger-hover"
              disabled={!user.active}
              title="Deactivate account"
              aria-label={`Deactivate ${user.email}`}
              onClick={() => onDeactivate(user.id)}
            >
              <Trash2 size={15} />
            </button>
          </div>
        ))}
        {users.length === 0 && <div className="empty-documents">No registered accounts.</div>}
      </div>
    </section>
  );
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(value));
}

function formatBytes(value: number) {
  if (value < 1024) return `${value} B`;
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(0)} KB`;
  return `${(value / (1024 * 1024)).toFixed(1)} MB`;
}

function shortType(value: string) {
  return value?.split('/').pop()?.replace('vnd.openxmlformats-officedocument.', '').replace('application.', '').toUpperCase() || 'FILE';
}

export default App;
