package nl.townsandkingdoms.wildshape.concurrent.cache;

public interface SafeCloseable extends AutoCloseable {
    void close();
}
