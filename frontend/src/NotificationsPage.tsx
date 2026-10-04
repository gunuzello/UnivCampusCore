import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { request } from "./api";
import { Panel, Empty, ErrorMessage, Action, date } from "./ui";
export type Notification = {
  id: number;
  message: string;
  path: string;
  read: boolean;
  createdAt: string;
};
export default function NotificationsPage() {
  const query = useQueryClient();
  const [unreadOnly, setUnreadOnly] = useState(false);
  const q = useQuery({
    queryKey: ["notifications"],
    queryFn: () => request<Notification[]>("/notifications"),
  });
  const unread = q.data?.filter((n) => !n.read) || [];
  const items = unreadOnly ? unread : q.data;
  return (
    <>
      <div className="page-heading">
        <h1>알림</h1>
        <p>신청 결과와 캠퍼스 활동의 새로운 소식을 확인해요.</p>
      </div>
      <Panel title="받은 소식">
        <div className="toolbar">
          <label className="checks">
            <input
              type="checkbox"
              checked={unreadOnly}
              onChange={(e) => setUnreadOnly(e.target.checked)}
            />
            읽지 않은 알림만
          </label>
          {q.data && (
            <span className="count-label">
              읽지 않음 {unread.length}개 · 전체 {q.data.length}개
            </span>
          )}
          {unread.length > 0 && (
            <Action
              label="모두 읽음"
              onAction={async () => {
                // Each update remains scoped to the signed-in user's own notification.
                try {
                  await Promise.all(unread.map((n) => request("/notifications/" + n.id, "PATCH")));
                } finally {
                  await query.invalidateQueries({ queryKey: ["notifications"] });
                }
              }}
            />
          )}
        </div>
        <ErrorMessage error={q.error} />
        {q.isPending && <Empty>알림을 불러오고 있어요…</Empty>}
        {!q.isPending && !q.error && !items?.length && (
          <Empty>
            {unreadOnly
              ? "읽지 않은 알림이 없어요. 모든 소식을 확인했어요."
              : "아직 받은 알림이 없어요. 신청 결과와 활동 소식을 여기에 안내해 드려요."}
          </Empty>
        )}
        {items?.map((n) => (
          <div className={"list-row notification " + (!n.read ? "unread" : "")} key={n.id}>
            <div>
              {!n.read && <span className="badge">새 소식</span>}
              <Link to={n.path || "/"}>{n.message}</Link>
              <p className="meta">{date(n.createdAt)}</p>
            </div>
            {!n.read && (
              <Action
                label="읽음 처리"
                onAction={async () => {
                  await request("/notifications/" + n.id, "PATCH");
                  await query.invalidateQueries({ queryKey: ["notifications"] });
                }}
              />
            )}
          </div>
        ))}
      </Panel>
    </>
  );
}
