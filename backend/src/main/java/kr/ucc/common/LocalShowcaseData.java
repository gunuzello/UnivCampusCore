package kr.ucc.common;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Fictional, Korean campus scenarios for local demonstrations; no real students or official notices. */
@Component
@Profile("local")
@Order(20)
public class LocalShowcaseData implements ApplicationRunner {

  private static final String VERSION = "showcase.v1.complete";
  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private final JdbcTemplate jdbc;
  private final PasswordEncoder passwords;

  public LocalShowcaseData(JdbcTemplate jdbc, PasswordEncoder passwords) {
    this.jdbc = jdbc;
    this.passwords = passwords;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    // A completed bundle is immutable even when its records are edited, cancelled or deleted later.
    // The transaction also ensures that a failed startup cannot leave half a showcase behind.
    if (tracked(VERSION).isPresent()) return;
    var leader = user("leader@ucc.local");
    var student = user("student@ucc.local");
    var staff = user("staff@ucc.local");
    if (leader == null || student == null || staff == null) return;
    var councils = jdbc.queryForList(
      "select o.id from organizations o join memberships m on m.organization_id=o.id " +
        "where o.type='STUDENT_COUNCIL' and m.user_id=? and m.role in ('STAFF','LEADER') order by o.id",
      Long.class,
      leader
    );
    if (councils.isEmpty()) return;
    var council = councils.get(0);
    var today = LocalDate.now(SEOUL);
    var now = Instant.now();
    var hash = passwords.encode("ucc-local-2026!");
    var people = new ArrayList<Long>();
    String[][] profiles = {
      { "hayeon", "정하연", "컴퓨터공학과", "25110011" },
      { "doyun", "최도윤", "디자인학과", "24120012" },
      { "seoyeon", "한서연", "ITM전공", "26100013" },
      { "junho", "오준호", "기계시스템디자인공학과", "25130014" },
      { "yuna", "서유나", "문예창작학과", "26140015" },
      { "jiho", "강지호", "전기정보공학과", "24150016" },
      { "sumin", "윤수민", "산업공학과", "25160017" },
      { "taehyun", "임태현", "건축학부", "26170018" },
      { "chaewon", "배채원", "ITM전공", "25100019" },
      { "hyunwoo", "신현우", "컴퓨터공학과", "26110020" },
      { "eunseo", "조은서", "디자인학과", "25120021" },
      { "minjun", "권민준", "경영학과", "24180022" },
    };
    for (var p : profiles) {
      String email = p[0] + "@ucc.local";
      var id = user(email);
      if (id == null) id = insert(
        "user." + p[0],
        "app_users",
        fields(
          "email",
          email,
          "password_hash",
          hash,
          "name",
          p[1],
          "department",
          p[2],
          "student_number",
          p[3]
        )
      );
      people.add(id);
    }
    var pixel = club(
      "pixel",
      "픽셀 · 디지털 디자인",
      "디자인·콘텐츠",
      "작은 아이디어를 화면과 콘텐츠로 옮기는 디자인 동아리입니다. Figma 스터디, 포스터 제작, 캠퍼스 브랜드 프로젝트를 함께해요. 경험보다 꾸준히 참여하려는 마음을 환영합니다.",
      leader,
      staff,
      List.of(student, people.get(1), people.get(4), people.get(10)),
      "매주 수요일 18:30 · 디자인 작업실",
      15000,
      "PERIOD",
      at(today, -2, 9),
      at(today, 16, 18)
    );
    var code = club(
      "code",
      "코드웨이브 · 웹 개발",
      "개발·프로젝트",
      "기획부터 배포까지 직접 만들어 보는 웹 개발 동아리입니다. 매주 코드 리뷰와 페어 프로그래밍을 진행하고, 학기 말에는 작은 서비스를 소개하는 데모데이를 열어요. 입문 트랙과 프로젝트 트랙을 함께 운영합니다.",
      leader,
      staff,
      List.of(people.get(0), people.get(2), people.get(5), people.get(9)),
      "매주 화요일 19:00 · 미래관 스터디룸",
      10000,
      "ALWAYS",
      null,
      null
    );
    var run = club(
      "run",
      "한걸음 · 캠퍼스 러닝",
      "스포츠·친목",
      "기록보다 함께 달리는 즐거움을 소중히 여기는 러닝 크루입니다. 캠퍼스 3km 입문 코스와 중랑천 5km 코스를 나누어 운영해요. 준비 운동과 정리 운동까지 함께하니 처음 달리는 분도 편하게 참여하세요.",
      leader,
      staff,
      List.of(student, people.get(3), people.get(6), people.get(11)),
      "매주 목요일 18:00 · 운동장 정문 집결",
      0,
      "ALWAYS",
      null,
      null
    );
    var photo = club(
      "photo",
      "포커스 · 일상을 담는 사진",
      "사진·문화",
      "휴대폰부터 필름 카메라까지, 각자의 시선으로 캠퍼스의 일상을 기록합니다. 월 2회 출사와 사진 피드백 모임을 진행하고 학기 말 작은 사진전을 준비해요. 장비가 없어도 참여할 수 있습니다.",
      leader,
      staff,
      List.of(people.get(4), people.get(7), people.get(10)),
      "격주 토요일 10:00 · 출사 장소 사전 안내",
      12000,
      "PERIOD",
      at(today, 3, 9),
      at(today, 20, 18)
    );

    List<ProgramSpec> specs = List.of(
      new ProgramSpec(
        "design",
        "캠퍼스 생활 개선 아이디어 공모전",
        "공모전",
        "디자인,기획,UX,공모전",
        "수강 신청, 공간 찾기, 분실물 등 캠퍼스 생활에서 느낀 불편을 해결할 아이디어를 제안해 주세요.\n\n참여 대상: 재학생 개인 또는 2~4인 팀\n제출물: 문제 정의 1쪽, 해결안 소개 5쪽 이내, 선택 사항인 화면 시안\n진행: 온라인 제안서 접수 → 피드백 세션 → 최종 발표\n심사 기준: 문제의 구체성, 사용자의 필요, 구현 가능성\n\n신청 안내와 제출 준비는 학생회 문의를 통해 확인할 수 있습니다.",
        12,
        18,
        14,
        14,
        17
      ),
      new ProgramSpec(
        "web",
        "처음 만드는 웹 서비스 실습 캠프",
        "교육·특강",
        "개발,React,TypeScript,웹",
        "웹 개발이 처음인 학생을 위한 2일 실습 프로그램입니다. 화면 만들기, API 요청, 팀별 미니 프로젝트를 차근차근 진행해요.\n\n준비물: 개인 노트북과 충전기\n권장 경험: HTML/CSS를 한 번 접해 본 정도\n1일차: React 컴포넌트와 상태 관리\n2일차: REST API 연결과 결과 발표\n운영 방식: 실습 70%, 강의 30% · 멘토 피드백 제공",
        8,
        18,
        10,
        10,
        17
      ),
      new ProgramSpec(
        "career",
        "선배와 나누는 ITM 커리어 라운드테이블",
        "진로·취업",
        "진로,ITM,데이터,기획",
        "제품 기획, 데이터 분석, 개발 직무를 경험한 선배들과 소그룹으로 이야기를 나눕니다. 완성된 이력서가 없어도 참여할 수 있어요.\n\n진행: 직무 소개 30분 → 테이블별 질의응답 60분 → 자유 교류 30분\n사전 질문: 관심 직무와 지금 가장 고민하는 점을 적어 주세요.\n장소와 세부 안내는 참가자에게 전날 공지합니다.",
        6,
        18,
        8,
        18,
        20
      ),
      new ProgramSpec(
        "pitch",
        "가을학기 학생 창업 아이디어 피치",
        "창업",
        "창업,기획,발표,팀",
        "아직 제품이 없는 아이디어도 환영합니다. 고객의 불편과 해결 방향을 3분 발표로 소개하고, 다른 학생 팀과 피드백을 주고받아요.\n\n권장 팀 규모: 2~5명\n준비 자료: 문제·고객·해결안·다음 실험을 담은 발표 자료 5쪽 이내\n성과: 피드백 기록과 다음 2주 실험 계획\n별도의 외부 투자나 지원금 신청은 포함하지 않습니다.",
        15,
        18,
        18,
        14,
        17
      ),
      new ProgramSpec(
        "photo",
        "가을 캠퍼스 사진 산책과 전시 워크숍",
        "문화·체험",
        "사진,콘텐츠,문화,전시",
        "평소 지나치던 공간을 새롭게 바라보는 사진 산책입니다. 스마트폰과 카메라 모두 사용할 수 있어요.\n\n진행: 구도와 빛 기초 30분 → 캠퍼스 사진 산책 60분 → 사진 선택과 피드백 60분\n준비물: 촬영 가능한 기기, 편한 신발\n선정 작품은 참가자의 동의를 받아 동아리 내부 전시에 활용합니다.",
        10,
        18,
        13,
        10,
        13
      ),
      new ProgramSpec(
        "research",
        "데이터로 읽는 캠퍼스 리서치 스터디",
        "스터디",
        "데이터,분석,리서치,Python",
        "캠퍼스 공간과 학생 생활을 주제로 간단한 설문 설계와 데이터 분석을 함께 익힙니다. 통계 도구를 처음 사용하는 분도 참여할 수 있어요.\n\n4주 구성: 질문 만들기 → 익명 설문 설계 → 데이터 정리 → 결과 시각화\n다루는 도구: 스프레드시트, Python, 발표 자료\n원칙: 개인정보 수집을 최소화하고 결과는 집계된 형태로만 공유합니다.",
        9,
        18,
        12,
        18,
        20
      )
    );
    var programs = new LinkedHashMap<String, Long>();
    for (var spec : specs)
      programs.put(
        spec.key,
        insert(
          "program." + spec.key,
          "programs",
          fields(
            "organization_id",
            council,
            "title",
            spec.title,
            "content",
            spec.content,
            "category",
            spec.category,
            "tags",
            spec.tags,
            "application_url",
            "",
            "deadline",
            at(today, spec.deadlineDay, spec.deadlineHour),
            "starts_at",
            at(today, spec.startDay, spec.startHour),
            "ends_at",
            at(today, spec.startDay, spec.endHour),
            "published",
            true
          )
        )
      );

    var serviceTeam = team(
      "campus",
      leader,
      "캠퍼스 길찾기 서비스, 함께 만들어요",
      "처음 방문한 건물에서 강의실을 쉽게 찾을 수 있는 서비스를 만드는 팀입니다. 작은 범위를 완성한 뒤 사용자의 피드백으로 개선하려고 해요.\n\n현재 진행: 문제 인터뷰와 화면 구조 정리\n함께할 역할: 프론트엔드 1명, UX 디자인 1명\n활동: 주 1회 대면 모임 + 비동기 진행 공유\n목표: 4주 안에 주요 건물 2곳을 탐색할 수 있는 프로토타입",
      "프론트엔드 개발,UX 디자인",
      "개발,디자인,UX,React",
      5,
      at(today, 14, 18),
      "OPEN"
    );
    teamApplication(
      "campus.design",
      serviceTeam,
      people.get(1),
      "UX 디자인",
      "사용자 인터뷰 결과를 화면 흐름으로 정리하고 Figma 프로토타입을 만들겠습니다.",
      "ACCEPTED",
      now
    );
    teamApplication(
      "campus.data",
      serviceTeam,
      people.get(0),
      "백엔드 개발",
      "건물과 강의실 데이터를 정리하고 검색 API를 맡고 싶습니다.",
      "ACCEPTED",
      now
    );
    teamApplication(
      "campus.pending",
      serviceTeam,
      people.get(9),
      "프론트엔드 개발",
      "React 스터디에서 만든 프로젝트 경험을 바탕으로 지도와 검색 화면을 구현하고 싶습니다.",
      "PENDING",
      now
    );
    teamEntry(
      "campus.kickoff",
      serviceTeam,
      "MEETING",
      "사용자 인터뷰 결과 공유",
      "인터뷰 메모를 읽고 사용자가 길을 찾기 어려워하는 순간을 정리합니다. 각자 가장 중요한 문제 2개를 준비해 주세요.",
      null,
      at(today, 2, 18),
      at(today, 2, 19),
      false
    );
    teamEntry(
      "campus.prototype",
      serviceTeam,
      "TASK",
      "건물 검색 화면 1차 시안",
      "검색창, 건물 카드, 강의실 찾기 흐름을 Figma로 연결하고 3명의 사용자에게 확인받습니다.",
      people.get(1),
      at(today, 5, 18),
      null,
      false
    );
    teamEntry(
      "campus.api",
      serviceTeam,
      "TASK",
      "건물·층 데이터 구조 정리",
      "건물명, 별칭, 층, 주요 시설을 구분해 공통 데이터 형식을 제안합니다.",
      people.get(0),
      at(today, 4, 18),
      null,
      true
    );
    teamEntry(
      "campus.deadline",
      serviceTeam,
      "DEADLINE",
      "클릭 가능한 프로토타입 공유",
      "핵심 흐름 1개를 완성해 중간 피드백 모임에서 사용해 봅니다.",
      null,
      at(today, 9, 18),
      null,
      false
    );
    var pitchTeam = team(
      "pitch",
      staff,
      "텀블러 사용을 늘리는 캠퍼스 실험팀",
      "일회용 컵을 줄이기 위해 학생들이 텀블러를 더 자주 쓰도록 돕는 작은 실험을 준비합니다. 캠퍼스 리서치와 아이디어 피치에 함께 참여할 팀원을 찾고 있어요.\n\n하고 싶은 일: 짧은 인터뷰, 사용 경험 지도, 2주 참여 실험\n필요 역할: 서비스 기획, 데이터 분석, 콘텐츠 제작\n참여 시간: 주 3~4시간, 시험 기간에는 일정을 조정합니다.",
      "서비스 기획,데이터 분석,콘텐츠 제작",
      "창업,기획,데이터,콘텐츠",
      4,
      at(today, 11, 18),
      "OPEN"
    );
    teamApplication(
      "pitch.member",
      pitchTeam,
      people.get(6),
      "데이터 분석",
      "설문과 참여 기록을 정리해 실험 전후의 변화를 확인하고 싶습니다.",
      "ACCEPTED",
      now
    );
    teamApplication(
      "pitch.pending",
      pitchTeam,
      people.get(4),
      "콘텐츠 제작",
      "참여 안내 카드와 결과 스토리를 제작하고 싶습니다.",
      "PENDING",
      now
    );
    teamEntry(
      "pitch.meeting",
      pitchTeam,
      "MEETING",
      "가설과 실험 대상 정하기",
      "어떤 불편을 줄이면 텀블러를 더 자주 쓰게 될지 가설을 정리합니다.",
      null,
      at(today, 3, 19),
      at(today, 3, 20),
      false
    );
    teamEntry(
      "pitch.research",
      pitchTeam,
      "STAGE",
      "1단계 · 인터뷰와 문제 정의",
      "학생 8명 인터뷰를 진행하고 반복해서 등장한 불편을 분류합니다.",
      null,
      null,
      null,
      true
    );
    var studyTeam = team(
      "study",
      people.get(0),
      "매주 하나씩, React 같이 배우는 스터디",
      "공식 문서를 읽고 작은 화면을 직접 구현하며 React를 익힙니다. 지식 공유와 코드 리뷰를 편하게 주고받는 것이 목표예요.\n\n방식: 매주 주제 1개 → 개인 실습 → 10분씩 결과 공유\n예정 주제: 컴포넌트, 상태, 폼, API 요청, 접근성\n초보자도 환영하며 꾸준히 참여할 수 있는 분을 찾습니다.",
      "학습 파트너",
      "React,TypeScript,개발,스터디",
      6,
      at(today, 18, 18),
      "OPEN"
    );
    teamApplication(
      "study.member",
      studyTeam,
      people.get(9),
      "학습 파트너",
      "작은 기능을 완성하고 서로 코드 리뷰를 하며 공부하고 싶습니다.",
      "ACCEPTED",
      now
    );
    teamEntry(
      "study.next",
      studyTeam,
      "MEETING",
      "2주차 · 상태와 입력 폼",
      "개인 소개 카드에 수정 가능한 입력 폼을 붙여 보고 상태 변경 흐름을 설명합니다.",
      null,
      at(today, 4, 19),
      at(today, 4, 20),
      false
    );
    var archiveTeam = team(
      "archive",
      leader,
      "캠퍼스 분실물 보드 · 학기 프로젝트",
      "학생회실에 들어온 분실물을 빠르게 찾을 수 있는 게시 화면을 만든 팀입니다. 인터뷰, 화면 설계, 프로토타입 발표를 마쳤고 후속 팀이 참고할 수 있도록 작업 기록을 남겼어요.\n\n결과: 품목·발견 장소별 검색 화면과 운영 안내\n배운 점: 물품 사진보다 발견 장소와 날짜가 탐색에 중요했습니다.\n후속 과제: 실제 운영 담당자와 확인한 뒤 개인정보 노출을 줄이는 정책을 정리합니다.",
      "기획,개발,디자인",
      "개발,디자인,프로젝트",
      4,
      at(today, -20, 18),
      "COMPLETED"
    );
    teamApplication(
      "archive.member",
      archiveTeam,
      people.get(2),
      "서비스 기획",
      "인터뷰와 운영 흐름 설계를 맡았습니다.",
      "ACCEPTED",
      at(today, -25, 18)
    );
    teamEntry(
      "archive.stage",
      archiveTeam,
      "STAGE",
      "최종 발표와 회고 완료",
      "사용성 점검 5건을 반영했고, 다음 프로젝트에서는 질문을 더 짧게 설계하기로 했습니다.",
      null,
      null,
      null,
      true
    );
    teamEntry(
      "archive.task",
      archiveTeam,
      "TASK",
      "분실물 등록 흐름 시안",
      "발견 장소, 날짜, 물품 유형을 필수로 묶고 연락 정보는 공개하지 않는 흐름을 정리했습니다.",
      people.get(2),
      at(today, -10, 18),
      null,
      true
    );

    var career = event(
      "career",
      council,
      "ITM 선후배 네트워킹 데이",
      "전공 공부와 진로 고민을 함께 나누는 소규모 네트워킹입니다. 관심 분야별로 자리를 나누고, 선배에게 묻고 싶은 질문을 미리 받아 준비해요.\n\n18:00 입장 및 안내\n18:15 선배의 학교생활 이야기\n18:45 관심 분야별 테이블 대화\n19:40 자유 교류와 마무리\n\n지각하는 경우 신청 답변에 예상 도착 시간을 알려 주세요.",
      "무궁관 세미나실",
      36,
      today,
      6,
      7,
      18,
      20,
      List.of("관심 있는 진로 분야를 알려 주세요.", "선배에게 묻고 싶은 질문이 있나요?"),
      now
    );
    applyEvent(
      "career.student",
      career,
      student,
      List.of("서비스 기획과 UX 디자인", "학기 중 프로젝트와 전공 공부를 어떻게 병행하셨나요?"),
      now
    );
    for (int i = 0; i < 8; i++) applyEvent(
      "career." + i,
      career,
      people.get(i),
      List.of(
        List.of("웹 개발", "서비스 디자인", "데이터 분석", "제품 기획").get(i % 4),
        "첫 프로젝트에서 역할을 나누는 방법이 궁금합니다."
      ),
      now.minusSeconds((i + 1) * 3600L)
    );
    var lunch = event(
      "lunch",
      council,
      "중간고사 응원 간식 나눔",
      "열심히 준비한 하루에 잠깐의 쉼표를 더해요. 신청자에게 샌드위치와 음료를 준비합니다.\n\n수령 시간: 12:00~14:00\n수령 장소: 학생회실 앞 안내 데스크\n확인 방법: 본인 신청 내역 화면 제시\n주의: 중복 수령은 어렵고, 수령이 어려워지면 마감 전에 신청을 취소해 주세요.",
      "학생회실 앞 안내 데스크",
      80,
      today,
      9,
      10,
      12,
      14,
      List.of("식품 알레르기 또는 피해야 하는 재료가 있나요?"),
      now
    );
    for (int i = 0; i < 10; i++) applyEvent(
      "lunch." + i,
      lunch,
      people.get(i),
      List.of(i == 4 ? "견과류를 피하고 싶습니다." : "없습니다."),
      now.minusSeconds((i + 1) * 1800L)
    );
    var welcome = event(
      "pixel",
      pixel,
      "Figma로 만드는 나의 첫 포스터",
      "이미지와 글자를 배치하며 디자인의 기초를 익히는 입문 워크숍입니다. 동아리 가입 전에도 참여할 수 있어요.\n\n진행: 짧은 도구 소개 → 참고 포스터 분석 → 개인 실습 → 서로 피드백\n준비물: 노트북, 본인이 소개하고 싶은 주제 1개\n완성 목표: A4 크기의 포스터 1장",
      "다빈치관 디자인 스튜디오",
      20,
      today,
      4,
      5,
      18,
      20,
      List.of("Figma를 사용해 본 적이 있나요?"),
      now
    );
    for (int i = 0; i < 5; i++) applyEvent(
      "pixel." + i,
      welcome,
      people.get(i),
      List.of(i % 2 == 0 ? "처음 사용합니다." : "간단한 화면을 만들어 봤습니다."),
      now.minusSeconds((i + 1) * 1200L)
    );
    event(
      "run",
      run,
      "한걸음 오픈런 · 캠퍼스 3km",
      "처음 달리는 학생을 위한 편안한 러닝 모임입니다. 말하면서 달릴 수 있는 속도로 움직이고 필요하면 걷는 구간을 함께합니다.\n\n18:00 집결과 준비 운동\n18:15 캠퍼스 3km 러닝\n18:50 정리 운동과 소감 공유\n\n편한 운동화와 개인 물을 준비해 주세요. 비가 오면 안전을 위해 일정을 다시 안내합니다.",
      "운동장 정문",
      24,
      today,
      2,
      3,
      18,
      19,
      List.of("러닝 경험과 편한 거리를 알려 주세요."),
      now
    );

    var recruitment = recruitment(
      "council",
      council,
      "가을학기 학생회 기획·홍보팀 추가 모집",
      "학과 행사를 함께 준비할 기획·홍보팀원을 모집합니다. 한 번에 모든 일을 잘하기보다 서로 배우며 약속한 일을 마무리할 수 있는 분을 기다려요.\n\n기획팀: 행사 아이디어, 일정과 체크리스트, 참여자 안내\n홍보팀: 안내 카드, 사진과 짧은 글, 행사 기록\n활동: 주 1회 회의와 행사 전 준비 모임\n진행: 지원서 확인 → 짧은 대화 → 결과 안내",
      today,
      13,
      now
    );
    applyRecruitment(
      "council.student",
      recruitment,
      student,
      List.of(
        "기획팀",
        "학생들이 실제로 필요로 하는 행사를 만들고 싶습니다. 인터뷰 내용을 정리하고 일정표를 만드는 일을 맡고 싶어요."
      ),
      "REVIEWING",
      now
    );
    for (int i = 0; i < 7; i++) applyRecruitment(
      "council." + i,
      recruitment,
      people.get(i),
      List.of(
        i % 2 == 0 ? "기획팀" : "홍보팀",
        "학기 프로젝트에서 맡은 일을 끝까지 정리한 경험이 있습니다. 서로 진행 상황을 공유하고 학생들이 쉽게 이해할 수 있는 안내를 만들고 싶습니다."
      ),
      i < 2 ? "REVIEWING" : "SUBMITTED",
      now.minusSeconds((i + 1) * 5400L)
    );
    var clubRecruitment = recruitment(
      "code",
      code,
      "코드웨이브 프로젝트 트랙 모집",
      "4주 동안 작은 웹 서비스를 완성하는 프로젝트 트랙을 모집합니다. 코드 작성뿐 아니라 문제 정의, 화면 설계, 테스트와 회고까지 함께해요.\n\n지원 대상: HTML/CSS 또는 프로그래밍 기초를 익힌 학생\n모집 역할: 개발, 디자인, 서비스 기획\n정기 활동: 매주 화요일 19시\n지원서에서는 완성된 결과보다 배운 과정과 꾸준한 참여 의지를 봅니다.",
      today,
      17,
      now
    );
    for (int i = 0; i < 4; i++) applyRecruitment(
      "code." + i,
      clubRecruitment,
      people.get(i + 8),
      List.of(
        i % 2 == 0 ? "프론트엔드 개발" : "서비스 기획",
        "학기 안에 작더라도 실제로 사용할 수 있는 결과물을 완성하고 싶습니다. 매주 정기 모임에 참여할 수 있습니다."
      ),
      "SUBMITTED",
      now.minusSeconds((i + 1) * 7200L)
    );

    meeting(
      "council.next",
      council,
      "가을학기 운영회의 · 행사 준비 점검",
      today,
      1,
      18,
      "이번 주 행사와 추가 모집 진행 상황을 확인합니다. 담당자는 체크리스트에서 진행한 항목과 도움이 필요한 부분을 준비해 주세요.",
      List.of(leader, staff),
      List.of(
        "응원 간식 구성과 수령 동선",
        "네트워킹 질문 취합과 테이블 배치",
        "추가 모집 지원서 검토 일정"
      ),
      List.of(),
      now
    );
    meeting(
      "council.past",
      council,
      "운영회의 기록 · 가을학기 일정 확정",
      today,
      -6,
      18,
      "학기 행사 일정을 조율하고 홍보·신청·현장 운영의 역할을 나누었습니다.",
      List.of(leader, staff),
      List.of("행사 일정 충돌 확인", "홍보 게시 일정", "신청 정원과 개인정보 안내"),
      List.of(
        "간식행사 신청은 수령 전날 18시에 마감한다.",
        "홍보 카드에는 시간·장소·신청 마감일을 같은 위치에 표시한다.",
        "현장 확인은 신청 내역 화면으로 진행하고 별도 명단 공개를 하지 않는다."
      ),
      now
    );
    meeting(
      "pixel.next",
      pixel,
      "픽셀 정기 모임 · 포스터 시안 피드백",
      today,
      2,
      18,
      "각자 만든 포스터를 소개하고 읽기 쉬운 정보 구조와 시선 흐름을 함께 살펴봅니다. 피드백은 좋은 점 1개와 개선 제안 1개씩 나눠요.",
      List.of(leader, staff, student, people.get(1)),
      List.of("워크숍 준비 자료 점검", "회원 포스터 시안 피드백", "다음 디자인 스터디 주제 선택"),
      List.of(),
      now
    );
    meeting(
      "code.next",
      code,
      "코드웨이브 코드 리뷰와 데모",
      today,
      3,
      19,
      "프로젝트별로 이번 주 구현한 기능을 5분씩 소개합니다. 해결이 필요한 문제는 재현 방법과 기대한 동작을 정리해 주세요.",
      List.of(leader, staff, people.get(0), people.get(9)),
      List.of("팀별 진행 공유", "입력 폼과 오류 처리 코드 리뷰", "프로젝트 트랙 오리엔테이션 준비"),
      List.of(),
      now
    );

    notice(
      "council.public",
      council,
      "학생회실 이용 시간과 문의 안내",
      "학생회실은 평일 12:00~18:00에 운영합니다. 점심 시간에는 대여 물품 수령과 간단한 문의를 도와드려요.\n\n대여: 신청 내역을 확인한 뒤 본인이 직접 수령해 주세요.\n분실물: 물품 종류와 발견 장소를 알려 주시면 보관 여부를 확인합니다.\n건의: 소속 공간의 건의 메뉴를 이용하면 답변을 함께 확인할 수 있습니다.\n\n수업과 행사 일정으로 운영 시간이 달라지는 날은 이곳에 미리 안내하겠습니다.",
      "PUBLIC",
      now.minusSeconds(3600)
    );
    notice(
      "council.internal",
      council,
      "운영진 안내 · 행사 당일 체크리스트",
      "담당자는 행사 시작 30분 전에 도착해 동선을 확인해 주세요.\n\n1. 안내문과 신청 확인 화면 준비\n2. 입구와 수령대 위치 확인\n3. 알레르기 안내와 남은 수량 점검\n4. 행사 종료 후 공간 정리와 분실물 확인\n5. 운영 중 불편했던 점을 기록에 남기기\n\n상황이 달라지면 현장 담당자에게 먼저 공유하고, 참가자에게는 같은 문장으로 안내해 주세요.",
      "MEMBERS",
      now.minusSeconds(7200)
    );
    notice(
      "pixel.welcome",
      pixel,
      "픽셀에 처음 오는 분들을 위한 안내",
      "디자인 경험이 없어도 괜찮아요. 첫 모임에서는 각자 좋아하는 포스터나 앱 화면을 하나 소개합니다.\n\n노트북이 있으면 가져오고, 없는 분은 소그룹 실습에 함께 참여할 수 있어요. 질문과 피드백을 편하게 나누는 분위기를 가장 중요하게 생각합니다.",
      "PUBLIC",
      now.minusSeconds(5400)
    );
    notice(
      "code.welcome",
      code,
      "코드웨이브 입문 트랙 진행 방식",
      "입문 트랙은 매주 작은 과제를 완성하고 서로 읽어 보는 방식으로 진행합니다.\n\n모르는 내용을 숨기지 않고 질문하기, 리뷰에서는 사람보다 코드의 동작을 이야기하기, 약속한 시간을 지키기. 이 세 가지를 함께 지켜 주세요.",
      "PUBLIC",
      now.minusSeconds(4800)
    );
    notice(
      "run.welcome",
      run,
      "오픈런 준비물과 안전 안내",
      "편한 운동화, 개인 물, 체온 조절이 가능한 옷을 준비해 주세요. 달리기 전 몸 상태를 확인하고 불편함이 있으면 진행자에게 알려 주세요.\n\n처음 온 분은 입문 그룹에서 걷기와 달리기를 섞어 참여할 수 있습니다. 기록 경쟁보다 안전한 완주를 우선합니다.",
      "PUBLIC",
      now.minusSeconds(4200)
    );
    notice(
      "photo.welcome",
      photo,
      "포커스 다음 모집과 첫 출사 안내",
      "다음 모집은 소개 모임과 함께 시작합니다. 첫 출사는 캠퍼스의 작은 표지와 빛을 주제로 진행할 예정이에요.\n\n장비는 스마트폰도 충분합니다. 사진을 공개할 때는 인물 촬영 동의와 위치 정보 노출을 먼저 확인합니다.",
      "PUBLIC",
      now.minusSeconds(3000)
    );

    var battery = rental(
      "battery",
      council,
      "보조배터리 10,000mAh",
      "수업과 이동 중 충전이 필요할 때 이용해 주세요. C타입 케이블이 함께 제공되며 수령·반납은 학생회실에서 진행합니다. 반납 전에는 구성품을 확인해 주세요.",
      8,
      3,
      now
    );
    var hdmi = rental(
      "hdmi",
      council,
      "HDMI 발표 연결 세트",
      "HDMI 케이블과 C타입 변환 젠더를 함께 빌려 드립니다. 발표 공간의 입력 단자를 미리 확인하고, 행사 당일 사용 전 연결을 점검해 주세요.",
      4,
      2,
      now
    );
    rental(
      "tripod",
      council,
      "스마트폰 촬영 삼각대",
      "팀 발표와 행사 기록에 사용할 수 있는 스마트폰 삼각대입니다. 홀더를 무리하게 조이지 말고, 촬영 후 접어서 반납해 주세요.",
      3,
      5,
      now
    );
    rental(
      "board",
      council,
      "휴대용 보드와 마커 세트",
      "소그룹 기획과 스터디를 위한 A3 크기의 휴대용 보드입니다. 전용 마커 3색과 지우개가 포함되어 있어요. 사용 후 깨끗이 닦아 주세요.",
      5,
      3,
      now
    );
    loan(
      "battery.borrowed",
      battery,
      people.get(2),
      "BORROWED",
      now.minusSeconds(86400),
      now.minusSeconds(36000),
      at(today, 2, 18),
      null
    );
    loan(
      "battery.requested",
      battery,
      student,
      "REQUESTED",
      now.minusSeconds(3600),
      null,
      null,
      null
    );
    loan(
      "hdmi.returned",
      hdmi,
      people.get(8),
      "RETURNED",
      at(today, -4, 12),
      at(today, -3, 12),
      at(today, -1, 18),
      at(today, -2, 17)
    );

    work(
      "task.snack",
      council,
      leader,
      staff,
      "TASK",
      "간식 수령 안내 카드 제작",
      "마감일, 수령 시간, 장소, 확인 방법을 한 장에 정리해 주세요. 작은 화면에서도 날짜와 장소가 먼저 읽히도록 구성하고 게시 전 운영진이 함께 확인합니다.",
      "PENDING",
      null,
      at(today, 4, 18),
      now
    );
    work(
      "task.career",
      council,
      leader,
      leader,
      "TASK",
      "네트워킹 사전 질문 정리",
      "신청 답변을 관심 분야별로 묶고 중복 질문을 정리합니다. 개인정보는 발표 자료에 넣지 않고 질문 내용만 공유해 주세요.",
      "PENDING",
      null,
      at(today, 5, 18),
      now.minusSeconds(1800)
    );
    work(
      "task.place",
      council,
      staff,
      staff,
      "TASK",
      "행사 공간과 좌석 배치 확인",
      "세미나실 사용 시간을 확인하고 테이블별 인원과 안내 위치를 정리했습니다. 행사 전날 한 번 더 최종 점검합니다.",
      "DONE",
      null,
      at(today, -1, 18),
      now.minusSeconds(7200)
    );
    work(
      "suggestion.outlet",
      council,
      student,
      null,
      "SUGGESTION",
      "학생회실 충전 가능한 자리 안내",
      "충전기를 빌린 뒤 가까운 콘센트 위치를 찾기 어려웠습니다. 사용 가능한 자리와 주의 사항을 함께 안내해 주시면 좋겠습니다.",
      "ANSWERED",
      "좋은 의견 감사합니다. 대여 안내에 콘센트가 있는 자리와 이용 주의 사항을 추가하겠습니다. 통행에 방해되지 않는 위치도 함께 표시할게요.",
      null,
      now.minusSeconds(3000)
    );
    work(
      "suggestion.time",
      council,
      people.get(6),
      null,
      "SUGGESTION",
      "간식 수령 시간을 조금 더 나눌 수 있을까요?",
      "연속 수업이 있는 학생도 참여할 수 있도록 수령 시간을 점심과 늦은 오후로 나누는 방안을 검토해 주시면 좋겠습니다.",
      "PENDING",
      null,
      null,
      now.minusSeconds(1800)
    );
    work(
      "task.pixel",
      pixel,
      leader,
      staff,
      "TASK",
      "워크숍 실습 파일과 참고 포스터 준비",
      "무료로 사용할 수 있는 글꼴과 이미지로 예제 파일을 만들고, 참고 포스터는 출처와 함께 내부 실습 자료에 정리합니다.",
      "PENDING",
      null,
      at(today, 3, 18),
      now
    );

    insert(
      "note.events",
      "handover_notes",
      fields(
        "organization_id",
        council,
        "title",
        "간식행사 운영 · 신청부터 수령까지",
        "period",
        "가을학기 운영 기록",
        "content",
        "준비 순서\n1. 대상과 정원을 먼저 정하고 수령 시간을 확정한다.\n2. 알레르기 질문은 필요한 범위에서만 받는다.\n3. 신청 마감 후 수량을 확정하고 현장 안내문을 만든다.\n\n현장 운영\n입구 안내와 수령 확인을 분리하면 대기 줄이 짧아졌다. 신청 내역 화면으로 본인을 확인하고, 수령 상태 변경은 담당자 한 명이 맡았다.\n\n다음 기수에 전할 점\n연속 수업으로 늦게 오는 학생을 위한 마지막 수령 시간을 미리 안내하고, 잔여 수량 처리 기준을 운영진이 합의해 두자.",
        "updated_at",
        now.minusSeconds(86400 * 5L)
      )
    );
    insert(
      "note.recruitment",
      "handover_notes",
      fields(
        "organization_id",
        council,
        "title",
        "운영진 모집 · 질문과 검토 기준",
        "period",
        "가을학기 운영 기록",
        "content",
        "지원 질문은 관심 역할과 참여 가능 시간을 구체적으로 확인할 수 있도록 짧게 구성한다.\n\n검토 기준\n- 맡고 싶은 일과 그 이유가 연결되는가\n- 함께 진행할 때 필요한 약속을 이해하는가\n- 경험이 적어도 배운 과정을 설명할 수 있는가\n\n지원자의 답변은 담당자만 확인하고 결과 안내 후에도 외부에 공유하지 않는다. 검토 메모는 다음 담당자가 판단 과정을 이해할 수 있도록 사실과 의견을 나누어 적는다.",
        "updated_at",
        now.minusSeconds(86400 * 3L)
      )
    );
    insert(
      "schedule.snack",
      "schedules",
      fields(
        "organization_id",
        council,
        "title",
        "간식 포장과 수령 동선 점검",
        "starts_at",
        at(today, 9, 17),
        "ends_at",
        at(today, 9, 18),
        "description",
        "운영진 준비 일정입니다. 구성품 수량을 확인하고 안내문과 수령 확인 화면을 함께 점검합니다."
      )
    );
    insert(
      "schedule.photo",
      "schedules",
      fields(
        "organization_id",
        photo,
        "title",
        "가을 캠퍼스 출사 사전 답사",
        "starts_at",
        at(today, 6, 10),
        "ends_at",
        at(today, 6, 11),
        "description",
        "안전하게 걸을 수 있는 동선과 촬영 가능한 공간을 확인합니다."
      )
    );

    profile(
      leader,
      "기획,개발,UX",
      "프로젝트,공모전,진로",
      "서비스 기획,데이터베이스,프로젝트 관리",
      "React,문서 정리,인터뷰",
      "학생회에서 행사 기획과 운영을 맡고 있습니다. 참여자가 헷갈리지 않는 안내와 다음 기수도 사용할 수 있는 기록을 만드는 데 관심이 있어요.\n\n최근 활동: 간식행사 운영, 선후배 네트워킹 준비, 캠퍼스 길찾기 프로젝트 기획"
    );
    profile(
      student,
      "디자인,개발,기획",
      "공모전,스터디,문화",
      "UX 디자인,데이터베이스",
      "Figma,React,발표 자료",
      "작은 아이디어를 실제 화면으로 옮기는 과정을 배우고 있습니다. 팀 프로젝트에서 사용자 경험과 읽기 쉬운 안내를 함께 고민하고 싶어요.\n\n최근 활동: 포스터 디자인 실습, 웹 서비스 스터디\n다음 목표: 사용자 인터뷰를 바탕으로 한 프로토타입 완성"
    );
    profile(
      staff,
      "콘텐츠,기획,데이터",
      "프로젝트,창업,진로",
      "콘텐츠 기획,경영정보,통계",
      "콘텐츠 제작,설문 설계,스프레드시트",
      "학과 행사에서 홍보와 현장 운영을 맡고 있습니다. 사람들의 참여를 돕는 문장과 시각 자료를 만들고, 피드백을 다음 행사에 반영하는 일을 좋아해요."
    );
    saved(leader, "PROGRAM", programs.get("design"));
    saved(leader, "PROGRAM", programs.get("career"));
    saved(student, "PROGRAM", programs.get("web"));
    saved(student, "PROGRAM", programs.get("photo"));
    saved(student, "TEAM", serviceTeam);
    saved(student, "EVENT", career);
    saved(staff, "PROGRAM", programs.get("pitch"));
    saved(staff, "TEAM", pitchTeam);
    notification(
      "leader.task",
      leader,
      "네트워킹 준비 일정을 확인해 주세요.",
      "/organization?org=" + council,
      false,
      now.minusSeconds(2400)
    );
    notification(
      "student.answer",
      student,
      "충전 가능한 자리 안내 건의에 답변이 도착했어요.",
      "/organization?org=" + council,
      false,
      now.minusSeconds(1800)
    );
    notification(
      "staff.task",
      staff,
      "간식 수령 안내 카드 제작 업무를 맡았어요.",
      "/organization?org=" + council,
      false,
      now.minusSeconds(3600)
    );
    track(VERSION, "BUNDLE", null);
  }

