/*
 * Copyright 2021 Jonathan Lindegaard Starup
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

namespace Concurrent/ReentrantLock {

    use Concurrent/Condition.Condition
    use Concurrent/Condition.Condition.Condition

    ///
    /// A wrapper around a Java ReentrantLock.
    ///
    @Internal
    pub enum ReentrantLock(##java.util.concurrent.locks.ReentrantLock)

    ///
    /// Creates an instance of ReentrantLock with the given fairness policy.
    /// `fair = False` is the default for Java.
    ///
    @Internal
    pub def newLock(fair: Bool): ReentrantLock \ IO =
        import new java.util.concurrent.locks.ReentrantLock(Bool): ##java.util.concurrent.locks.ReentrantLock \ IO as reentrantLock;
        ReentrantLock(reentrantLock(fair))

    ///
    /// Returns true if this lock has fairness set true.
    ///
    @Internal
    pub def isFair(lock: ReentrantLock): Bool \ {} =
        import java.util.concurrent.locks.ReentrantLock.isFair(): Bool \ {};
        let ReentrantLock(l) = lock;
        isFair(l)

    ///
    /// Acquires the lock.
    ///
    @Internal
    pub def lock(lock: ReentrantLock): Unit \ IO =
        import java.util.concurrent.locks.ReentrantLock.lock(): Unit \ IO as lockIt;
        let ReentrantLock(l) = lock;
        lockIt(l)

    ///
    /// Acquires the lock unless the current thread is interrupted.
    ///
    @Internal
    pub def lockInterruptibly(lock: ReentrantLock): Result[Unit, ##java.lang.InterruptedException] \ IO =
        import java.util.concurrent.locks.ReentrantLock.lockInterruptibly(): Unit \ IO;
        let ReentrantLock(l) = lock;
        try {
            Ok(lockInterruptibly(l))
        } catch {
            case e: ##java.lang.InterruptedException => Err(e)
        }

    ///
    /// Returns a Condition instance for use with this Lock instance.
    ///
    @Internal
    pub def newCondition(lock: ReentrantLock): Condition \ IO =
        import java.util.concurrent.locks.ReentrantLock.newCondition(): ##java.util.concurrent.locks.Condition \ IO;
        let ReentrantLock(l) = lock;
        Condition(newCondition(l))

    ///
    /// Acquires the lock only if it is not held by another thread at the time of invocation.
    ///
    @Internal
    pub def tryLock(lock: ReentrantLock): Bool \ IO =
        import java.util.concurrent.locks.ReentrantLock.tryLock(): Bool \ IO;
        let ReentrantLock(l) = lock;
        tryLock(l)

    ///
    /// Acquires the lock if it is not held by another thread within the given waiting time (nanos) and the current thread has not been interrupted.
    ///
    @Internal
    pub def tryLockNanos(lock: ReentrantLock, nanosTimeout: Int64): Result[Bool, ##java.lang.InterruptedException] \ IO =
        import java.util.concurrent.locks.ReentrantLock.tryLock(Int64, ##java.util.concurrent.TimeUnit): Bool \ IO;
        import static get java.util.concurrent.TimeUnit.NANOSECONDS: ##java.util.concurrent.TimeUnit \ IO as nano;
        let ReentrantLock(l) = lock;
        try {
            Ok(tryLock(l, nanosTimeout, nano()))
        } catch {
            case e: ##java.lang.InterruptedException => Err(e)
        }

    ///
    /// Attempts to release this lock.
    ///
    @Internal
    pub def unlock(lock: ReentrantLock): Result[Unit, ##java.lang.IllegalMonitorStateException] \ IO =
        import java.util.concurrent.locks.ReentrantLock.unlock(): Unit \ IO;
        let ReentrantLock(l) = lock;
        try {
            Ok(unlock(l))
        } catch {
            case e: ##java.lang.IllegalMonitorStateException => Err(e)
        }

}
