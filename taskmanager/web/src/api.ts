const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';
const SESSION_KEY = 'fieldnote.session';

export type Session = {
  accessToken: string;
  refreshToken: string;
  userId: string;
  email: string;
  systemRole: 'ADMIN' | 'OWNER' | 'USER';
};

export type ProjectMember = {
  userId: string;
  email: string;
  fullName: string;
  systemRole: string;
  projectRole: 'OWNER' | 'MEMBER';
  joinedAt: string;
};

export type Project = {
  id: string;
  name: string;
  description: string | null;
  createdById: string;
  createdByName: string;
  createdAt: string;
  members: ProjectMember[];
};

export type ProjectDocument = {
  id: string;
  projectId: string;
  filename: string;
  mediaType: string;
  sizeBytes: number;
  uploadedById: string;
  uploadedByName: string;
  uploadedAt: string;
};

export type ManagedUser = {
  id: string;
  email: string;
  fullName: string;
  systemRole: 'ADMIN' | 'OWNER' | 'USER';
  active: boolean;
  createdAt: string;
};

type Envelope<T> = { success: boolean; data: T; message?: string };

export function readSession(): Session | null {
  const value = localStorage.getItem(SESSION_KEY);
  if (!value) return null;
  try {
    return JSON.parse(value) as Session;
  } catch {
    localStorage.removeItem(SESSION_KEY);
    return null;
  }
}

function saveSession(session: Session) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY);
}

async function refreshSession(): Promise<boolean> {
  const session = readSession();
  if (!session?.refreshToken) return false;
  try {
    const result = await request<Session>('/api/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken: session.refreshToken }),
    }, false);
    saveSession(result);
    return true;
  } catch {
    clearSession();
    return false;
  }
}

async function request<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const session = readSession();
  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json');
  if (session?.accessToken) headers.set('Authorization', `Bearer ${session.accessToken}`);
  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  let response: Response;
  try {
    response = await fetch(`${API_URL}${path}`, { ...init, headers });
  } catch {
    throw new Error('Cannot reach the API at localhost:8080. Check that the backend is running.');
  }

  if (response.status === 401 && retry && !path.startsWith('/api/auth/')) {
    if (await refreshSession()) return request<T>(path, init, false);
  }
  if (response.status === 204) return undefined as T;

  const body = await response.json().catch(() => null) as Envelope<T> | null;
  if (!response.ok) {
    const error = new Error(body?.message ?? `Request failed (${response.status})`);
    Object.assign(error, { status: response.status });
    throw error;
  }
  return body?.data as T;
}

async function authenticate(path: '/api/auth/login' | '/api/auth/register', body: unknown) {
  const result = await request<Session>(path, { method: 'POST', body: JSON.stringify(body) }, false);
  saveSession(result);
  return result;
}

export const api = {
  login: (email: string, password: string) => authenticate('/api/auth/login', { email, password }),
  register: (email: string, password: string, fullName: string) =>
    authenticate('/api/auth/register', { email, password, fullName }),
  async logout() {
    const session = readSession();
    if (session) {
      await request('/api/auth/logout', {
        method: 'POST',
        body: JSON.stringify({ refreshToken: session.refreshToken }),
      }, false).catch(() => undefined);
    }
    clearSession();
  },
  projects: () => request<Project[]>('/api/projects'),
  createProject: (name: string, description: string) =>
    request<Project>('/api/projects', { method: 'POST', body: JSON.stringify({ name, description }) }),
  updateProject: (projectId: string, name: string, description: string) =>
    request<Project>(`/api/projects/${projectId}`, {
      method: 'PUT', body: JSON.stringify({ name, description }),
    }),
  deleteProject: (projectId: string) =>
    request<void>(`/api/projects/${projectId}`, { method: 'DELETE' }),
  addMember: (projectId: string, email: string) =>
    request<ProjectMember>(`/api/projects/${projectId}/members`, {
      method: 'POST', body: JSON.stringify({ email }),
    }),
  updateProjectMemberRole: (projectId: string, userId: string, projectRole: ProjectMember['projectRole']) =>
    request<ProjectMember>(`/api/projects/${projectId}/members/${userId}`, {
      method: 'PATCH', body: JSON.stringify({ projectRole }),
    }),
  removeMember: (projectId: string, userId: string) =>
    request<void>(`/api/projects/${projectId}/members/${userId}`, { method: 'DELETE' }),
  documents: (projectId: string, query: string) => {
    const params = query ? `?q=${encodeURIComponent(query)}` : '';
    return request<ProjectDocument[]>(`/api/projects/${projectId}/documents${params}`);
  },
  upload: (projectId: string, file: File) => {
    const data = new FormData();
    data.append('file', file);
    return request<ProjectDocument>(`/api/projects/${projectId}/documents`, { method: 'POST', body: data });
  },
  deleteDocument: (projectId: string, documentId: string) =>
    request<void>(`/api/projects/${projectId}/documents/${documentId}`, { method: 'DELETE' }),
  async download(projectId: string, document: ProjectDocument) {
    const session = readSession();
    const response = await fetch(`${API_URL}/api/projects/${projectId}/documents/${document.id}/download`, {
      headers: session ? { Authorization: `Bearer ${session.accessToken}` } : {},
    });
    if (!response.ok) throw new Error(`Download failed (${response.status})`);
    const url = URL.createObjectURL(await response.blob());
    const anchor = window.document.createElement('a');
    anchor.href = url;
    anchor.download = document.filename;
    anchor.click();
    URL.revokeObjectURL(url);
  },
  users: () => request<ManagedUser[]>('/api/admin/users'),
  updateUser: (userId: string, systemRole: string) =>
    request<ManagedUser>(`/api/admin/users/${userId}`, {
      method: 'PATCH', body: JSON.stringify({ systemRole }),
    }),
  deactivateUser: (userId: string) =>
    request<void>(`/api/admin/users/${userId}`, { method: 'DELETE' }),
};