  private record ProgramSpec(
    String key,
    String title,
    String category,
    String tags,
    String content,
    int deadlineDay,
    int deadlineHour,
    int startDay,
    int startHour,
    int endHour
  ) {}

  private static Instant at(LocalDate date, int day, int hour) {
    return date.plusDays(day).atTime(hour, 0).atZone(SEOUL).toInstant();
  }

  private Long user(String email) {
    var ids = jdbc.queryForList("select id from app_users where email=?", Long.class, email);
    return ids.isEmpty() ? null : ids.get(0);
  }

  private Optional<Long> tracked(String key) {
    var values = jdbc.queryForList(
      "select coalesce(entity_id,0) from local_showcase_fixtures where fixture_key=?",
      Long.class,
      key
    );
    return values.stream().findFirst();
  }

  private void track(String key, String type, Long id) {
    jdbc.update(
      "insert into local_showcase_fixtures(fixture_key,entity_type,entity_id) values (?,?,?) on conflict (fixture_key) do nothing",
      key,
      type,
      id
    );
  }

  /** Table and column names are private constants; all content is bound as SQL parameters. */
  private Long insert(String key, String table, LinkedHashMap<String, Object> values) {
    var existing = tracked(key);
    if (existing.isPresent()) return existing.get();
    var columns = String.join(",", values.keySet());
    var placeholders = String.join(",", Collections.nCopies(values.size(), "?"));
    var parameters = values
      .values()
      .stream()
      .map(v -> v instanceof Instant i ? Timestamp.from(i) : v)
      .toArray();
    var id = jdbc.queryForObject(
      "insert into " + table + "(" + columns + ") values (" + placeholders + ") returning id",
      Long.class,
      parameters
    );
    track(key, table, id);
    return id;
  }

