/**
 * 
 */
package player.marshaller;

import java.io.IOException;
import java.util.ArrayList;

import org.infinispan.protostream.MessageMarshaller;

import player.entity.Team;

/**
 * @author seoi
 */
public class TeamMarshaller implements MessageMarshaller<Team> {

    @Override
    public Class<? extends Team> getJavaClass() {
        return Team.class;
    }

    @Override
    public String getTypeName() {
        return Team.class.getCanonicalName();
    }

    @Override
    public Team readFrom(org.infinispan.protostream.MessageMarshaller.ProtoStreamReader reader) throws IOException {
        Team entity = new Team();
        entity.setTeamNo(reader.readInt("teamNo"));
        entity.setCharacterIds(reader.readCollection("characterIds", new ArrayList<String>(), String.class));
        entity.setFormation(reader.readInt("formation"));
        return entity;
    }

    @Override
    public void writeTo(org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter writer, Team t) throws IOException {
        writer.writeInt("teamNo", t.getTeamNo());
        writer.writeCollection("characterIds", t.getCharacterIds(), String.class);
        writer.writeInt("formation", t.getFormation());
    }

}
