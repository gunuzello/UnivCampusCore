import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { request, type Organization } from "./api";
import { Panel, Field, Empty, ErrorMessage, date, instant, Action } from "./ui";
type Entry = {
  key: string;
  id: number;
  type: string;
  title: string;
  startsAt: string;
  endsAt: string;
  description: string;
  path: string | null;
  canManage: boolean;
};
const local = (v: string) => {
  const d = new Date(v);
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
};
export default function CalendarPage({ org }: { org?: Organization }) {
  const query = useQueryClient();
  const [cursor, setCursor] = useState(new Date());
  const [selectedDay, setSelectedDay] = useState(new Date());
  const [mode, setMode] = useState<"month" | "week" | "day">("month");
  const [form, setForm] = useState(false);
  const [editing, setEditing] = useState<Entry>();
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);
  let from: Date, to: Date;
  if (mode === "month") {
    from = new Date(cursor.getFullYear(), cursor.getMonth(), 1);
    to = new Date(cursor.getFullYear(), cursor.getMonth() + 1, 1);
  } else if (mode === "day") {
    from = new Date(cursor.getFullYear(), cursor.getMonth(), cursor.getDate());
    to = new Date(from);
    to.setDate(to.getDate() + 1);
  } else {
    from = new Date(cursor.getFullYear(), cursor.getMonth(), cursor.getDate());
    from.setDate(from.getDate() - ((from.getDay() + 6) % 7));
    to = new Date(from);
    to.setDate(to.getDate() + 7);
  }
  const path =
    "/me/calendar?from=" +
    encodeURIComponent(from.toISOString()) +
    "&to=" +
    encodeURIComponent(to.toISOString()) +
    (org ? "&org=" + org!.id : "");
  const q = useQuery({
    queryKey: ["calendar", org?.id, from.toISOString(), mode],
    queryFn: () => request<Entry[]>(path),
  });

  const days: Date[] = [];
  for (let d = new Date(from); d < to; d.setDate(d.getDate() + 1)) days.push(new Date(d));
  const blanks = mode === "month" ? (from.getDay() + 6) % 7 : 0;
  const dayEntries = (d: Date) => {
    const start = new Date(d.getFullYear(), d.getMonth(), d.getDate());
    const end = new Date(start);
    end.setDate(end.getDate() + 1);
    return (
      q.data?.filter(
        (e) => Date.parse(e.startsAt) < end.getTime() && Date.parse(e.endsAt) > start.getTime(),
      ) || []
    );
  };
  const selectedEntries = dayEntries(selectedDay);
  return (
    <>
      <div className="page-heading">
        <h1>캘린더</h1>
        <p>소속 활동, 프로그램, 팀 모임과 업무 마감을 한눈에 봐요.</p>
      </div>
      <div className="toolbar calendar-toolbar">
        <button
          className="secondary"
          onClick={() => {
            const d = new Date(cursor);
            if (mode === "month") {
              d.setDate(1);
              d.setMonth(d.getMonth() - 1);
            } else d.setDate(d.getDate() - (mode === "day" ? 1 : 7));
            setCursor(d);
            setSelectedDay(d);
          }}
        >
          ← 이전
        </button>
        <strong>
          {from.getFullYear()}년 {from.getMonth() + 1}월
          {mode === "week"
            ? " " + from.getDate() + "일 주"
            : mode === "day"
              ? " " + from.getDate() + "일"
              : ""}
        </strong>
        <button
          className="secondary"
          onClick={() => {
            const d = new Date(cursor);
            if (mode === "month") {
              d.setDate(1);
              d.setMonth(d.getMonth() + 1);
            } else d.setDate(d.getDate() + (mode === "day" ? 1 : 7));
            setCursor(d);
            setSelectedDay(d);
          }}
        >
          다음 →
        </button>
        <button
          aria-pressed={mode === "day" && cursor.toDateString() === new Date().toDateString()}
          className={
            mode === "day" && cursor.toDateString() === new Date().toDateString()
              ? "primary"
              : "secondary"
          }
          onClick={() => {
            const today = new Date();
            setMode("day");
            setCursor(today);
            setSelectedDay(today);
          }}
        >
          오늘
        </button>
        <button
          aria-pressed={mode === "month"}
          className={mode === "month" ? "primary" : "secondary"}
          onClick={() => setMode("month")}
        >
          월
        </button>
        <button
          aria-pressed={mode === "week"}
          className={mode === "week" ? "primary" : "secondary"}
          onClick={() => setMode("week")}
        >
          주
        </button>
        {org?.role && org?.role !== "MEMBER" && (
          <button
            className="primary"
            onClick={() => {
              setForm(!form);
              setEditing(undefined);
            }}
          >
            내부 일정 추가
          </button>
        )}
      </div>
      <ErrorMessage error={q.error} />
      {q.isPending && (
        <p role="status" className="muted">
          일정을 불러오고 있어요…
        </p>
      )}
      {form && (
        <Panel title={editing ? "내부 일정 수정" : "새 내부 일정"}>
          <form
            key={editing?.id || "new-" + selectedDay.toDateString()}
            onSubmit={async (e) => {
              e.preventDefault();
              setBusy(true);
              setError(undefined);
              const f = Object.fromEntries(new FormData(e.currentTarget));
              try {
                await request(
                  editing ? "/schedules/" + editing.id : "/organizations/" + org!.id + "/schedules",
                  editing ? "PATCH" : "POST",
                  {
                    ...f,
                    startsAt: instant(String(f.startsAt)),
                    endsAt: instant(String(f.endsAt)),
                  },
                );
                setForm(false);
                setEditing(undefined);
                await query.invalidateQueries({ queryKey: ["calendar"] });
                await query.invalidateQueries({ queryKey: ["archive"] });
                await query.invalidateQueries({ queryKey: ["schedule"] });
              } catch (e) {
                setError(e);
              } finally {
                setBusy(false);
              }
            }}
          >
            <Field label="일정 제목">
              <input name="title" required maxLength={200} defaultValue={editing?.title} />
            </Field>
            <div className="columns">
              <Field label="시작">
                <input
                  type="datetime-local"
                  name="startsAt"
                  required
                  defaultValue={
                    editing
                      ? local(editing.startsAt)
                      : local(
                          new Date(
                            selectedDay.getFullYear(),
                            selectedDay.getMonth(),
                            selectedDay.getDate(),
                            18,
                          ).toISOString(),
                        )
                  }
                />
              </Field>
              <Field label="종료">
                <input
                  type="datetime-local"
                  name="endsAt"
                  required
                  defaultValue={
                    editing
                      ? local(editing.endsAt)
                      : local(
                          new Date(
                            selectedDay.getFullYear(),
                            selectedDay.getMonth(),
                            selectedDay.getDate(),
                            19,
                          ).toISOString(),
                        )
                  }
                />
              </Field>
            </div>
            <Field label="설명">
              <textarea name="description" maxLength={10000} defaultValue={editing?.description} />
            </Field>
            <ErrorMessage error={error} />
            <button className="primary" disabled={busy}>
              일정 저장
            </button>
            <button type="button" className="text-button" onClick={() => setForm(false)}>
              닫기
            </button>
          </form>
        </Panel>
      )}
      {mode !== "day" && (
        <Panel title="통합 일정">
          <div className="calendar-grid">
            {["월", "화", "수", "목", "금", "토", "일"].map((d) => (
              <div className="calendar-label" key={d}>
                {d}
              </div>
            ))}
            {Array.from({ length: blanks }, (_, i) => (
              <div key={"blank-" + i} />
            ))}
            {days.map((d) => {
              const entries = dayEntries(d);
              return (
                <div
                  className={
                    "calendar-day " +
                    (d.toDateString() === new Date().toDateString() ? "today " : "") +
                    (d.toDateString() === selectedDay.toDateString() ? "selected" : "")
                  }
                  key={d.toISOString()}
                >
                  <small className="desktop-day">{d.getDate()}</small>
                  <button
                    className="mobile-day"
                    aria-label={`${d.getMonth() + 1}월 ${d.getDate()}일, 일정 ${entries.length}개`}
                    aria-pressed={d.toDateString() === selectedDay.toDateString()}
                    onClick={() => setSelectedDay(d)}
                  >
                    <span>{d.getDate()}</span>
                    <span className="day-count">{entries.length ? `${entries.length}개` : ""}</span>
                  </button>
                  <div className="desktop-events">
                    {entries.map((e) =>
                      e.path ? (
                        <Link key={e.key} to={e.path} className={e.type.toLowerCase()}>
                          {e.title}
                        </Link>
                      ) : (
                        <button
                          key={e.key}
                          className="calendar-entry"
                          onClick={() => {
                            if (e.canManage) {
                              setEditing(e);
                              setForm(true);
                            }
                          }}
                        >
                          {e.title}
                        </button>
                      ),
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </Panel>
      )}
      <section
        className={(mode === "day" ? "daily-agenda" : "mobile-agenda") + " card"}
        aria-live="polite"
      >
        <h2>
          {selectedDay.getMonth() + 1}월 {selectedDay.getDate()}일 일정{" "}
          <span className="badge">{selectedEntries.length}개</span>
        </h2>
        {!q.isPending && !selectedEntries.length && <Empty>이 날은 예정된 일정이 없어요.</Empty>}
        {selectedEntries.map((e) => (
          <div className="list-row" key={e.key}>
            <div>
              {e.path ? (
                <Link to={e.path}>
                  <strong>{e.title} →</strong>
                </Link>
              ) : (
                <strong>{e.title}</strong>
              )}
              <p className="meta">{date(e.startsAt)}</p>
            </div>
            {e.canManage && (
              <button
                className="secondary"
                onClick={() => {
                  setEditing(e);
                  setForm(true);
                }}
              >
                수정
              </button>
            )}
          </div>
        ))}
      </section>
      {mode !== "day" && (
        <Panel title="기간 내 일정 목록">
          {!q.isPending && !q.error && !q.data?.length && <Empty>이 기간의 일정이 없어요.</Empty>}
          {q.data?.map((e) => (
            <div className="list-row" key={e.key}>
              <div>
                {e.path ? (
                  <Link to={e.path}>
                    <strong>{e.title} →</strong>
                  </Link>
                ) : (
                  <strong>{e.title}</strong>
                )}
                <p className="meta">
                  {date(e.startsAt)} — {date(e.endsAt)}
                </p>
                <p className="muted">{e.description?.slice(0, 150)}</p>
              </div>
              {e.canManage && (
                <div className="toolbar">
                  <button
                    className="secondary"
                    onClick={() => {
                      setEditing(e);
                      setForm(true);
                    }}
                  >
                    수정
                  </button>
                  <Action
                    label="삭제"
                    danger
                    onAction={async () => {
                      if (!confirm("내부 일정을 삭제할까요?")) return;
                      await request("/schedules/" + e.id, "DELETE");
                      await query.invalidateQueries({ queryKey: ["calendar"] });
                      await query.invalidateQueries({ queryKey: ["archive"] });
                      await query.invalidateQueries({ queryKey: ["schedule"] });
                    }}
                  />
                </div>
              )}
            </div>
          ))}
        </Panel>
      )}
    </>
  );
}
