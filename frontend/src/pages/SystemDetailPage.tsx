import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import type { SystemDto } from '../models/SystemDto';
import type { ConfigurationDto } from '../models/ConfigurationDto';
import type { ConfigurationValueHistoryDto } from '../models/ConfigurationValueHistoryDto';
import type { EnvironmentDto } from '../models/EnvironmentDto';
import type { ApiKeyDto, IssuedApiKeyDto } from '../models/ApiKeyDto';
import { getSystem } from '../api/SystemRepository';
import {
  createConfiguration,
  deleteConfiguration,
  deleteConfigurationValue,
  getConfigurationValueHistory,
  listConfigurations,
  renameConfiguration,
  revealHistoricConfigurationValue,
  revealConfigurationValue,
  setConfigurationValue,
} from '../api/ConfigurationRepository';
import { createEnvironment, deleteEnvironment, listEnvironments, updateEnvironment } from '../api/EnvironmentRepository';
import { createApiKey, listApiKeys, renameApiKey, revokeApiKey } from '../api/ApiKeyRepository';
import DataTable from '../components/DataTable';
import Modal from '../components/Modal';
import ConfirmDialog from '../components/ConfirmDialog';
import RoleGuard from '../components/RoleGuard';
import LoadingSpinner from '../components/LoadingSpinner';

const EXTERNAL_ID_PATTERN = '^[A-Za-z0-9][A-Za-z0-9_-]*$';
const EXTERNAL_ID_TITLE = 'Letters, digits, hyphens, and underscores only — no spaces.';

