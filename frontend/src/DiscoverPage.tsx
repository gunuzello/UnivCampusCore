import { useState } from "react";
import type { Organization } from "./api";
import { ProgramList } from "./ProgramPages";
import { TeamList } from "./TeamPages";
import ClubList from "./ClubList";
export default function DiscoverPage({ org }: { org?: Organization }) {
  const tabs = ["프로그램", "팀 구하기", "동아리"];
  const [tab, setTab] = useState("프로그램");
  return (
    <>
      <div className="page-heading">
        <h1>찾기</h1>
        <p>캠퍼스의 기회를 찾고, 함께할 사람을 만나요.</p>
      </div>
      <div className="segmented" role="tablist" aria-label="찾기 유형">
        {tabs.map((t, i) => (
          <button
            key={t}
            role="tab"
            id={"discover-tab-" + i}
            aria-controls="discover-content"
            tabIndex={t === tab ? 0 : -1}
            onKeyDown={(e) => {
              const key = e.key;
              if (!["ArrowLeft", "ArrowRight", "Home", "End"].includes(key)) return;
              e.preventDefault();
              const next =
                key === "Home"
                  ? 0
                  : key === "End"
                    ? tabs.length - 1
                    : (i + (key === "ArrowRight" ? 1 : -1) + tabs.length) % tabs.length;
              setTab(tabs[next]);
              document.getElementById("discover-tab-" + next)?.focus();
            }}
            aria-selected={t === tab}
            className={t === tab ? "primary" : "secondary"}
            onClick={() => setTab(t)}
          >
            {t}
          </button>
        ))}
      </div>
      <div
        role="tabpanel"
        id="discover-content"
        aria-labelledby={"discover-tab-" + tabs.indexOf(tab)}
      >
        {tab === "프로그램" ? (
          <ProgramList key={org?.id} org={org} />
        ) : tab === "팀 구하기" ? (
          <TeamList />
        ) : (
          <ClubList />
        )}
      </div>
    </>
  );
}
