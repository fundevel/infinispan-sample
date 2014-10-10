package mailbox.marshaller;

import java.io.IOException;

import mailbox.entity.Mail;

import org.infinispan.protostream.MessageMarshaller;

public class MailMarshaller implements MessageMarshaller<Mail> {

    @Override
    public Class<? extends Mail> getJavaClass() {
        return Mail.class;
    }

    @Override
    public String getTypeName() {
        return Mail.class.getCanonicalName();
    }

    @Override
    public Mail readFrom(org.infinispan.protostream.MessageMarshaller.ProtoStreamReader reader) throws IOException {
        Mail entity = new Mail();
        entity.setId(reader.readString("id"));
        entity.setFromChannelUserId(reader.readString("fromChannelUserId"));
        entity.setType(reader.readInt("type"));
        entity.setAttachment(reader.readInt("attachment"));
        entity.setQuantity(reader.readInt("quantity"));
        entity.setSent(reader.readLong("sent"));
        entity.setExpiring(reader.readLong("expiring"));
        return entity;
    }

    @Override
    public void writeTo(org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter writer, Mail t) throws IOException {
        writer.writeString("id", t.getId());
        writer.writeString("fromChannelUserId", t.getFromChannelUserId());
        writer.writeInt("type", t.getType());
        writer.writeInt("attachment", t.getAttachment());
        writer.writeInt("quantity", t.getQuantity());
        writer.writeLong("sent", t.getSent());
        writer.writeLong("expiring", t.getExpiring());
    }
}
