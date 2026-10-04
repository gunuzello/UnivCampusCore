package kr.ucc.user;
import jakarta.persistence.*;
@Entity @Table(name="app_users")
public class User {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,unique=true) public String email;
 @Column(nullable=false) public String passwordHash;
 @Column(nullable=false) public String name;
 public String department;
 public String studentNumber;
 protected User(){}
 public User(String email,String passwordHash,String name,String department,String studentNumber){this.email=email;this.passwordHash=passwordHash;this.name=name;this.department=department;this.studentNumber=studentNumber;}
 public Profile profile(){return new Profile(id,email,name,department,studentNumber);}
 public record Profile(Long id,String email,String name,String department,String studentNumber){}
}
