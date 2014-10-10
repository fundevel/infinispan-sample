package player.marshaller;

import java.io.IOException;
import java.util.ArrayList;

import org.infinispan.protostream.MessageMarshaller;

import player.entity.Character;
import player.entity.Player;
import player.entity.Team;

public class PlayerMarshaller implements MessageMarshaller<Player> {

    @Override
    public Class<? extends Player> getJavaClass() {
        return Player.class;
    }

    @Override
    public String getTypeName() {
        return Player.class.getCanonicalName();
    }

    @Override
    public Player readFrom(org.infinispan.protostream.MessageMarshaller.ProtoStreamReader reader) throws IOException {
        Player entity = new Player();
        entity.setCash(reader.readInt("cash"));
        entity.setTicket(reader.readInt("ticket"));
        entity.setPoint(reader.readInt("point"));
        entity.setLevel(reader.readInt("level"));
        entity.setExp(reader.readLong("exp"));
        entity.setLeaderCharacterId(reader.readString("leaderCharacterId"));
        entity.setCharacters(reader.readCollection("characters", new ArrayList<Character>(), Character.class));
        entity.setTeams(reader.readCollection("teams", new ArrayList<Team>(), Team.class));
        return entity;
    }

    @Override
    public void writeTo(org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter writer, Player t) throws IOException {
        writer.writeInt("cash", t.getCash());
        writer.writeInt("ticket", t.getTicket());
        writer.writeInt("point", t.getPoint());
        writer.writeInt("level", t.getLevel());
        writer.writeLong("exp", t.getExp());
        writer.writeString("leaderCharacterId", t.getLeaderCharacterId());
        writer.writeCollection("characters", t.getCharacters(), Character.class);
        writer.writeCollection("teams", t.getTeams(), Team.class);
    }

}
