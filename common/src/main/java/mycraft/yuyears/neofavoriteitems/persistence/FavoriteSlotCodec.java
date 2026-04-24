package mycraft.yuyears.neofavoriteitems.persistence;

public interface FavoriteSlotCodec {
    byte[] serialize();

    void deserialize(byte[] data);
}