export default function SystemDetailPage() {
  const { systemId } = useParams<{ systemId: string }>();
  const id = Number(systemId);

  const [system, setSystem] = useState<SystemDto | null>(null);
  const [environments, setEnvironments] = useState<EnvironmentDto[] | null>(null);
  const [secrets, setConfigurations] = useState<ConfigurationDto[] | null>(null);
  const [activeEnvironmentId, setActiveEnvironmentId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [revealed, setRevealed] = useState<Record<number, string>>({});
  const [showCreateConfiguration, setShowCreateConfiguration] = useState(false);
  const [renamingConfiguration, setRenamingConfiguration] = useState<ConfigurationDto | null>(null);
  const [editingValueFor, setEditingValueFor] = useState<ConfigurationDto | null>(null);
  const [pendingDeleteConfiguration, setPendingDeleteConfiguration] = useState<ConfigurationDto | null>(null);
  const [pendingClearValueFor, setPendingClearValueFor] = useState<ConfigurationDto | null>(null);
  const [historyFor, setHistoryFor] = useState<ConfigurationDto | null>(null);
  const [history, setHistory] = useState<ConfigurationValueHistoryDto[]>([]);
  const [revealedHistory, setRevealedHistory] = useState<Record<number, string>>({});
  const [showManageEnvironments, setShowManageEnvironments] = useState(false);

  const refresh = () => {
    getSystem(id)
      .then(setSystem)
      .catch(() => setError('Failed to load system.'));
    listEnvironments(id)
      .then((envs) => {
        setEnvironments(envs);
        setActiveEnvironmentId((current) => current ?? envs[0]?.id ?? null);
      })
      .catch(() => setError('Failed to load environments.'));
    listConfigurations(id)
      .then(setConfigurations)
      .catch(() => setError('Failed to load secrets.'));
  };

  useEffect(refresh, [id]);

  // Revealed values are only valid for the environment they were fetched in — switching
  // environments must hide them rather than keep showing the previous environment's value.
  useEffect(() => setRevealed({}), [activeEnvironmentId]);

  if (error) {
    return <p style={{ color: 'var(--danger)' }}>{error}</p>;
  }

  if (!system || !environments || !secrets) {
    return <LoadingSpinner />;
  }

  const activeEnvironment = environments.find((e) => e.id === activeEnvironmentId) ?? null;

  const handleReveal = async (secret: ConfigurationDto) => {
    if (!activeEnvironmentId) return;
    const { value } = await revealConfigurationValue(secret.id, activeEnvironmentId);
    setRevealed((prev) => ({ ...prev, [secret.id]: value }));
  };

  const handleHide = (configurationId: number) => {
    setRevealed((prev) => {
      const next = { ...prev };
      delete next[configurationId];
      return next;
    });
  };

  const openHistory = async (secret: ConfigurationDto) => {
    setHistoryFor(secret);
    setRevealedHistory({});
    setHistory(await getConfigurationValueHistory(secret.id, activeEnvironmentId ?? undefined));
  };

  const handleRevealHistoric = async (historyId: number) => {
    if (!historyFor) return;
    const { value } = await revealHistoricConfigurationValue(historyFor.id, historyId);
    setRevealedHistory((prev) => ({ ...prev, [historyId]: value }));
  };

  const handleHideHistoric = (historyId: number) => {
    setRevealedHistory((prev) => {
      const next = { ...prev };
      delete next[historyId];
      return next;
    });
  };

  return (
    <div>
      <p>
        <Link to="/">← All systems</Link>
      </p>
      <h1 style={{ margin: '0 0 0.25rem' }}>{system.name}</h1>
      {system.description && <p style={{ color: 'var(--text-muted)' }}>{system.description}</p>}

      <EnvironmentTabs
        environments={environments}
        activeId={activeEnvironmentId}
        onSelect={setActiveEnvironmentId}
        onManage={() => setShowManageEnvironments(true)}
      />

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', margin: '1.25rem 0 0.5rem' }}>
        <h2 style={{ margin: 0, fontSize: 18 }}>Configurations</h2>
        <RoleGuard when={(u) => u.canWrite}>
          <button type="button" onClick={() => setShowCreateConfiguration(true)}>
            + New Configuration
          </button>
        </RoleGuard>
      </div>

      {!activeEnvironment && (
        <p style={{ color: 'var(--text-muted)' }}>
          No environment selected — create one to start setting secret values.
        </p>
      )}

      <DataTable
        rows={secrets}
        rowKey={(s) => s.id}
        emptyMessage="No secrets in this system yet."
        columns={[
          { header: 'Name', render: (s) => s.name },
          {
            header: activeEnvironment ? `Value (${activeEnvironment.name})` : 'Value',
            render: (s) => {
              if (!activeEnvironmentId) {
                return <span style={{ color: 'var(--text-muted)' }}>—</span>;
              }
              const hasValue = s.valuesSetInEnvironmentIds.includes(activeEnvironmentId);
              if (revealed[s.id] !== undefined) {
                return (
                  <span>
                    <code>{revealed[s.id]}</code>{' '}
                    <button type="button" onClick={() => handleHide(s.id)}>
                      Hide
                    </button>
                  </span>
                );
              }
              if (!hasValue) {
                return (
                  <span>
                    <span style={{ color: 'var(--text-muted)' }}>Not set</span>{' '}
                    <RoleGuard when={(u) => u.canWrite}>
                      <button type="button" onClick={() => setEditingValueFor(s)}>
                        Set value
                      </button>
                    </RoleGuard>
                  </span>
                );
              }
              return (
                <span>
                  ••••••••{' '}
                  <RoleGuard when={(u) => u.canRevealConfigurationValue}>
                    <button type="button" onClick={() => void handleReveal(s)}>
                      Reveal
                    </button>
                  </RoleGuard>
                </span>
              );
            },
          },
          { header: 'Updated by', render: (s) => s.updated.userDisplayName ?? '—' },
          {
            header: '',
            render: (s) => {
              const hasValue = activeEnvironmentId !== null && s.valuesSetInEnvironmentIds.includes(activeEnvironmentId);
              return (
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button type="button" aria-label="History" title="History" onClick={() => void openHistory(s)}>
                    🕒
                  </button>
                  <RoleGuard when={(u) => u.canWrite}>
                    {activeEnvironmentId ? (
                      <button type="button" aria-label={hasValue ? 'Update value' : 'Set value'} title={hasValue ? 'Update value' : 'Set value'} onClick={() => setEditingValueFor(s)}>
                        📝
                      </button>
                    ) : null}
                  </RoleGuard>
                  <RoleGuard when={(u) => u.canWrite}>
                    <button type="button" aria-label="Rename" title="Rename" onClick={() => setRenamingConfiguration(s)}>
                      ✎
                    </button>
                  </RoleGuard>
                  <RoleGuard when={(u) => u.canDelete}>
                    {hasValue && (
                      <button type="button" aria-label="Clear value" title="Clear value" onClick={() => setPendingClearValueFor(s)}>
                        ⌫
                      </button>
                    )}
                  </RoleGuard>
                  <RoleGuard when={(u) => u.canDelete}>
                    <button type="button" aria-label="Delete" title="Delete" onClick={() => setPendingDeleteConfiguration(s)}>
                      🗑
                    </button>
                  </RoleGuard>
                </div>
              );
            },
          },
        ]}
      />

      {showCreateConfiguration && (
        <Modal title="New Configuration" onClose={() => setShowCreateConfiguration(false)}>
          <NameForm
            label="Name"
            onSubmit={async (name) => {
              await createConfiguration(id, name);
              setShowCreateConfiguration(false);
              refresh();
            }}
          />
        </Modal>
      )}

      {renamingConfiguration && (
        <Modal title={`Rename "${renamingConfiguration.name}"`} onClose={() => setRenamingConfiguration(null)}>
          <NameForm
            label="Name"
            initialValue={renamingConfiguration.name}
            onSubmit={async (name) => {
              await renameConfiguration(renamingConfiguration.id, name);
              setRenamingConfiguration(null);
              refresh();
            }}
          />
        </Modal>
      )}

      {editingValueFor && activeEnvironmentId && (
        <Modal title={`Set value for "${editingValueFor.name}" in ${activeEnvironment?.name ?? ''}`} onClose={() => setEditingValueFor(null)}>
          <ValueForm
            onSubmit={async (value) => {
              await setConfigurationValue(editingValueFor.id, activeEnvironmentId, value);
              setEditingValueFor(null);
              refresh();
            }}
          />
        </Modal>
      )}

      {pendingDeleteConfiguration && (
        <Modal title="Delete configuration" onClose={() => setPendingDeleteConfiguration(null)}>
          <p>Delete "{pendingDeleteConfiguration.name}" and its value in every environment? This cannot be undone.</p>
          <p style={{ marginTop: '0.5rem' }}>
            Please type <strong>{pendingDeleteConfiguration.name}</strong> to confirm.
          </p>
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              const form = e.target as HTMLFormElement;
              const input = form.elements.namedItem('confirmName') as HTMLInputElement;
              if (input.value === pendingDeleteConfiguration.name) {
                await deleteConfiguration(pendingDeleteConfiguration.id);
                setPendingDeleteConfiguration(null);
                refresh();
              }
            }}
          >
            <input
              name="confirmName"
              type="text"
              autoComplete="off"
              required
              pattern={pendingDeleteConfiguration.name}
              style={{ display: 'block', width: '100%', marginBottom: '1rem' }}
            />
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
              <button type="button" onClick={() => setPendingDeleteConfiguration(null)}>
                Cancel
              </button>
              <button
                type="submit"
                style={{ background: 'var(--danger)', color: '#fff', border: 'none', borderRadius: 4, padding: '0.4rem 0.8rem' }}
              >
                Delete
              </button>
            </div>
          </form>
        </Modal>
      )}

      {pendingClearValueFor && activeEnvironmentId && (
        <ConfirmDialog
          title="Clear value"
          message={`Remove the value of "${pendingClearValueFor.name}" in ${activeEnvironment?.name ?? 'this environment'}? This cannot be undone.`}
          confirmLabel="Clear"
          onConfirm={async () => {
            await deleteConfigurationValue(pendingClearValueFor.id, activeEnvironmentId);
            setPendingClearValueFor(null);
            refresh();
          }}
          onCancel={() => setPendingClearValueFor(null)}
        />
      )}

      {historyFor && (
        <Modal
          title={`History for "${historyFor.name}"${activeEnvironment ? ` in ${activeEnvironment.name}` : ''}`}
          onClose={() => setHistoryFor(null)}
        >
          <DataTable
            rows={history}
            rowKey={(h) => h.id}
            emptyMessage="No history recorded."
            columns={[
              { header: 'Action', render: (h) => h.action },
              {
                header: 'Value',
                render: (h) =>
                  revealedHistory[h.id] !== undefined ? (
                    <span>
                      <code>{revealedHistory[h.id]}</code>{' '}
                      <button type="button" onClick={() => handleHideHistoric(h.id)}>
                        Hide
                      </button>
                    </span>
                  ) : (
                    <span>
                      ••••••••{' '}
                      <RoleGuard when={(u) => u.canRevealConfigurationValue}>
                        <button type="button" onClick={() => void handleRevealHistoric(h.id)}>
                          Reveal
                        </button>
                      </RoleGuard>
                    </span>
                  ),
              },
              { header: 'By', render: (h) => h.changed.userDisplayName ?? '—' },
              { header: 'When', render: (h) => new Date(h.changed.at).toLocaleString() },
            ]}
          />
        </Modal>
      )}

      {showManageEnvironments && (
        <ManageEnvironmentsModal
          systemId={id}
          environments={environments}
          onClose={() => setShowManageEnvironments(false)}
          onChanged={refresh}
        />
      )}

      <h2 style={{ margin: '1.5rem 0 0.5rem', fontSize: 18 }}>API Keys</h2>
      <p style={{ color: 'var(--text-muted)', margin: '0 0 0.5rem' }}>
        Machine credentials that let CI/CD jobs pull this system's secrets without a user session.
      </p>

      <ApiKeysSection systemId={id} />
    </div>
  );
}

