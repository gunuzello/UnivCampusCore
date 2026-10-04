package kr.ucc.recruitment;
import jakarta.persistence.*;import java.time.Instant;import java.util.*;
@Entity @Table(name="recruitments") public class Recruitment {
 public enum Status{DRAFT,PUBLISHED,CLOSED,CANCELLED}
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long organizationId;
 @Column(nullable=false) public String title;
 @Column(columnDefinition="text") public String description;
 @Column(nullable=false) public Instant opensAt;
 @Column(nullable=false) public Instant closesAt;
 
 @Enumerated(EnumType.STRING) @Column(nullable=false) public Status status=Status.DRAFT;
 @ElementCollection @CollectionTable(name="recruitment_questions",joinColumns=@JoinColumn(name="recruitment_id")) @OrderColumn(name="position") @Column(name="question",nullable=false,length=500) public List<String> questions=new ArrayList<>();
 public Instant createdAt=Instant.now();
 protected Recruitment(){}
}
