import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { request, type Organization } from "./api";
import { Panel, Field, ErrorMessage, date, instant, Action } from "./ui";
export type ClubInfo = {
  details: {
    meetingCycle: string;
    entryFee: number;
    photoUrl: string;
    recruitmentMode: string;
    opensAt: string | null;
    closesAt: string | null;
  };
  open: boolean;
  subscribed: boolean;
  memberCount: number;
};
const local = (v: string) => {
  const d = new Date(v);
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
};
export default function ClubDetailsPanel({ org }: { org: Organization }) {
  const client = useQueryClient();
  const [edit, setEdit] = useState(false);
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);
  const path = "/organizations/" + org.id + "/club-details";
  const q = useQuery({
    queryKey: ["club-details", org.id],
    queryFn: () => request<ClubInfo>(path),
  });
  const refresh = async () => {
    await client.invalidateQueries({ queryKey: ["club-details"] });
    await client.invalidateQueries({ queryKey: ["notifications"] });
  };
  return (
    <Panel title="동아리 활동과 모집">
      <ErrorMessage error={q.error} />
      {q.data && (
        <>
          <p>
            구성원 {q.data.memberCount}명 · 입회비 {q.data.details.entryFee.toLocaleString()}원
          </p>
          <p>모임 · {q.data.details.meetingCycle || "미등록"}</p>
          <span className="badge">{q.data.open ? "가입 신청 가능" : "현재 모집하지 않아요"}</span>
          {q.data.details.recruitmentMode === "PERIOD" && (
            <p className="meta">
              {date(q.data.details.opensAt!)} — {date(q.data.details.closesAt!)}
            </p>
          )}
          {q.data.details.photoUrl && (
            <img
              src={q.data.details.photoUrl}
              alt={org.name + " 활동 사진"}
              referrerPolicy="no-referrer"
              style={{ maxWidth: "100%", maxHeight: 320, borderRadius: 16 }}
            />
          )}
          {(!q.data.open || q.data.subscribed) && !org.role && (
            <Action
              label={q.data.subscribed ? "모집 알림 취소" : "모집 열리면 알림 받기"}
              onAction={async () => {
                await request(path + "/subscription", q.data!.subscribed ? "DELETE" : "PUT");
                await refresh();
              }}
            />
          )}
          {org.role && org.role !== "MEMBER" && (
            <button className="secondary" onClick={() => setEdit(!edit)}>
              {edit ? "닫기" : "동아리 상세 수정"}
            </button>
          )}
          {edit && (
            <form
              key={JSON.stringify(q.data.details)}
              onSubmit={async (e) => {
                e.preventDefault();
                setBusy(true);
                setError(undefined);
                const f = new FormData(e.currentTarget);
                try {
                  await request(path, "PUT", {
                    ...Object.fromEntries(f),
                    entryFee: Number(f.get("entryFee")),
                    opensAt: f.get("opensAt") ? instant(String(f.get("opensAt"))) : null,
                    closesAt: f.get("closesAt") ? instant(String(f.get("closesAt"))) : null,
                  });
                  setEdit(false);
                  await refresh();
                } catch (e) {
                  setError(e);
                } finally {
                  setBusy(false);
                }
              }}
            >
              <Field label="모임 주기">
                <input
                  name="meetingCycle"
                  maxLength={2000}
                  defaultValue={q.data.details.meetingCycle}
                />
              </Field>
              <Field label="입회비 · 원">
                <input
                  name="entryFee"
                  type="number"
                  min={0}
                  max={10000000}
                  required
                  defaultValue={q.data.details.entryFee}
                />
              </Field>
              <Field label="활동 사진 주소">
                <input name="photoUrl" maxLength={2000} defaultValue={q.data.details.photoUrl} />
              </Field>
              <Field label="동아리 모집 방식">
                <select name="recruitmentMode" defaultValue={q.data.details.recruitmentMode}>
                  <option value="ALWAYS">상시 모집</option>
                  <option value="PERIOD">기간 지정</option>
                  <option value="CLOSED">모집 중지</option>
                </select>
              </Field>
              {[
                ["opensAt", "모집 시작"],
                ["closesAt", "모집 종료"],
              ].map(([name, label]) => (
                <Field key={name} label={label}>
                  <input
                    name={name}
                    type="datetime-local"
                    defaultValue={
                      q.data!.details[name as "opensAt" | "closesAt"]
                        ? local(q.data!.details[name as "opensAt" | "closesAt"]!)
                        : undefined
                    }
                  />
                </Field>
              ))}
              <ErrorMessage error={error} />
              <button className="primary" disabled={busy}>
                동아리 상세 저장
              </button>
            </form>
          )}
        </>
      )}
    </Panel>
  );
}