interface EnvironmentTabsProps {
  environments: EnvironmentDto[];
  activeId: number | null;
  onSelect: (id: number) => void;
  onManage: () => void;
}

function EnvironmentTabs({ environments, activeId, onSelect, onManage }: EnvironmentTabsProps) {
  return (
    <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', borderBottom: '1px solid var(--border)', paddingBottom: '0.5rem' }}>
      {environments.map((env) => (
        <button
          key={env.id}
          type="button"
          onClick={() => onSelect(env.id)}
          style={{
            padding: '0.35rem 0.9rem',
            borderRadius: 999,
            border: '1px solid var(--border)',
            background: env.id === activeId ? 'var(--accent)' : 'transparent',
            color: env.id === activeId ? 'var(--accent-contrast)' : 'var(--text)',
          }}
        >
          {env.name}
        </button>
      ))}
      <RoleGuard when={(u) => u.canWrite}>
        <button type="button" onClick={onManage} style={{ marginLeft: '0.5rem' }}>
          Manage Environments
        </button>
      </RoleGuard>
    </div>
  );
}

interface ManageEnvironmentsModalProps {
  systemId: number;
  environments: EnvironmentDto[];
  onClose: () => void;
  onChanged: () => void;
}

function ManageEnvironmentsModal({ systemId, environments, onClose, onChanged }: ManageEnvironmentsModalProps) {
  const [pendingDelete, setPendingDelete] = useState<EnvironmentDto | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editName, setEditName] = useState('');
  const [editExternalId, setEditExternalId] = useState('');

  const startEditing = (environment: EnvironmentDto) => {
    setEditingId(environment.id);
    setEditName(environment.name);
    setEditExternalId(environment.externalId);
  };

  const saveEditing = async () => {
    if (editingId === null) return;
    await updateEnvironment(editingId, editName, editExternalId);
    setEditingId(null);
    onChanged();
  };

  return (
    <Modal title="Manage Environments" onClose={onClose}>
      <DataTable
        rows={environments}
        rowKey={(e) => e.id}
        emptyMessage="No environments yet."
        columns={[
          {
            header: 'Name',
            render: (e) =>
              editingId === e.id ? (
                <input value={editName} onChange={(ev) => setEditName(ev.target.value)} required style={{ width: '100%' }} />
              ) : (
                e.name
              ),
          },
          {
            header: 'External ID',
            render: (e) =>
              editingId === e.id ? (
                <input
                  value={editExternalId}
                  onChange={(ev) => setEditExternalId(ev.target.value)}
                  pattern={EXTERNAL_ID_PATTERN}
                  title={EXTERNAL_ID_TITLE}
                  style={{ width: '100%' }}
                />
              ) : (
                <code>{e.externalId}</code>
              ),
          },
          {
            header: '',
            render: (e) =>
              editingId === e.id ? (
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button type="button" aria-label="Save" title="Save" onClick={() => void saveEditing()}>
                    ✓
                  </button>
                  <button type="button" aria-label="Cancel" title="Cancel" onClick={() => setEditingId(null)}>
                    ✕
                  </button>
                </div>
              ) : (
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <RoleGuard when={(u) => u.canWrite}>
                    <button type="button" aria-label="Edit" title="Edit" onClick={() => startEditing(e)}>
                      ✎
                    </button>
                  </RoleGuard>
                  <RoleGuard when={(u) => u.canDelete}>
                    <button type="button" onClick={() => setPendingDelete(e)}>
                      Delete
                    </button>
                  </RoleGuard>
                </div>
              ),
          },
        ]}
      />

      <div style={{ marginTop: '1rem', borderTop: '1px solid var(--border)', paddingTop: '1rem' }}>
        <NameForm
          label="New environment name"
          submitLabel="Add"
          onSubmit={async (name) => {
            await createEnvironment(systemId, name);
            onChanged();
          }}
        />
      </div>

      {pendingDelete && (
        <ConfirmDialog
          title="Delete environment"
          message={`Delete "${pendingDelete.name}" and every secret value set in it? This cannot be undone.`}
          confirmLabel="Delete"
          onConfirm={async () => {
            await deleteEnvironment(pendingDelete.id);
            setPendingDelete(null);
            onChanged();
          }}
          onCancel={() => setPendingDelete(null)}
        />
      )}
    </Modal>
  );
}

