import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { request, type Organization } from "./api";
import { Panel, Field, Empty, ErrorMessage, Action, Status, date } from "./ui";
type Item = {
  id: number;
  organizationId: number;
  name: string;
  description: string;
  totalQuantity: number;
  availableQuantity: number;
  loanDays: number;
  enabled: boolean;
  canManage: boolean;
};
type Loan = {
  id: number;
  itemId: number;
  organizationId: number;
  itemName: string;
  quantity: number;
  status: string;
  name: string;
  email: string;
  department: string;
  studentNumber: string;
  requestedAt: string;
  borrowedAt: string | null;
  dueAt: string | null;
  returnedAt: string | null;
  overdue: boolean;
};
export default function RentalPage({ org }: { org?: Organization }) {
  const query = useQueryClient();
  const manage = !!org?.role && org.role !== "MEMBER";
  const [tab, setTab] = useState("물품");
  const [editing, setEditing] = useState<Item>();
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);
  const [filter, setFilter] = useState("");
  const [search, setSearch] = useState("");
  const items = useQuery({
    queryKey: ["rental-items", org?.id],
    queryFn: () => request<Item[]>("/organizations/" + org!.id + "/rental-items"),
    enabled: !!org,
  });
  const mine = useQuery({
    queryKey: ["rentals", "mine"],
    queryFn: () => request<Loan[]>("/me/rentals"),
  });
  const history = useQuery({
    queryKey: ["rentals", "history", org?.id],
    queryFn: () => request<Loan[]>("/organizations/" + org!.id + "/rentals"),
    enabled: manage,
  });
  async function refresh() {
    await query.invalidateQueries({ queryKey: ["rental-items"] });
    await query.invalidateQueries({ queryKey: ["rentals"] });
    await query.invalidateQueries({ queryKey: ["notifications"] });
  }
  const all = tab === "운영 관리" ? history.data : mine.data;
  const filtered = all?.filter(
    (l) =>
      (!filter || (filter === "OVERDUE" ? l.overdue : l.status === filter)) &&
      [l.name, l.email, l.studentNumber, l.itemName].some((v) =>
        (v || "").toLowerCase().includes(search.trim().toLowerCase()),
      ),
  );
  return (
    <>
      <div className="page-heading">
        <h1>대여사업</h1>
        <p>필요한 물품을 빌리고, 수령과 반납 기록을 함께 관리해요.</p>
      </div>
      {!org ? (
        <Empty>학생회를 선택해 주세요.</Empty>
      ) : (
        <>
          <div className="segmented" role="tablist" aria-label="대여 화면">
            {["물품", "내 대여", ...(manage ? ["운영 관리"] : [])].map((t) => (
              <button
                key={t}
                role="tab"
                aria-selected={tab === t}
                className={tab === t ? "primary" : "secondary"}
                onClick={() => setTab(t)}
              >
                {t}
              </button>
            ))}
          </div>
          {tab === "물품" ? (
            <>
              <ErrorMessage error={items.error} />
              {manage && (
                <div className="toolbar">
                  <button
                    className="primary"
                    onClick={() => {
                      setCreating(!creating);
                      setEditing(undefined);
                      setError(undefined);
                    }}
                  >
                    물품 등록
                  </button>
                </div>
              )}
              {(creating || editing) && manage && (
                <Panel title={editing ? "물품 수정" : "새 대여 물품"}>
                  <form
                    key={editing?.id || "new"}
                    onSubmit={async (e) => {
                      e.preventDefault();
                      setBusy(true);
                      setError(undefined);
                      const f = new FormData(e.currentTarget);
                      try {
                        await request(
                          editing
                            ? "/rental-items/" + editing.id
                            : "/organizations/" + org.id + "/rental-items",
                          editing ? "PATCH" : "POST",
                          {
                            name: f.get("name"),
                            description: f.get("description"),
                            totalQuantity: Number(f.get("totalQuantity")),
                            loanDays: Number(f.get("loanDays")),
                            enabled: f.has("enabled"),
                          },
                        );
                        setEditing(undefined);
                        setCreating(false);
                        await refresh();
                      } catch (e) {
                        setError(e);
                      } finally {
                        setBusy(false);
                      }
                    }}
                  >
                    <Field label="물품 이름">
                      <input name="name" required maxLength={200} defaultValue={editing?.name} />
                    </Field>
                    <Field label="물품 설명">
                      <textarea
                        name="description"
                        maxLength={10000}
                        defaultValue={editing?.description}
                      />
                    </Field>
                    <div className="columns">
                      <Field label="전체 재고">
                        <input
                          type="number"
                          name="totalQuantity"
                          min={0}
                          max={10000}
                          required
                          defaultValue={editing?.totalQuantity ?? 1}
                        />
                      </Field>
                      <Field label="대여 기간 · 일">
                        <input
                          type="number"
                          name="loanDays"
                          min={1}
                          max={60}
                          required
                          defaultValue={editing?.loanDays ?? 7}
                        />
                      </Field>
                    </div>
                    <label className="checks">
                      <input
                        type="checkbox"
                        name="enabled"
                        defaultChecked={editing?.enabled ?? true}
                      />
                      대여 신청 받기
                    </label>
                    <p className="meta">
                      신청 접수 시 재고를 확보해요. 수령 처리한 시각부터 대여 기간을 계산해요.
                    </p>
                    <ErrorMessage error={error} />
                    <div className="toolbar">
                      <button className="primary" disabled={busy}>
                        {busy ? "저장 중…" : "물품 저장"}
                      </button>
                      <button
                        type="button"
                        className="secondary"
                        onClick={() => {
                          setEditing(undefined);
                          setCreating(false);
                        }}
                      >
                        닫기
                      </button>
                    </div>
                  </form>
                </Panel>
              )}
              {items.isPending ? (
                <p>물품을 불러오고 있어요…</p>
              ) : items.data?.length ? (
                <div className="grid">
                  {items.data.map((i) => (
                    <Panel key={i.id} title={i.name}>
                      <span className="badge">
                        {i.enabled ? "대여 가능 " + i.availableQuantity + "개" : "신청 중지"}
                      </span>
                      <p className="prewrap">{i.description}</p>
                      <p className="meta">
                        전체 {i.totalQuantity}개 · 수령 후 {i.loanDays}일 대여
                      </p>
                      {manage && (
                        <button
                          className="secondary"
                          onClick={() => {
                            setEditing(i);
                            setCreating(false);
                            setError(undefined);
                          }}
                        >
                          물품 수정
                        </button>
                      )}
                      {mine.data?.some(
                        (l) => l.itemId === i.id && ["REQUESTED", "BORROWED"].includes(l.status),
                      ) ? (
                        <p className="meta">
                          이미 신청하거나 대여 중이에요. 내 대여에서 확인해 주세요.
                        </p>
                      ) : i.enabled && i.availableQuantity > 0 ? (
                        <form
                          onSubmit={async (e) => {
                            e.preventDefault();
                            const f = new FormData(e.currentTarget);
                            setError(undefined);
                            setBusy(true);
                            try {
                              await request("/rental-items/" + i.id + "/loans", "POST", {
                                quantity: Number(f.get("quantity")),
                              });
                              await refresh();
                              setTab("내 대여");
                            } catch (e) {
                              setError(e);
                            } finally {
                              setBusy(false);
                            }
                          }}
                        >
                          <Field label={i.name + " 신청 수량"}>
                            <input
                              type="number"
                              name="quantity"
                              min={1}
                              max={Math.min(10, i.availableQuantity)}
                              defaultValue={1}
                              required
                            />
                          </Field>
                          <button className="primary" disabled={busy}>
                            대여 신청
                          </button>
                        </form>
                      ) : (
                        <p className="muted">지금은 신청할 수 없어요.</p>
                      )}
                    </Panel>
                  ))}
                </div>
              ) : (
                !items.error && <Empty>등록된 대여 물품이 없어요.</Empty>
              )}
              <ErrorMessage error={error} />
            </>
          ) : (
            <>
              <div className="applicant-filters">
                <Field label="대여 기록 검색">
                  <input
                    type="search"
                    maxLength={200}
                    value={search}
                    placeholder="물품 · 이름 · 이메일 · 학번"
                    onChange={(e) => setSearch(e.target.value)}
                  />
                </Field>
                <Field label="대여 상태">
                  <select value={filter} onChange={(e) => setFilter(e.target.value)}>
                    {[
                      ["", "전체 상태"],
                      ["REQUESTED", "수령 대기"],
                      ["BORROWED", "대여 중"],
                      ["OVERDUE", "연체"],
                      ["RETURNED", "반납 완료"],
                      ["CANCELLED", "취소"],
                      ["REJECTED", "반려"],
                    ].map(([v, t]) => (
                      <option key={v} value={v}>
                        {t}
                      </option>
                    ))}
                  </select>
                </Field>
              </div>
              <ErrorMessage error={tab === "운영 관리" ? history.error : mine.error} />
              {(tab === "운영 관리" ? history.isPending : mine.isPending) ? (
                <p>기록을 불러오고 있어요…</p>
              ) : filtered?.length ? (
                <>
                  {filtered.map((l) => (
                    <Panel key={l.id} title={l.itemName + " · " + l.quantity + "개"}>
                      <div className="toolbar">
                        {l.status === "REJECTED" ? (
                          <span className="badge rejected">반려</span>
                        ) : (
                          <Status value={l.status} />
                        )}
                        {l.overdue && <span className="badge rejected">연체</span>}
                      </div>
                      {tab === "운영 관리" && (
                        <p>
                          {l.name} · {l.email}
                          <br />
                          {l.department} · {l.studentNumber}
                        </p>
                      )}
                      <p className="meta">신청 · {date(l.requestedAt)}</p>
                      {l.borrowedAt && <p className="meta">수령 · {date(l.borrowedAt)}</p>}
                      {l.dueAt && <p className="meta">반납 기한 · {date(l.dueAt)}</p>}
                      {l.returnedAt && <p className="meta">반납 · {date(l.returnedAt)}</p>}
                      <div className="toolbar">
                        {tab === "내 대여" && l.status === "REQUESTED" && (
                          <Action
                            label="대여 신청 취소"
                            onAction={async () => {
                              await request("/rentals/" + l.id + "/cancellation", "POST");
                              await refresh();
                            }}
                          />
                        )}
                        {tab === "운영 관리" && l.status === "REQUESTED" && (
                          <>
                            <Action
                              label="수령 처리"
                              onAction={async () => {
                                await request("/rentals/" + l.id + "/status", "PATCH", {
                                  status: "BORROWED",
                                });
                                await refresh();
                              }}
                            />
                            <Action
                              label="신청 반려"
                              onAction={async () => {
                                await request("/rentals/" + l.id + "/status", "PATCH", {
                                  status: "REJECTED",
                                });
                                await refresh();
                              }}
                            />
                          </>
                        )}
                        {tab === "운영 관리" && l.status === "BORROWED" && (
                          <Action
                            label="반납 처리"
                            onAction={async () => {
                              await request("/rentals/" + l.id + "/status", "PATCH", {
                                status: "RETURNED",
                              });
                              await refresh();
                            }}
                          />
                        )}
                      </div>
                    </Panel>
                  ))}
                </>
              ) : (
                all && <Empty>조건에 맞는 대여 기록이 없어요.</Empty>
              )}
            </>
          )}
        </>
      )}
    </>
  );
}
