package kr.ucc;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.*;
import kr.ucc.common.ApiException;
import kr.ucc.organization.*;
import kr.ucc.rental.*;
import kr.ucc.user.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RentalRulesTest {

  @Autowired
  RentalService rentals;

  @Autowired
  RentalLoanRepository loans;

  @Autowired
  UserRepository users;

  @Autowired
  OrganizationRepository organizations;

  @Autowired
  MembershipRepository memberships;

  Long leader, student, other, org;

  @BeforeEach
  void setup() {
    leader = users.save(new User("rentalleader@test.dev", "unused", "대표", null, null)).id;
    student = users
      .save(new User("rentalstudent@test.dev", "unused", "학생", "ITM", "26000001"))
      .id;
    other = users.save(new User("rentalother@test.dev", "unused", "다른 학생", null, null)).id;
    org = organizations.save(new Organization("대여 학생회", null, null)).id;
    memberships.save(new Membership(org, leader, Membership.Role.LEADER));
  }

  RentalService.Input input(int quantity, boolean enabled) {
    return new RentalService.Input("우산", "비 오는 날", quantity, 7, enabled);
  }

  @Test
  void requestPickupReturnAndPrivacy() {
    var item = rentals.create(org, leader, input(2, true));
    var loan = rentals.request(item.id(), student, new RentalService.Request(1));
    assertEquals(1, rentals.list(org, student).get(0).availableQuantity());
    assertEquals(loan.id(), rentals.mine(student).get(0).id());
    assertTrue(rentals.mine(other).isEmpty());
    assertThrows(ApiException.class, () -> rentals.history(org, student));
    assertThrows(ApiException.class, () -> rentals.cancel(loan.id(), other));
    assertThrows(ApiException.class, () ->
      rentals.change(loan.id(), student, new RentalService.Change(RentalLoan.Status.BORROWED))
    );
    var borrowed = rentals.change(
      loan.id(),
      leader,
      new RentalService.Change(RentalLoan.Status.BORROWED)
    );
    assertEquals(borrowed.borrowedAt().plusSeconds(7 * 86400), borrowed.dueAt());
    assertThrows(ApiException.class, () -> rentals.cancel(loan.id(), student));
    var returned = rentals.change(
      loan.id(),
      leader,
      new RentalService.Change(RentalLoan.Status.RETURNED)
    );
    assertNotNull(returned.returnedAt());
    assertEquals(2, rentals.list(org, student).get(0).availableQuantity());
    assertEquals(RentalLoan.Status.RETURNED, rentals.history(org, leader).get(0).status());
    rentals.request(item.id(), student, new RentalService.Request(1));
  }

  @Test
  void stockDuplicatesDisabledItemsAndInvalidTransitions() {
    assertThrows(ApiException.class, () -> rentals.create(org, student, input(1, true)));
    var item = rentals.create(org, leader, input(1, true));
    var loan = rentals.request(item.id(), student, new RentalService.Request(1));
    assertEquals(
      "ALREADY_REQUESTED",
      assertThrows(ApiException.class, () ->
        rentals.request(item.id(), student, new RentalService.Request(1))
      ).code
    );
    assertEquals(
      "OUT_OF_STOCK",
      assertThrows(ApiException.class, () ->
        rentals.request(item.id(), other, new RentalService.Request(1))
      ).code
    );
    assertEquals(
      "STOCK_IN_USE",
      assertThrows(ApiException.class, () -> rentals.update(item.id(), leader, input(0, true))).code
    );
    assertThrows(ApiException.class, () ->
      rentals.change(loan.id(), leader, new RentalService.Change(RentalLoan.Status.RETURNED))
    );
    rentals.cancel(loan.id(), student);
    assertEquals(1, rentals.list(org, student).get(0).availableQuantity());
    var next = rentals.request(item.id(), other, new RentalService.Request(1));
    rentals.update(item.id(), leader, input(1, false));
    assertTrue(rentals.list(org, student).isEmpty());
    assertEquals(1, rentals.list(org, leader).size());
    assertEquals(
      "RENTAL_DISABLED",
      assertThrows(ApiException.class, () ->
        rentals.request(item.id(), student, new RentalService.Request(1))
      ).code
    );
    rentals.change(next.id(), leader, new RentalService.Change(RentalLoan.Status.REJECTED));
    assertEquals(1, rentals.list(org, leader).get(0).availableQuantity());
    assertThrows(ApiException.class, () ->
      rentals.change(next.id(), leader, new RentalService.Change(RentalLoan.Status.BORROWED))
    );
  }

  @Test
  void overdueAndOrganizationBoundary() {
    var item = rentals.create(org, leader, input(1, true));
    var loan = rentals.request(item.id(), student, new RentalService.Request(1));
    rentals.change(loan.id(), leader, new RentalService.Change(RentalLoan.Status.BORROWED));
    loans.findById(loan.id()).orElseThrow().dueAt = Instant.now().minusSeconds(1);
    assertTrue(rentals.mine(student).get(0).overdue());
    var otherOrg = organizations.save(new Organization("다른 학생회", null, null));
    memberships.save(new Membership(otherOrg.id, other, Membership.Role.LEADER));
    assertThrows(ApiException.class, () -> rentals.update(item.id(), other, input(2, true)));
    assertThrows(ApiException.class, () ->
      rentals.change(loan.id(), other, new RentalService.Change(RentalLoan.Status.RETURNED))
    );
    var returned = rentals.change(
      loan.id(),
      leader,
      new RentalService.Change(RentalLoan.Status.RETURNED)
    );
    assertFalse(returned.overdue());
  }
}
