export type Profile = {
  id: number;
  email: string;
  name: string;
  department: string;
  studentNumber: string;
};
export type Organization = {
  id: number;
  name: string;
  department: string;
  description: string;
  role: "MEMBER" | "STAFF" | "LEADER" | null;
};
export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
  ) {
    super(message);
  }
}
let csrf: string | undefined;
export async function request<T>(path: string, method = "GET", body?: unknown): Promise<T> {
  if (method !== "GET" && !csrf) {
    const token = await fetch("/api/v1/auth/csrf", { credentials: "include" });
    if (!token.ok)
      throw new ApiError(
        token.status,
        "SERVER_UNAVAILABLE",
        "서버에 연결할 수 없어요. 백엔드 실행 상태를 확인해 주세요.",
      );
    csrf = (await token.json()).token;
  }
  const response = await fetch("/api/v1" + path, {
    method,
    credentials: "include",
    headers: { "Content-Type": "application/json", ...(csrf ? { "X-CSRF-TOKEN": csrf } : {}) },
    ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
  });
  if (response.status === 204) {
    if (path === "/auth/logout") csrf = undefined;
    return null as T;
  }
  const text = await response.text();
  let data;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    throw new ApiError(
      response.status,
      "INVALID_RESPONSE",
      "서버 응답을 확인할 수 없어요. 잠시 후 다시 시도해 주세요.",
    );
  }
  if (!response.ok) {
    if (response.status === 403) csrf = undefined;
    throw new ApiError(
      response.status,
      data?.code || "SERVER_UNAVAILABLE",
      data?.message || "요청을 처리하지 못했습니다.",
    );
  }
  return data;
}

export async function download(path: string, filename: string): Promise<void> {
  const response = await fetch("/api/v1" + path, { credentials: "include" });
  if (!response.ok) {
    const data = await response.json().catch(() => null);
    throw new ApiError(
      response.status,
      data?.code || "DOWNLOAD_FAILED",
      data?.message || "명단을 내려받지 못했어요. 다시 로그인하거나 잠시 후 시도해 주세요.",
    );
  }
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 10000);
}