interface ApiKeysSectionProps {
  systemId: number;
}

function ApiKeysSection({ systemId }: ApiKeysSectionProps) {
  const [apiKeys, setApiKeys] = useState<ApiKeyDto[] | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editName, setEditName] = useState('');
  const [pendingRevoke, setPendingRevoke] = useState<ApiKeyDto | null>(null);
  const [issued, setIssued] = useState<IssuedApiKeyDto | null>(null);
  const [copied, setCopied] = useState(false);

  const handleCopy = async (text: string) => {
    try {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(text);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      } else {
        const textArea = document.createElement('textarea');
        textArea.value = text;
        textArea.style.position = 'fixed';
        textArea.style.left = '-999999px';
        textArea.style.top = '-999999px';
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        const successful = document.execCommand('copy');
        textArea.remove();
        if (successful) {
          setCopied(true);
          setTimeout(() => setCopied(false), 2000);
        } else {
          console.error('Fallback: Copy command was unsuccessful');
        }
      }
    } catch (err) {
      console.error('Failed to copy', err);
    }
  };

  const refresh = () => {
    listApiKeys(systemId).then(setApiKeys);
  };

  useEffect(refresh, [systemId]);

  const startEditing = (apiKey: ApiKeyDto) => {
    setEditingId(apiKey.id);
    setEditName(apiKey.name);
  };

  const saveEditing = async () => {
    if (editingId === null) return;
    await renameApiKey(systemId, editingId, editName);
    setEditingId(null);
    refresh();
  };

  return (
    <div>
      <DataTable
        rows={apiKeys ?? []}
        rowKey={(k) => k.id}
        emptyMessage="No API keys yet."
        columns={[
          {
            header: 'Name',
            render: (k) =>
              editingId === k.id ? (
                <input value={editName} onChange={(e) => setEditName(e.target.value)} required style={{ width: '100%' }} />
              ) : (
                k.name
              ),
          },
          { header: 'Created by', render: (k) => k.created.userDisplayName ?? '—' },
          { header: 'Last used', render: (k) => (k.lastUsedAt ? new Date(k.lastUsedAt).toLocaleString() : 'Never') },
          {
            header: '',
            render: (k) =>
              editingId === k.id ? (
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button type="button" aria-label="Save" title="Save" onClick={() => void saveEditing()}>
                    ✓
                  </button>
                  <button type="button" aria-label="Cancel" title="Cancel" onClick={() => setEditingId(null)}>
                    ✕
                  </button>
                </div>
              ) : (
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <RoleGuard when={(u) => u.canWrite}>
                    <button type="button" aria-label="Rename" title="Rename" onClick={() => startEditing(k)}>
                      ✎
                    </button>
                  </RoleGuard>
                  <RoleGuard when={(u) => u.canDelete}>
                    <button type="button" aria-label="Revoke" title="Revoke" onClick={() => setPendingRevoke(k)}>
                      🗑
                    </button>
                  </RoleGuard>
                </div>
              ),
          },
        ]}
      />

      <RoleGuard when={(u) => u.canWrite}>
        <div style={{ marginTop: '1rem', borderTop: '1px solid var(--border)', paddingTop: '1rem' }}>
          <NameForm
            label="New API key name"
            submitLabel="Create"
            onSubmit={async (name) => {
              const created = await createApiKey(systemId, name);
              refresh();
              setIssued(created);
            }}
          />
        </div>
      </RoleGuard>

      {issued && (
        <Modal title="API key created" onClose={() => setIssued(null)}>
          <p>
            Copy this token now — for security, it's shown only this once and can't be retrieved again.
          </p>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', background: 'var(--border)', padding: '0.75rem', borderRadius: 6 }}>
            <code style={{ flex: 1, wordBreak: 'break-all' }}>
              {issued.token}
            </code>
            <button
              type="button"
              onClick={() => void handleCopy(issued.token)}
              title={copied ? "Copied!" : "Copy to clipboard"}
              style={{
                background: 'transparent',
                border: 'none',
                cursor: 'pointer',
                padding: '0.25rem',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: copied ? '#10b981' : 'inherit'
              }}
            >
              {copied ? (
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M20 6L9 17l-5-5"></path>
                </svg>
              ) : (
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                  <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                </svg>
              )}
            </button>
          </div>
          <div style={{ marginTop: '1rem', textAlign: 'right' }}>
            <button type="button" onClick={() => setIssued(null)}>
              Done
            </button>
          </div>
        </Modal>
      )}

      {pendingRevoke && (
        <ConfirmDialog
          title="Revoke API key"
          message={`Revoke "${pendingRevoke.name}"? Any client using it will immediately lose access. This cannot be undone.`}
          confirmLabel="Revoke"
          onConfirm={async () => {
            await revokeApiKey(systemId, pendingRevoke.id);
            setPendingRevoke(null);
            refresh();
          }}
          onCancel={() => setPendingRevoke(null)}
        />
      )}
    </div>
  );
}

interface NameFormProps {
  label: string;
  submitLabel?: string;
  initialValue?: string;
  onSubmit: (name: string) => Promise<void>;
}

function NameForm({ label, submitLabel = 'Save', initialValue = '', onSubmit }: NameFormProps) {
  const [name, setName] = useState(initialValue);

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        void onSubmit(name).then(() => setName(''));
      }}
      style={{ display: 'flex', gap: '0.5rem', alignItems: 'flex-end' }}
    >
      <label style={{ flex: 1 }}>
        {label}
        <input value={name} onChange={(e) => setName(e.target.value)} required style={{ display: 'block', width: '100%' }} />
      </label>
      <button type="submit">{submitLabel}</button>
    </form>
  );
}

function ValueForm({ onSubmit }: { onSubmit: (value: string) => Promise<void> }) {
  const [value, setValue] = useState('');

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        void onSubmit(value);
      }}
      style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}
    >
      <label>
        Value
        <input
          type="password"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          required
          style={{ display: 'block', width: '100%' }}
        />
      </label>
      <button type="submit">Save</button>
    </form>
  );
}