  private static LinkedHashMap<String, Object> fields(Object... pairs) {
    var fields = new LinkedHashMap<String, Object>();
    for (int i = 0; i < pairs.length; i += 2) fields.put((String) pairs[i], pairs[i + 1]);
    return fields;
  }

  private Long club(
    String key,
    String name,
    String department,
    String description,
    Long leader,
    Long staff,
    List<Long> members,
    String cycle,
    int fee,
    String mode,
    Instant opens,
    Instant closes
  ) {
    var id = insert(
      "club." + key,
      "organizations",
      fields("name", name, "department", department, "description", description, "type", "CLUB")
    );
    membership("club." + key + ".leader", id, leader, "LEADER");
    membership("club." + key + ".staff", id, staff, "STAFF");
    for (Long member : members)
      membership("club." + key + ".member." + member, id, member, "MEMBER");
    jdbc.update(
      "insert into club_details(organization_id,meeting_cycle,entry_fee,photo_url,recruitment_mode,opens_at,closes_at) values (?,?,?,'',?,?,?) on conflict (organization_id) do nothing",
      id,
      cycle,
      fee,
      mode,
      opens == null ? null : Timestamp.from(opens),
      closes == null ? null : Timestamp.from(closes)
    );
    return id;
  }

  private void membership(String key, Long org, Long user, String role) {
    var existing = jdbc.queryForList(
      "select id from memberships where organization_id=? and user_id=?",
      Long.class,
      org,
      user
    );
    if (existing.isEmpty()) insert(
      key,
      "memberships",
      fields("organization_id", org, "user_id", user, "role", role)
    );
  }

