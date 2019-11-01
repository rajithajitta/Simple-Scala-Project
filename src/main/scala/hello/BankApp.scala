package hello

import java.util.Scanner

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}
import hello.BankApp.Atm.{CheckPassword, ValidatePassword}
import hello.BankApp.BankAccount.{AuthenticationFailure, AuthenticationSuccess, Statement}

object BankApp extends App {

  object Atm {

    case class ValidatePassword(account: ActorRef)

    case class CheckPassword(atm: ActorRef, account: ActorRef)

  }

  class Atm extends Actor {
    override def receive: Receive = {
      case ValidatePassword(account) =>
        println("Enter password for you Account(12345 is correct password)")
        val password = new Scanner(System.in).next()
        if (password.equals("12345")) {
          println("User is authenticated")
          sender ! AuthenticationSuccess("User is authenticated", account)
        }
        else {
          println("Authenticated failed for user")
          sender ! AuthenticationFailure("Authenticated failed for user")
        }
    }

  }

  object BankAccount {

    case class Deposit(amount: Int)

    case class Withdraw(amount: Int)

    case object Statement

    case class TransactionSuccess(message: String)

    case class TransactionFailure(reason: String)

    case class AuthenticationSuccess(message: String, account: ActorRef)

    case class AuthenticationFailure(message: String)

  }

  class BankAccount extends Actor {

    import BankAccount._

    var funds = 0

    override def receive: Receive = {
      case Deposit(amount) =>
        if (amount < 0) println("invalid deposit amount")
        else {
          funds += amount
          println(s"\n successfully deposited $amount and you current balance is $funds")
        }
      case Withdraw(amount) =>
        if (amount < 0)
          println("\n invalid withdraw amount")
        else if (amount > funds) println("insufficient funds")
        else {
          funds -= amount
          println(s"\n successfully withdrew $amount and you current balance is $funds")
        }
    }
  }

  object Person {

    case class LiveTheLife(account: ActorRef)

  }

  class Person extends Actor with ActorLogging {

    import Atm._
    import Person._
    import BankAccount._

    var input = "yes"

    override def receive: Receive = {
      case CheckPassword(atm, account) => atm ! ValidatePassword(account)
      case AuthenticationSuccess(message, account) => log.info(message.toString)
        userTransactions(account)
      case AuthenticationFailure(message) => log.info(message.toString)

    }

    def userTransactions(account: ActorRef) = {
      while (input.equalsIgnoreCase("yes")) {
        println("do you want to do withdraw or deposit(w/d)")
        val word = new Scanner(System.in)
        val action = word.next()
        if (action.equalsIgnoreCase("w")) {
          println("How much you want to withdraw ")
          val withAmout = new Scanner(System.in)
          val amout = withAmout.nextInt()
          account ! Withdraw(amout)
        }
        else {
          println("How much you want to deposit ")
          val deposit = new Scanner(System.in)
          val amout = deposit.nextInt()
          account ! Deposit(amout)
        }
        println("do you want to continue, please enter yes/no")
        val yesOrNo = new Scanner(System.in)
        input = yesOrNo.next()
      }
    }

  }


  val system = ActorSystem("actorCapabilitiesDemo")
  val atm = system.actorOf(Props[Atm], "atm")
  val account = system.actorOf(Props[BankAccount], "bankAccount")
  val person = system.actorOf(Props[Person], "billionaire")

  person ! CheckPassword(atm, account)

}