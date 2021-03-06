package io.jfc.auto

import algebra.Eq
import org.scalacheck.{ Arbitrary, Gen }
import cats.data.{ NonEmptyList, Validated, Xor }
import cats.laws.discipline.eq._
import io.jfc.{ Decode, Encode, Json }
import io.jfc.test.{ CodecTests, JfcSuite }
import org.scalacheck.Prop.forAll
import shapeless._

class GenericCodecTests extends JfcSuite {
  case class Qux[A](i: Int, a: A)

  object Qux {
    implicit def eqQux[A: Eq]: Eq[Qux[A]] = Eq.by(_.a)

    implicit def arbitraryQux[A](implicit A: Arbitrary[A]): Arbitrary[Qux[A]] =
      Arbitrary(
        for {
          i <- Arbitrary.arbitrary[Int]
          a <- A.arbitrary
        } yield Qux(i, a)
      )
  }

  sealed trait Foo
  case class Bar(i: Int, s: String) extends Foo
  case class Baz(xs: List[String]) extends Foo

  object Foo {
    implicit val eqFoo: Eq[Foo] = Eq.fromUniversalEquals

    implicit val arbitraryFoo: Arbitrary[Foo] = Arbitrary(
      Gen.oneOf(
        for {
          i <- Arbitrary.arbitrary[Int]
          s <- Arbitrary.arbitrary[String]
        } yield Bar(i, s),
        Gen.listOf(Arbitrary.arbitrary[String]).map(Baz.apply)
       )
    )
  }

  checkAll("Codec[Tuple1[Int]]", CodecTests[Tuple1[Int]].codec)
  checkAll("Codec[(Int, Int)]", CodecTests[(Int, Int)].codec)
  checkAll("Codec[(Int, Int, Int)]", CodecTests[(Int, Int, Int)].codec)
  checkAll("Codec[Qux[Int]]", CodecTests[Qux[Int]].codec)
  checkAll("Codec[Foo]", CodecTests[Foo].codec)

  test("Decode[Int => Qux[String]]") {
    check {
      forAll { (i: Int, s: String) =>
        Json.obj("a" -> Json.string(s)).as[Int => Qux[String]].map(_(i)) === Xor.right(Qux(i, s))
      }
    }
  }

  test("Decoding a JSON array without enough elements into a tuple should fail") {
    check {
      forAll { (i: Int, s: String) =>
        Json.array(Json.int(i), Json.string(s)).as[(Int, String, Double)].isLeft
      }
    }
  }

  test("Decoding a JSON array with too many elements into a tuple should fail") {
    check {
      forAll { (i: Int, s: String, d: Double) =>
        Json.array(Json.int(i), Json.string(s), Json.numberOrNull(d)).as[(Int, String)].isLeft
      }
    }
  }

  test("Decoding with Decode[CNil] should fail") {
    assert(Json.empty.as[CNil].isLeft)
  }

  test("Encoding with Encode[CNil] should throw an exception") {
    intercept[RuntimeException](Encode[CNil].apply(null: CNil))
  }
}