  private Long team(
    String key,
    Long owner,
    String title,
    String content,
    String roles,
    String tags,
    int capacity,
    Instant deadline,
    String status
  ) {
    return insert(
      "team." + key,
      "teams",
      fields(
        "owner_id",
        owner,
        "title",
        title,
        "content",
        content,
        "roles",
        roles,
        "tags",
        tags,
        "capacity",
        capacity,
        "deadline",
        deadline,
        "status",
        status
      )
    );
  }

  private void teamApplication(
    String key,
    Long team,
    Long user,
    String role,
    String message,
    String status,
    Instant updated
  ) {
    insert(
      "team.application." + key,
      "team_applications",
      fields(
        "team_id",
        team,
        "user_id",
        user,
        "role",
        role,
        "message",
        message,
        "status",
        status,
        "updated_at",
        updated
      )
    );
  }

  private void teamEntry(
    String key,
    Long team,
    String kind,
    String title,
    String content,
    Long assignee,
    Instant starts,
    Instant ends,
    boolean done
  ) {
    insert(
      "team.entry." + key,
      "team_entries",
      fields(
        "team_id",
        team,
        "kind",
        kind,
        "title",
        title,
        "content",
        content,
        "assignee_id",
        assignee,
        "starts_at",
        starts,
        "ends_at",
        ends,
        "done",
        done
      )
    );
  }

