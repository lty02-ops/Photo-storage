import { Download, Link, RefreshCw, Trash2, UploadCloud, X } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import "./styles.css";

const API_BASE_URL = ""; // use relative paths and rely on Vite dev proxy
const SOCIAL_PROVIDERS = [
  { id: "naver", icon: "N", label: "\uB124\uC774\uBC84 \uB85C\uADF8\uC778" },
  { id: "kakao", icon: "\u25CF", label: "\uCE74\uCE74\uC624 \uB85C\uADF8\uC778" },
  { id: "google", icon: "G", label: "Google \uB85C\uADF8\uC778" },
];

export default function App() {
  const [authMode, setAuthMode] = useState("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [session, setSession] = useState(() => {
    const saved = localStorage.getItem("photo-storage-session");
    return saved ? JSON.parse(saved) : null;
  });
  const [photos, setPhotos] = useState([]);
  const [message, setMessage] = useState("");
  const [authError, setAuthError] = useState("");
  const [isBusy, setIsBusy] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [previewPhoto, setPreviewPhoto] = useState(null);
  const [systemInfo, setSystemInfo] = useState(null);

  const authHeaders = useMemo(() => {
    if (!session?.token) {
      return {};
    }
    return { Authorization: `Bearer ${session.token}` };
  }, [session]);

  useEffect(() => {
    fetch(`${API_BASE_URL}/api/system/info`)
      .then((response) => {
        if (!response.ok) {
          throw new Error("Failed to load system info");
        }
        return response.json();
      })
      .then(setSystemInfo)
      .catch(() => setSystemInfo(null));
  }, []);

  useEffect(() => {
    if (window.location.pathname !== "/oauth/callback") {
      return;
    }

    const params = new URLSearchParams(window.location.search);
    const token = params.get("token");
    const email = params.get("email");
    const role = params.get("role");
    const id = Number(params.get("id"));
    if (token && email && role && id) {
      const socialSession = { token, user: { id, email, role } };
      localStorage.setItem("photo-storage-session", JSON.stringify(socialSession));
      setSession(socialSession);
      setMessage(`Logged in as ${email}`);
      window.history.replaceState({}, "", "/");
    }
  }, []);

  useEffect(() => {
    if (session) {
      loadPhotos();
    }
  }, [session]);

  useEffect(() => {
    return () => {
      if (previewPhoto?.url) {
        URL.revokeObjectURL(previewPhoto.url);
      }
    };
  }, [previewPhoto]);

  async function request(path, options = {}) {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: {
        ...authHeaders,
        ...options.headers,
      },
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: "Request failed." }));
      throw new Error(error.message || "Request failed.");
    }

    if (response.status === 204) {
      return null;
    }

    return response.json();
  }

  function startSocialLogin(provider) {
    window.location.href = `${API_BASE_URL}/oauth2/authorization/${provider}`;
  }

  async function handleAuth(event) {
    event.preventDefault();
    setIsBusy(true);
    setMessage("");
    setAuthError("");
    try {
      const result = await request(`/api/auth/${authMode}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });

      if (authMode === "signup") {
        setAuthMode("login");
        setPassword("");
        setMessage("Signup complete. Please log in.");
        return;
      }

      setSession(result);
      localStorage.setItem("photo-storage-session", JSON.stringify(result));
      setMessage("");
    } catch (error) {
      setAuthError(error.message);
    } finally {
      setIsBusy(false);
    }
  }

  async function loadPhotos() {
    setIsRefreshing(true);
    try {
      const result = await request("/api/photos");
      setPhotos(result);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setIsRefreshing(false);
    }
  }

  async function handleUpload(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const file = form.elements.photo.files[0];
    if (!file) {
      setMessage("Choose a photo first.");
      return;
    }

    setMessage("");
    try {
      const formData = new FormData();
      formData.append("file", file);
      await request("/api/photos", {
        method: "POST",
        body: formData,
      });
      form.reset();
      setMessage("Photo uploaded.");
      await loadPhotos();
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function createShareLink(photoId) {
    try {
      const result = await request(`/api/photos/${photoId}/share`, { method: "POST" });
      const absoluteUrl = `${API_BASE_URL}${result.url}`;
      await navigator.clipboard.writeText(absoluteUrl);
      setMessage(`Share link copied: ${absoluteUrl}`);
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function downloadPhoto(photo) {
    try {
      const response = await fetch(`${API_BASE_URL}/api/photos/${photo.id}/download`, {
        headers: authHeaders,
      });
      if (!response.ok) {
        throw new Error("Download failed.");
      }
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = photo.originalFilename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function openPhotoPreview(photo) {
    try {
      const response = await fetch(`${API_BASE_URL}/api/photos/${photo.id}/download`, {
        headers: authHeaders,
      });
      if (!response.ok) {
        throw new Error("Preview failed.");
      }
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      setPreviewPhoto((current) => {
        if (current?.url) {
          URL.revokeObjectURL(current.url);
        }
        return { url, filename: photo.originalFilename };
      });
    } catch (error) {
      setMessage(error.message);
    }
  }

  function closePhotoPreview() {
    setPreviewPhoto((current) => {
      if (current?.url) {
        URL.revokeObjectURL(current.url);
      }
      return null;
    });
  }

  async function deletePhoto(photoId) {
    try {
      const response = await fetch(`${API_BASE_URL}/api/photos/${photoId}`, {
        method: "DELETE",
        headers: authHeaders,
      });
      if (!response.ok) {
        const error = await response.json().catch(() => ({ message: "Delete failed." }));
        throw new Error(error.message);
      }
      setMessage("Photo deleted.");
      await loadPhotos();
    } catch (error) {
      setMessage(error.message);
    }
  }

  function logout() {
    localStorage.removeItem("photo-storage-session");
    setSession(null);
    setPhotos([]);
  }

  return (
    <main className="app-shell">
      <section className="topbar">
        <div className="brand">
          <UploadCloud size={26} aria-hidden="true" />
          <div>
            <h1>Photo Storage</h1>
            {systemInfo && (
              <p className="cloud-info">
                Served by {systemInfo.cloudProvider}
                {" · "}
                {systemInfo.podName}
              </p>
            )}
          </div>
        </div>
        {session && (
          <div className="account">
            <span>{session.user.email}</span>
            <button type="button" onClick={logout}>Logout</button>
          </div>
        )}
      </section>

      {!session ? (
        <section className="auth-layout">
          <form className="auth-panel" onSubmit={handleAuth}>
            <div className="segmented">
              <button
                type="button"
                className={authMode === "login" ? "active" : ""}
                onClick={() => {
                  setAuthMode("login");
                  setAuthError("");
                }}
              >
                Login
              </button>
              <button
                type="button"
                className={authMode === "signup" ? "active" : ""}
                onClick={() => {
                  setAuthMode("signup");
                  setAuthError("");
                  setMessage("");
                }}
              >
                Signup
              </button>
            </div>
            <label>
              Email
              <input
                value={email}
                onChange={(event) => {
                  setEmail(event.target.value);
                  setAuthError("");
                }}
                type="email"
                required
              />
            </label>
            <label>
              Password
              <input
                value={password}
                onChange={(event) => {
                  setPassword(event.target.value);
                  setAuthError("");
                }}
                type="password"
                required
              />
            </label>
            {authError && <p className="auth-error">{authError}</p>}
            {message && <p className="auth-success">{message}</p>}
            <button className="primary auth-submit" type="submit" disabled={isBusy}>
              {isBusy ? "Please wait" : authMode === "login" ? "Login" : "Create Account"}
            </button>
            <div className="social-divider">or</div>
            <div className="social-actions">
              {SOCIAL_PROVIDERS.map((provider) => (
                <button
                  key={provider.id}
                  type="button"
                  className={`social-button ${provider.id}`}
                  onClick={() => startSocialLogin(provider.id)}
                >
                  <span className="social-icon" aria-hidden="true">{provider.icon}</span>
                  <span>{provider.label}</span>
                </button>
              ))}
            </div>
          </form>
        </section>
      ) : (
        <section className="workspace">
          <form className="upload-band" onSubmit={handleUpload}>
            <div>
              <h2>Upload Photo</h2>
              <p>Images up to 10 MB are stored through the backend storage interface.</p>
            </div>
            <input name="photo" type="file" accept="image/*" />
            <button className="primary" type="submit">
              <UploadCloud size={18} aria-hidden="true" />
              Upload
            </button>
          </form>

          {message && <p className="message">{message}</p>}

          <div className="dashboard-grid">
            <section className="photo-section">
              <div className="section-heading">
                <h2>My Photos</h2>
                <button type="button" className="icon-button" onClick={loadPhotos} title="Refresh" aria-label="Refresh photos">
                  <RefreshCw className={isRefreshing ? "spin" : ""} size={18} aria-hidden="true" />
                </button>
              </div>
              <div className="photo-list">
                {photos.length === 0 ? (
                  <p className="empty">No photos uploaded yet.</p>
                ) : (
                  photos.map((photo) => (
                    <article
                      className="photo-row clickable"
                      key={photo.id}
                      onClick={() => openPhotoPreview(photo)}
                      onKeyDown={(event) => {
                        if (event.key === "Enter" || event.key === " ") {
                          event.preventDefault();
                          openPhotoPreview(photo);
                        }
                      }}
                      role="button"
                      tabIndex={0}
                      title="Open photo preview"
                    >
                      <div>
                        <strong>{photo.originalFilename}</strong>
                        <span>
                          {formatBytes(photo.sizeBytes)} | {photo.storageProvider} | {photo.replicationStatus}
                        </span>
                      </div>
                      <div className="actions">
                        <button
                          type="button"
                          className="icon-button"
                          onClick={(event) => {
                            event.stopPropagation();
                            downloadPhoto(photo);
                          }}
                          title="Download"
                        >
                          <Download size={18} aria-hidden="true" />
                        </button>
                        <button
                          type="button"
                          className="icon-button"
                          onClick={(event) => {
                            event.stopPropagation();
                            createShareLink(photo.id);
                          }}
                          title="Create share link"
                        >
                          <Link size={18} aria-hidden="true" />
                        </button>
                        <button
                          type="button"
                          className="icon-button danger"
                          onClick={(event) => {
                            event.stopPropagation();
                            deletePhoto(photo.id);
                          }}
                          title="Delete"
                        >
                          <Trash2 size={18} aria-hidden="true" />
                        </button>
                      </div>
                    </article>
                  ))
                )}
              </div>
            </section>

          </div>

          {previewPhoto && (
            <div className="preview-backdrop" onClick={closePhotoPreview}>
              <div className="preview-dialog" onClick={(event) => event.stopPropagation()}>
                <div className="preview-heading">
                  <strong>{previewPhoto.filename}</strong>
                  <button type="button" className="icon-button" onClick={closePhotoPreview} title="Close" aria-label="Close preview">
                    <X size={18} aria-hidden="true" />
                  </button>
                </div>
                <img src={previewPhoto.url} alt={previewPhoto.filename} />
              </div>
            </div>
          )}
        </section>
      )}
    </main>
  );
}

function formatBytes(bytes) {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}
