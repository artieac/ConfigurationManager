import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import type { SystemDto } from '../models/SystemDto';
import type { SystemHistoryDto } from '../models/SystemHistoryDto';
import { createSystem, deleteSystem, getSystemHistory, listSystems, updateSystem } from '../api/SystemRepository';
import DataTable from '../components/DataTable';
import Modal from '../components/Modal';
import ConfirmDialog from '../components/ConfirmDialog';
import RoleGuard from '../components/RoleGuard';
import LoadingSpinner from '../components/LoadingSpinner';

const EXTERNAL_ID_PATTERN = '^[A-Za-z0-9][A-Za-z0-9_-]*$';
const EXTERNAL_ID_TITLE = 'Letters, digits, hyphens, and underscores only — no spaces.';

/**
 * `entries` is newest-first (matches the API). For an UPDATED row this diffs
 * against the next-older entry; CREATED/DELETED (or a row with no older
 * entry to diff against) just show the snapshot at that point. Fields that
 * are null on either side are skipped rather than diffed — old rows written
 * before external_id/description were tracked in history have no snapshot
 * for them, so a "null → X" line would be misleading, not informative.
 */
function describeSystemHistoryChange(entry: SystemHistoryDto, previous: SystemHistoryDto | undefined): string {
  if (entry.action !== 'UPDATED' || !previous) {
    const parts = [`Name: ${entry.systemName}`];
    if (entry.externalId) parts.push(`External ID: ${entry.externalId}`);
    if (entry.description) parts.push(`Description: ${entry.description}`);
    return parts.join(', ');
  }

  const parts: string[] = [];
  if (entry.systemName !== previous.systemName) {
    parts.push(`Name: "${previous.systemName}" → "${entry.systemName}"`);
  }
  if (previous.externalId != null && entry.externalId != null && entry.externalId !== previous.externalId) {
    parts.push(`External ID: "${previous.externalId}" → "${entry.externalId}"`);
  }
  if (previous.description != null && entry.description != null && entry.description !== previous.description) {
    parts.push(`Description: "${previous.description || '—'}" → "${entry.description || '—'}"`);
  }
  return parts.length > 0 ? parts.join('; ') : 'No change recorded';
}