  private Long event(
    String key,
    Long org,
    String title,
    String description,
    String location,
    int capacity,
    LocalDate day,
    int deadlineDay,
    int startDay,
    int startHour,
    int endHour,
    List<String> questions,
    Instant created
  ) {
    var id = insert(
      "event." + key,
      "events",
      fields(
        "organization_id",
        org,
        "title",
        title,
        "description",
        description,
        "opens_at",
        at(day, -2, 9),
        "closes_at",
        at(day, deadlineDay, 18),
        "starts_at",
        at(day, startDay, startHour),
        "ends_at",
        at(day, startDay, endHour),
        "location",
        location,
        "capacity",
        capacity,
        "status",
        "PUBLISHED",
        "created_at",
        created
      )
    );
    collection("event_questions", "event_id", id, "question", questions);
    return id;
  }

  private void applyEvent(
    String key,
    Long event,
    Long user,
    List<String> answers,
    Instant submitted
  ) {
    var id = application(
      "event.application." + key,
      "event_applications",
      "event_id",
      event,
      user,
      "REGISTERED",
      submitted
    );
    collection("event_answers", "application_id", id, "answer", answers);
  }

  private Long recruitment(
    String key,
    Long org,
    String title,
    String description,
    LocalDate day,
    int deadlineDay,
    Instant created
  ) {
    var id = insert(
      "recruitment." + key,
      "recruitments",
      fields(
        "organization_id",
        org,
        "title",
        title,
        "description",
        description,
        "opens_at",
        at(day, -2, 9),
        "closes_at",
        at(day, deadlineDay, 18),
        "status",
        "PUBLISHED",
        "created_at",
        created
      )
    );
    collection(
      "recruitment_questions",
      "recruitment_id",
      id,
      "question",
      List.of("관심 있는 역할을 알려 주세요.", "지원 동기와 함께하고 싶은 일을 적어 주세요.")
    );
    return id;
  }

