package player.marshaller;

import java.io.IOException;

import org.infinispan.protostream.MessageMarshaller;

import player.entity.Character;

public class CharacterMarshaller implements MessageMarshaller<Character> {

    @Override
    public Class<? extends Character> getJavaClass() {
        return Character.class;
    }

    @Override
    public String getTypeName() {
        return Character.class.getCanonicalName();
    }

    @Override
    public Character readFrom(org.infinispan.protostream.MessageMarshaller.ProtoStreamReader reader) throws IOException {
        Character entity = new Character();
        entity.setId(reader.readString("id"));
        entity.setType(reader.readInt("type"));
        entity.setLevel(reader.readInt("level"));
        entity.setExp(reader.readLong("exp"));
        entity.setWeapon(reader.readInt("weapon"));
        entity.setArmor(reader.readInt("armor"));
        entity.setAccessory(reader.readInt("accessory"));
        entity.setForce(reader.readInt("force"));
        return entity;
    }

    @Override
    public void writeTo(org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter writer, Character t) throws IOException {
        writer.writeString("id", t.getId());
        writer.writeInt("type", t.getType());
        writer.writeInt("level", t.getLevel());
        writer.writeLong("exp", t.getExp());
        writer.writeInt("weapon", t.getWeapon());
        writer.writeInt("armor", t.getArmor());
        writer.writeInt("accessory", t.getAccessory());
        writer.writeInt("force", t.getForce());
    }

}
