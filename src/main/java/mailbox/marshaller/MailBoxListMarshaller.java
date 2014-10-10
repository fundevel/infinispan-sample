package mailbox.marshaller;

import java.io.IOException;
import java.util.ArrayList;

import mailbox.entity.MailBoxList;

import org.infinispan.protostream.MessageMarshaller;

public class MailBoxListMarshaller implements MessageMarshaller<MailBoxList> {

    @Override
    public Class<? extends MailBoxList> getJavaClass() {
        return MailBoxList.class;
    }

    @Override
    public String getTypeName() {
        return MailBoxList.class.getCanonicalName();
    }

    @Override
    public MailBoxList readFrom(org.infinispan.protostream.MessageMarshaller.ProtoStreamReader reader) throws IOException {
        MailBoxList entity = new MailBoxList();
        entity.setMailBoxKeys(reader.readCollection("mailBoxKeys", new ArrayList<String>(), String.class));
        return entity;
    }

    @Override
    public void writeTo(org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter writer, MailBoxList t) throws IOException {
        writer.writeCollection("mailBoxKeys", t.getMailBoxKeys(), String.class);
    }

}
