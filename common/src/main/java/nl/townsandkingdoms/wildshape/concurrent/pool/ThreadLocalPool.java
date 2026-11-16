package nl.townsandkingdoms.wildshape.concurrent.pool;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicBoolean;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import nl.townsandkingdoms.wildshape.concurrent.Resource;

/**
 * Reuses thread-owned scratch values and their resource handles. Ownership ends
 * on close: do not use a closed handle after another caller has borrowed it.
 */
public class ThreadLocalPool<T> {
    private int size;
    private Supplier<T> factory;
    private Consumer<T> cleaner;
    private ThreadLocal<Pool<T>> local;
    
    public ThreadLocalPool(int size, Supplier<T> factory) {
        this(size, factory, t -> {});
    }
    
    public ThreadLocalPool(int size, Supplier<T> factory, Consumer<T> cleaner) {
        this.size = size;
        this.factory = factory;
        this.cleaner = cleaner;
        this.local = ThreadLocal.withInitial(this::createPool);
    }
    
    public Resource<T> get() {
        return this.local.get().retain();
    }
    
    private Pool<T> createPool() {
        return new Pool<>(this.size, this.factory, this.cleaner);
    }
    
    private static class Pool<T> {
        private int size;
        private Supplier<T> factory;
        private Consumer<T> cleaner;
        private List<Resource<T>> pool;
        private int index;
        private final Thread owner = Thread.currentThread();
        
        private Pool(int size, Supplier<T> factory, Consumer<T> cleaner) {
            this.size = size;
            this.index = size - 1;
            this.factory = factory;
            this.cleaner = cleaner;
            this.pool = new ObjectArrayList<>(size);
            for (int i = 0; i < size; ++i) {
                this.pool.add(new PoolResource<>(factory.get(), this, true));
            }
        }
        
        private Resource<T> retain() {
            if (this.index >= 0) {
                PoolResource<T> value = (PoolResource<T>) this.pool.remove(this.index);
                --this.index;
                value.closed.set(false);
                return value;
            }
            return new PoolResource<>(this.factory.get(), this, false);
        }
        
        private void restore(Resource<T> resource) {
            // A resource may leave a task through a future, but an owner's list
            // must never be mutated by another worker. Cross-thread returns are
            // cleaned and left to GC instead of entering the thread-local pool.
            this.cleaner.accept(resource.get());
            if (Thread.currentThread() == this.owner && this.index + 1 < this.size) {
                this.pool.add(resource);
                ++this.index;
            }
        }
    }
    
    private static class PoolResource<T> implements Resource<T> {
        private T value;
        private Pool<T> pool;
        private final AtomicBoolean closed;

        private PoolResource(T value, Pool<T> pool, boolean closed) {
            this.value = value;
            this.pool = pool;
            this.closed = new AtomicBoolean(closed);
        }
        
        @Override
        public T get() {
            return this.value;
        }
        
        @Override
        public boolean isOpen() {
            return !this.closed.get();
        }

        @Override
        public void close() {
            if (this.closed.compareAndSet(false, true)) this.pool.restore(this);
        }
    }
}
