import { useState, type FormEvent } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { request } from "./api";
import { ThemeToggle } from "./Theme";
import { useDemoMode } from "./Environment";
import { Field, ErrorMessage } from "./ui";
export default function AuthPage() {
  const demo = useDemoMode();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [signup, setSignup] = useState(false);
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);
  const query = useQueryClient();
  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setBusy(true);
    setError(undefined);
    const d = Object.fromEntries(new FormData(e.currentTarget));
    try {
      if (signup) await request("/auth/signup", "POST", d);
      await request("/auth/login", "POST", { email: d.email, password: d.password });
      await query.invalidateQueries({ queryKey: ["me"] });
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  return (
    <main className="auth">
      <div className="auth-theme">
        <ThemeToggle />
      </div>
      <div className="auth-story">
        <img src="/ucc-logo.png" alt="UCC" />
        <p className="eyebrow">UNIVERSITY CAMPUS CORE</p>
        <h1>
          계속해서 이어지는
          <br />
          캠퍼스의 이야기.
        </h1>
        <p>
          모집부터 행사, 회의와 기록까지.
          <br />
          우리 캠퍼스의 기회와 이야기를 한곳에서 이어가요.
        </p>
        <div className="pills">
          <span>모집과 지원</span>
          <span>행사 운영</span>
          <span>활동 기록</span>
        </div>
      </div>
      <section className="card auth-card">
        <h2>{signup ? "UCC 시작하기" : "다시 만나서 반가워요"}</h2>
        <p className="muted">
          {signup ? "기본 정보를 입력해 주세요." : "이메일로 로그인해 주세요."}
        </p>
        <form onSubmit={submit}>
          <Field label="이메일">
            <input
              name="email"
              type="email"
              autoComplete="email"
              required
              maxLength={254}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="name@example.com"
            />
          </Field>
          <Field label="비밀번호">
            <input
              name="password"
              type={showPassword ? "text" : "password"}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              minLength={8}
              maxLength={72}
              required
              autoComplete={signup ? "new-password" : "current-password"}
            />
          </Field>
          <button
            type="button"
            className="text-button password-toggle"
            onClick={() => setShowPassword(!showPassword)}
            aria-pressed={showPassword}
          >
            {showPassword ? "비밀번호 숨기기" : "비밀번호 보기"}
          </button>
          {signup && (
            <>
              <Field label="이름">
                <input name="name" required maxLength={80} autoComplete="name" />
              </Field>
              <Field label="학과">
                <input name="department" maxLength={120} />
              </Field>
              <Field label="학번">
                <input name="studentNumber" maxLength={40} />
              </Field>
            </>
          )}
          <ErrorMessage error={error} />
          <button className="primary wide" disabled={busy}>
            {busy ? "처리 중…" : signup ? "가입하고 시작하기" : "로그인"}
          </button>
        </form>
        <button
          className="text-button wide"
          onClick={() => {
            setSignup(!signup);
            setPassword("");
            setError(undefined);
          }}
        >
          {signup ? "이미 계정이 있어요" : "처음이라면 회원가입"}
        </button>
        {demo && !signup && (
          <div className="demo-accounts">
            <strong>캠퍼스 미리보기</strong>
            <p>역할을 골라 가상의 캠퍼스를 둘러보세요.</p>
            <div className="toolbar">
              {[
                ["leader", "대표"],
                ["staff", "운영진"],
                ["student", "학생"],
              ].map(([account, label]) => (
                <button
                  type="button"
                  className="secondary"
                  key={account}
                  onClick={() => {
                    setEmail(account + "@ucc.local");
                    setPassword("ucc-local-2026!");
                    setError(undefined);
                  }}
                >
                  {label} 계정
                </button>
              ))}
            </div>
            <p className="hint">
              계정을 선택한 뒤 로그인해 주세요. 데이터와 인물은 모두 가상입니다.
            </p>
          </div>
        )}
      </section>
    </main>
  );
}
