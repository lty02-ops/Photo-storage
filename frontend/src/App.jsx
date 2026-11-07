import { Download, Image as ImageIcon, Link, RefreshCw, Trash2, UploadCloud, X } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import "./styles.css";

const API_BASE_URL = ""; // use relative paths and rely on Vite dev proxy
const SOCIAL_PROVIDERS = [
  { id: "naver", icon: "N", label: "\uB124\uC774\uBC84 \uB85C\uADF8\uC778" },
  { id: "kakao", icon: "\u25CF", label: "\uCE74\uCE74\uC624 \uB85C\uADF8\uC778" },
  { id: "google", icon: "G", label: "구글 로그인" },
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
          throw new Error("서버 정보를 불러오지 못했습니다.");
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
      setMessage(`${email} 계정으로 로그인했습니다.`);
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
      const error = await response.json().catch(() => ({ message: "요청을 처리하지 못했습니다." }));
      throw new Error(error.message || "요청을 처리하지 못했습니다.");
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
        setMessage("회원가입이 완료되었습니다. 로그인해주세요.");
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
      setMessage("먼저 업로드할 사진을 선택해주세요.");
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
      setMessage("사진을 업로드했습니다.");
      await loadPhotos();
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function createShareLink(photoId) {
  try {
    const result = await request(`/api/photos/${photoId}/share`, {
      method: "POST",
    });

    const absoluteUrl = new URL(result.url, window.location.origin).href;

    try {
      await navigator.clipboard.writeText(absoluteUrl);
      setMessage(`공유 링크를 복사했습니다: ${absoluteUrl}`);
    } catch {
      setMessage(`공유 링크를 만들었습니다: ${absoluteUrl}`);
    }
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
        throw new Error("사진을 내려받지 못했습니다.");
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
        throw new Error("사진 미리보기를 불러오지 못했습니다.");
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
        const error = await response.json().catch(() => ({ message: "사진을 삭제하지 못했습니다." }));
        throw new Error(error.message);
      }
      setMessage("사진을 삭제했습니다.");
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
            <h1>사진 보관함</h1>
            {systemInfo && (
              <p className="cloud-info">
                {cloudLabel(systemInfo.cloudProvider)}에서 연결 중
              </p>
            )}
          </div>
        </div>
        {session && (
          <div className="account">
            <span>{session.user.email}</span>
            <button type="button" onClick={logout}>로그아웃</button>
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
                로그인
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
                회원가입
              </button>
            </div>
            <label>
              이메일
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
              비밀번호
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
              {isBusy ? "처리 중…" : authMode === "login" ? "로그인" : "회원가입"}
            </button>
            <div className="social-divider">또는 소셜 계정으로 로그인</div>
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
              <h2>사진 올리기</h2>
              <p>최대 10메가바이트의 이미지를 올릴 수 있어요.</p>
            </div>
            <input name="photo" type="file" accept="image/*" aria-label="업로드할 사진 선택" />
            <button className="primary" type="submit">
              <UploadCloud size={18} aria-hidden="true" />
              Upload
            </button>
          </form>

          {message && <p className="message" role="status">{message}</p>}

          <div className="dashboard-grid">
            <section className="photo-section">
              <div className="section-heading">
                <h2>내 사진 <small className="photo-count">{photos.length}장</small></h2>
                <button type="button" className="icon-button" onClick={loadPhotos} title="새로고침" aria-label="사진 목록 새로고침">
                  <RefreshCw className={isRefreshing ? "spin" : ""} size={18} aria-hidden="true" />
                </button>
              </div>
              <div className="photo-list">
                {photos.length === 0 ? (
                  <p className="empty">아직 보관한 사진이 없어요. 첫 사진을 올려보세요.</p>
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
                      title="사진 미리보기"
                    >
                      <div className="photo-summary">
                        <PhotoThumbnail photoId={photo.id} token={session.token} />
                        <div className="photo-details">
                        <strong>{photo.originalFilename}</strong>
                        <div className="photo-meta">
                          <span>{formatBytes(photo.sizeBytes)}</span>
                          <span className="storage-badge">{storageLabel(photo.storageProvider)}에 원본 저장</span>
                          <span className={`replication-badge status-${photo.replicationStatus?.toLowerCase()}`}>
                            {replicationLabel(photo)}
                          </span>
                        </div>
                        </div>
                      </div>
                      <div className="actions">
                        <button
                          type="button"
                          className="icon-button"
                          onClick={(event) => {
                            event.stopPropagation();
                            downloadPhoto(photo);
                          }}
                          title="내려받기" aria-label="사진 내려받기"
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
                          title="공유 링크 만들기" aria-label="사진 공유 링크 만들기"
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
                          title="삭제" aria-label="사진 삭제"
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
                  <button type="button" className="icon-button" onClick={closePhotoPreview} title="닫기" aria-label="미리보기 닫기">
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

function PhotoThumbnail({ photoId, token }) {
  const containerRef = useRef(null);
  const [imageUrl, setImageUrl] = useState(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    let objectUrl;
    let observer;
    setImageUrl(null);
    setFailed(false);

    async function load() {
      observer?.disconnect();
      try {
        const response = await fetch(`${API_BASE_URL}/api/photos/${photoId}/download`, {
          headers: { Authorization: `Bearer ${token}` },
          signal: controller.signal,
        });
        if (!response.ok) throw new Error("사진을 불러오지 못했습니다.");
        const blob = await response.blob();
        if (controller.signal.aborted) return;
        objectUrl = URL.createObjectURL(blob);
        setImageUrl(objectUrl);
      } catch (error) {
        if (error.name !== "AbortError") setFailed(true);
      }
    }

    if ("IntersectionObserver" in window) {
      observer = new IntersectionObserver((entries) => {
        if (entries.some((entry) => entry.isIntersecting)) load();
      }, { rootMargin: "160px" });
      observer.observe(containerRef.current);
    } else {
      load();
    }

    return () => {
      observer?.disconnect();
      controller.abort();
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [photoId, token]);

  return (
    <div ref={containerRef} className="photo-thumbnail"
      title={failed ? "미리보기를 불러오지 못했습니다. 눌러서 다시 확인하세요." : "사진 미리보기"}>
      {imageUrl && !failed ? (
        <img src={imageUrl} alt="" decoding="async" onError={() => setFailed(true)} />
      ) : (
        <ImageIcon size={24} aria-hidden="true" />
      )}
    </div>
  );
}

function formatBytes(bytes) {
  if (bytes < 1024) {
    return `${bytes}바이트`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)}킬로바이트`;
  }
  return `${(bytes / 1024 / 1024).toFixed(1)}메가바이트`;
}

function cloudLabel(provider) {
  return { gcp: "구글 클라우드", aws: "아마존 클라우드", local: "로컬 서버" }[provider] || "서버";
}

function storageLabel(provider) {
  return { gcs: "구글 저장소", s3: "아마존 저장소", local: "로컬 저장소" }[provider] || "저장소";
}

function replicationLabel(photo) {
  const target = { gcs: "아마존 저장소", s3: "구글 저장소" }[photo.storageProvider];
  switch (photo.replicationStatus) {
    case "COMPLETED": return target ? `${target} 복제 완료` : "복제 완료";
    case "PENDING": return "복제 대기 중";
    case "FAILED": return "복제 실패";
    case "NOT_REQUIRED": return "복제 대상 없음";
    default: return "복제 상태 확인 중";
  }
}
