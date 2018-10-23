package com.example;
    
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity (name = "users")
public class User {

  @Id
  private String email;

  private String firstName;

  private String lastName;

  protected User() {}

  public String getEmail() {
    return email;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  @Override public String toString() {
    return "User{" +
        "email='" + email + '\'' +
        ", firstName='" + firstName + '\'' +
        ", lastName='" + lastName + '\'' +
        '}';
  }
}
