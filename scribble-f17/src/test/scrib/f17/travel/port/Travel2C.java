package f17.travel.port;

import static demo.fase17.travel.port.Travel2.Travel.Travel.A;
import static demo.fase17.travel.port.Travel2.Travel.Travel.C;
import static demo.fase17.travel.port.Travel2.Travel.Travel.S;
import static demo.fase17.travel.port.Travel2.Travel.Travel.accpt;
import static demo.fase17.travel.port.Travel2.Travel.Travel.confirm;
import static demo.fase17.travel.port.Travel2.Travel.Travel.pay;
import static demo.fase17.travel.port.Travel2.Travel.Travel.query;
import static demo.fase17.travel.port.Travel2.Travel.Travel.quote;
import static demo.fase17.travel.port.Travel2.Travel.Travel.reject;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.scribble.net.Buf;
import org.scribble.net.ObjectStreamFormatter;
import org.scribble.net.session.ExplicitEndpoint;
import org.scribble.net.session.SocketChannelEndpoint;

import demo.fase17.travel.port.Travel2.Travel.Travel;
import demo.fase17.travel.port.Travel2.Travel.channels.C.Travel_C_1;
import demo.fase17.travel.port.Travel2.Travel.channels.C.Travel_C_2;
import demo.fase17.travel.port.Travel2.Travel.roles.C;

import static demo.fase17.travel.port.Travel2.Travel.Travel.*;

public class Travel2C
{
	private static final int MAX = 500;
	private static final List<String> QUERIES = IntStream.range(97, 122).mapToObj((i) -> new Character((char) i).toString()).collect(Collectors.toList());
	
	public static void main(String[] args) throws Exception
	{
		Travel travel = new Travel();
		try (ExplicitEndpoint<Travel, C> se
				= new ExplicitEndpoint<>(travel, C, new ObjectStreamFormatter()))
		{
			run(new Travel_C_1(se).connect(A, SocketChannelEndpoint::new, "localhost", 8888),
					QUERIES);
		}
	}

	private static void run(Travel_C_2 s2, List<String> qs) throws Exception
	{
		if (qs.isEmpty())
		{
			System.out.println("Rejecting: ");
			s2.send(A, reject);
		}	
		else
		{
			System.out.println("Querying: " + qs.get(0));
			Buf<Integer> b = new Buf<>();
			s2 = s2.send(A, query, qs.get(0))
			       .receive(A, quote, b);
			System.out.println("Quoted: " + b.val);
			if (b.val > MAX)
			{
				run(s2, qs.subList(1, qs.size()));
			}
			else
			{
				System.out.println("Accepting: ");
				s2.send(A,accpt)
				.receive(A, port, b)
				.connect(S, SocketChannelEndpoint::new, "localhost", b.val)
					.send(S, pay, "...")
					.receive(S, confirm, b)
					.send(A, ack, b.val);
				System.out.println("Done: " + b.val);
			}
		}
	}	
}
