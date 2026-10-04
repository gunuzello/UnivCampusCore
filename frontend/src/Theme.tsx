import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { Moon, Sun } from "./Icons";
import { Panel, Field } from "./ui";
type Preference = "light" | "dark" | "system";
const ThemeContext = createContext({
  preference: "system" as Preference,
  dark: false,
  setPreference: (_: Preference) => {},
});
function readPreference(): Preference {
  try {
    const saved = localStorage.getItem("ucc-theme");
    return saved === "dark" || saved === "light" ? saved : "system";
  } catch {
    return "system";
  }
}
export function ThemeProvider({ children }: { children: ReactNode }) {
  const [preference, setPreference] = useState<Preference>(readPreference);
  const [systemDark, setSystemDark] = useState(
    () => window.matchMedia("(prefers-color-scheme: dark)").matches,
  );
  const dark = preference === "dark" || (preference === "system" && systemDark);
  useEffect(() => {
    const media = window.matchMedia("(prefers-color-scheme: dark)");
    const update = () => setSystemDark(media.matches);
    media.addEventListener("change", update);
    return () => media.removeEventListener("change", update);
  }, []);
  useEffect(() => {
    document.documentElement.dataset.theme = dark ? "dark" : "light";
    document.documentElement.style.colorScheme = dark ? "dark" : "light";
    document
      .querySelector('meta[name="theme-color"]')
      ?.setAttribute("content", dark ? "#111827" : "#f3f5f8");
    try {
      localStorage.setItem("ucc-theme", preference);
    } catch {
      /* 브라우저 저장이 제한되어도 테마 전환은 동작한다. */
    }
  }, [preference, dark]);
  return (
    <ThemeContext.Provider value={{ preference, dark, setPreference }}>
      {children}
    </ThemeContext.Provider>
  );
}
export function ThemeToggle() {
  const { dark, setPreference } = useContext(ThemeContext);
  return (
    <button
      className="theme-toggle secondary"
      aria-label={dark ? "라이트모드로 전환" : "다크모드로 전환"}
      title={dark ? "라이트모드로 전환" : "다크모드로 전환"}
      onClick={() => setPreference(dark ? "light" : "dark")}
    >
      {dark ? <Sun size={19} /> : <Moon size={19} />}
    </button>
  );
}
export function ThemeSettings() {
  const { preference, setPreference } = useContext(ThemeContext);
  return (
    <Panel title="화면 설정">
      <p className="muted">편안한 화면으로 캠퍼스 소식을 살펴보세요.</p>
      <Field label="화면 테마">
        <select value={preference} onChange={(e) => setPreference(e.target.value as Preference)}>
          <option value="system">기기 설정에 맞추기</option>
          <option value="light">라이트모드</option>
          <option value="dark">다크모드</option>
        </select>
      </Field>
      <p className="meta">선택한 테마는 이 브라우저에 저장돼요.</p>
    </Panel>
  );
}
