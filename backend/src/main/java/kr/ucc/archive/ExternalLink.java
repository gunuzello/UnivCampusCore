package kr.ucc.archive;

import jakarta.persistence.*;

@Entity
@Table(name = "external_links")
public class ExternalLink {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long organizationId;
  public String targetType;
  public Long targetId;
  public String title;

  @Column(length = 2000)
  public String url;

  @Column(columnDefinition = "text")
  public String description;

  protected ExternalLink() {}

  public ExternalLink(
    Long organizationId,
    String targetType,
    Long targetId,
    String title,
    String url,
    String description
  ) {
    this.organizationId = organizationId;
    this.targetType = targetType;
    this.targetId = targetId;
    this.title = title;
    this.url = url;
    this.description = description;
  }

  public record View(
    Long id,
    String targetType,
    Long targetId,
    String title,
    String url,
    String description
  ) {}

  public View view() {
    return new View(id, targetType, targetId, title, url, description);
  }
}
