import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { request, type Profile } from "./api";
import { Panel, Field, Empty, ErrorMessage, date } from "./ui";
type Response = { userId: number; status: "GOING" | "NOT_GOING" | "UNDECIDED"; updatedAt: string };
const labels = { GOING: "참석 예정", NOT_GOING: "불참 예정", UNDECIDED: "미정" };
export default function MeetingResponsesPanel({
  id,
  attendees,
  startsAt,
  members,
}: {
  id: number;
  attendees: number[];
  startsAt: string;
  members?: { user: Profile }[];
}) {
  const client = useQueryClient();
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);
  const [saved, setSaved] = useState(false);
  const me = useQuery({ queryKey: ["me"], queryFn: () => request<Profile>("/me") });
  const q = useQuery({
    queryKey: ["meeting-responses", id],
    queryFn: () => request<Response[]>("/meetings/" + id + "/responses"),
  });
  const own = q.data?.find((r) => r.userId === me.data?.id);
  const open = Date.parse(startsAt) > Date.now();
  return (
    <Panel title="참석 의사">
      <p className="muted">
        회의 참석 예정 여부를 공유해요. 실제 출석 기록과는 별개이며, 시작 시각이 바뀌면 다시 응답해
        주세요.
      </p>
      <ErrorMessage error={q.error} />
      {me.data &&
        attendees.includes(me.data.id) &&
        !q.isPending &&
        !q.error &&
        (open ? (
          <form
            key={own?.updatedAt || "new"}
            onSubmit={async (e) => {
              e.preventDefault();
              setError(undefined);
              setBusy(true);
              setSaved(false);
              const status = new FormData(e.currentTarget).get("status");
              try {
                await request("/meetings/" + id + "/responses/me", "PUT", { status });
                await client.invalidateQueries({ queryKey: ["meeting-responses", id] });
                setSaved(true);
              } catch (e) {
                setError(e);
              } finally {
                setBusy(false);
              }
            }}
          >
            <Field label="내 참석 의사">
              <select name="status" defaultValue={own?.status || "UNDECIDED"}>
                <option value="UNDECIDED">미정</option>
                <option value="GOING">참석 예정</option>
                <option value="NOT_GOING">불참 예정</option>
              </select>
            </Field>
            <ErrorMessage error={error} />
            {saved && <p role="status">참석 의사를 저장했어요.</p>}
            <button className="primary" disabled={busy}>
              {busy ? "저장 중…" : "참석 의사 저장"}
            </button>
          </form>
        ) : (
          <p className="muted">회의가 시작되어 응답 변경이 마감됐어요.</p>
        ))}
      {q.isPending ? (
        <p>응답을 불러오고 있어요…</p>
      ) : (
        !q.error && (
          <>
            <p className="meta">
              참석 예정 {q.data?.filter((r) => r.status === "GOING").length || 0}명 · 불참 예정{" "}
              {q.data?.filter((r) => r.status === "NOT_GOING").length || 0}명 · 미정/미응답{" "}
              {attendees.length - (q.data?.filter((r) => r.status !== "UNDECIDED").length || 0)}명
            </p>
            {!attendees.length && <Empty>지정한 참석 대상이 없어요.</Empty>}
            {attendees.map((user) => {
              const r = q.data?.find((x) => x.userId === user);
              return (
                <div className="list-row" key={user}>
                  <div>
                    <strong>
                      {members?.find((x) => x.user.id === user)?.user.name ||
                        "이전 구성원 #" + user}
                    </strong>
                    {r && <p className="meta">{date(r.updatedAt)}</p>}
                  </div>
                  <span className="badge">{r ? labels[r.status] : "미응답"}</span>
                </div>
              );
            })}
          </>
        )
      )}
    </Panel>
  );
}
