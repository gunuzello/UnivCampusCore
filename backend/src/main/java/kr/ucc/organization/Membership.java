package kr.ucc.organization;
import jakarta.persistence.*;
@Entity @Table(name="memberships",uniqueConstraints=@UniqueConstraint(columnNames={"organization_id","user_id"}))
public class Membership {
 public enum Role{MEMBER,STAFF,LEADER}
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(name="organization_id",nullable=false) public Long organizationId;
 @Column(name="user_id",nullable=false) public Long userId;
 @Enumerated(EnumType.STRING) @Column(nullable=false) public Role role;
 protected Membership(){} public Membership(Long organizationId,Long userId,Role role){this.organizationId=organizationId;this.userId=userId;this.role=role;}
}
