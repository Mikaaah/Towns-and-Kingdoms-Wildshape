package nl.townsandkingdoms.wildshape.concurrent.task;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

public abstract class LazyCallable<T> implements Callable<T>, Future<T>, Supplier<T> {
	private StampedLock lock;
	protected volatile T value;
	private volatile boolean initialized;

	public LazyCallable() {
		this.lock = new StampedLock();
		this.value = null;
		this.initialized = false;
	}

	@Override
    public T call() {
        // Publish completion only after value, including a valid null result.
        // Reading initialized first gives a completed reader the value published
        // by that writer. A stamp validated before reading initialized could
        // otherwise let a newly completed writer expose a stale local value.
        if (this.initialized) return this.value;
        long write = this.lock.writeLock();
        try {
            if (!this.initialized) {
                T result = this.create();
                this.value = result;
                this.initialized = true;
            }
            return this.value;
        } finally {
            this.lock.unlockWrite(write);
        }
    }

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
    public boolean isDone() {
        return this.initialized;
    }

	@Override
	public T get() {
		return this.call();
	}

	@Override
	public T get(long timeout, TimeUnit unit) {
		return this.call();
	}

	protected abstract T create();

	public static LazyCallable<Void> adapt(Runnable runnable) {
		return new RunnableAdapter(runnable);
	}

	public static <T> LazyCallable<T> adapt(Callable<T> callable) {
		if (callable instanceof LazyCallable<T> c) {
			return c;
		}
		return new CallableAdapter<>(callable);
	}

	public static <T> LazyCallable<T> adaptComplete(Callable<T> callable) {
		return new CompleteAdapter<>(callable);
	}

	public static class CallableAdapter<T> extends LazyCallable<T> {
		private Callable<T> callable;

		public CallableAdapter(Callable<T> callable) {
			this.callable = callable;
		}

		@Override
		protected T create() {
			try {
				return this.callable.call();
			} catch (Throwable t) {
				t.printStackTrace();
				return null;
			}
		}
	}

	public static class FutureAdapter<T> extends LazyCallable<T> {
		private Future<T> future;

		FutureAdapter(Future<T> future) {
			this.future = future;
		}

		@Override
		public boolean isDone() {
			return this.future.isDone();
		}

		@Override
		protected T create() {
			try {
				return this.future.get();
			} catch (Throwable t) {
				t.printStackTrace();
				return null;
			}
		}
	}

	public static class RunnableAdapter extends LazyCallable<Void> {
		private Runnable runnable;

		RunnableAdapter(Runnable runnable) {
			this.runnable = runnable;
		}

		@Override
		protected Void create() {
			this.runnable.run();
			return null;
		}
	}

	public static class CompleteAdapter<T> extends LazyCallable<T> {
		private Callable<T> callable;

		public CompleteAdapter(Callable<T> callable) {
			this.callable = callable;
		}

		@Override
		protected T create() {
			try {
				return this.callable.call();
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		public boolean isDone() {
			return true;
		}
	}
}