  private void applyRecruitment(
    String key,
    Long recruitment,
    Long user,
    List<String> answers,
    String status,
    Instant submitted
  ) {
    var id = application(
      "recruitment.application." + key,
      "recruitment_applications",
      "recruitment_id",
      recruitment,
      user,
      status,
      submitted
    );
    collection("recruitment_answers", "application_id", id, "answer", answers);
  }

  private Long application(
    String key,
    String table,
    String column,
    Long activity,
    Long user,
    String status,
    Instant submitted
  ) {
    var profile = jdbc.queryForMap(
      "select name,email,department,student_number from app_users where id=?",
      user
    );
    var fields = fields(
      column,
      activity,
      "user_id",
      user,
      "status",
      status,
      "submitted_at",
      submitted
    );
    fields.putAll(profile);
    return insert(key, table, fields);
  }

  private void collection(
    String table,
    String foreignColumn,
    Long id,
    String valueColumn,
    List<String> values
  ) {
    for (int i = 0; i < values.size(); i++) jdbc.update(
      "insert into " +
        table +
        "(" +
        foreignColumn +
        ",position," +
        valueColumn +
        ") values (?,?,?) on conflict do nothing",
      id,
      i,
      values.get(i)
    );
  }

  private void meeting(
    String key,
    Long org,
    String title,
    LocalDate day,
    int offset,
    int hour,
    String content,
    List<Long> members,
    List<String> agendas,
    List<String> decisions,
    Instant now
  ) {
    var id = insert(
      "meeting." + key,
      "meetings",
      fields(
        "organization_id",
        org,
        "title",
        title,
        "starts_at",
        at(day, offset, hour),
        "ends_at",
        at(day, offset, hour + 1),
        "content",
        content
      )
    );
    for (int i = 0; i < members.size(); i++) {
      var member = members.get(i);
      jdbc.update(
        "insert into meeting_attendees(meeting_id,user_id) values (?,?) on conflict do nothing",
        id,
        member
      );
      insert(
        "meeting.response." + key + "." + member,
        "meeting_responses",
        fields(
          "meeting_id",
          id,
          "user_id",
          member,
          "status",
          i == members.size() - 1 && members.size() > 2 ? "UNDECIDED" : "GOING",
          "updated_at",
          now
        )
      );
    }
    collection("meeting_agendas", "meeting_id", id, "content", agendas);
    collection("meeting_decisions", "meeting_id", id, "content", decisions);
  }

