package monifu.concurrent.atomic

import monifu.misc.Unsafe
import scala.annotation.tailrec
import scala.concurrent.TimeoutException
import scala.concurrent.duration.FiniteDuration


final class AtomicAny[T] private (initialValue: T) extends BlockableAtomic[T] {
  @volatile private[this] var ref = initialValue
  private[this] val offset = AtomicAny.addressOffset

  @inline def get: T = ref

  @inline def set(update: T): Unit = {
    ref = update
  }

  def update(value: T): Unit = set(value)
  def `:=`(value: T): Unit = set(value)

  @inline def compareAndSet(expect: T, update: T): Boolean = {
    val current = ref
    current == expect && Unsafe.compareAndSwapObject(this, offset, current.asInstanceOf[AnyRef], update.asInstanceOf[AnyRef])
  }

  @tailrec
  def getAndSet(update: T): T = {
    val current = ref
    if (Unsafe.compareAndSwapObject(this, offset, current.asInstanceOf[AnyRef], update.asInstanceOf[AnyRef]))
      current
    else
      getAndSet(update)
  }

  @inline def lazySet(update: T): Unit = {
    Unsafe.putOrderedObject(this, offset, update.asInstanceOf[AnyRef])
  }

  @tailrec
  def transformAndExtract[U](cb: (T) => (U, T)): U = {
    val current = get
    val (extract, update) = cb(current)
    if (!compareAndSet(current, update))
      transformAndExtract(cb)
    else
      extract
  }

  @tailrec
  def transformAndGet(cb: (T) => T): T = {
    val current = get
    val update = cb(current)
    if (!compareAndSet(current, update))
      transformAndGet(cb)
    else
      update
  }

  @tailrec
  def getAndTransform(cb: (T) => T): T = {
    val current = get
    val update = cb(current)
    if (!compareAndSet(current, update))
      getAndTransform(cb)
    else
      current
  }

  @tailrec
  def transform(cb: (T) => T): Unit = {
    val current = get
    val update = cb(current)
    if (!compareAndSet(current, update))
      transform(cb)
  }

  @tailrec
  @throws(classOf[InterruptedException])
  def waitForCompareAndSet(expect: T, update: T): Unit =
    if (!compareAndSet(expect, update)) {
      interruptedCheck()
      waitForCompareAndSet(expect, update)
    }

  @tailrec
  @throws(classOf[InterruptedException])
  def waitForCompareAndSet(expect: T, update: T, maxRetries: Int): Boolean =
    if (!compareAndSet(expect, update))
      if (maxRetries > 0) {
        interruptedCheck()
        waitForCompareAndSet(expect, update, maxRetries - 1)
      }
      else
        false
    else
      true

  @throws(classOf[InterruptedException])
  @throws(classOf[TimeoutException])
  def waitForCompareAndSet(expect: T, update: T, waitAtMost: FiniteDuration): Unit = {
    val waitUntil = System.nanoTime + waitAtMost.toNanos
    waitForCompareAndSet(expect, update, waitUntil)
  }

  @tailrec
  @throws(classOf[InterruptedException])
  @throws(classOf[TimeoutException])
  private[monifu] def waitForCompareAndSet(expect: T, update: T, waitUntil: Long): Unit =
    if (!compareAndSet(expect, update)) {
      interruptedCheck()
      timeoutCheck(waitUntil)
      waitForCompareAndSet(expect, update, waitUntil)
    }

  @tailrec
  @throws(classOf[InterruptedException])
  def waitForValue(expect: T): Unit =
    if (get != expect) {
      interruptedCheck()
      waitForValue(expect)
    }

  @throws(classOf[InterruptedException])
  @throws(classOf[TimeoutException])
  def waitForValue(expect: T, waitAtMost: FiniteDuration): Unit = {
    val waitUntil = System.nanoTime + waitAtMost.toNanos
    waitForValue(expect, waitUntil)
  }

  @tailrec
  @throws(classOf[InterruptedException])
  @throws(classOf[TimeoutException])
  private[monifu] def waitForValue(expect: T, waitUntil: Long): Unit =
    if (get != expect) {
      interruptedCheck()
      timeoutCheck(waitUntil)
      waitForValue(expect, waitUntil)
    }

  @tailrec
  @throws(classOf[InterruptedException])
  def waitForCondition(p: T => Boolean): Unit =
    if (!p(get)) {
      interruptedCheck()
      waitForCondition(p)
    }

  @throws(classOf[InterruptedException])
  @throws(classOf[TimeoutException])
  def waitForCondition(waitAtMost: FiniteDuration, p: T => Boolean): Unit = {
    val waitUntil = System.nanoTime + waitAtMost.toNanos
    waitForCondition(waitUntil, p)
  }

  @tailrec
  @throws(classOf[InterruptedException])
  @throws(classOf[TimeoutException])
  private[monifu] def waitForCondition(waitUntil: Long, p: T => Boolean): Unit =
    if (!p(get)) {
      interruptedCheck()
      timeoutCheck(waitUntil)
      waitForCondition(waitUntil, p)
    }
}

object AtomicAny {
  def apply[T](initialValue: T): AtomicAny[T] =
    new AtomicAny[T](initialValue)

  private val addressOffset =
    Unsafe.objectFieldOffset(classOf[AtomicAny[_]].getFields.find(_.getName.endsWith("ref")).get)
}
