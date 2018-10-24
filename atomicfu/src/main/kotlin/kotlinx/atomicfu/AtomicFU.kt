/*
 * Copyright 2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("AtomicFU")
@file:Suppress("NOTHING_TO_INLINE", "RedundantVisibilityModifier")

package kotlinx.atomicfu

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater
import java.util.concurrent.atomic.AtomicLongFieldUpdater
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

/**
 * Creates atomic reference with a given [initial] value.
 *
 * It can only be used in initialize of private read-only property, like this:
 *
 * ```
 * private val f = atomic<Type>(initial)
 * ```
 */
public actual fun <T> atomic(initial: T): AtomicRef<T> = AtomicRef<T>(initial)

/**
 * Creates atomic [Int] with a given [initial] value.
 *
 * It can only be used in initialize of private read-only property, like this:
 *
 * ```
 * private val f = atomic(initialInt)
 * ```
 */
public actual fun atomic(initial: Int): AtomicInt = AtomicInt(initial)

/**
 * Creates atomic [Long] with a given [initial] value.
 *
 * It can only be used in initialize of private read-only property, like this:
 *
 * ```
 * private val f = atomic(initialLong)
 * ```
 */
public actual fun atomic(initial: Long): AtomicLong = AtomicLong(initial)

/**
 * Creates atomic [ULong] with a given [initial] value.
 *
 * It can only be used in initialize of private read-only property, like this:
 *
 * ```
 * private val f = atomic(initialULong)
 * ```
 */
public actual fun atomic(initial: ULong): AtomicULong = AtomicULong(initial)

/**
 * Creates atomic [Boolean] with a given [initial] value.
 *
 * It can only be used in initialize of private read-only property, like this:
 *
 * ```
 * private val f = atomic(initialBoolean)
 * ```
 */
public actual fun atomic(initial: Boolean): AtomicBoolean = AtomicBoolean(initial)

// ==================================== AtomicRef ====================================

/**
 * Atomic reference to a variable of type [T] with volatile reads/writes via
 * [value] property and various atomic read-modify-write operations
 * like [compareAndSet] and others.
 */
@Suppress("UNCHECKED_CAST")
public actual class AtomicRef<T> internal constructor(value: T) {
    /**
     * Reading/writing this property maps to read/write of volatile variable.
     */
    @Volatile
    public actual var value: T = value
        set(value) {
            interceptor.beforeUpdate(this)
            field = value
            interceptor.afterSet(this, value)
        }

    /**
     * Maps to [AtomicReferenceFieldUpdater.lazySet].
     */
    public actual fun lazySet(value: T) {
        interceptor.beforeUpdate(this)
        FU.lazySet(this, value)
        interceptor.afterSet(this, value)
    }

    /**
     * Maps to [AtomicReferenceFieldUpdater.compareAndSet].
     */
    public actual fun compareAndSet(expect: T, update: T): Boolean {
        interceptor.beforeUpdate(this)
        val result = FU.compareAndSet(this, expect, update)
        if (result) interceptor.afterRMW(this, expect, update)
        return result
    }

    /**
     * Maps to [AtomicReferenceFieldUpdater.getAndSet].
     */
    public actual fun getAndSet(value: T): T {
        interceptor.beforeUpdate(this)
        val oldValue = FU.getAndSet(this, value) as T
        interceptor.afterRMW(this, oldValue, value)
        return oldValue
    }

    override fun toString(): String = value.toString()

    private companion object {
        private val FU = AtomicReferenceFieldUpdater.newUpdater(AtomicRef::class.java, Any::class.java, "value")
    }
}


// ==================================== AtomicBoolean ====================================

/**
 * Atomic reference to an [Boolean] variable with volatile reads/writes via
 * [value] property and various atomic read-modify-write operations
 * like [compareAndSet] and others.
 */