export default function SystemsPage() {
  const [systems, setSystems] = useState<SystemDto[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<SystemDto | null>(null);
  const [historyFor, setHistoryFor] = useState<SystemDto | null>(null);
  const [history, setHistory] = useState<SystemHistoryDto[]>([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  const [editingId, setEditingId] = useState<number | null>(null);
  const [editName, setEditName] = useState('');
  const [editExternalId, setEditExternalId] = useState('');
  const [editDescription, setEditDescription] = useState('');

  const refresh = () => {
    listSystems()
      .then(setSystems)
      .catch(() => setError('Failed to load systems.'));
  };

  useEffect(refresh, []);

  const historyChanges = useMemo(() => {
    const map = new Map<number, string>();
    history.forEach((entry, index) => {
      map.set(entry.id, describeSystemHistoryChange(entry, history[index + 1]));
    });
    return map;
  }, [history]);

  if (error) {
    return <p style={{ color: 'var(--danger)' }}>{error}</p>;
  }

  if (!systems) {
    return <LoadingSpinner />;
  }

  const handleCreate = async () => {
    await createSystem(name, description);
    setShowCreate(false);
    setName('');
    setDescription('');
    refresh();
  };

  const handleDelete = async () => {
    if (!pendingDelete) return;
    await deleteSystem(pendingDelete.id);
    setPendingDelete(null);
    refresh();
  };

  const openHistory = async (system: SystemDto) => {
    setHistoryFor(system);
    setHistory(await getSystemHistory(system.id));
  };

  const startEditing = (system: SystemDto) => {
    setEditingId(system.id);
    setEditName(system.name);
    setEditExternalId(system.externalId);
    setEditDescription(system.description ?? '');
  };

  const cancelEditing = () => {
    setEditingId(null);
  };

  const saveEditing = async () => {
    if (editingId === null) return;
    await updateSystem(editingId, editName, editExternalId, editDescription);
    setEditingId(null);
    refresh();
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
        <h1 style={{ margin: 0 }}>Systems</h1>
        <RoleGuard when={(u) => u.canWrite}>
          <button type="button" onClick={() => setShowCreate(true)}>
            + New System
          </button>
        </RoleGuard>
      </div>

      <DataTable
        rows={systems}
        rowKey={(s) => s.id}
        emptyMessage="No systems yet. Create one to start storing secrets."
        columns={[
          {
            header: 'Name',
            render: (s) =>
              editingId === s.id ? (
                <input value={editName} onChange={(e) => setEditName(e.target.value)} required style={{ width: '100%' }} />
              ) : (
                <Link to={`/systems/${s.id}`}>{s.name}</Link>
              ),
          },
          {
            header: 'External ID',
            render: (s) =>
              editingId === s.id ? (
                <input
                  value={editExternalId}
                  onChange={(e) => setEditExternalId(e.target.value)}
                  pattern={EXTERNAL_ID_PATTERN}
                  title={EXTERNAL_ID_TITLE}
                  style={{ width: '100%' }}
                />
              ) : (
                <code>{s.externalId}</code>
              ),
          },
          {
            header: 'Description',
            render: (s) =>
              editingId === s.id ? (
                <input value={editDescription} onChange={(e) => setEditDescription(e.target.value)} style={{ width: '100%' }} />
              ) : (
                s.description ?? '—'
              ),
          },
          { header: 'Created by', render: (s) => s.created.userDisplayName ?? '—' },
          {
            header: '',
            render: (s) =>
              editingId === s.id ? (
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button type="button" aria-label="Save" title="Save" onClick={() => void saveEditing()}>
                    ✓
                  </button>
                  <button type="button" aria-label="Cancel" title="Cancel" onClick={cancelEditing}>
                    ✕
                  </button>
                </div>
              ) : (
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button type="button" onClick={() => void openHistory(s)}>
                    History
                  </button>
                  <RoleGuard when={(u) => u.canWrite}>
                    <button type="button" aria-label="Edit" title="Edit" onClick={() => startEditing(s)}>
                      ✎
                    </button>
                  </RoleGuard>
                  <RoleGuard when={(u) => u.canDelete}>
                    <button type="button" onClick={() => setPendingDelete(s)}>
                      Delete
                    </button>
                  </RoleGuard>
                </div>
              ),
          },
        ]}
      />

      {showCreate && (
        <Modal title="New System" onClose={() => setShowCreate(false)}>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              void handleCreate();
            }}
            style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}
          >
            <label>
              Name
              <input value={name} onChange={(e) => setName(e.target.value)} required style={{ display: 'block', width: '100%' }} />
            </label>
            <label>
              Description
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                style={{ display: 'block', width: '100%' }}
              />
            </label>
            <button type="submit">Create</button>
          </form>
        </Modal>
      )}

      {pendingDelete && (
        <ConfirmDialog
          title="Delete system"
          message={`Delete "${pendingDelete.name}" and all its secrets? This cannot be undone.`}
          confirmLabel="Delete"
          onConfirm={() => void handleDelete()}
          onCancel={() => setPendingDelete(null)}
        />
      )}

      {historyFor && (
        <Modal title={`History for "${historyFor.name}"`} onClose={() => setHistoryFor(null)}>
          <DataTable
            rows={history}
            rowKey={(h) => h.id}
            emptyMessage="No history recorded."
            columns={[
              { header: 'Action', render: (h) => h.action },
              { header: 'Change', render: (h) => historyChanges.get(h.id) ?? '' },
              { header: 'By', render: (h) => h.changed.userDisplayName ?? '—' },
              { header: 'When', render: (h) => new Date(h.changed.at).toLocaleString() },
            ]}
          />
        </Modal>
      )}
    </div>
  );
}
