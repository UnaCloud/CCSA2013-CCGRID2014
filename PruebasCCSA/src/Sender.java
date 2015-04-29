import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Sender {
	public static void send() {
		Properties props = new Properties();
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.socketFactory.port", "465");
		props.put("mail.smtp.socketFactory.class",
				"javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", "465");

		Session session = Session.getDefaultInstance(props,
				new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication("unacloudiaas","testqaz123");
			}
		});

		try {

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress("unacloudiaas@gmail.com"));
			message.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse("cesar.forero.gz@gmail.com"));
			message.setSubject("Notificación Urgente de despliegue de pruebas");
			message.setText("Hola César," +
					"\n\n Le informamos que hay un error en el despliegue actual de pruebas."
					+ "\n\n"
					+ "Favor ingrese a Unacloud y libere las maquinas que no arrancaron.");
			Transport.send(message);
			
//			Message message2 = new MimeMessage(session);
//			message2.setFrom(new InternetAddress("unacloudiaas@gmail.com"));
//			message2.setRecipients(Message.RecipientType.TO,
//					InternetAddress.parse("co.diaz@uniandes.edu.co"));
//			message2.setSubject("Notificación Urgente de despliegue de pruebas");
//			message2.setText("Hola César," +
//					"\n\n Le informamos que hay un error en el despliegue actual de pruebas."
//					+ "\n\n"
//					+ "Favor ingrese a Unacloud y libere las maquinas que no arrancaron.");
//			Transport.send(message2);

			System.out.println("Mail Enviado Correctamente");

		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}

}