@Suppress("UNCHECKED_CAST")
public actual class AtomicBoolean internal constructor(v: Boolean) {

    @Volatile
    private var _value: Int = if (v) 1 else 0

    /**
     * Reading/writing this property maps to read/write of volatile variable.
     */
    public actual var value: Boolean
        get() = _value != 0
        set(value) {
            interceptor.beforeUpdate(this)
            _value = if (value) 1 else 0
            interceptor.afterSet(this, value)
        }

    /**
     * Maps to [AtomicIntegerFieldUpdater.lazySet].
     */
    public actual fun lazySet(value: Boolean) {
        interceptor.beforeUpdate(this)
        val v = if (value) 1 else 0
        FU.lazySet(this, v)
        interceptor.afterSet(this, value)
    }

    /**
     * Maps to [AtomicIntegerFieldUpdater.compareAndSet].
     */
    public actual fun compareAndSet(expect: Boolean, update: Boolean): Boolean {
        interceptor.beforeUpdate(this)
        val e = if (expect) 1 else 0
        val u = if (update) 1 else 0
        val result = FU.compareAndSet(this, e, u)
        if (result) interceptor.afterRMW(this, expect, update)
        return result
    }

    /**
     * Maps to [AtomicIntegerFieldUpdater.getAndSet].
     */
    public actual fun getAndSet(value: Boolean): Boolean {
        interceptor.beforeUpdate(this)
        val v = if (value) 1 else 0
        val oldValue = FU.getAndSet(this, v)
        interceptor.afterRMW(this, (oldValue == 1), value)
        return oldValue == 1
    }

    override fun toString(): String = value.toString()

    private companion object {
        private val FU = AtomicIntegerFieldUpdater.newUpdater(AtomicBoolean::class.java, "_value")
    }
}

// ==================================== AtomicInt ====================================

/**
 * Atomic reference to an [Int] variable with volatile reads/writes via
 * [value] property and various atomic read-modify-write operations
 * like [compareAndSet] and others.
 */
public actual class AtomicInt internal constructor(value: Int) {
    /**
     * Reads/writes of this property maps to read/write of volatile variable.
     */
    @Volatile
    public actual var value: Int = value
        set(value) {
            interceptor.beforeUpdate(this)
            field = value
            interceptor.afterSet(this, value)
        }

    /**
     * Maps to [AtomicIntegerFieldUpdater.lazySet].
     */
    public actual fun lazySet(value: Int) {
        interceptor.beforeUpdate(this)
        FU.lazySet(this, value)
        interceptor.afterSet(this, value)
    }

    /**
     * Maps to [AtomicIntegerFieldUpdater.compareAndSet].
     */
    public actual fun compareAndSet(expect: Int, update: Int): Boolean {
        interceptor.beforeUpdate(this)
        val result = FU.compareAndSet(this, expect, update)
        if (result) interceptor.afterRMW(this, expect, update)
        return result
    }

    /**
     * Maps to [AtomicIntegerFieldUpdater.getAndSet].
     */
    public actual fun getAndSet(value: Int): Int {
        interceptor.beforeUpdate(this)
        val oldValue = FU.getAndSet(this, value)
        interceptor.afterRMW(this, oldValue, value)
        return oldValue
    }

    /**
     * Maps to [AtomicIntegerFieldUpdater.getAndIncrement].
     */
    public actual fun getAndIncrement(): Int {
        interceptor.beforeUpdate(this)
        val oldValue = FU.getAndIncrement(this)
        interceptor.afterRMW(this, oldValue, oldValue + 1)
        return oldValue
    }

    /**
     * Maps to [AtomicIntegerFieldUpdater.getAndDecrement].
     */
    public actual fun getAndDecrement(): Int {
        interceptor.beforeUpdate(this)
        val oldValue = FU.getAndDecrement(this)
        interceptor.afterRMW(this, oldValue, oldValue - 1)
        return oldValue
    }

    /**
     * Maps to [AtomicIntegerFieldUpdater.getAndAdd].
     */
    public actual fun getAndAdd(delta: Int): Int {
        interceptor.beforeUpdate(this)
        val oldValue = FU.getAndAdd(this, delta)
        interceptor.afterRMW(this, oldValue, oldValue + delta)
        return oldValue
    }

    /**
     * Maps to [AtomicIntegerFieldUpdater.addAndGet].
     */
    public actual fun addAndGet(delta: Int): Int {
        interceptor.beforeUpdate(this)
        val newValue = FU.addAndGet(this, delta)
        interceptor.afterRMW(this, newValue - delta, newValue)
        return newValue
    }

    /**
     * Maps to [AtomicIntegerFieldUpdater.incrementAndGet].
     */
    public actual fun incrementAndGet(): Int {
        interceptor.beforeUpdate(this)
        val newValue = FU.incrementAndGet(this)
        interceptor.afterRMW(this, newValue - 1, newValue)
        return newValue
    }

    /**
     * Maps to [AtomicIntegerFieldUpdater.decrementAndGet].
     */
    public actual fun decrementAndGet(): Int {
        interceptor.beforeUpdate(this)
        val newValue = FU.decrementAndGet(this)
        interceptor.afterRMW(this, newValue + 1, newValue)
        return newValue
    }

    /**
     * Performs atomic addition of [delta].
     */
    public actual inline operator fun plusAssign(delta: Int) { getAndAdd(delta) }

    /**
     * Performs atomic subtraction of [delta].
     */
    public actual inline operator fun minusAssign(delta: Int) { getAndAdd(-delta) }

    override fun toString(): String = value.toString()

    private companion object {
        private val FU = AtomicIntegerFieldUpdater.newUpdater(AtomicInt::class.java, "value")
    }
}