  private void notice(
    String key,
    Long org,
    String title,
    String content,
    String visibility,
    Instant time
  ) {
    insert(
      "notice." + key,
      "organization_notices",
      fields(
        "organization_id",
        org,
        "title",
        title,
        "content",
        content,
        "visibility",
        visibility,
        "created_at",
        time,
        "updated_at",
        time
      )
    );
  }

  private Long rental(
    String key,
    Long org,
    String name,
    String description,
    int quantity,
    int days,
    Instant created
  ) {
    return insert(
      "rental." + key,
      "rental_items",
      fields(
        "organization_id",
        org,
        "name",
        name,
        "description",
        description,
        "total_quantity",
        quantity,
        "loan_days",
        days,
        "enabled",
        true,
        "created_at",
        created
      )
    );
  }

  private void loan(
    String key,
    Long item,
    Long user,
    String status,
    Instant requested,
    Instant borrowed,
    Instant due,
    Instant returned
  ) {
    var profile = jdbc.queryForMap(
      "select name,email,department,student_number from app_users where id=?",
      user
    );
    var fields = fields(
      "item_id",
      item,
      "user_id",
      user,
      "quantity",
      1,
      "status",
      status,
      "requested_at",
      requested,
      "borrowed_at",
      borrowed,
      "due_at",
      due,
      "returned_at",
      returned
    );
    fields.putAll(profile);
    insert("rental.loan." + key, "rental_loans", fields);
  }

