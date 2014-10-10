package mailbox.marshaller;

import java.io.IOException;
import java.util.ArrayList;

import mailbox.entity.Mail;
import mailbox.entity.MailBox;

import org.infinispan.protostream.MessageMarshaller;

public class MailBoxMarshaller implements MessageMarshaller<MailBox> {

    @Override
    public Class<? extends MailBox> getJavaClass() {
        return MailBox.class;
    }

    @Override
    public String getTypeName() {
        return MailBox.class.getCanonicalName();
    }

    @Override
    public MailBox readFrom(org.infinispan.protostream.MessageMarshaller.ProtoStreamReader reader) throws IOException {
        MailBox entity = new MailBox();
        entity.setMails(reader.readCollection("mails", new ArrayList<Mail>(), Mail.class));
        return entity;
    }

    @Override
    public void writeTo(org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter writer, MailBox t) throws IOException {
        writer.writeCollection("mails", t.getMails(), Mail.class);
    }

}