// ==================================== AtomicLong ====================================

/**
 * Atomic reference to a [ULong] variable with volatile reads/writes via
 * [value] property and various atomic read-modify-write operations
 * like [compareAndSet] and others.
 */
public actual class AtomicLong internal constructor(value: Long) {
    /**
     * Reads/writes of this property maps to read/write of volatile variable.
     */
    @Volatile
    public actual var value: Long = value
        set(value) {
            interceptor.beforeUpdate(this)
            field = value
            interceptor.afterSet(this, value)
        }

    /**
     * Maps to [AtomicLongFieldUpdater.lazySet].
     */
    public actual fun lazySet(value: Long) {
        interceptor.beforeUpdate(this)
        FU.lazySet(this, value)
        interceptor.afterSet(this, value)
    }

    /**
     * Maps to [AtomicLongFieldUpdater.compareAndSet].
     */
    public actual fun compareAndSet(expect: Long, update: Long): Boolean {
        interceptor.beforeUpdate(this)
        val result = FU.compareAndSet(this, expect, update)
        if (result) interceptor.afterRMW(this, expect, update)
        return result
    }

    /**
     * Maps to [AtomicLongFieldUpdater.getAndSet].
     */
    public actual fun getAndSet(value: Long): Long {
        interceptor.beforeUpdate(this)
        val oldValue = FU.getAndSet(this, value)
        interceptor.afterRMW(this, oldValue, value)
        return oldValue
    }

    /**
     * Maps to [AtomicLongFieldUpdater.getAndIncrement].
     */
    public actual fun getAndIncrement(): Long {
        interceptor.beforeUpdate(this)
        val oldValue = FU.getAndIncrement(this)
        interceptor.afterRMW(this, oldValue, oldValue + 1)
        return oldValue
    }

    /**
     * Maps to [AtomicLongFieldUpdater.getAndDecrement].
     */
    public actual fun getAndDecrement(): Long {
        interceptor.beforeUpdate(this)
        val oldValue = FU.getAndDecrement(this)
        interceptor.afterRMW(this, oldValue, oldValue - 1)
        return oldValue
    }

    /**
     * Maps to [AtomicLongFieldUpdater.getAndAdd].
     */
    public actual fun getAndAdd(delta: Long): Long {
        interceptor.beforeUpdate(this)
        val oldValue = FU.getAndAdd(this, delta)
        interceptor.afterRMW(this, oldValue, oldValue + delta)
        return oldValue
    }

    /**
     * Maps to [AtomicLongFieldUpdater.addAndGet].
     */
    public actual fun addAndGet(delta: Long): Long {
        interceptor.beforeUpdate(this)
        val newValue = FU.addAndGet(this, delta)
        interceptor.afterRMW(this, newValue - delta, newValue)
        return newValue
    }

    /**
     * Maps to [AtomicLongFieldUpdater.incrementAndGet].
     */
    public actual fun incrementAndGet(): Long {
        interceptor.beforeUpdate(this)
        val newValue = FU.incrementAndGet(this)
        interceptor.afterRMW(this, newValue - 1, newValue)
        return newValue
    }

    /**
     * Maps to [AtomicLongFieldUpdater.decrementAndGet].
     */
    public actual fun decrementAndGet(): Long {
        interceptor.beforeUpdate(this)
        val newValue = FU.decrementAndGet(this)
        interceptor.afterRMW(this, newValue + 1, newValue)
        return newValue
    }

    /**
     * Performs atomic addition of [delta].
     */
    public actual inline operator fun plusAssign(delta: Long) { getAndAdd(delta) }

    /**
     * Performs atomic subtraction of [delta].
     */
    public actual inline operator fun minusAssign(delta: Long) { getAndAdd(-delta) }

    override fun toString(): String = value.toString()

    private companion object {
        private val FU = AtomicLongFieldUpdater.newUpdater(AtomicLong::class.java, "value")
    }
}

// ==================================== AtomicLong ====================================

/**
 * Atomic reference to a [Long] variable with volatile reads/writes via
 * [value] property and various atomic read-modify-write operations
 * like [compareAndSet] and others.
 */