  private void work(
    String key,
    Long org,
    Long creator,
    Long assignee,
    String kind,
    String title,
    String content,
    String status,
    String response,
    Instant due,
    Instant updated
  ) {
    insert(
      "work." + key,
      "organization_work",
      fields(
        "organization_id",
        org,
        "creator_id",
        creator,
        "assignee_id",
        assignee,
        "kind",
        kind,
        "title",
        title,
        "content",
        content,
        "status",
        status,
        "response",
        response,
        "due_at",
        due,
        "updated_at",
        updated
      )
    );
  }

  private void profile(
    Long user,
    String interests,
    String activities,
    String courses,
    String skills,
    String portfolio
  ) {
    jdbc.update(
      "insert into interest_profiles(user_id,interests,activities,courses,skills,portfolio) values (?,?,?,?,?,?) on conflict (user_id) do nothing",
      user,
      interests,
      activities,
      courses,
      skills,
      portfolio
    );
  }

  private void saved(Long user, String type, Long target) {
    jdbc.update(
      "insert into saved_activities(user_id,type,target_id) values (?,?,?) on conflict (user_id,type,target_id) do nothing",
      user,
      type,
      target
    );
  }

  private void notification(
    String key,
    Long user,
    String message,
    String path,
    boolean read,
    Instant created
  ) {
    insert(
      "notification." + key,
      "notifications",
      fields("user_id", user, "message", message, "path", path, "read", read, "created_at", created)
    );
  }
}
