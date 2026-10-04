package kr.ucc.rental;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import kr.ucc.common.ApiException;
import kr.ucc.notification.Notifications;
import kr.ucc.organization.*;
import kr.ucc.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RentalService {

  private final RentalItemRepository items;
  private final RentalLoanRepository loans;
  private final OrganizationAccess access;
  private final OrganizationRepository organizations;
  private final UserRepository users;
  private final Notifications notifications;

  public RentalService(
    RentalItemRepository items,
    RentalLoanRepository loans,
    OrganizationAccess access,
    OrganizationRepository organizations,
    UserRepository users,
    Notifications notifications
  ) {
    this.items = items;
    this.loans = loans;
    this.access = access;
    this.organizations = organizations;
    this.users = users;
    this.notifications = notifications;
  }

  public record Input(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 10000) String description,
    @Min(0) @Max(10000) int totalQuantity,
    @Min(1) @Max(60) int loanDays,
    boolean enabled
  ) {}

  public record Request(@Min(1) @Max(10) int quantity) {}

  public record Change(@NotNull RentalLoan.Status status) {}

  public record ItemView(
    Long id,
    Long organizationId,
    String name,
    String description,
    int totalQuantity,
    long availableQuantity,
    int loanDays,
    boolean enabled,
    boolean canManage
  ) {}

  public record LoanView(
    Long id,
    Long itemId,
    Long organizationId,
    String itemName,
    int quantity,
    RentalLoan.Status status,
    String name,
    String email,
    String department,
    String studentNumber,
    Instant requestedAt,
    Instant borrowedAt,
    Instant dueAt,
    Instant returnedAt,
    boolean overdue
  ) {}

  private boolean manage(Long org, Long user) {
    try {
      access.staff(org, user);
      return true;
    } catch (ApiException ignored) {
      return false;
    }
  }

  private ItemView view(RentalItem item, boolean manage) {
    return new ItemView(
      item.id,
      item.organizationId,
      item.name,
      item.description,
      item.totalQuantity,
      item.totalQuantity - loans.reserved(item.id),
      item.loanDays,
      item.enabled,
      manage
    );
  }

  private LoanView view(RentalLoan loan, RentalItem item) {
    return new LoanView(
      loan.id,
      item.id,
      item.organizationId,
      item.name,
      loan.quantity,
      loan.status,
      loan.name,
      loan.email,
      loan.department,
      loan.studentNumber,
      loan.requestedAt,
      loan.borrowedAt,
      loan.dueAt,
      loan.returnedAt,
      loan.status == RentalLoan.Status.BORROWED &&
        loan.dueAt != null &&
        loan.dueAt.isBefore(Instant.now())
    );
  }

  @Transactional(readOnly = true)
  public List<ItemView> list(Long org, Long user) {
    if (!organizations.existsById(org)) throw ApiException.missing();
    boolean manage = manage(org, user);
    return items
      .findByOrganizationIdOrderByCreatedAtDesc(org)
      .stream()
      .filter(i -> i.enabled || manage)
      .map(i -> view(i, manage))
      .toList();
  }

  public ItemView create(Long org, Long user, Input input) {
    access.staff(org, user);
    var item = new RentalItem();
    item.organizationId = org;
    set(item, input);
    return view(items.save(item), true);
  }

  public ItemView update(Long id, Long user, Input input) {
    var item = items.lockById(id).orElseThrow(ApiException::missing);
    access.staff(item.organizationId, user);
    if (input.totalQuantity() < loans.reserved(id)) throw ApiException.bad(
      "STOCK_IN_USE",
      "신청·대여 중인 수량보다 재고를 줄일 수 없습니다."
    );
    set(item, input);
    return view(item, true);
  }

  private void set(RentalItem item, Input input) {
    item.name = input.name().strip();
    item.description = input.description();
    item.totalQuantity = input.totalQuantity();
    item.loanDays = input.loanDays();
    item.enabled = input.enabled();
  }

  public LoanView request(Long id, Long user, Request input) {
    var item = items.lockById(id).orElseThrow(ApiException::missing);
    if (!item.enabled) throw ApiException.bad("RENTAL_DISABLED", "대여 신청이 중지된 물품입니다.");
    if (
      loans.existsByItemIdAndUserIdAndStatusIn(
        id,
        user,
        List.of(RentalLoan.Status.REQUESTED, RentalLoan.Status.BORROWED)
      )
    ) throw ApiException.bad("ALREADY_REQUESTED", "이미 신청하거나 대여 중인 물품입니다.");
    if (input.quantity() < 1 || input.quantity() > 10) throw ApiException.bad(
      "INVALID_QUANTITY",
      "수량은 1~10개로 입력해 주세요."
    );
    if (item.totalQuantity - loans.reserved(id) < input.quantity()) throw ApiException.bad(
      "OUT_OF_STOCK",
      "대여 가능한 수량이 부족합니다."
    );
    var profile = users.findById(user).orElseThrow(ApiException::missing);
    var loan = new RentalLoan();
    loan.itemId = id;
    loan.userId = user;
    loan.quantity = input.quantity();
    loan.status = RentalLoan.Status.REQUESTED;
    loan.name = profile.name;
    loan.email = profile.email;
    loan.department = profile.department;
    loan.studentNumber = profile.studentNumber;
    loans.saveAndFlush(loan);
    notifications.send(user, item.name + " 대여 신청을 접수했습니다.", "/rentals");
    return view(loan, item);
  }

  public LoanView cancel(Long id, Long user) {
    var itemId = loans.itemId(id).orElseThrow(ApiException::missing);
    var item = items.lockById(itemId).orElseThrow(ApiException::missing);
    // Load the loan only after acquiring its item lock.
    var loan = loans.findById(id).orElseThrow(ApiException::missing);
    if (!loan.userId.equals(user)) throw ApiException.forbidden();
    if (loan.status != RentalLoan.Status.REQUESTED) throw ApiException.bad(
      "INVALID_STATE",
      "수령 전 신청만 취소할 수 있습니다."
    );
    loan.status = RentalLoan.Status.CANCELLED;
    notifications.send(user, item.name + " 대여 신청을 취소했습니다.", "/rentals");
    return view(loan, item);
  }

  public LoanView change(Long id, Long user, Change input) {
    var itemId = loans.itemId(id).orElseThrow(ApiException::missing);
    var item = items.lockById(itemId).orElseThrow(ApiException::missing);
    access.staff(item.organizationId, user);
    var loan = loans.findById(id).orElseThrow(ApiException::missing);
    var next = input.status();
    if (
      loan.status == RentalLoan.Status.REQUESTED &&
      (next == RentalLoan.Status.BORROWED || next == RentalLoan.Status.REJECTED)
    ) {
      if (next == RentalLoan.Status.BORROWED) {
        loan.borrowedAt = Instant.now();
        loan.dueAt = loan.borrowedAt.plus(Duration.ofDays(item.loanDays));
      }
    } else if (loan.status == RentalLoan.Status.BORROWED && next == RentalLoan.Status.RETURNED) {
      loan.returnedAt = Instant.now();
    } else throw ApiException.bad("INVALID_STATE", "현재 대여 상태에서 처리할 수 없는 변경입니다.");
    loan.status = next;
    notifications.send(loan.userId, item.name + " 대여 상태가 변경되었습니다.", "/rentals");
    return view(loan, item);
  }

  @Transactional(readOnly = true)
  public List<LoanView> mine(Long user) {
    return loans
      .findByUserIdOrderByRequestedAtDesc(user)
      .stream()
      .map(l -> view(l, items.findById(l.itemId).orElseThrow(ApiException::missing)))
      .toList();
  }

  @Transactional(readOnly = true)
  public List<LoanView> history(Long org, Long user) {
    access.staff(org, user);
    var all = items.findByOrganizationIdOrderByCreatedAtDesc(org);
    var byId = new HashMap<Long, RentalItem>();
    all.forEach(i -> byId.put(i.id, i));
    if (all.isEmpty()) return List.of();
    return loans
      .findByItemIdInOrderByRequestedAtDesc(new ArrayList<>(byId.keySet()))
      .stream()
      .map(l -> view(l, byId.get(l.itemId)))
      .toList();
  }
}
