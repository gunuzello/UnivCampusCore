package kr.ucc.event;
import jakarta.persistence.*;import java.time.Instant;import java.util.*;
@Entity @Table(name="events") public class Event {
 public enum Status{DRAFT,PUBLISHED,CLOSED,COMPLETED,CANCELLED}
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long organizationId;
 @Column(nullable=false) public String title;
 @Column(columnDefinition="text") public String description;
 @Column(nullable=false) public Instant opensAt;
 @Column(nullable=false) public Instant closesAt;
 public Instant startsAt;public Instant endsAt;public String location;public int capacity;
 @Enumerated(EnumType.STRING) @Column(nullable=false) public Status status=Status.DRAFT;
 @ElementCollection @CollectionTable(name="event_questions",joinColumns=@JoinColumn(name="event_id")) @OrderColumn(name="position") @Column(name="question",nullable=false,length=500) public List<String> questions=new ArrayList<>();
 public Instant createdAt=Instant.now();
 protected Event(){}
}
