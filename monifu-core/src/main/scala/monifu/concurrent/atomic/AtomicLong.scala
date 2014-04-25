package monifu.concurrent.atomic

import java.util.concurrent.atomic.{AtomicLong => JavaAtomicLong}
import scala.annotation.tailrec

final class AtomicLong private (ref: JavaAtomicLong) extends AtomicNumber[Long] {
  def get: Long = ref.get()

  def set(update: Long) = ref.set(update)

  def lazySet(update: Long) = ref.lazySet(update)

  def compareAndSet(expect: Long, update: Long): Boolean =
    ref.compareAndSet(expect, update)

  def weakCompareAndSet(expect: Long, update: Long): Boolean =
    ref.weakCompareAndSet(expect, update)

  def getAndSet(update: Long): Long =
    ref.getAndSet(update)

  override def increment(): Unit =
    ref.incrementAndGet()

  override def decrement(): Unit =
    ref.decrementAndGet()

  override def incrementAndGet(): Long =
    ref.incrementAndGet()

  override def decrementAndGet(): Long =
    ref.decrementAndGet()

  override def decrementAndGet(v: Int): Long =
    ref.addAndGet(-v)

  override def getAndIncrement(): Long =
    ref.getAndIncrement

  override def getAndDecrement(): Long =
    ref.getAndDecrement

  override def getAndDecrement(v: Int): Long =
    ref.getAndAdd(-v)

  override def decrement(v: Int): Unit =
    ref.addAndGet(-v)

  @tailrec
  def increment(v: Int): Unit = {
    val current = get
    val update = incrOp(current, v)
    if (!compareAndSet(current, update))
      increment(v)
  }

  @tailrec
  def add(v: Long): Unit = {
    val current = get
    val update = plusOp(current, v)
    if (!compareAndSet(current, update))
      add(v)
  }

  @tailrec
  def incrementAndGet(v: Int): Long = {
    val current = get
    val update = incrOp(current, v)
    if (!compareAndSet(current, update))
      incrementAndGet(v)
    else
      update
  }

  @tailrec
  def addAndGet(v: Long): Long = {
    val current = get
    val update = plusOp(current, v)
    if (!compareAndSet(current, update))
      addAndGet(v)
    else
      update
  }

  @tailrec
  def getAndIncrement(v: Int): Long = {
    val current = get
    val update = incrOp(current, v)
    if (!compareAndSet(current, update))
      getAndIncrement(v)
    else
      current
  }

  @tailrec
  def getAndAdd(v: Long): Long = {
    val current = get
    val update = plusOp(current, v)
    if (!compareAndSet(current, update))
      getAndAdd(v)
    else
      current
  }

  @tailrec
  def subtract(v: Long): Unit = {
    val current = get
    val update = minusOp(current, v)
    if (!compareAndSet(current, update))
      subtract(v)
  }

  @tailrec
  def subtractAndGet(v: Long): Long = {
    val current = get
    val update = minusOp(current, v)
    if (!compareAndSet(current, update))
      subtractAndGet(v)
    else
      update
  }

  @tailrec
  def getAndSubtract(v: Long): Long = {
    val current = get
    val update = minusOp(current, v)
    if (!compareAndSet(current, update))
      getAndSubtract(v)
    else
      current
  }

  @inline private[this] def plusOp(a: Long, b: Long) = a + b
  @inline private[this] def minusOp(a: Long, b: Long) = a - b
  @inline private[this] def incrOp(a: Long, b: Int): Long = a + b
}

object AtomicLong {
  def apply(initialValue: Long): AtomicLong = new AtomicLong(new JavaAtomicLong(initialValue))
}