@ExperimentalUnsignedTypes
public actual class AtomicULong internal constructor(value: ULong) {
    /**
     * Reads/writes of this property maps to read/write of volatile variable.
     */
    @Volatile
    private var _value: Long = value.toLong()

    public actual var value: ULong
        set(value) {
            interceptor.beforeUpdate(this)
            _value = value.toLong()
            interceptor.afterSet(this, value)
        }
    get() = _value.toULong()

    /**
     * Maps to [AtomicLongFieldUpdater.lazySet].
     */
    public actual fun lazySet(value: ULong) {
        interceptor.beforeUpdate(this)
        FU.lazySet(this, value.toLong())
        interceptor.afterSet(this, value)
    }

    /**
     * Maps to [AtomicLongFieldUpdater.compareAndSet].
     */
    public actual fun compareAndSet(expect: ULong, update: ULong): Boolean {
        interceptor.beforeUpdate(this)
        val result = FU.compareAndSet(this, expect.toLong(), update.toLong())
        if (result) interceptor.afterRMW(this, expect, update)
        return result
    }

    /**
     * Maps to [AtomicLongFieldUpdater.getAndSet].
     */
    public actual fun getAndSet(value: ULong): ULong {
        interceptor.beforeUpdate(this)
        val oldValue = FU.getAndSet(this, value.toLong()).toULong()
        interceptor.afterRMW(this, oldValue, value)
        return oldValue.toULong()
    }

    /**
     * Maps to [AtomicLongFieldUpdater.getAndIncrement].
     */
    public actual fun getAndIncrement(): ULong {
        interceptor.beforeUpdate(this)
        val oldValue = FU.getAndIncrement(this).toULong()
        interceptor.afterRMW(this, oldValue, oldValue + 1.toULong())
        return oldValue
    }

    /**
     * Maps to [AtomicLongFieldUpdater.getAndDecrement].
     */
    public actual fun getAndDecrement(): ULong {
        interceptor.beforeUpdate(this)
        val oldValue = FU.getAndDecrement(this).toULong()
        interceptor.afterRMW(this, oldValue, oldValue - 1.toULong())
        return oldValue
    }

    /**
     * Maps to [AtomicLongFieldUpdater.getAndAdd].
     */
    public actual fun getAndAdd(delta: ULong): ULong {
        interceptor.beforeUpdate(this)
        val oldValue = FU.getAndAdd(this, delta.toLong()).toULong()
        interceptor.afterRMW(this, oldValue, oldValue + delta)
        return oldValue
    }

    /**
     * Maps to [AtomicLongFieldUpdater.addAndGet].
     */
    public actual fun addAndGet(delta: ULong): ULong {
        interceptor.beforeUpdate(this)
        val newValue = FU.addAndGet(this, delta.toLong()).toULong()
        interceptor.afterRMW(this, newValue - delta, newValue)
        return newValue
    }

    /**
     * Maps to [AtomicLongFieldUpdater.getAndAdd].
     */
    public actual fun getAndSubtract(delta: ULong): ULong {
        interceptor.beforeUpdate(this)
        val oldValue = FU.getAndAdd(this, -delta.toLong()).toULong()
        interceptor.afterRMW(this, oldValue, oldValue - delta)
        return oldValue
    }

    /**
     * Maps to [AtomicLongFieldUpdater.addAndGet].
     */
    public actual fun subtractAndGet(delta: ULong): ULong {
        interceptor.beforeUpdate(this)
        val newValue = FU.addAndGet(this, -delta.toLong()).toULong()
        interceptor.afterRMW(this, newValue + delta, newValue)
        return newValue
    }

    /**
     * Maps to [AtomicLongFieldUpdater.incrementAndGet].
     */
    public actual fun incrementAndGet(): ULong {
        interceptor.beforeUpdate(this)
        val newValue = FU.incrementAndGet(this).toULong()
        interceptor.afterRMW(this, newValue - 1.toULong(), newValue)
        return newValue
    }

    /**
     * Maps to [AtomicLongFieldUpdater.decrementAndGet].
     */
    public actual fun decrementAndGet(): ULong {
        interceptor.beforeUpdate(this)
        val newValue = FU.decrementAndGet(this).toULong()
        interceptor.afterRMW(this, newValue + 1.toULong(), newValue)
        return newValue
    }

    override fun toString(): String = value.toString()

    private companion object {
        private val FU = AtomicLongFieldUpdater.newUpdater(AtomicULong::class.java, "_value")
    }
}