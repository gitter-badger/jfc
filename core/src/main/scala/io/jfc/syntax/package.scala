package io.jfc

/**
 * Provides syntax via enrichment classes.
 */
package object syntax {
  implicit class EncodeOps[A](val a: A) extends AnyVal {
    def toJson(implicit e: Encode[A]): Json = e(a)
  }
}
